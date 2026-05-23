# Phase 3: Backend REST API (Spring Boot)

> **Hedef:** Faz 1-2'de oluşturduğumuz veriyi RESTful bir API olarak expose etmek. Pagination, filtering, sorting, validation, error handling, JWT authentication, rate limiting, caching ve OpenAPI dokümantasyonu ile production-grade bir backend.

---

## 📋 İçindekiler

1. [Faz Özeti](#1-faz-özeti)
2. [API Tasarımı (Endpoint Listesi)](#2-api-tasarımı-endpoint-listesi)
3. [Klasör Yapısı](#3-klasör-yapısı)
4. [Dependency'ler](#4-dependencyler)
5. [DTO Layer](#5-dto-layer)
6. [Mapper Layer (MapStruct)](#6-mapper-layer-mapstruct)
7. [Service Layer](#7-service-layer)
8. [Controller Layer](#8-controller-layer)
9. [Filtering & Specifications](#9-filtering--specifications)
10. [Validation](#10-validation)
11. [Global Exception Handler](#11-global-exception-handler)
12. [Security (JWT)](#12-security-jwt)
13. [Rate Limiting](#13-rate-limiting)
14. [Caching (Redis)](#14-caching-redis)
15. [Audit Logging](#15-audit-logging)
16. [Photo Proxy Endpoint](#16-photo-proxy-endpoint)
17. [OpenAPI / Swagger](#17-openapi--swagger)
18. [CORS Konfigürasyonu](#18-cors-konfigürasyonu)
19. [Testing](#19-testing)
20. [Doğrulama](#20-doğrulama)
21. [Mülakat Soruları](#21-mülakat-soruları)
22. [Definition of Done](#22-definition-of-done)

---

## 1. Faz Özeti

**Bu fazın sonunda elimizde olacaklar:**

- Versioned REST API (`/api/v1/...`)
- Pagination + filtering + sorting (Spring Data + Specifications)
- DTO ↔ Entity mapping (MapStruct)
- Bean Validation ile input validation
- JWT-based authentication (admin endpoint'leri korunmuş)
- Redis ile response cache
- Bucket4j ile rate limiting (IP başına)
- Global exception handler (RFC 7807 Problem Details)
- Audit logging (kim neyi değiştirdi)
- Swagger UI canlı dokümantasyon
- Photo proxy endpoint (API key güvenliği)
- CORS yapılandırması (frontend için)

**Bu fazda YAPMAYACAĞIZ:**

- Frontend (Faz 4)
- Docker Compose entegrasyonu (Faz 5)

---

## 2. API Tasarımı (Endpoint Listesi)

### 2.1 Public endpoint'ler (auth gerektirmez)

| Method | Path | Açıklama |
|--------|------|----------|
| `GET` | `/api/v1/salons` | Liste + pagination + filter |
| `GET` | `/api/v1/salons/{id}` | Tek salon detayı |
| `GET` | `/api/v1/districts` | Tüm district'ler |
| `GET` | `/api/v1/services` | Tüm hizmet kategorileri |
| `GET` | `/api/v1/photos/{photoRef}` | Photo proxy (Google'dan stream) |
| `GET` | `/actuator/health` | Health check |
| `GET` | `/swagger-ui.html` | API documentation |
| `GET` | `/v3/api-docs` | OpenAPI JSON spec |

### 2.2 Protected endpoint'ler (JWT gerekli, ADMIN role)

| Method | Path | Açıklama |
|--------|------|----------|
| `POST` | `/api/v1/auth/login` | Login → JWT döner (public) |
| `PUT` | `/api/v1/salons/{id}` | Tam güncelleme |
| `PATCH` | `/api/v1/salons/{id}` | Kısmi güncelleme |
| `DELETE` | `/api/v1/salons/{id}` | Soft delete |
| `POST` | `/api/v1/admin/ingest` | Veri toplama tetikle |
| `GET` | `/api/v1/admin/audit-log` | Audit log oku |

### 2.3 Query parametreleri (`GET /api/v1/salons`)

| Parametre | Tip | Örnek | Açıklama |
|-----------|-----|-------|----------|
| `page` | int | `0` | Sayfa (0-indexed) |
| `size` | int | `20` | Sayfa boyutu (max 100) |
| `sort` | string | `rating,desc` | Sıralama |
| `district` | string | `mokotow` | District slug filter |
| `service` | string | `haircut` | Service name filter |
| `minRating` | decimal | `4.0` | Minimum rating |
| `maxPriceLevel` | int | `2` | Max price level (1-4) |
| `search` | string | `anna` | Name'de fuzzy search |

### 2.4 Örnek response (liste)

```json
{
  "content": [
    {
      "id": 1,
      "name": "Salon Fryzjerski Anna",
      "district": "Mokotów",
      "rating": 4.7,
      "reviewCount": 142,
      "priceLevel": 2,
      "photoUrl": "/api/v1/photos/places%2FABC%2Fphotos%2FXYZ"
    }
  ],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 127,
    "totalPages": 7
  }
}
```

### 2.5 Örnek response (detay)

```json
{
  "id": 1,
  "name": "Salon Fryzjerski Anna",
  "address": "ul. Marszałkowska 1, 00-001 Warsaw, Poland",
  "district": {
    "id": 4,
    "name": "Mokotów",
    "slug": "mokotow"
  },
  "phone": "+48221234567",
  "website": "https://salonanna.pl",
  "latitude": 52.2297,
  "longitude": 21.0122,
  "rating": 4.7,
  "reviewCount": 142,
  "priceLevel": 2,
  "photoUrl": "/api/v1/photos/places%2FABC%2Fphotos%2FXYZ",
  "services": [
    { "id": 1, "name": "Haircut", "category": "HAIR" },
    { "id": 2, "name": "Hair Coloring", "category": "HAIR" }
  ],
  "isActive": true,
  "createdAt": "2026-05-23T10:00:00Z",
  "updatedAt": "2026-05-23T10:00:00Z"
}
```

---

## 3. Klasör Yapısı

```
backend/src/main/java/com/kaandev/salonexplorer/
├── SalonExplorerApplication.java
├── config/
│   ├── SecurityConfig.java
│   ├── CorsConfig.java
│   ├── RedisConfig.java
│   ├── OpenApiConfig.java
│   ├── JpaConfig.java
│   └── RateLimitConfig.java
├── controller/
│   ├── SalonController.java
│   ├── DistrictController.java
│   ├── ServiceController.java
│   ├── PhotoController.java
│   ├── AuthController.java
│   └── AdminController.java
├── service/
│   ├── SalonService.java
│   ├── DistrictService.java
│   ├── ServiceCatalogService.java
│   ├── AuthService.java
│   ├── AuditService.java
│   └── PhotoProxyService.java
├── security/
│   ├── JwtService.java
│   ├── JwtAuthenticationFilter.java
│   ├── CustomUserDetailsService.java
│   └── RateLimitFilter.java
├── domain/
│   ├── entity/   (Faz 2'den)
│   ├── dto/
│   │   ├── SalonListItemDto.java
│   │   ├── SalonDetailDto.java
│   │   ├── SalonUpdateRequest.java
│   │   ├── SalonPatchRequest.java
│   │   ├── DistrictDto.java
│   │   ├── ServiceDto.java
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java
│   │   ├── PagedResponse.java
│   │   └── ProblemDetailDto.java
│   └── specification/
│       └── SalonSpecifications.java
├── mapper/
│   ├── SalonMapper.java
│   ├── DistrictMapper.java
│   └── ServiceMapper.java
├── repository/   (Faz 2'den, genişletildi)
└── exception/
    ├── GlobalExceptionHandler.java
    ├── ResourceNotFoundException.java
    ├── DuplicateResourceException.java
    └── BusinessException.java
```

---

## 4. Dependency'ler

`pom.xml`'a eklenecekler:

```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>

<!-- Redis (cache + rate limit storage) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>

<!-- Bucket4j rate limiting -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.10.1</version>
</dependency>
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-redis</artifactId>
    <version>8.10.1</version>
</dependency>

<!-- MapStruct -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.6.3</version>
</dependency>

<!-- OpenAPI / Swagger -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

### MapStruct + Lombok annotation processor

`pom.xml`'ın `<build>` bölümüne:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <source>21</source>
                <target>21</target>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                    </path>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok-mapstruct-binding</artifactId>
                        <version>0.2.0</version>
                    </path>
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                        <version>1.6.3</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

> **Önemli:** Lombok + MapStruct birlikte kullanılırken `lombok-mapstruct-binding` mutlaka olmalı, yoksa MapStruct Lombok'un getter/setter'larını göremez.

---

## 5. DTO Layer

DTO'ları **record** olarak yazıyoruz — immutable, boilerplate yok, modern Java.

### 5.1 `SalonListItemDto.java`

```java
package com.kaandev.salonexplorer.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Salon summary for list views")
public record SalonListItemDto(
    @Schema(example = "1") Long id,
    @Schema(example = "Salon Fryzjerski Anna") String name,
    @Schema(example = "Mokotów") String district,
    @Schema(example = "4.7") BigDecimal rating,
    @Schema(example = "142") Integer reviewCount,
    @Schema(example = "2") Short priceLevel,
    @Schema(example = "/api/v1/photos/places%2FABC%2Fphotos%2FXYZ") String photoUrl
) {
}
```

### 5.2 `SalonDetailDto.java`

```java
package com.kaandev.salonexplorer.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

@Schema(description = "Full salon details")
public record SalonDetailDto(
    Long id,
    String name,
    String address,
    DistrictDto district,
    String phone,
    String website,
    BigDecimal latitude,
    BigDecimal longitude,
    BigDecimal rating,
    Integer reviewCount,
    Short priceLevel,
    String photoUrl,
    Set<ServiceDto> services,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {
}
```

### 5.3 `SalonUpdateRequest.java`

```java
package com.kaandev.salonexplorer.domain.dto;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.util.Set;

public record SalonUpdateRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 255)
    String name,

    @NotBlank(message = "Address is required")
    String address,

    Long districtId,

    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone must be in E.164 format")
    String phone,

    @URL(message = "Website must be a valid URL")
    @Size(max = 500)
    String website,

    @DecimalMin(value = "0.0") @DecimalMax(value = "5.0")
    BigDecimal rating,

    @Min(0)
    Integer reviewCount,

    @Min(1) @Max(4)
    Short priceLevel,

    Set<Long> serviceIds,

    Boolean isActive
) {
}
```

### 5.4 `SalonPatchRequest.java`

Tüm field'ları `Optional`/nullable — sadece gönderilen alanlar güncellenir:

```java
package com.kaandev.salonexplorer.domain.dto;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.util.Set;

public record SalonPatchRequest(
    @Size(min = 2, max = 255) String name,
    String address,
    Long districtId,
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$") String phone,
    @URL @Size(max = 500) String website,
    @DecimalMin("0.0") @DecimalMax("5.0") BigDecimal rating,
    @Min(0) Integer reviewCount,
    @Min(1) @Max(4) Short priceLevel,
    Set<Long> serviceIds,
    Boolean isActive
) {
}
```

### 5.5 `DistrictDto.java` & `ServiceDto.java`

```java
package com.kaandev.salonexplorer.domain.dto;

public record DistrictDto(Long id, String name, String slug) {}
```

```java
package com.kaandev.salonexplorer.domain.dto;

import com.kaandev.salonexplorer.domain.enums.ServiceCategory;

public record ServiceDto(Long id, String name, ServiceCategory category) {}
```

### 5.6 `PagedResponse.java`

```java
package com.kaandev.salonexplorer.domain.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PagedResponse<T>(
    List<T> content,
    PageMetadata page
) {
    public static <T> PagedResponse<T> from(Page<T> springPage) {
        return new PagedResponse<>(
            springPage.getContent(),
            new PageMetadata(
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages()
            )
        );
    }

    public record PageMetadata(int number, int size, long totalElements, int totalPages) {}
}
```

### 5.7 Login DTO'ları

```java
package com.kaandev.salonexplorer.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @Email @NotBlank String email,
    @NotBlank String password
) {
}
```

```java
package com.kaandev.salonexplorer.domain.dto;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds
) {
    public static LoginResponse of(String token, long expiresIn) {
        return new LoginResponse(token, "Bearer", expiresIn);
    }
}
```

---

## 6. Mapper Layer (MapStruct)

### 6.1 `SalonMapper.java`

```java
package com.kaandev.salonexplorer.mapper;

import com.kaandev.salonexplorer.domain.dto.*;
import com.kaandev.salonexplorer.domain.entity.Salon;
import com.kaandev.salonexplorer.domain.entity.Service;
import org.mapstruct.*;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(
    componentModel = "spring",
    uses = { DistrictMapper.class, ServiceMapper.class },
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface SalonMapper {

    @Mapping(target = "district", source = "district.name")
    @Mapping(target = "photoUrl", expression = "java(toPhotoProxyUrl(salon.getPhotoUrl()))")
    SalonListItemDto toListItem(Salon salon);

    @Mapping(target = "photoUrl", expression = "java(toPhotoProxyUrl(salon.getPhotoUrl()))")
    SalonDetailDto toDetail(Salon salon);

    @Mapping(target = "id",              ignore = true)
    @Mapping(target = "googlePlaceId",   ignore = true)
    @Mapping(target = "createdAt",       ignore = true)
    @Mapping(target = "updatedAt",       ignore = true)
    @Mapping(target = "district",        ignore = true)
    @Mapping(target = "services",        ignore = true)
    @Mapping(target = "photoUrl",        ignore = true)
    @Mapping(target = "latitude",        ignore = true)
    @Mapping(target = "longitude",       ignore = true)
    void updateFromRequest(SalonUpdateRequest req, @MappingTarget Salon salon);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",              ignore = true)
    @Mapping(target = "googlePlaceId",   ignore = true)
    @Mapping(target = "createdAt",       ignore = true)
    @Mapping(target = "updatedAt",       ignore = true)
    @Mapping(target = "district",        ignore = true)
    @Mapping(target = "services",        ignore = true)
    @Mapping(target = "photoUrl",        ignore = true)
    @Mapping(target = "latitude",        ignore = true)
    @Mapping(target = "longitude",       ignore = true)
    void patchFromRequest(SalonPatchRequest req, @MappingTarget Salon salon);

    default String toPhotoProxyUrl(String photoRef) {
        if (photoRef == null) return null;
        // URL encode the reference (places/X/photos/Y → places%2FX%2Fphotos%2FY)
        return "/api/v1/photos/" + java.net.URLEncoder.encode(photoRef, java.nio.charset.StandardCharsets.UTF_8);
    }
}
```

> **`@MappingTarget`:** Mevcut entity'yi günceller, yeni instance oluşturmaz.
> **`NullValuePropertyMappingStrategy.IGNORE`:** PATCH için kritik — null gelen field'ları update etme.

### 6.2 `DistrictMapper.java` & `ServiceMapper.java`

```java
package com.kaandev.salonexplorer.mapper;

import com.kaandev.salonexplorer.domain.dto.DistrictDto;
import com.kaandev.salonexplorer.domain.entity.District;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DistrictMapper {
    DistrictDto toDto(District district);
}
```

```java
package com.kaandev.salonexplorer.mapper;

import com.kaandev.salonexplorer.domain.dto.ServiceDto;
import com.kaandev.salonexplorer.domain.entity.Service;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ServiceMapper {
    ServiceDto toDto(Service service);
}
```

---

## 7. Service Layer

### 7.1 `SalonService.java`

```java
package com.kaandev.salonexplorer.service;

import com.kaandev.salonexplorer.domain.dto.*;
import com.kaandev.salonexplorer.domain.entity.Salon;
import com.kaandev.salonexplorer.domain.entity.Service;
import com.kaandev.salonexplorer.domain.specification.SalonSpecifications;
import com.kaandev.salonexplorer.exception.ResourceNotFoundException;
import com.kaandev.salonexplorer.mapper.SalonMapper;
import com.kaandev.salonexplorer.repository.DistrictRepository;
import com.kaandev.salonexplorer.repository.SalonRepository;
import com.kaandev.salonexplorer.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class SalonService {

    private final SalonRepository salonRepository;
    private final DistrictRepository districtRepository;
    private final ServiceRepository serviceRepository;
    private final SalonMapper mapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PagedResponse<SalonListItemDto> list(
        String districtSlug,
        String serviceName,
        BigDecimal minRating,
        Short maxPriceLevel,
        String search,
        Pageable pageable
    ) {
        Specification<Salon> spec = Specification
            .where(SalonSpecifications.isActive())
            .and(SalonSpecifications.hasDistrictSlug(districtSlug))
            .and(SalonSpecifications.hasService(serviceName))
            .and(SalonSpecifications.minRating(minRating))
            .and(SalonSpecifications.maxPriceLevel(maxPriceLevel))
            .and(SalonSpecifications.nameContains(search));

        Page<SalonListItemDto> page = salonRepository.findAll(spec, pageable)
            .map(mapper::toListItem);

        return PagedResponse.from(page);
    }

    @Cacheable(value = "salonDetail", key = "#id")
    @Transactional(readOnly = true)
    public SalonDetailDto getById(Long id) {
        Salon salon = salonRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Salon not found: " + id));
        return mapper.toDetail(salon);
    }

    @CacheEvict(value = "salonDetail", key = "#id")
    @Transactional
    public SalonDetailDto update(Long id, SalonUpdateRequest request) {
        Salon salon = salonRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Salon not found: " + id));

        // Diff'i audit log için yakala
        var before = mapper.toDetail(salon);

        mapper.updateFromRequest(request, salon);
        applyDistrict(salon, request.districtId());
        applyServices(salon, request.serviceIds());

        Salon saved = salonRepository.save(salon);
        var after = mapper.toDetail(saved);

        auditService.logUpdate("Salon", id, before, after);
        log.info("Updated salon id={}", id);

        return after;
    }

    @CacheEvict(value = "salonDetail", key = "#id")
    @Transactional
    public SalonDetailDto patch(Long id, SalonPatchRequest request) {
        Salon salon = salonRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Salon not found: " + id));

        var before = mapper.toDetail(salon);

        mapper.patchFromRequest(request, salon);
        if (request.districtId() != null) applyDistrict(salon, request.districtId());
        if (request.serviceIds()  != null) applyServices(salon, request.serviceIds());

        Salon saved = salonRepository.save(salon);
        var after = mapper.toDetail(saved);

        auditService.logUpdate("Salon", id, before, after);
        log.info("Patched salon id={}", id);

        return after;
    }

    @CacheEvict(value = "salonDetail", key = "#id")
    @Transactional
    public void softDelete(Long id) {
        Salon salon = salonRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Salon not found: " + id));
        salon.setIsActive(false);
        salonRepository.save(salon);

        auditService.logDelete("Salon", id);
        log.info("Soft-deleted salon id={}", id);
    }

    private void applyDistrict(Salon salon, Long districtId) {
        if (districtId == null) {
            salon.setDistrict(null);
            return;
        }
        var district = districtRepository.findById(districtId)
            .orElseThrow(() -> new ResourceNotFoundException("District not found: " + districtId));
        salon.setDistrict(district);
    }

    private void applyServices(Salon salon, Set<Long> serviceIds) {
        if (serviceIds == null) return;
        Set<Service> services = new HashSet<>(serviceRepository.findAllById(serviceIds));
        if (services.size() != serviceIds.size()) {
            throw new ResourceNotFoundException("One or more services not found");
        }
        salon.setServices(services);
    }
}
```

### 7.2 `DistrictService.java` & `ServiceCatalogService.java`

```java
package com.kaandev.salonexplorer.service;

import com.kaandev.salonexplorer.domain.dto.DistrictDto;
import com.kaandev.salonexplorer.mapper.DistrictMapper;
import com.kaandev.salonexplorer.repository.DistrictRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DistrictService {

    private final DistrictRepository repository;
    private final DistrictMapper mapper;

    @Cacheable("districts")
    @Transactional(readOnly = true)
    public List<DistrictDto> findAll() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }
}
```

`ServiceCatalogService.java` benzer şekilde.

### 7.3 `SalonRepository` güncellemesi

Specification desteği için interface'i güncelle:

```java
package com.kaandev.salonexplorer.repository;

import com.kaandev.salonexplorer.domain.entity.Salon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SalonRepository
        extends JpaRepository<Salon, Long>, JpaSpecificationExecutor<Salon> {

    Optional<Salon> findByGooglePlaceId(String googlePlaceId);
    boolean existsByGooglePlaceId(String googlePlaceId);
    long countByIsActiveTrue();
}
```

---

## 8. Controller Layer

### 8.1 `SalonController.java`

```java
package com.kaandev.salonexplorer.controller;

import com.kaandev.salonexplorer.domain.dto.*;
import com.kaandev.salonexplorer.service.SalonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/salons")
@RequiredArgsConstructor
@Tag(name = "Salons", description = "Beauty salon CRUD operations")
public class SalonController {

    private final SalonService salonService;

    @GetMapping
    @Operation(summary = "List salons with pagination and filtering")
    public PagedResponse<SalonListItemDto> list(
        @Parameter(description = "District slug filter") @RequestParam(required = false) String district,
        @Parameter(description = "Service name filter")  @RequestParam(required = false) String service,
        @Parameter(description = "Minimum rating 0-5")   @RequestParam(required = false) BigDecimal minRating,
        @Parameter(description = "Max price level 1-4")  @RequestParam(required = false) Short maxPriceLevel,
        @Parameter(description = "Name fuzzy search")    @RequestParam(required = false) String search,
        @ParameterObject @PageableDefault(size = 20, sort = "rating") Pageable pageable
    ) {
        return salonService.list(district, service, minRating, maxPriceLevel, search, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get salon details by ID")
    public SalonDetailDto getById(@PathVariable Long id) {
        return salonService.getById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Replace salon (admin only)")
    public SalonDetailDto update(
        @PathVariable Long id,
        @Valid @RequestBody SalonUpdateRequest request
    ) {
        return salonService.update(id, request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Partial update (admin only)")
    public SalonDetailDto patch(
        @PathVariable Long id,
        @Valid @RequestBody SalonPatchRequest request
    ) {
        return salonService.patch(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Soft delete (admin only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        salonService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### 8.2 `DistrictController.java`

```java
package com.kaandev.salonexplorer.controller;

import com.kaandev.salonexplorer.domain.dto.DistrictDto;
import com.kaandev.salonexplorer.service.DistrictService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/districts")
@RequiredArgsConstructor
@Tag(name = "Districts")
public class DistrictController {

    private final DistrictService districtService;

    @GetMapping
    public List<DistrictDto> findAll() {
        return districtService.findAll();
    }
}
```

`ServiceController.java` benzer şekilde.

---

## 9. Filtering & Specifications

JPA Specifications ile dinamik query'ler — Criteria API'nin temiz kullanımı.

### 9.1 `SalonSpecifications.java`

```java
package com.kaandev.salonexplorer.domain.specification;

import com.kaandev.salonexplorer.domain.entity.Salon;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class SalonSpecifications {

    private SalonSpecifications() {}

    public static Specification<Salon> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }

    public static Specification<Salon> hasDistrictSlug(String slug) {
        if (slug == null || slug.isBlank()) return null;
        return (root, query, cb) -> cb.equal(root.get("district").get("slug"), slug.toLowerCase());
    }

    public static Specification<Salon> hasService(String serviceName) {
        if (serviceName == null || serviceName.isBlank()) return null;
        return (root, query, cb) -> {
            // DISTINCT ekle, çünkü join çoğullar oluşturabilir
            query.distinct(true);
            Join<Object, Object> services = root.join("services");
            return cb.equal(cb.lower(services.get("name")), serviceName.toLowerCase());
        };
    }

    public static Specification<Salon> minRating(BigDecimal min) {
        if (min == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("rating"), min);
    }

    public static Specification<Salon> maxPriceLevel(Short max) {
        if (max == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("priceLevel"), max);
    }

    public static Specification<Salon> nameContains(String search) {
        if (search == null || search.isBlank()) return null;
        return (root, query, cb) -> cb.like(
            cb.lower(root.get("name")),
            "%" + search.toLowerCase() + "%"
        );
    }
}
```

> **Pattern not:** `null` döndüren bir Specification, `.and(null)` ile chain edildiğinde Spring tarafından ignore edilir. Bu sayede opsiyonel filter'lar temiz şekilde compose edilir.

---

## 10. Validation

### 10.1 Bean Validation kullanımı

Controller'da `@Valid` annotation'ı, DTO'da Jakarta constraint'leri (`@NotBlank`, `@Email`, `@Pattern`, vs.) — Faz 5'te DTO'lara konuldu.

### 10.2 Custom validator (opsiyonel — district ID var mı kontrolü)

```java
package com.kaandev.salonexplorer.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DistrictExistsValidator.class)
public @interface DistrictExists {
    String message() default "District does not exist";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

> Bu opsiyonel — service layer'da zaten kontrol var. Sadece "enterprise-level" göstermek için eklenebilir.

---

## 11. Global Exception Handler

RFC 7807 Problem Details standardını kullanıyoruz — Spring 6.1+'da built-in.

### 11.1 Custom exception'lar

```java
package com.kaandev.salonexplorer.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}
```

```java
package com.kaandev.salonexplorer.exception;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) { super(message); }
}
```

```java
package com.kaandev.salonexplorer.exception;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) { super(message); }
}
```

### 11.2 `GlobalExceptionHandler.java`

```java
package com.kaandev.salonexplorer.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
        ResourceNotFoundException ex, HttpServletRequest request
    ) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://api.salonexplorer.com/errors/not-found"));
        problem.setTitle("Resource Not Found");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
        MethodArgumentNotValidException ex, HttpServletRequest request
    ) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setType(URI.create("https://api.salonexplorer.com/errors/validation"));
        problem.setTitle("Validation Error");
        problem.setInstance(URI.create(request.getRequestURI()));

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
            fieldErrors.put(err.getField(), err.getDefaultMessage())
        );
        problem.setProperty("errors", fieldErrors);
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials(
        BadCredentialsException ex, HttpServletRequest request
    ) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        problem.setTitle("Unauthorized");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(
        AccessDeniedException ex, HttpServletRequest request
    ) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
        problem.setTitle("Forbidden");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex, HttpServletRequest request) {
        // ⚠️ CRITICAL: stack trace asla client'a sızmasın
        log.error("Unhandled exception at {}: ", request.getRequestURI(), ex);

        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred"
        );
        problem.setTitle("Internal Server Error");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());
        // Stack trace YOK!
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}
```

### 11.3 Örnek error response

```json
{
  "type": "https://api.salonexplorer.com/errors/validation",
  "title": "Validation Error",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/v1/salons/1",
  "timestamp": "2026-05-23T10:00:00Z",
  "errors": {
    "phone": "Phone must be in E.164 format",
    "rating": "must be less than or equal to 5.0"
  }
}
```

---

## 12. Security (JWT)

### 12.1 `JwtService.java`

```java
package com.kaandev.salonexplorer.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.expiration-ms}")
    private long expirationMs;

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", userDetails.getAuthorities());
        return buildToken(claims, userDetails.getUsername());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }

    private String buildToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
            .claims(claims)
            .subject(subject)
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(getSignInKey())
            .compact();
    }

    private boolean isExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
            .verifyWith(getSignInKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return resolver.apply(claims);
    }

    private SecretKey getSignInKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
```

### 12.2 `JwtAuthenticationFilter.java`

```java
package com.kaandev.salonexplorer.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);
        final String username = jwtService.extractUsername(token);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails user = userDetailsService.loadUserByUsername(username);
            if (jwtService.isTokenValid(token, user)) {
                var auth = new UsernamePasswordAuthenticationToken(
                    user, null, user.getAuthorities()
                );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

### 12.3 `SecurityConfig.java`

```java
package com.kaandev.salonexplorer.config;

import com.kaandev.salonexplorer.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // Stateless API → CSRF gerek yok
            .cors(cors -> {})              // CorsConfig'den gelir
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoint'ler
                .requestMatchers(HttpMethod.GET, "/api/v1/salons", "/api/v1/salons/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/districts", "/api/v1/services").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/photos/**").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                // Geri kalanı auth gerektirir
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);  // strength 12 (default 10, daha güvenli)
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

### 12.4 `application-local.yml` — JWT secret

```yaml
security:
  jwt:
    # 256-bit Base64 secret (production'da env variable!)
    secret: ${JWT_SECRET:Y2hhbmdlbWVfdGhpc19pc19hX3NhbXBsZV9zZWNyZXRfZm9yX2Rldl9vbmx5XzMyYg==}
    expiration-ms: 3600000  # 1 saat
```

`.env` ekle:

```bash
JWT_SECRET=<openssl rand -base64 32 ile üret>
```

Generate komutu:

```bash
openssl rand -base64 32
```

### 12.5 `AuthController.java`

```java
package com.kaandev.salonexplorer.controller;

import com.kaandev.salonexplorer.domain.dto.LoginRequest;
import com.kaandev.salonexplorer.domain.dto.LoginResponse;
import com.kaandev.salonexplorer.security.JwtService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        var auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        var user = (UserDetails) auth.getPrincipal();
        String token = jwtService.generateToken(user);
        return LoginResponse.of(token, jwtService.getExpirationSeconds());
    }
}
```

### 12.6 `CustomUserDetailsService.java`

```java
package com.kaandev.salonexplorer.security;

import com.kaandev.salonexplorer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return User.builder()
            .username(user.getEmail())
            .password(user.getPasswordHash())
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())))
            .disabled(!user.getIsEnabled())
            .build();
    }
}
```

> **Not:** `User` entity'sini ve `UserRepository`'yi Faz 2 entity'lerinin yanına eklemen gerekiyor. Migration zaten var (V2).

### 12.7 İlk admin user'ı seed et

`V5__seed_admin_user.sql` migration ekle:

```sql
-- Default password: 'Admin123!' — bcrypt hash strength 12
-- Production'da bunu DEĞİŞTİR
INSERT INTO users (email, password_hash, role, is_enabled) VALUES
    ('admin@salonexplorer.local',
     '$2a$12$YOUR_BCRYPT_HASH_HERE',
     'ADMIN',
     TRUE)
ON CONFLICT (email) DO NOTHING;
```

Hash üretmek için bir kerelik küçük bir Java main, ya da online BCrypt generator (strength 12).

---

## 13. Rate Limiting

### 13.1 `RateLimitFilter.java`

```java
package com.kaandev.salonexplorer.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int CAPACITY = 100;
    private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);

    // Production'da Redis ile distributed bucket kullanılır.
    // MVP için in-memory:
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String ip = resolveClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(ip, k -> newBucket());

        if (bucket.tryConsume(1)) {
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(bucket.getAvailableTokens()));
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(REFILL_PERIOD.toSeconds()));
            response.getWriter().write("{\"detail\":\"Rate limit exceeded\"}");
        }
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(CAPACITY, Refill.intervally(CAPACITY, REFILL_PERIOD));
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

Filter chain'e ekle (`SecurityConfig`'de):

```java
.addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class)
```

---

## 14. Caching (Redis)

### 14.1 `RedisConfig.java`

```java
package com.kaandev.salonexplorer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        var objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .activateDefaultTyping(
                com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator.builder()
                    .allowIfBaseType(Object.class).build(),
                ObjectMapper.DefaultTyping.NON_FINAL
            );

        var serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        var defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues()
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withCacheConfiguration("salonDetail",
                defaultConfig.entryTtl(Duration.ofMinutes(15)))
            .withCacheConfiguration("districts",
                defaultConfig.entryTtl(Duration.ofHours(24)))
            .build();
    }
}
```

### 14.2 `application-local.yml` — Redis

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
      connect-timeout: 2000ms
```

### 14.3 `docker-compose.yml`'a Redis ekle

```yaml
services:
  # ... postgres

  redis:
    image: redis:7.4-alpine
    container_name: salon-redis
    restart: unless-stopped
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5
    networks:
      - salon-network

volumes:
  postgres_data:
  redis_data:
```

---

## 15. Audit Logging

### 15.1 `AuditService.java`

```java
package com.kaandev.salonexplorer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaandev.salonexplorer.domain.entity.AuditLog;
import com.kaandev.salonexplorer.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;  // request-scoped

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUpdate(String entityType, Long entityId, Object before, Object after) {
        try {
            var diff = computeDiff(before, after);
            saveLog(entityType, entityId, "UPDATE", diff);
        } catch (Exception e) {
            log.error("Failed to write audit log for {} {}: {}", entityType, entityId, e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDelete(String entityType, Long entityId) {
        saveLog(entityType, entityId, "DELETE", null);
    }

    private void saveLog(String type, Long id, String action, String changes) {
        var entry = AuditLog.builder()
            .entityType(type)
            .entityId(id)
            .action(action)
            .changes(changes)
            .ipAddress(resolveIp())
            .userAgent(request.getHeader("User-Agent"))
            .userId(resolveCurrentUserId())
            .build();
        repository.save(entry);
    }

    private String computeDiff(Object before, Object after) throws Exception {
        // Basit: tüm field'ları JSON olarak yaz.
        // Production'da: JsonPatch ile diff.
        var node = objectMapper.createObjectNode();
        node.set("before", objectMapper.valueToTree(before));
        node.set("after", objectMapper.valueToTree(after));
        return objectMapper.writeValueAsString(node);
    }

    private String resolveIp() {
        String xff = request.getHeader("X-Forwarded-For");
        return xff != null ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }

    private Long resolveCurrentUserId() {
        // SecurityContext'ten user'ı çek
        // TODO: principal'da userId tutmak için custom UserDetails extend et
        return null;
    }
}
```

> **`REQUIRES_NEW`:** Audit log ana transaction fail olsa bile yazılsın diye ayrı transaction.

---

## 16. Photo Proxy Endpoint

Frontend'in API key'i görmemesi için Google Places photo'larını backend üzerinden proxy'liyoruz.

### 16.1 `PhotoController.java`

```java
package com.kaandev.salonexplorer.controller;

import com.kaandev.salonexplorer.service.PhotoProxyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/photos")
@RequiredArgsConstructor
@Tag(name = "Photos", description = "Photo proxy for Google Places")
public class PhotoController {

    private final PhotoProxyService photoProxyService;

    @GetMapping("/{photoRef}")
    public ResponseEntity<byte[]> getPhoto(@PathVariable String photoRef) {
        String decoded = URLDecoder.decode(photoRef, StandardCharsets.UTF_8);
        byte[] image = photoProxyService.fetchPhoto(decoded);
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .header("Cache-Control", "public, max-age=86400")  // 1 gün
            .body(image);
    }
}
```

### 16.2 `PhotoProxyService.java`

```java
package com.kaandev.salonexplorer.service;

import com.kaandev.salonexplorer.config.GooglePlacesProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class PhotoProxyService {

    private final RestClient googlePlacesRestClient;
    private final GooglePlacesProperties props;

    @Cacheable(value = "photos", key = "#photoRef")
    public byte[] fetchPhoto(String photoRef) {
        // photoRef: "places/X/photos/Y"
        return googlePlacesRestClient.get()
            .uri("/{ref}/media?maxWidthPx=800&key={key}", photoRef, props.apiKey())
            .retrieve()
            .body(byte[].class);
    }
}
```

---

## 17. OpenAPI / Swagger

### 17.1 `OpenApiConfig.java`

```java
package com.kaandev.salonexplorer.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Warsaw Salon Explorer API")
                .description("REST API for browsing and managing Warsaw beauty salons")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Kaan Yavuz")
                    .url("https://mustafakaanyavuz.com"))
                .license(new License().name("MIT")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Local development")
            ))
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
```

### 17.2 Swagger UI erişim

Uygulama çalışırken:
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

---

## 18. CORS Konfigürasyonu

### 18.1 `CorsConfig.java`

```java
package com.kaandev.salonexplorer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        var source = new UrlBasedCorsConfigurationSource();
        var config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        config.setExposedHeaders(List.of("X-Rate-Limit-Remaining"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

### 18.2 `application-local.yml`

```yaml
cors:
  allowed-origins:
    - http://localhost:3000
    - http://127.0.0.1:3000
```

> Production'da `https://yourdomain.com`. **Asla `*` kullanma** auth varken.

---

## 19. Testing

### 19.1 `SalonServiceTest.java` — unit test

```java
package com.kaandev.salonexplorer.service;

import com.kaandev.salonexplorer.domain.entity.Salon;
import com.kaandev.salonexplorer.exception.ResourceNotFoundException;
import com.kaandev.salonexplorer.mapper.SalonMapper;
import com.kaandev.salonexplorer.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalonServiceTest {

    @Mock private SalonRepository salonRepository;
    @Mock private DistrictRepository districtRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private SalonMapper mapper;
    @Mock private AuditService auditService;
    @InjectMocks private SalonService service;

    @Test
    void getById_notFound_throwsException() {
        when(salonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Salon not found: 99");
    }

    @Test
    void softDelete_marksInactive() {
        Salon salon = Salon.builder().name("Test").build();
        salon.setIsActive(true);
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));

        service.softDelete(1L);

        assertThat(salon.getIsActive()).isFalse();
        verify(salonRepository).save(salon);
        verify(auditService).logDelete("Salon", 1L);
    }
}
```

### 19.2 `SalonControllerIntegrationTest.java` — integration test

```java
package com.kaandev.salonexplorer.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class SalonControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;

    @Test
    void listSalons_returnsEmpty_whenDbEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/salons"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/salons/9999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }
}
```

### 19.3 Test dependency'leri

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 20. Doğrulama

### 20.1 Backend'i başlat

```bash
# DB + Redis
docker compose up -d postgres redis

# Backend
cd backend
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

### 20.2 cURL ile endpoint testleri

**Liste:**

```bash
curl http://localhost:8080/api/v1/salons | jq
```

**Filter:**

```bash
curl "http://localhost:8080/api/v1/salons?district=mokotow&minRating=4.0&size=5" | jq
```

**Detay:**

```bash
curl http://localhost:8080/api/v1/salons/1 | jq
```

**Login:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@salonexplorer.local","password":"Admin123!"}' | jq
```

**Update (token ile):**

```bash
TOKEN="<paste_jwt_here>"
curl -X PATCH http://localhost:8080/api/v1/salons/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"rating": 4.9}' | jq
```

**Unauthorized (token yok):**

```bash
curl -X PATCH http://localhost:8080/api/v1/salons/1 \
  -H "Content-Type: application/json" \
  -d '{"rating": 4.9}'
# Beklenen: 401 + ProblemDetail JSON
```

**Validation hatası:**

```bash
curl -X PATCH http://localhost:8080/api/v1/salons/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"rating": 99.0}'
# Beklenen: 400 + errors.rating mesajı
```

### 20.3 Swagger UI

http://localhost:8080/swagger-ui.html → tüm endpoint'leri interaktif test edebilirsin.

### 20.4 Cache testi

```bash
# İlk istek (DB'ye gider, ~50ms)
time curl http://localhost:8080/api/v1/salons/1

# İkinci istek (Redis'ten, ~5ms)
time curl http://localhost:8080/api/v1/salons/1

# Redis'i izle (başka terminal'de):
docker exec -it salon-redis redis-cli MONITOR
```

### 20.5 Rate limit testi

```bash
# 101 istek at, 101.si 429 dönmeli
for i in {1..101}; do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/v1/salons
done | tail -5
# Beklenen: 200 200 200 429 429
```

---

## 21. Mülakat Soruları

**S: Neden API versioning (`/api/v1/`)?**
Geriye dönük uyumluluk. Breaking change yapacaksak `/api/v2/` çıkarırız, eski client'lar `/v1/` üzerinde kalmaya devam eder. Mobile uygulama gibi senkronize edemediğin client'lar için kritik.

**S: Pagination'da neden cursor değil offset?**
MVP için offset (Spring `Pageable`) yeterli ve standart. Çok büyük datasetlerde (1M+) cursor-based pagination performans için tercih edilir çünkü `OFFSET 100000` PostgreSQL'i yavaşlatır. Bizim 100-1000 kayıtta sorun yok.

**S: PUT vs PATCH farkı?**
PUT: kaynağın tam halini gönderirsin, eksik alanlar default/null olur. PATCH: sadece değişen alanları gönderirsin, diğerleri korunur. `NullValuePropertyMappingStrategy.IGNORE` ile MapStruct PATCH semantics'ini doğru handle ediyor.

**S: JWT neden? Session değil?**
Stateless. Backend instance'ları arasında session paylaşımı gerektirmiyor, horizontal scale kolay. Kubernetes ortamında her pod aynı token'ı doğrulayabilir. Trade-off: token revocation zor (Redis blacklist ile çözülür).

**S: JWT secret'ı nerede tutarsın production'da?**
Asla code/repo'da değil. Asla env variable'da bile direkt değil (ortam dump'ında görülür). AWS Secrets Manager, HashiCorp Vault, GCP Secret Manager — runtime'da çekilir, rotation otomatik.

**S: BCrypt strength neden 12?**
Default 10 hâlâ ok ama 2026 hardware için 12 daha güvenli. Login ~250ms sürer (kabul edilebilir). Brute force maliyetini exponential olarak arttırır. 14+ olursa response time fazla artar.

**S: Cache eviction stratejin?**
TTL + manual eviction. TTL'ler farklı (district 24h, salon detail 15min) — değişme sıklığına göre. Update endpoint'leri `@CacheEvict` ile cache'i temizliyor → stale data garanti yok. Stronger guarantee için event-driven invalidation (Kafka).

**S: Specification pattern neden? `@Query` annotation yetmez mi?**
8 farklı filter parametresi var, kombinasyonları 2^8 = 256. Her biri için ayrı method yazmak imkansız. Specification = composable Criteria API → dinamik query, compile-time tip güvenliği, refactor dostu.

**S: `@PreAuthorize` controller'da, neden service'te değil?**
Spring Security best practice: controller'da çünkü "kim erişebilir" bir HTTP concern. Service business logic'tir, auth'a karşı bilinçsiz olmalı. Test'te `@WithMockUser` ile mock'lamak kolay.

**S: Rate limit'i Redis'e neden taşıyacaksın production'da?**
Multi-instance deployment'ta her instance'ın kendi in-memory bucket'ı olur → kullanıcı 3 instance'a 100'er request atabilir = 300. Redis ile distributed bucket → gerçek 100 sınırı.

**S: ProblemDetail (RFC 7807) neden?**
Standart. OpenAPI specification destekliyor, client kütüphaneleri (axios interceptor, vs.) bu formatı tanıyor. Custom JSON yapısı kullanırsan her client için ayrı parser yazılır.

**S: `@Transactional(readOnly = true)` neden önemli?**
Hibernate dirty check'i atlar (performans), connection'ı read-only mode'a alır (master/replica routing'de replica'ya yönlendirir), accidental write koruması sağlar.

**S: Audit log neden ayrı transaction (`REQUIRES_NEW`)?**
Ana transaction commit oldu, audit log yazılırken Redis çöktü → tüm rollback olursa user'ın gördüğü "kayıt güncellendi" ile gerçek state uyuşmaz. Ayrı transaction'da audit fail olsa bile ana işlem korunur (log'a düşer).

**S: CORS production'da nasıl?**
Sadece kendi domain'in (`https://salonexplorer.com`). `*` + `Allow-Credentials: true` zaten spec gereği imkansız. CDN/load balancer seviyesinde de OPTIONS handle edilebilir performans için.

**S: API'yi nasıl scale ederdin?**
- Stateless → horizontal scale (Kubernetes deployment + HPA)
- Read replica → SELECT'leri ayır
- Redis cluster (cache + rate limit)
- API Gateway (Kong/Traefik) → rate limit + auth offload
- CDN cache (CloudFront) — public endpoint'ler için
- Database connection pool tuning (HikariCP)
- gRPC inter-service iletişim
- Observability: OpenTelemetry → Jaeger/Tempo

---

## 22. Definition of Done

- [ ] Tüm DTO'lar record olarak yazıldı, validation annotation'ları konuldu
- [ ] MapStruct mapper'lar çalışıyor, generated kod `target/generated-sources/`'da
- [ ] `GET /api/v1/salons` çalışıyor, pagination/filter/sort doğru
- [ ] `GET /api/v1/salons/{id}` çalışıyor, 404 ProblemDetail dönüyor invalid ID'de
- [ ] `PUT` ve `PATCH` `/api/v1/salons/{id}` JWT olmadan 401 dönüyor
- [ ] Valid JWT ile PUT/PATCH başarılı
- [ ] Validation hatası 400 + field-level error JSON dönüyor
- [ ] `POST /api/v1/auth/login` doğru credential'la JWT döndürüyor, yanlışta 401
- [ ] Wrong password → BCrypt karşılaştırması fail oluyor, log'da password görünmüyor
- [ ] Swagger UI açılıyor, "Authorize" butonu çalışıyor (bearer token ekleyince protected endpoint'ler çağrılabiliyor)
- [ ] Redis cache çalışıyor (ikinci request belirgin hızlı, `MONITOR` ile görülüyor)
- [ ] `@CacheEvict` update sonrası cache'i temizliyor
- [ ] Rate limit aktif, 100+ request 429 dönüyor
- [ ] CORS frontend origin'inden allow, başka origin'den reject
- [ ] Audit log tablosunda update'ler görünüyor (kim, ne zaman, değişiklik)
- [ ] Photo proxy çalışıyor, response header'da API key yok
- [ ] En az 5 unit test, 2 integration test (Testcontainers) yeşil
- [ ] `mvn test` tüm testler geçiyor
- [ ] `mvn package` jar üretiyor
- [ ] Hiçbir secret commit'e girmemiş
- [ ] `application-prod.yml` placeholder env variable'larla hazır

---

## ➡️ Sonraki Adım

**Faz 4: Frontend (Next.js)**

Faz 4'te:
- Next.js 16 App Router projesi
- TanStack Query ile server state
- shadcn/ui + Tailwind
- Listing page (filtreler, pagination)
- Detail page
- Edit form (react-hook-form + zod)
- Admin login (JWT httpOnly cookie)
- Loading skeletons, error boundaries, toast notifications
