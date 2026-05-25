"""
Import scraped price lists into the service_offerings table.

Reads scraped_prices.json (output of scrape_prices.py) and upserts
each salon's services into the DB. Existing offerings for a salon are
replaced on each run so re-running is safe.

Usage:
    pip install psycopg2-binary python-dotenv
    python scripts/import_prices.py                          # reads scraped_prices.json
    python scripts/import_prices.py --input my_file.json
    python scripts/import_prices.py --dry-run               # preview without writing
"""

import argparse
import json
import os
import sys
from pathlib import Path

try:
    import psycopg2
    from psycopg2.extras import execute_values
except ImportError:
    print("Missing dependency: pip install psycopg2-binary")
    sys.exit(1)

try:
    from dotenv import load_dotenv
    load_dotenv(Path(__file__).parent.parent / ".env")
except ImportError:
    pass  # python-dotenv optional; fall back to env vars


def get_conn():
    return psycopg2.connect(
        host=os.getenv("POSTGRES_HOST", "localhost"),
        port=int(os.getenv("POSTGRES_PORT", 5432)),
        dbname=os.getenv("POSTGRES_DB", "salon_explorer"),
        user=os.getenv("POSTGRES_USER", "salon_admin"),
        password=os.getenv("POSTGRES_PASSWORD", ""),
    )


def import_prices(data: list[dict], dry_run: bool = False):
    conn = get_conn()
    cur = conn.cursor()

    total_salons = 0
    total_services = 0
    skipped = 0

    for entry in data:
        salon_id = entry.get("id")
        services = entry.get("services", [])

        if not salon_id or not services:
            skipped += 1
            continue

        cur.execute("SELECT id FROM salons WHERE id = %s", (salon_id,))
        if not cur.fetchone():
            print(f"  ⚠ Salon {salon_id} not found in DB, skipping")
            skipped += 1
            continue

        rows = []
        for svc in services:
            name = (svc.get("name") or "").strip()[:255]
            if not name:
                continue
            price = svc.get("price_pln")
            if price is not None:
                try:
                    price = round(float(price), 2)
                    if price <= 0 or price > 50_000:
                        price = None
                except (ValueError, TypeError):
                    price = None
            duration = svc.get("duration_minutes")
            if duration is not None:
                try:
                    duration = int(duration)
                    if duration <= 0 or duration > 600:
                        duration = None
                except (ValueError, TypeError):
                    duration = None
            category = (svc.get("category") or "").strip()[:100] or None

            rows.append((salon_id, name, category, price, duration))

        if not rows:
            skipped += 1
            continue

        if dry_run:
            print(f"  [DRY] Salon {salon_id} '{entry['name']}': {len(rows)} services")
            for r in rows[:3]:
                print(f"    {r[1][:50]} | {r[3]} zł | {r[4]} min")
        else:
            # Replace existing offerings for this salon
            cur.execute("DELETE FROM service_offerings WHERE salon_id = %s", (salon_id,))
            execute_values(
                cur,
                """INSERT INTO service_offerings (salon_id, name, category, price_pln, duration_minutes)
                   VALUES %s""",
                rows,
            )
            print(f"  ✓ Salon {salon_id} '{entry['name']}': {len(rows)} services imported")

        total_salons += 1
        total_services += len(rows)

    if not dry_run:
        conn.commit()
    cur.close()
    conn.close()

    print()
    print("─" * 50)
    print(f"Salons updated : {total_salons}")
    print(f"Services total : {total_services}")
    print(f"Skipped        : {skipped}")
    if dry_run:
        print("(dry-run — nothing was written)")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", default="scraped_prices.json")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    path = Path(args.input)
    if not path.exists():
        print(f"File not found: {path}")
        print("Run scrape_prices.py first.")
        sys.exit(1)

    with open(path, encoding="utf-8") as f:
        data = json.load(f)

    print(f"Importing {len(data)} salons from {path} ...")
    if args.dry_run:
        print("(dry-run mode)\n")

    import_prices(data, dry_run=args.dry_run)


if __name__ == "__main__":
    main()
