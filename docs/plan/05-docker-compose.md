# Phase 5: Docker Compose Full-Stack Integration

## 1. Phase Summary

## 2. Architecture Diagram

## 3. Backend Dockerfile

### 3.1 backend/Dockerfile

### 3.2 backend/.dockerignore

### 3.3 Multi-Stage Advantages

## 4. Frontend Dockerfile

### 4.1 Enable Next.js Standalone Output

### 4.2 frontend/Dockerfile

### 4.3 frontend/.dockerignore

### 4.4 Frontend Health Endpoint

## 5. Full docker-compose.yml

### 5.1 docker-compose.yml

### 5.2 Optional: docker-compose.ingest.yml

## 6. Environment Management

### 6.1 .env.example

### 6.2 .env — Real Values (gitignored)

### 6.3 .gitignore

### 6.4 Production Secret Management

## 7. Network & Service Discovery

### 7.1 Service Name = DNS Hostname

### 7.2 Network Isolation

## 8. Health Check & Startup Order

### 8.1 depends_on with Condition

### 8.2 Startup Sequence

### 8.3 Health Check Best Practices

## 9. Volume & Persistence

### 9.1 Named Volumes

### 9.2 Bind Mount (Config Only)

### 9.3 Backup Strategy (Production Note)

## 10. Backend Configuration Updates

### 10.1 application-docker.yml

### 10.2 Spring Profiles Activation

### 10.3 Graceful Shutdown

## 11. Frontend Configuration Updates

### 11.1 Build-Time vs Runtime Env

### 11.2 Server Component Fetch Inside Container

### 11.3 Browser Fetch

### 11.4 next/image Host Whitelist

## 12. Build & Running

### 12.1 Initial Setup

### 12.2 Build & Start

### 12.3 View Logs

### 12.4 Data Ingestion

### 12.5 Smoke Test

### 12.6 Stop & Cleanup

### 12.7 Single Service Rebuild

## 13. Reverse Proxy (Optional)

### 13.1 Caddy Basic Setup

### 13.2 docker-compose.override.yml

## 14. Image Size Optimization

### 14.1 Size Comparison

### 14.2 Optimization Techniques

### 14.3 Check Image Size

## 15. Security Hardening

### 15.1 Applied (in code above)

### 15.2 Extra Steps for Production

### 15.3 Image Vulnerability Scan

### 15.4 Production Network Policy

## 16. Verification & Smoke Test

### 16.1 End-to-End Checklist

### 16.2 Resource Usage

### 16.3 Volume Persistence Test

### 16.4 Network Isolation Test

## 17. Common Issues

## 18. Interview Questions

## 19. Definition of Done
