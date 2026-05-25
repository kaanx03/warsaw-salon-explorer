# Phase 3: Backend REST API (Spring Boot)

## 1. Phase Summary

## 2. API Design (Endpoint List)

### 2.1 Public Endpoints

### 2.2 Protected Endpoints (JWT Required)

### 2.3 Query Parameters (GET /api/v1/salons)

### 2.4 Example Response (List)

### 2.5 Example Response (Detail)

## 3. Project Structure

## 4. Dependencies

## 5. DTO Layer

### 5.1 SalonListItemDto.java

### 5.2 SalonDetailDto.java

### 5.3 SalonUpdateRequest.java

### 5.4 SalonPatchRequest.java

### 5.5 DistrictDto.java & ServiceDto.java

### 5.6 PagedResponse.java

### 5.7 Login DTOs

## 6. Mapper Layer (MapStruct)

### 6.1 SalonMapper.java

### 6.2 DistrictMapper.java & ServiceMapper.java

## 7. Service Layer

### 7.1 SalonService.java

### 7.2 DistrictService.java & ServiceCatalogService.java

### 7.3 SalonRepository Update

## 8. Controller Layer

### 8.1 SalonController.java

### 8.2 DistrictController.java

## 9. Filtering & Specifications

### 9.1 SalonSpecifications.java

## 10. Validation

### 10.1 Bean Validation

### 10.2 Custom Validator (Optional)

## 11. Global Exception Handler

### 11.1 Custom Exceptions

### 11.2 GlobalExceptionHandler.java

### 11.3 Example Error Response

## 12. Security (JWT)

### 12.1 JwtService.java

### 12.2 JwtAuthenticationFilter.java

### 12.3 SecurityConfig.java

### 12.4 application-local.yml — JWT Secret

### 12.5 AuthController.java

### 12.6 CustomUserDetailsService.java

### 12.7 Seed Admin User

## 13. Rate Limiting

### 13.1 RateLimitFilter.java

## 14. Caching (Redis)

### 14.1 RedisConfig.java

### 14.2 application-local.yml — Redis

### 14.3 docker-compose.yml — Redis

## 15. Audit Logging

### 15.1 AuditService.java

## 16. Photo Proxy Endpoint

### 16.1 PhotoController.java

### 16.2 PhotoProxyService.java

## 17. OpenAPI / Swagger

### 17.1 OpenApiConfig.java

### 17.2 Swagger UI Access

## 18. CORS Configuration

### 18.1 CorsConfig.java

### 18.2 application-local.yml

## 19. Testing

### 19.1 SalonServiceTest.java

### 19.2 SalonControllerIntegrationTest.java

### 19.3 Test Dependencies

## 20. Verification

### 20.1 Start Backend

### 20.2 cURL Endpoint Tests

### 20.3 Swagger UI

### 20.4 Cache Test

### 20.5 Rate Limit Test

## 21. Interview Questions

## 22. Definition of Done
