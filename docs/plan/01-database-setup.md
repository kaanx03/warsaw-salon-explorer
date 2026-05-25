# Phase 1: Database Setup (PostgreSQL + Docker)

## 1. Phase Summary

## 2. Prerequisites

## 3. Project Structure

## 4. Schema Design

### 4.1 ERD

### 4.2 Table Descriptions

## 5. Docker Compose Setup

### 5.1 .env.example

### 5.2 .gitignore

### 5.3 docker-compose.yml

### 5.4 Init Script — Application User

### 5.5 Start the Container

### 5.6 Connection Test

## 6. Flyway Migration Strategy

### 6.1 Naming Convention

### 6.2 Golden Rule: Migrations are Immutable

### 6.3 V1__create_core_tables.sql

### 6.4 V2__create_user_and_audit_tables.sql

### 6.5 V3__create_indexes.sql

### 6.6 V4__seed_districts.sql

## 7. Spring Boot Connection

### 7.1 pom.xml Dependencies

### 7.2 application.yml

### 7.3 application-local.yml

### 7.4 SalonExplorerApplication.java

### 7.5 First Run

## 8. Verification & Testing

### 8.1 Manual Check

### 8.2 Spring Boot Health Endpoint

## 9. Common Issues

## 10. Interview Questions

## 11. Definition of Done
