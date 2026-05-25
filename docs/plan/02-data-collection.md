# Phase 2: Data Collection (Google Places API → Database)

## 1. Phase Summary

## 2. Google Places API Strategy

### 2.1 Which APIs to Use

### 2.2 Query Strategy

### 2.3 Language Parameter

### 2.4 Field Masking (Cost Control)

## 3. API Setup & Authentication

### 3.1 Google Cloud Console Steps

### 3.2 .env Update

### 3.3 .env.example Update

### 3.4 Quota & Cost Awareness

## 4. Project Structure

## 5. Domain Entities

### 5.1 BaseEntity.java

### 5.2 District.java

### 5.3 ServiceCategory.java Enum

### 5.4 Service.java

### 5.5 Salon.java

### 5.6 @EnableJpaAuditing

## 6. HTTP Client & Configuration

### 6.1 GooglePlacesProperties.java

### 6.2 IngestionProperties.java

### 6.3 application-local.yml Update

### 6.4 RestClientConfig.java

### 6.5 pom.xml Dependencies

## 7. DTOs (Google Response Models)

### 7.1 PlacesSearchRequest.java

### 7.2 PlaceDto.java

### 7.3 PlacesSearchResponse.java

## 8. Ingestion Service

### 8.1 GooglePlacesClient.java

### 8.2 IngestionResult.java

### 8.3 IngestionService.java

## 9. Data Normalization

### 9.1 PhoneNormalizer.java

### 9.2 SalonNormalizer.java

## 10. District Resolver

### 10.1 DistrictResolver.java

## 11. Repository Layer

### 11.1 SalonRepository.java

### 11.2 DistrictRepository.java

### 11.3 ServiceRepository.java

## 12. Ingestion Trigger (CLI Command)

### 12.1 IngestionRunner.java

### 12.2 Run Command

## 13. Resilience & Error Handling

### 13.1 Resilience4j Configuration

### 13.2 Error Handling Strategy

## 14. Logging & Observability

### 14.1 Structured Logging

### 14.2 Ingestion Metrics

## 15. Verification & Testing

### 15.1 Manual Verification

### 15.2 DB Check

### 15.3 Idempotency Test

### 15.4 Unit Tests

## 16. Interview Questions

## 17. Definition of Done
