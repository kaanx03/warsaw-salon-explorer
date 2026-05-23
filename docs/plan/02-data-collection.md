# Phase 2: Data Collection (Google Places API → Database)

> **Hedef:** Google Places API'den Varşova'daki en az 100 beauty/hair salonu çekip, veriyi normalize edip, deduplikasyon yaparak Faz 1'de hazırladığımız tablolara doldurmak. Tüm bunlar production-grade bir ingestion pipeline olarak çalışacak.

---

## 📋 İçindekiler

1. [Faz Özeti](#1-faz-özeti)
2. [Google Places API Stratejisi](#2-google-places-api-stratejisi)
3. [API Setup & Authentication](#3-api-setup--authentication)
4. [Klasör Yapısı](#4-klasör-yapısı)
5. [Domain Entity'leri](#5-domain-entityleri)
6. [HTTP Client & Konfigürasyon](#6-http-client--konfigürasyon)
7. [DTO'lar (Google Response Modelleri)](#7-dtolar-google-response-modelleri)
8. [Ingestion Service](#8-ingestion-service)
9. [Veri Normalizasyonu](#9-veri-normalizasyonu)
10. [District Resolver](#10-district-resolver)
11. [Repository Layer](#11-repository-layer)
12. [Ingestion Trigger (CLI Komut)](#12-ingestion-trigger-cli-komut)
13. [Resilience & Error Handling](#13-resilience--error-handling)
14. [Logging & Observability](#14-logging--observability)
15. [Doğrulama & Test](#15-doğrulama--test)
16. [Mülakat Soruları](#16-mülakat-soruları)
17. [Definition of Done](#17-definition-of-done)

---

## 1. Faz Özeti

**Bu fazın sonunda elimizde olacaklar:**

- Google Places API'ye bağlanan, retry/rate-limit aware bir HTTP client
- Spring Boot içinde modüler bir `ingestion` paketi
- `GooglePlacesClient` (raw API çağrıları)
- `IngestionService` (orchestrasyon)
- `SalonNormalizer` (phone format, data cleanup)
- `DistrictResolver` (lat/lng → Varşova district eşleme)
- `--spring.profiles.active=ingest` ile çalışan CLI komutu
- DB'de en az 100 deduplikate edilmiş salon kaydı
- Audit logging + ingestion metrics

**Bu fazda YAPMAYACAĞIZ:**

- REST API endpoint'leri (Faz 3)
- Frontend (Faz 4)
- Otomatik schedule (cron) — ileride eklenir

---

## 2. Google Places API Stratejisi

### 2.1 Hangi API'ler kullanılacak?

Google'ın **iki versiyonu var**: legacy ve **Places API (New)**. Biz **yeni versiyonu** kullanacağız çünkü:

- Daha hızlı response
- Daha esnek field masking (sadece istediğin alanlar geliyor → daha ucuz)
- Yeni feature'lar bu API'ye ekleniyor

| Endpoint | Amaç | URL |
|----------|------|-----|
| **Text Search (New)** | "beauty salon Warsaw" gibi sorgu | `POST https://places.googleapis.com/v1/places:searchText` |
| **Place Details (New)** | Tek bir place için zenginleştirme | `GET https://places.googleapis.com/v1/places/{placeId}` |
| **Nearby Search (New)** | Lat/lng + radius ile arama | `POST https://places.googleapis.com/v1/places:searchNearby` |

### 2.2 Sorgu stratejisi

Tek bir "beauty salon Warsaw" sorgusu ~20 sonuç döner. 100+ için **paralel sorgular** atacağız:

**Yöntem A: Kategori bazlı (basit, MVP için)**

```
"beauty salon Warsaw"
"hair salon Warsaw"
"barber Warsaw"
"nail salon Warsaw"
"spa Warsaw"
```

Her biri ~20 sonuç → ~100 unique (deduplikasyon ile).

**Yöntem B: District bazlı (daha kapsamlı)**

```
"beauty salon Mokotów Warsaw"
"beauty salon Śródmieście Warsaw"
"beauty salon Wola Warsaw"
... (18 district × 2-3 kategori)
```

Yöntem A ile başla, az gelirse B'ye geç.

### 2.3 Dil parametresi

`languageCode=pl` kullan. Salon isimleri Polonyaca daha doğru gelir, adres formatı yerel standartta olur.

### 2.4 Field masking (maliyet kontrolü)

Places API (New)'da **sadece istediğin field'lar için para ödersin**. Header'da belirtilir:

```
X-Goog-FieldMask: places.id,places.displayName,places.formattedAddress,places.rating,places.userRatingCount,places.priceLevel,places.location,places.internationalPhoneNumber,places.websiteUri,places.photos
```

> **Mülakatta savunma:** "Field masking ile request başına maliyet 30%+ düşürülebilir. Production'da kritik."

---

## 3. API Setup & Authentication

### 3.1 Google Cloud Console adımları

1. https://console.cloud.google.com → yeni proje aç (`warsaw-salon-explorer`)
2. **Billing** → kredi kartı bağla (zorunlu, ama $200/ay free tier var, bu task için fazlasıyla yeter)
3. **APIs & Services → Library** → şunları enable et:
   - **Places API (New)**
   - **Geocoding API** (district resolve için)
4. **APIs & Services → Credentials → Create credentials → API key**
5. API key'i sınırla:
   - **Application restrictions:** IP addresses → kendi IP'n (development)
   - **API restrictions:** sadece yukarıdaki iki API
6. Key'i kopyala → `.env` dosyasına ekle (asla commit etme!)

### 3.2 `.env` güncellemesi

```bash
# .env (gitignore'da)
GOOGLE_MAPS_API_KEY=AIzaSy...your_real_key_here
GOOGLE_PLACES_LANGUAGE=pl
GOOGLE_PLACES_REGION=pl
INGESTION_BATCH_SIZE=20
INGESTION_RATE_LIMIT_PER_SECOND=10
```

### 3.3 `.env.example` güncellemesi

```bash
# .env.example (git'te)
GOOGLE_MAPS_API_KEY=your_api_key_here
GOOGLE_PLACES_LANGUAGE=pl
GOOGLE_PLACES_REGION=pl
INGESTION_BATCH_SIZE=20
INGESTION_RATE_LIMIT_PER_SECOND=10
```

### 3.4 Quota & maliyet farkındalığı

| API çağrısı | Yaklaşık maliyet | Bizim kullanımımız |
|-------------|------------------|---------------------|
| Text Search | $32 / 1000 call | ~10 call (5 kategori × 2 sayfa) |
| Place Details (Basic) | $17 / 1000 call | ~150 call |
| Geocoding | $5 / 1000 call | ~150 call |

**Toplam tahmin:** ~$3-4 — $200 free tier içinde sorun değil.

---

## 4. Klasör Yapısı

Faz 2 sonunda backend şöyle olacak:

```
backend/src/main/java/com/kaandev/salonexplorer/
├── SalonExplorerApplication.java
├── config/
│   ├── RestClientConfig.java
│   ├── GooglePlacesProperties.java
│   └── IngestionProperties.java
├── domain/
│   ├── entity/
│   │   ├── Salon.java
│   │   ├── District.java
│   │   ├── Service.java
│   │   └── BaseEntity.java
│   └── enums/
│       └── ServiceCategory.java
├── repository/
│   ├── SalonRepository.java
│   ├── DistrictRepository.java
│   └── ServiceRepository.java
└── ingestion/
    ├── client/
    │   ├── GooglePlacesClient.java
    │   └── dto/
    │       ├── PlacesSearchRequest.java
    │       ├── PlacesSearchResponse.java
    │       ├── PlaceDetailsResponse.java
    │       └── PlaceDto.java
    ├── normalizer/
    │   ├── SalonNormalizer.java
    │   ├── PhoneNormalizer.java
    │   └── DistrictResolver.java
    ├── service/
    │   ├── IngestionService.java
    │   └── IngestionResult.java
    └── runner/
        └── IngestionRunner.java
```

---

## 5. Domain Entity'leri

### 5.1 `BaseEntity.java` — ortak alanlar

```java
package com.kaandev.salonexplorer.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
```

> Ana uygulama sınıfına `@EnableJpaAuditing` eklemeyi unutma.

### 5.2 `District.java`

```java
package com.kaandev.salonexplorer.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "districts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class District {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
```

### 5.3 `ServiceCategory.java` enum

```java
package com.kaandev.salonexplorer.domain.enums;

public enum ServiceCategory {
    HAIR, NAILS, FACE, BODY, OTHER;

    public String dbValue() {
        return name().toLowerCase();
    }
}
```

### 5.4 `Service.java`

```java
package com.kaandev.salonexplorer.domain.entity;

import com.kaandev.salonexplorer.domain.enums.ServiceCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ServiceCategory category;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
```

### 5.5 `Salon.java` — ana entity

```java
package com.kaandev.salonexplorer.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "salons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Salon extends BaseEntity {

    @Column(name = "google_place_id", nullable = false, unique = true, length = 255)
    private String googlePlaceId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id")
    private District district;

    @Column(length = 50)
    private String phone;

    @Column(length = 500)
    private String website;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "price_level")
    private Short priceLevel;

    @Column(name = "photo_url", columnDefinition = "TEXT")
    private String photoUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "salon_services",
        joinColumns = @JoinColumn(name = "salon_id"),
        inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    @Builder.Default
    private Set<Service> services = new HashSet<>();
}
```

### 5.6 `@EnableJpaAuditing` — main class güncelle

```java
package com.kaandev.salonexplorer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SalonExplorerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SalonExplorerApplication.class, args);
    }
}
```

---

## 6. HTTP Client & Konfigürasyon

### 6.1 `GooglePlacesProperties.java`

```java
package com.kaandev.salonexplorer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google.places")
public record GooglePlacesProperties(
    String apiKey,
    String baseUrl,
    String languageCode,
    String regionCode,
    int connectTimeoutMs,
    int readTimeoutMs
) {
}
```

### 6.2 `IngestionProperties.java`

```java
package com.kaandev.salonexplorer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "ingestion")
public record IngestionProperties(
    int batchSize,
    int rateLimitPerSecond,
    int maxRetries,
    long retryBackoffMs,
    List<String> searchQueries
) {
}
```

### 6.3 `application-local.yml` güncellemesi

```yaml
# ... önceki içerik

google:
  places:
    api-key: ${GOOGLE_MAPS_API_KEY}
    base-url: https://places.googleapis.com/v1
    language-code: ${GOOGLE_PLACES_LANGUAGE:pl}
    region-code: ${GOOGLE_PLACES_REGION:pl}
    connect-timeout-ms: 5000
    read-timeout-ms: 15000

ingestion:
  batch-size: ${INGESTION_BATCH_SIZE:20}
  rate-limit-per-second: ${INGESTION_RATE_LIMIT_PER_SECOND:10}
  max-retries: 3
  retry-backoff-ms: 1000
  search-queries:
    - "beauty salon Warsaw"
    - "hair salon Warsaw"
    - "barber Warsaw"
    - "nail salon Warsaw"
    - "spa Warsaw"
```

### 6.4 `RestClientConfig.java`

```java
package com.kaandev.salonexplorer.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties({ GooglePlacesProperties.class, IngestionProperties.class })
public class RestClientConfig {

    @Bean
    public RestClient googlePlacesRestClient(GooglePlacesProperties props) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(props.connectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(props.readTimeoutMs()));

        return RestClient.builder()
            .baseUrl(props.baseUrl())
            .requestFactory(factory)
            .defaultHeader("X-Goog-Api-Key", props.apiKey())
            .defaultHeader("Content-Type", "application/json")
            .build();
    }
}
```

> **Neden `RestClient`?** Spring 6.1+ ile gelen modern synchronous HTTP client. `RestTemplate`'in halefi, `WebClient`'tan daha basit (reactive gerekmiyor bizim için).

### 6.5 `pom.xml`'a eklenecek dependency'ler

```xml
<!-- Configuration properties processor (IDE autocompletion için) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>

<!-- Phone number normalization -->
<dependency>
    <groupId>com.googlecode.libphonenumber</groupId>
    <artifactId>libphonenumber</artifactId>
    <version>8.13.50</version>
</dependency>

<!-- Resilience4j (retry, rate limiter) -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.2.0</version>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-reactor</artifactId>
    <version>2.2.0</version>
</dependency>
```

---

## 7. DTO'lar (Google Response Modelleri)

Google API'nin döndüğü JSON'u Java record'larla map'liyoruz.

### 7.1 `PlacesSearchRequest.java`

```java
package com.kaandev.salonexplorer.ingestion.client.dto;

public record PlacesSearchRequest(
    String textQuery,
    String languageCode,
    String regionCode,
    Integer pageSize,
    String pageToken
) {
    public static PlacesSearchRequest of(String query, String lang, String region) {
        return new PlacesSearchRequest(query, lang, region, 20, null);
    }
}
```

### 7.2 `PlaceDto.java`

```java
package com.kaandev.salonexplorer.ingestion.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlaceDto(
    String id,
    DisplayName displayName,
    String formattedAddress,
    Location location,
    Double rating,
    @JsonProperty("userRatingCount") Integer userRatingCount,
    @JsonProperty("priceLevel") String priceLevel,
    @JsonProperty("internationalPhoneNumber") String internationalPhoneNumber,
    @JsonProperty("websiteUri") String websiteUri,
    List<Photo> photos,
    List<AddressComponent> addressComponents
) {
    public record DisplayName(String text, String languageCode) {}
    public record Location(Double latitude, Double longitude) {}
    public record Photo(String name, Integer widthPx, Integer heightPx) {}
    public record AddressComponent(
        String longText,
        String shortText,
        List<String> types
    ) {}
}
```

### 7.3 `PlacesSearchResponse.java`

```java
package com.kaandev.salonexplorer.ingestion.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlacesSearchResponse(
    List<PlaceDto> places,
    String nextPageToken
) {
    public List<PlaceDto> placesOrEmpty() {
        return places != null ? places : List.of();
    }
}
```

---

## 8. Ingestion Service

### 8.1 `GooglePlacesClient.java`

```java
package com.kaandev.salonexplorer.ingestion.client;

import com.kaandev.salonexplorer.config.GooglePlacesProperties;
import com.kaandev.salonexplorer.ingestion.client.dto.PlaceDto;
import com.kaandev.salonexplorer.ingestion.client.dto.PlacesSearchRequest;
import com.kaandev.salonexplorer.ingestion.client.dto.PlacesSearchResponse;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GooglePlacesClient {

    private static final String SEARCH_FIELD_MASK =
        "places.id,places.displayName,places.formattedAddress,places.location," +
        "places.rating,places.userRatingCount,places.priceLevel," +
        "places.internationalPhoneNumber,places.websiteUri,places.photos," +
        "places.addressComponents,nextPageToken";

    private static final String DETAILS_FIELD_MASK =
        "id,displayName,formattedAddress,location,rating,userRatingCount," +
        "priceLevel,internationalPhoneNumber,websiteUri,photos,addressComponents";

    private final RestClient googlePlacesRestClient;
    private final GooglePlacesProperties props;

    @Retry(name = "googlePlacesApi")
    public PlacesSearchResponse searchText(String query) {
        log.info("Calling Places Text Search: query='{}', lang={}", query, props.languageCode());

        var request = PlacesSearchRequest.of(query, props.languageCode(), props.regionCode());

        return googlePlacesRestClient.post()
            .uri("/places:searchText")
            .header("X-Goog-FieldMask", SEARCH_FIELD_MASK)
            .body(request)
            .retrieve()
            .body(PlacesSearchResponse.class);
    }

    @Retry(name = "googlePlacesApi")
    public PlaceDto getDetails(String placeId) {
        log.debug("Fetching details for placeId={}", placeId);

        return googlePlacesRestClient.get()
            .uri("/places/{id}?languageCode={lang}", placeId, props.languageCode())
            .header("X-Goog-FieldMask", DETAILS_FIELD_MASK)
            .retrieve()
            .body(PlaceDto.class);
    }

    public List<PlaceDto> searchAllPages(String query) {
        // İlk versiyon: sadece ilk sayfa. Sonra pagination eklenebilir.
        var response = searchText(query);
        return response.placesOrEmpty();
    }
}
```

### 8.2 `IngestionResult.java`

```java
package com.kaandev.salonexplorer.ingestion.service;

public record IngestionResult(
    int totalFetched,
    int inserted,
    int updated,
    int skipped,
    int failed,
    long durationMs
) {
    public static IngestionResult empty() {
        return new IngestionResult(0, 0, 0, 0, 0, 0);
    }

    public IngestionResult merge(IngestionResult other) {
        return new IngestionResult(
            this.totalFetched + other.totalFetched,
            this.inserted + other.inserted,
            this.updated + other.updated,
            this.skipped + other.skipped,
            this.failed + other.failed,
            this.durationMs + other.durationMs
        );
    }
}
```

### 8.3 `IngestionService.java`

```java
package com.kaandev.salonexplorer.ingestion.service;

import com.kaandev.salonexplorer.config.IngestionProperties;
import com.kaandev.salonexplorer.domain.entity.Salon;
import com.kaandev.salonexplorer.ingestion.client.GooglePlacesClient;
import com.kaandev.salonexplorer.ingestion.client.dto.PlaceDto;
import com.kaandev.salonexplorer.ingestion.normalizer.SalonNormalizer;
import com.kaandev.salonexplorer.repository.SalonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private final GooglePlacesClient placesClient;
    private final SalonRepository salonRepository;
    private final SalonNormalizer normalizer;
    private final IngestionProperties props;

    public IngestionResult ingestAll() {
        log.info("==> Starting ingestion. Queries: {}", props.searchQueries());
        var start = Instant.now();
        var result = IngestionResult.empty();

        for (String query : props.searchQueries()) {
            try {
                var queryResult = ingestQuery(query);
                result = result.merge(queryResult);
                log.info("Query '{}' done: {}", query, queryResult);
            } catch (Exception e) {
                log.error("Query '{}' failed entirely: {}", query, e.getMessage(), e);
            }
        }

        var duration = Duration.between(start, Instant.now()).toMillis();
        log.info("==> Ingestion complete in {}ms: {}", duration, result);
        return result;
    }

    private IngestionResult ingestQuery(String query) {
        var start = Instant.now();
        int fetched = 0, inserted = 0, updated = 0, skipped = 0, failed = 0;

        List<PlaceDto> places = placesClient.searchAllPages(query);
        fetched = places.size();

        for (PlaceDto place : places) {
            try {
                var outcome = upsertSalon(place);
                switch (outcome) {
                    case INSERTED -> inserted++;
                    case UPDATED  -> updated++;
                    case SKIPPED  -> skipped++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("Failed to ingest placeId={}: {}", place.id(), e.getMessage());
            }
        }

        var duration = Duration.between(start, Instant.now()).toMillis();
        return new IngestionResult(fetched, inserted, updated, skipped, failed, duration);
    }

    @Transactional
    protected UpsertOutcome upsertSalon(PlaceDto place) {
        // Required field validation
        if (place.id() == null || place.displayName() == null || place.formattedAddress() == null) {
            log.debug("Skipping place with missing required fields: {}", place.id());
            return UpsertOutcome.SKIPPED;
        }

        Optional<Salon> existing = salonRepository.findByGooglePlaceId(place.id());

        if (existing.isPresent()) {
            Salon salon = existing.get();
            normalizer.applyUpdates(salon, place);
            salonRepository.save(salon);
            return UpsertOutcome.UPDATED;
        } else {
            Salon salon = normalizer.toEntity(place);
            salonRepository.save(salon);
            return UpsertOutcome.INSERTED;
        }
    }

    enum UpsertOutcome { INSERTED, UPDATED, SKIPPED }
}
```

---

## 9. Veri Normalizasyonu

### 9.1 `PhoneNormalizer.java`

```java
package com.kaandev.salonexplorer.ingestion.normalizer;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PhoneNormalizer {

    private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

    /**
     * Normalize phone number to E.164 format (+48 22 123 45 67 → +48221234567).
     * Returns null if input is invalid or empty.
     */
    public String normalize(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return null;
        }
        try {
            var number = phoneUtil.parse(rawPhone, "PL");
            if (!phoneUtil.isValidNumber(number)) {
                log.debug("Invalid phone number: {}", rawPhone);
                return null;
            }
            return phoneUtil.format(number, PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            log.debug("Phone parse failed: {} → {}", rawPhone, e.getMessage());
            return null;
        }
    }
}
```

### 9.2 `SalonNormalizer.java`

```java
package com.kaandev.salonexplorer.ingestion.normalizer;

import com.kaandev.salonexplorer.domain.entity.Salon;
import com.kaandev.salonexplorer.ingestion.client.dto.PlaceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SalonNormalizer {

    private final PhoneNormalizer phoneNormalizer;
    private final DistrictResolver districtResolver;

    private static final Map<String, Short> PRICE_LEVEL_MAP = Map.of(
        "PRICE_LEVEL_INEXPENSIVE",  (short) 1,
        "PRICE_LEVEL_MODERATE",     (short) 2,
        "PRICE_LEVEL_EXPENSIVE",    (short) 3,
        "PRICE_LEVEL_VERY_EXPENSIVE", (short) 4
    );

    private static final String PHOTO_URL_TEMPLATE =
        "https://places.googleapis.com/v1/%s/media?maxWidthPx=800&key=%s";

    public Salon toEntity(PlaceDto place) {
        var salon = Salon.builder()
            .googlePlaceId(place.id())
            .name(place.displayName().text())
            .address(place.formattedAddress())
            .phone(phoneNormalizer.normalize(place.internationalPhoneNumber()))
            .website(truncate(place.websiteUri(), 500))
            .latitude(toBigDecimal(place.location() != null ? place.location().latitude() : null, 7))
            .longitude(toBigDecimal(place.location() != null ? place.location().longitude() : null, 7))
            .rating(toBigDecimal(place.rating(), 1))
            .reviewCount(place.userRatingCount() != null ? place.userRatingCount() : 0)
            .priceLevel(mapPriceLevel(place.priceLevel()))
            .photoUrl(extractFirstPhotoUrl(place))
            .isActive(true)
            .build();

        salon.setDistrict(districtResolver.resolve(place));
        return salon;
    }

    public void applyUpdates(Salon existing, PlaceDto place) {
        existing.setName(place.displayName().text());
        existing.setAddress(place.formattedAddress());
        existing.setPhone(phoneNormalizer.normalize(place.internationalPhoneNumber()));
        existing.setWebsite(truncate(place.websiteUri(), 500));
        existing.setRating(toBigDecimal(place.rating(), 1));
        existing.setReviewCount(place.userRatingCount() != null ? place.userRatingCount() : 0);
        existing.setPriceLevel(mapPriceLevel(place.priceLevel()));
        existing.setPhotoUrl(extractFirstPhotoUrl(place));
        existing.setDistrict(districtResolver.resolve(place));
    }

    private Short mapPriceLevel(String googleLevel) {
        if (googleLevel == null) return null;
        return PRICE_LEVEL_MAP.get(googleLevel);
    }

    private BigDecimal toBigDecimal(Double value, int scale) {
        if (value == null) return null;
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private String extractFirstPhotoUrl(PlaceDto place) {
        if (place.photos() == null || place.photos().isEmpty()) return null;
        // Google'ın photo name'i: "places/XXX/photos/YYY"
        // Bunu media endpoint URL'ine çeviriyoruz
        // NOT: Production'da API key URL'de görünmesin diye proxy endpoint yapılır
        return place.photos().get(0).name();  // sadece reference'ı saklıyoruz
    }
}
```

> **Önemli:** Photo URL içinde API key olmamalı. Reference saklıyoruz, frontend'e gönderirken backend proxy endpoint kullanır (Faz 3'te).

---

## 10. District Resolver

Google'ın `addressComponents`'i içinde district bilgisi var. Bunu kullanıp DB'deki `districts` tablosuna eşliyoruz.

### 10.1 `DistrictResolver.java`

```java
package com.kaandev.salonexplorer.ingestion.normalizer;

import com.kaandev.salonexplorer.domain.entity.District;
import com.kaandev.salonexplorer.ingestion.client.dto.PlaceDto;
import com.kaandev.salonexplorer.repository.DistrictRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class DistrictResolver {

    private static final List<String> DISTRICT_TYPE_PRIORITIES = List.of(
        "sublocality_level_1",
        "administrative_area_level_3",
        "locality"
    );

    private final DistrictRepository districtRepository;
    private final Map<String, District> cache = new ConcurrentHashMap<>();

    /**
     * Resolves a district from Google's addressComponents.
     * Returns null if no district could be matched.
     */
    public District resolve(PlaceDto place) {
        if (place.addressComponents() == null) return null;

        for (String typePriority : DISTRICT_TYPE_PRIORITIES) {
            for (var component : place.addressComponents()) {
                if (component.types() == null) continue;
                if (component.types().contains(typePriority)) {
                    District d = lookup(component.longText());
                    if (d != null) return d;
                    d = lookup(component.shortText());
                    if (d != null) return d;
                }
            }
        }
        log.debug("No district resolved for place: {}", place.id());
        return null;
    }

    private District lookup(String rawName) {
        if (rawName == null || rawName.isBlank()) return null;
        String slug = toSlug(rawName);

        return cache.computeIfAbsent(slug, this::queryDb);
    }

    private District queryDb(String slug) {
        Optional<District> result = districtRepository.findBySlug(slug);
        return result.orElse(null);
    }

    /**
     * "Praga-Południe" → "praga-poludnie"
     * "Śródmieście"   → "srodmiescie"
     */
    private String toSlug(String name) {
        String lower = name.toLowerCase().trim();
        // Polish-specific replacements first
        lower = lower
            .replace("ł", "l").replace("ą", "a").replace("ę", "e")
            .replace("ó", "o").replace("ś", "s").replace("ć", "c")
            .replace("ź", "z").replace("ż", "z").replace("ń", "n");
        // Strip remaining diacritics
        return Normalizer.normalize(lower, Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
            .replaceAll("[^a-z0-9-]", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
    }
}
```

---

## 11. Repository Layer

### 11.1 `SalonRepository.java`

```java
package com.kaandev.salonexplorer.repository;

import com.kaandev.salonexplorer.domain.entity.Salon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalonRepository extends JpaRepository<Salon, Long> {

    Optional<Salon> findByGooglePlaceId(String googlePlaceId);

    boolean existsByGooglePlaceId(String googlePlaceId);

    long countByIsActiveTrue();
}
```

### 11.2 `DistrictRepository.java`

```java
package com.kaandev.salonexplorer.repository;

import com.kaandev.salonexplorer.domain.entity.District;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DistrictRepository extends JpaRepository<District, Long> {
    Optional<District> findBySlug(String slug);
}
```

### 11.3 `ServiceRepository.java`

```java
package com.kaandev.salonexplorer.repository;

import com.kaandev.salonexplorer.domain.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceRepository extends JpaRepository<Service, Long> {
    Optional<Service> findByName(String name);
}
```

---

## 12. Ingestion Trigger (CLI Komut)

Ingestion'ı **sadece** belirli bir profile aktif olduğunda çalıştırıyoruz — yoksa normal startup'ta da çalışır, istemiyoruz.

### 12.1 `IngestionRunner.java`

```java
package com.kaandev.salonexplorer.ingestion.runner;

import com.kaandev.salonexplorer.ingestion.service.IngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("ingest")
@RequiredArgsConstructor
public class IngestionRunner implements ApplicationRunner {

    private final IngestionService ingestionService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("########################################");
        log.info("# Ingestion profile activated          #");
        log.info("########################################");

        var result = ingestionService.ingestAll();

        log.info("########################################");
        log.info("# Final report:                        #");
        log.info("# Total fetched: {}", result.totalFetched());
        log.info("# Inserted:      {}", result.inserted());
        log.info("# Updated:       {}", result.updated());
        log.info("# Skipped:       {}", result.skipped());
        log.info("# Failed:        {}", result.failed());
        log.info("# Duration:      {}ms", result.durationMs());
        log.info("########################################");
    }
}
```

### 12.2 Çalıştırma komutu

```bash
# DB ayakta olmalı
docker compose up -d postgres

# Ingestion'ı tetikle
cd backend
SPRING_PROFILES_ACTIVE=local,ingest mvn spring-boot:run
```

> **`local,ingest` kombosu:** `local` profili DB bağlantısı için, `ingest` profili runner'ı aktive etmek için.

---

## 13. Resilience & Error Handling

### 13.1 Resilience4j konfigürasyonu — `application.yml`'a ekle

```yaml
resilience4j:
  retry:
    instances:
      googlePlacesApi:
        max-attempts: 3
        wait-duration: 1s
        exponential-backoff-multiplier: 2
        retry-exceptions:
          - org.springframework.web.client.HttpServerErrorException
          - org.springframework.web.client.ResourceAccessException
        ignore-exceptions:
          - org.springframework.web.client.HttpClientErrorException.BadRequest
          - org.springframework.web.client.HttpClientErrorException.NotFound
  ratelimiter:
    instances:
      googlePlacesApi:
        limit-for-period: 10
        limit-refresh-period: 1s
        timeout-duration: 5s
```

### 13.2 Error handling stratejisi

| Hata tipi | Davranış |
|-----------|----------|
| 4xx (Bad request, Not found) | Retry **etme**, log + skip |
| 5xx (Server error) | Retry 3 kez, exponential backoff |
| Network timeout | Retry 3 kez |
| Rate limit (429) | Retry + backoff |
| Required field missing | Skip, log warning |
| DB constraint violation | Skip, log error, continue |

### 13.3 Global exception handler (Faz 3'te genişletilecek)

Şimdilik ingestion içindeki `try-catch` yeterli. Faz 3'te REST için `@RestControllerAdvice` ekleyeceğiz.

---

## 14. Logging & Observability

### 14.1 Structured logging

Spring Boot'un default'u zaten yeterli, ama JSON formatına çevirmek production'da faydalı:

`pom.xml`'a:

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

`logback-spring.xml` (production profili için):

```xml
<configuration>
    <springProfile name="prod">
        <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
        </appender>
        <root level="INFO">
            <appender-ref ref="JSON"/>
        </root>
    </springProfile>
</configuration>
```

### 14.2 Ingestion metrics

Spring Boot Actuator + Micrometer ile counter ekle:

```java
// IngestionService'e ekle
private final MeterRegistry meterRegistry;

private void recordInsert() {
    meterRegistry.counter("ingestion.salon.inserted").increment();
}
```

`/actuator/metrics/ingestion.salon.inserted` endpoint'inden okunur. Production'da Prometheus'a gider.

---

## 15. Doğrulama & Test

### 15.1 Manuel doğrulama

```bash
# 1. DB ayakta
docker compose up -d postgres

# 2. Ingestion çalıştır
cd backend
SPRING_PROFILES_ACTIVE=local,ingest mvn spring-boot:run

# Beklenen log:
# ==> Starting ingestion. Queries: [beauty salon Warsaw, hair salon Warsaw, ...]
# Calling Places Text Search: query='beauty salon Warsaw', lang=pl
# Query 'beauty salon Warsaw' done: IngestionResult[totalFetched=20, inserted=20, ...]
# ...
# ==> Ingestion complete in XXXms
```

### 15.2 DB kontrolü

```bash
docker exec -it salon-postgres psql -U salon_admin -d salon_explorer
```

```sql
-- En az 100 salon var mı?
SELECT COUNT(*) FROM salons;
-- Beklenen: 100+

-- Deduplikasyon çalışıyor mu?
SELECT google_place_id, COUNT(*)
FROM salons
GROUP BY google_place_id
HAVING COUNT(*) > 1;
-- Beklenen: 0 satır

-- District resolve oranı
SELECT
    COUNT(*) FILTER (WHERE district_id IS NOT NULL) AS resolved,
    COUNT(*) FILTER (WHERE district_id IS NULL)     AS unresolved,
    ROUND(100.0 * COUNT(*) FILTER (WHERE district_id IS NOT NULL) / COUNT(*), 1) AS resolve_pct
FROM salons;
-- Beklenen: %80+ resolved

-- Top districts
SELECT d.name, COUNT(s.id) AS salon_count
FROM salons s
JOIN districts d ON s.district_id = d.id
GROUP BY d.name
ORDER BY salon_count DESC
LIMIT 5;

-- Field doluluk oranı (data quality)
SELECT
    COUNT(*) AS total,
    COUNT(phone)        AS with_phone,
    COUNT(website)      AS with_website,
    COUNT(rating)       AS with_rating,
    COUNT(price_level)  AS with_price,
    COUNT(photo_url)    AS with_photo
FROM salons;
```

### 15.3 İdempotency testi

Ingestion'ı **iki kez çalıştır**, ikinci runda `inserted=0`, `updated=20+` olmalı:

```bash
# İlk run
SPRING_PROFILES_ACTIVE=local,ingest mvn spring-boot:run
# inserted: 100+, updated: 0

# İkinci run (hemen ardından)
SPRING_PROFILES_ACTIVE=local,ingest mvn spring-boot:run
# inserted: 0, updated: 100+
```

### 15.4 Unit testleri (en az bunlar olsun)

**`PhoneNormalizerTest.java`:**

```java
package com.kaandev.salonexplorer.ingestion.normalizer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneNormalizerTest {

    private final PhoneNormalizer normalizer = new PhoneNormalizer();

    @Test
    void normalizesValidPolishNumber() {
        assertThat(normalizer.normalize("+48 22 123 45 67")).isEqualTo("+48221234567");
    }

    @Test
    void returnsNullForInvalid() {
        assertThat(normalizer.normalize("not a phone")).isNull();
    }

    @Test
    void returnsNullForBlank() {
        assertThat(normalizer.normalize("")).isNull();
        assertThat(normalizer.normalize(null)).isNull();
    }
}
```

**`DistrictResolverTest.java`** ve **`SalonNormalizerTest.java`** için Mockito kullan, DTO mock'la.

---

## 16. Mülakat Soruları

**S: Neden Google Places API seçtin? Booksy değil?**
Resmi API, ToS uyumlu. `google_place_id` ile garantili deduplikasyon. Scraping ile geliştirme süresi belirsiz olur (DOM değişir, IP banlanır). Booksy daha zengin service/price data verir, ileride enrichment layer olarak eklenebilir. MVP için Google Places yeterli ve savunulabilir.

**S: 100 kayda nasıl ulaştın? Tek sorgu yetmez.**
Tek text search ~20 sonuç. 5 farklı kategori sorgusu (beauty salon, hair salon, barber, nail salon, spa) ile 100+ unique kayıt çıkıyor. Pagination da var (`nextPageToken`), gerekirse devreye girer. Scale için district × kategori cartesian product yapılır.

**S: Eksik veriyi nasıl handle ettin?**
Required field'lar (id, name, address) yoksa kayıt skip. Optional field'lar (phone, rating, price) null kalır. Frontend "—" gösterir. Phone E.164'e normalize ediliyor, invalid format null kalıyor → constraint violation olmuyor.

**S: Aynı salonu iki kez kaydetmemeyi nasıl garantiledin?**
İki katmanlı: 1) DB seviyesinde `google_place_id` UNIQUE constraint. 2) Service seviyesinde `findByGooglePlaceId` ile upsert pattern → varsa update, yoksa insert.

**S: Ingestion fail olursa ne olur?**
Resilience4j ile retry (3 kez, exponential backoff). 5xx ve network hataları retry, 4xx hataları skip. Tek bir place fail olsa diğerleri devam ediyor (try-catch loop içinde). Final report'ta `failed` sayısı gösteriliyor.

**S: Production'da nasıl scale ederdin (tüm Polonya)?**
- Sorgu kuyruğa atılır (Kafka/RabbitMQ)
- Worker'lar paralel consume eder
- Rate limit'i Bucket4j ile global yönet
- Incremental update: `places.id` + `last_synced_at` ile sadece eski olanları yenile
- Distributed lock (Redis): aynı place'i iki worker işlemesin
- City listesi DB'de, her şehir için ayrı job
- Failed kayıtlar dead-letter queue'ya gider, manuel inceleme

**S: Photo URL'inde API key olur mu?**
Olmamalı. Şimdi sadece photo reference (`places/X/photos/Y`) saklıyoruz. Frontend istediğinde backend'in proxy endpoint'ine gider (`/api/v1/photos/{ref}`), backend Google'a key ile gider, response'u stream'ler. API key client'a hiç sızmaz.

**S: Polonyaca karakterler problem oldu mu?**
PostgreSQL UTF-8 ile `--encoding=UTF-8` init edildi, sorun yok. District slug'ı için manuel transliteration tablosu yazdım (ł→l, ę→e, vs.) çünkü Java'nın `Normalizer`'ı bunları handle etmez.

**S: `@Transactional` neden `upsertSalon`'da var, `ingestQuery`'de yok?**
İki sebep: 1) İşlem bazında atomicity istiyorum — bir salonun yarısı yazılmasın. 2) `ingestQuery`'i tek transaction yapsam, bir hata tüm batch'i rollback eder. Per-salon transaction → bir hata sadece o salonu etkiler, gerisi devam eder.

**S: Rate limiting neden client-side, server-side de var?**
Google bizi 429 ile rate limit'ler ama biz **proaktif** olarak da yavaşlamalıyız. Sebebi: 429 quota'ya sayılır, paramız yanar. Client-side rate limiter (Resilience4j) request'i hiç göndermeden bekletir.

---

## 17. Definition of Done

- [ ] Google Cloud projesinde Places API + Geocoding API enable
- [ ] API key oluşturulmuş, restriction'lar konmuş, `.env`'de
- [ ] `.env.example`'a yeni ENV değişkenleri eklenmiş, `.env` gitignore'da
- [ ] Tüm entity'ler yazılmış, `mvn compile` hatasız
- [ ] `mvn spring-boot:run` ile uygulama Hibernate validation'dan geçiyor (entity ↔ tablo eşleşiyor)
- [ ] `SPRING_PROFILES_ACTIVE=local,ingest mvn spring-boot:run` çalışıyor
- [ ] DB'de en az **100 unique salon** var
- [ ] District resolve oranı %80'in üzerinde
- [ ] Phone numarası olan kayıtlar E.164 formatında (`SELECT phone FROM salons WHERE phone IS NOT NULL LIMIT 5;` ile kontrol)
- [ ] İdempotency çalışıyor — iki kez çalıştır, ikinci run'da hep "updated", "inserted" değil
- [ ] Hiçbir kayıtta duplicate `google_place_id` yok
- [ ] En az **3 unit test** var (PhoneNormalizer, DistrictResolver slug, SalonNormalizer)
- [ ] Retry mekanizması çalışıyor (logları inceleyince retry attempt'leri görünmeli, en az bir kez 5xx alındığında)
- [ ] `IngestionResult` final raporu loglanıyor
- [ ] Hiçbir secret commit'e girmemiş (`git log -p | grep AIzaSy` boş dönmeli)

---

## ➡️ Sonraki Adım

**Faz 3: REST API (Spring Boot)**

Faz 3'te:
- DTO'lar (response/request modelleri)
- MapStruct ile entity ↔ DTO mapping
- `SalonController` (GET liste + detay, PUT/PATCH update)
- Pagination, filtering, sorting
- Global exception handler
- OpenAPI / Swagger UI
- Spring Security + JWT (admin endpoint'leri için)
- Redis cache layer
- Photo proxy endpoint
