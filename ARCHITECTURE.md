# Architecture Overview

Angular
↓
Spring Boot REST API
↓
Services
↓
MySQL

The application follows a clean layered architecture. Angular provides the user experience and authenticates against the Spring Boot backend through JWT-protected REST endpoints. Business logic for authentication, onboarding, plan generation, Strava import, and AI coaching lives in Spring Boot services. Persistent application data is stored in MySQL using JPA and Flyway migrations.
The application follows a clean layered architecture. Angular provides the user experience and authenticates against the Spring Boot backend through JWT-protected REST endpoints. Business logic for authentication, onboarding, plan generation, Strava import, and AI coaching lives in Spring Boot services. Persistent application data is stored in MySQL using JPA and Flyway migrations.

## Spring Boot AI Provider

Spring Boot
↓
AI Provider

The backend exposes an AI orchestration layer that builds a structured prompt from the user profile, business calculations, and recent Strava summary. The application validates the model response before storing the generated plan. If the provider is unavailable or returns malformed JSON, the system falls back to safe defaults and returns a useful error to the caller.

## Spring Boot Strava API

Spring Boot
↓
Strava API

The backend handles OAuth authorization and token exchange with Strava. Once connected, the application polls Strava for new activities, stores relevant metrics, aggregates them, and uses that summary during plan adaptation and dashboard reporting.
