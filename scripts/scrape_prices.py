"""
Price list scraper for Warsaw Salon Explorer.

Strategy per salon:
  1. booksy.com URL  → Booksy customer API (structured JSON)
  2. other website   →
       a) scrape homepage (handles #cennik single-page sites)
       b) discover cennik links from homepage navigation
       c) try known /cennik /cennik-zabiegow ... paths
       d) return best result (most services found)

Usage:
    pip install requests beautifulsoup4
    python scrape_prices.py                  # all 408 salons
    python scrape_prices.py --limit 20
    python scrape_prices.py --salon-id 42
    python scrape_prices.py --test           # runs built-in 10-site test
"""

import argparse
import json
import re
import time
from urllib.parse import urlparse, urljoin

import requests
from bs4 import BeautifulSoup

API_BASE = "http://localhost:8080/api/v1"
HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/124.0.0.0 Safari/537.36"
    ),
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
    "Accept-Language": "pl-PL,pl;q=0.9,en-US;q=0.8,en;q=0.7",
    "Accept-Encoding": "gzip, deflate",
    "Connection": "keep-alive",
    "Upgrade-Insecure-Requests": "1",
}

# Price patterns: "80 zł", "80,00 zł", "od 80 zł", "80 PLN", "80,-"
PRICE_RE = re.compile(
    r"(\d[\d\s]*(?:[.,]\d{1,2})?)\s*(?:zł|PLN|zl)\b"
    r"|(\d[\d\s]*(?:[.,]\d{1,2})?)\s*,-",
    re.IGNORECASE,
)

# URL keywords that indicate a price/service page
CENNIK_KEYWORDS = re.compile(
    r"cennik|cen[ai]|price|usług|uslugi|oferta|taryfa|zabieg|koszt",
    re.IGNORECASE,
)

# Fixed paths to always try
CENNIK_PATHS = [
    "/cennik",
    "/cennik/",
    "/cennik-uslug",
    "/cennik-zabiegow",
    "/oferta/cennik",
    "/oferta",
    "/uslugi",
    "/price-list",
    "/cennik-fryzjerski",
    "/cennik-kosmetyczny",
]

# Skip-list: social media and booking platforms that have no scrapeable cennik
SKIP_DOMAINS = {
    "facebook.com", "instagram.com", "twitter.com", "youtube.com",
    "google.com", "maps.google.com", "dikidi.net", "treatwell.com",
    "olicennik.pl",
}


# ── helpers ──────────────────────────────────────────────────────────────────

def should_skip(url: str) -> bool:
    try:
        host = urlparse(url).netloc.lower().lstrip("www.")
        return any(skip in host for skip in SKIP_DOMAINS)
    except Exception:
        return False


def get(url: str, timeout: int = 12) -> requests.Response | None:
    try:
        r = requests.get(url, headers=HEADERS, timeout=timeout, allow_redirects=True)
        return r if r.status_code == 200 else None
    except Exception:
        return None


def parse_price(text: str) -> float | None:
    m = PRICE_RE.search(text)
    if not m:
        return None
    raw = (m.group(1) or m.group(2) or "").replace(" ", "").replace(",", ".")
    try:
        return float(raw)
    except ValueError:
        return None


# ── HTML price extraction ────────────────────────────────────────────────────

def extract_prices(soup: BeautifulSoup) -> list[dict]:
    services = []
    seen: set[str] = set()

    def add(name: str, price_text: str):
        name = name.strip(" –-·|:\t\n")[:120]
        if not name or name in seen or len(name) < 3:
            return
        price = parse_price(price_text)
        if price is not None and (price < 5 or price > 20_000):
            return
        seen.add(name)
        services.append({"name": name, "price_pln": price, "raw": price_text.strip()[:80]})

    def col_texts(col) -> list[str]:
        return [t.strip() for t in col.stripped_strings if t.strip()]

    def is_price_only_col(texts: list[str]) -> bool:
        """Column contains only a price value (e.g. ['60 zł'] or ['60 zł', 'od'])."""
        return len(texts) >= 1 and PRICE_RE.search(texts[0]) and len(texts[0]) < 30

    # 1. Tables
    for table in soup.find_all("table"):
        for row in table.find_all("tr"):
            cells = [c.get_text(" ", strip=True) for c in row.find_all(["td", "th"])]
            if len(cells) < 2:
                continue
            name = cells[0]
            price_cell = cells[-1]
            if PRICE_RE.search(price_cell) or PRICE_RE.search(" ".join(cells)):
                add(name, price_cell)

    if len(services) >= 3:
        return services[:80]

    # 2. Elementor / page-builder column pairing
    # Pattern: adjacent columns where col[N] = name, col[N+1] = price only
    for section in soup.find_all(
        "div",
        class_=re.compile(r"elementor-section|elementor-container|wp-block-columns|e-con"),
    ):
        cols = section.find_all(
            "div",
            class_=re.compile(r"elementor-column|elementor-col-\d+|wp-block-column|e-con"),
            recursive=False,
        )
        # also try one level deeper (inner columns)
        if not cols:
            cols = section.find_all(
                "div",
                class_=re.compile(r"elementor-column|elementor-col-\d+|wp-block-column|e-con"),
            )

        col_text_list = [col_texts(c) for c in cols]
        i = 0
        while i < len(col_text_list) - 1:
            name_texts = col_text_list[i]
            price_texts = col_text_list[i + 1]
            if name_texts and is_price_only_col(price_texts):
                add(name_texts[0], price_texts[0])
                i += 2
            else:
                i += 1

    if len(services) >= 3:
        return services[:80]

    # 3. Definition lists
    for dt in soup.find_all("dt"):
        dd = dt.find_next_sibling("dd")
        if dd:
            add(dt.get_text(" ", strip=True), dd.get_text(" ", strip=True))

    if len(services) >= 3:
        return services[:80]

    # 4. Generic leaf elements with inline name + price
    for tag in soup.find_all(["li", "p", "div", "span", "h1", "h2", "h3", "h4"]):
        if tag.find(["li", "p", "div", "table"]):
            continue
        text = tag.get_text(" ", strip=True)
        m = PRICE_RE.search(text)
        if not m or len(text) < 5 or len(text) > 180:
            continue
        name = text[: m.start()]
        add(name, text[m.start():])

    return services[:80]


# ── Link discovery ───────────────────────────────────────────────────────────

def discover_cennik_links(soup: BeautifulSoup, base_url: str) -> list[str]:
    """Return URLs from nav/footer that look like price pages."""
    candidates = []
    seen: set[str] = set()

    for a in soup.find_all("a", href=True):
        href = a["href"].strip()
        link_text = a.get_text(strip=True)

        # Only follow internal or relative links
        parsed = urlparse(urljoin(base_url, href))
        if parsed.netloc and parsed.netloc != urlparse(base_url).netloc:
            continue

        full_url = urljoin(base_url, href).split("#")[0].rstrip("/")
        if full_url in seen or full_url == base_url.rstrip("/"):
            continue
        seen.add(full_url)

        if CENNIK_KEYWORDS.search(href) or CENNIK_KEYWORDS.search(link_text):
            candidates.append(full_url)

    return candidates[:8]  # don't spider too deep


# ── Booksy ───────────────────────────────────────────────────────────────────

def is_booksy(url: str) -> bool:
    return "booksy.com" in url.lower()


def extract_booksy_id(url: str) -> str | None:
    m = re.search(r"/(?:pl-pl|en-us)/(\d+)_", url)
    if m:
        return m.group(1)

    # subdomain.booksy.com → follow redirect
    parsed = urlparse(url)
    if parsed.netloc != "booksy.com" and "booksy.com" in parsed.netloc:
        r = get(url, timeout=10)
        if r:
            m = re.search(r"/(?:pl-pl|en-us)/(\d+)_", r.url)
            if m:
                return m.group(1)
    return None


def scrape_booksy(url: str) -> list[dict]:
    """
    Scrape Booksy page directly from HTML.
    Service cards are divs that contain the booking button text "Umów" or "Book".
    Each card has: service name (first text), price (zł pattern), duration (Xg Ymin).
    """
    # Follow subdomain redirect to actual Booksy page
    r = get(url, timeout=15)
    if not r:
        return []

    soup = BeautifulSoup(r.text, "html.parser")
    DURATION_RE = re.compile(r"(\d+g\s*\d*min|\d+g|\d+\s*min)", re.I)

    services = []
    seen: set[str] = set()

    # Find all service card divs: contain a booking button ("Umów" in Polish, "Book" in English)
    for card in soup.find_all("div"):
        texts = [t.strip() for t in card.stripped_strings if t.strip()]
        # Skip if no booking button text
        if not any(t in ("Umów", "Book", "Zarezerwuj") for t in texts):
            continue
        # Skip wrappers that contain multiple service cards (too many "Umów")
        if sum(1 for t in texts if t in ("Umów", "Book", "Zarezerwuj")) > 1:
            continue
        # Must have a price
        price_texts = [t for t in texts if PRICE_RE.search(t) and len(t) < 30]
        if not price_texts:
            continue

        # Service name = first text that's not a price, not a duration, not the button
        skip = {"Umów", "Book", "Zarezerwuj"}
        name = ""
        for t in texts:
            if t in skip:
                continue
            if PRICE_RE.search(t) and len(t) < 30:
                continue
            if DURATION_RE.match(t):
                continue
            name = t.strip()
            break

        if not name or name in seen or len(name) < 3:
            continue

        duration_min = None
        for t in texts:
            dm = DURATION_RE.match(t)
            if dm:
                raw = t.replace(" ", "")
                hours = re.search(r"(\d+)g", raw)
                mins = re.search(r"(\d+)min", raw)
                duration_min = (int(hours.group(1)) * 60 if hours else 0) + (int(mins.group(1)) if mins else 0)
                break

        seen.add(name)
        services.append({
            "name": name,
            "price_pln": parse_price(price_texts[0]),
            "duration_minutes": duration_min or None,
            "category": None,
        })

    return services


# ── Main scrape logic ────────────────────────────────────────────────────────

def scrape_website(base_url: str) -> tuple[list[dict], str | None]:
    """
    Try multiple strategies and return the result with the most services.
    """
    base_url = base_url.rstrip("/")
    best: list[dict] = []
    best_url: str | None = None

    def update_best(services, url):
        nonlocal best, best_url
        if len(services) > len(best):
            best, best_url = services, url

    # --- Strategy A: scrape homepage (handles #cennik single-page sites) ---
    r = get(base_url)
    if not r:
        return [], None
    homepage_soup = BeautifulSoup(r.text, "html.parser")
    hp_services = extract_prices(homepage_soup)
    update_best(hp_services, base_url)

    # --- Strategy B: discover cennik links from homepage nav ---
    discovered = discover_cennik_links(homepage_soup, base_url)
    for url in discovered:
        r2 = get(url)
        if r2:
            s = extract_prices(BeautifulSoup(r2.text, "html.parser"))
            update_best(s, url)

    # --- Strategy C: try known fixed paths ---
    for path in CENNIK_PATHS:
        url = base_url + path
        if url.rstrip("/") in (d.rstrip("/") for d in discovered):
            continue  # already tried
        r3 = get(url)
        if not r3:
            continue
        # Ignore if redirected back to homepage
        if r3.url.rstrip("/") == base_url:
            continue
        s = extract_prices(BeautifulSoup(r3.text, "html.parser"))
        update_best(s, r3.url)

    return best, best_url


# ── API helpers ──────────────────────────────────────────────────────────────

def get_all_salon_ids() -> list[int]:
    ids = []
    page = 0
    while True:
        r = requests.get(f"{API_BASE}/salons", params={"page": page, "size": 100}, timeout=10)
        r.raise_for_status()
        data = r.json()
        ids.extend(s["id"] for s in data["content"])
        if data["page"]["number"] >= data["page"]["totalPages"] - 1:
            break
        page += 1
    return ids


def get_salon_detail(salon_id: int) -> dict:
    r = requests.get(f"{API_BASE}/salons/{salon_id}", timeout=10)
    r.raise_for_status()
    return r.json()


# ── Process one salon ────────────────────────────────────────────────────────

def process_salon(salon: dict, verbose: bool = True) -> dict | None:
    website = salon.get("website")
    if not website:
        if verbose:
            print(f"  [{salon['id']}] {salon['name'][:50]} — no website")
        return None

    if should_skip(website):
        if verbose:
            print(f"  [{salon['id']}] {salon['name'][:50]} — skipped ({urlparse(website).netloc})")
        return None

    if verbose:
        print(f"  [{salon['id']}] {salon['name'][:50]}")
        print(f"       {website}")

    try:
        if is_booksy(website):
            services = scrape_booksy(website)
            source = "booksy_api"
            source_url = website
        else:
            services, source_url = scrape_website(website)
            source = "html_scrape"

        if services:
            if verbose:
                print(f"       ✓ {len(services)} services  ({source_url})")
            return {
                "id": salon["id"],
                "name": salon["name"],
                "website": website,
                "source": source,
                "source_url": source_url,
                "services": services,
            }
        else:
            if verbose:
                print("       — no prices found")
            return None

    except Exception as e:
        if verbose:
            print(f"       ✗ error: {e}")
        return None


# ── Built-in 10-site test ────────────────────────────────────────────────────

TEST_SITES = [
    {"id": 0, "name": "BS Nails Spa Wola",        "website": "http://bsnails-spa.pl/"},
    {"id": 0, "name": "Spa Sungate",              "website": "https://www.spasungate.pl/"},
    {"id": 0, "name": "Studio Sante",             "website": "http://www.studiosante.pl/"},
    {"id": 0, "name": "Thai Bali Spa",            "website": "https://thaibalispa.pl/"},
    {"id": 0, "name": "Time for Wax & Laser",     "website": "http://timeforwax.pl/"},
    {"id": 0, "name": "Cudna rzęsy",              "website": "https://www.przedluzanierzes-warszawa.pl/"},
    {"id": 0, "name": "Creative Hair (no prices)","website": "http://www.creativehair.pl/"},
    {"id": 0, "name": "Salon Elegante",           "website": "http://www.salonurodyelegante.pl/"},
    {"id": 0, "name": "Viki Beauty (Booksy)",     "website": "http://vikibeautyspacewarszawa.booksy.com/a/"},
    {"id": 0, "name": "Fryzjer Booksy test",      "website": "http://strefafryzurbitwywarszawskiej23.booksy.com/a/"},
]


def run_test():
    print("=" * 55)
    print("10-SITE TEST")
    print("=" * 55)
    found = 0
    for site in TEST_SITES:
        print()
        result = process_salon(site, verbose=True)
        if result:
            found += 1
            print(f"       Sample: {result['services'][0]}")
        time.sleep(0.5)
    print()
    print(f"Result: {found}/{len(TEST_SITES)} sites had prices")


# ── Entry point ──────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--limit",    type=int,  default=None)
    parser.add_argument("--salon-id", type=int,  default=None)
    parser.add_argument("--output",   default="scraped_prices.json")
    parser.add_argument("--delay",    type=float, default=0.8)
    parser.add_argument("--test",     action="store_true", help="Run 10-site test")
    args = parser.parse_args()

    if args.test:
        run_test()
        return

    if args.salon_id:
        salon = get_salon_detail(args.salon_id)
        result = process_salon(salon)
        results = [result] if result else []
    else:
        print("Fetching salon list from API...")
        ids = get_all_salon_ids()
        print(f"Total salons: {len(ids)}\n")
        if args.limit:
            ids = ids[: args.limit]

        results = []
        for i, salon_id in enumerate(ids, 1):
            print(f"[{i}/{len(ids)}]", end=" ")
            try:
                salon = get_salon_detail(salon_id)
            except Exception as e:
                print(f"  skip — API error: {e}")
                continue
            result = process_salon(salon)
            if result:
                results.append(result)
            time.sleep(args.delay)

    with open(args.output, "w", encoding="utf-8") as f:
        json.dump(results, f, ensure_ascii=False, indent=2)

    print(f"\n{'─'*50}")
    print(f"Done. Prices found for {len(results)} salons.")
    print(f"Saved to {args.output}")


if __name__ == "__main__":
    main()
