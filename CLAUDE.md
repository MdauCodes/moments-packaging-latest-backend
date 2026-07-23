# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Spring Boot 3 / Java 21 backend for Moments Packaging Kenya — a self-serve e-commerce platform where customers browse products, configure orders, and pay via PayHero (M-Pesa STK push), bank transfer, or cash on delivery. Enterprise orders (10,000+ units) route to a sales-quote endpoint instead of checkout.

## Commands

```powershell
# Run (uses local Postgres 17 on :5432, db moments_packaging_db, user postgres)
.\mvnw spring-boot:run

# Compile only
.\mvnw clean compile -q          # silent exit = BUILD SUCCESS

# Run tests
.\mvnw test

# Package
.\mvnw clean package -DskipTests   # -> target/moments-packaging-backend-*.jar
```

Always use the Maven wrapper (`.\mvnw`), never a system-installed `mvn`. JDK 21 expected at `C:\Program Files\Java\jdk-21`.

Once running: Swagger UI at `http://localhost:8080/swagger-ui.html`, health at `/actuator/health`, API docs at `/api-docs`.

Test coverage is currently minimal — only the default Spring context-load test exists (`MomentsPackagingBackendJavaFirstClientApplicationTests`).

## Architecture

**Feature-package structure.** Code is organized by business domain, not by technical layer: each package under `src/main/java/.../momentspackagingbackendjavafirstclient/` (e.g. `order`, `payment`, `cart`, `product`, `customer`, `referral`, `taxdocument`, `documentbundle`, `analytics`, `receipt`, `enquiry`, `lead`, `business`, `blog`, `industry`, `taxonomy`, `tag`, `settings`, `audit`, `user`, `auth`) is a vertical slice with its own `controller/dto/entity/repository/service` sub-packages. When working on a feature, everything relevant is almost always confined to that one package plus shared code in `common/`.

**`common/`** holds cross-cutting code shared by every domain: `config/` (Spring `@Configuration` classes — security, caching, async, rate limiting, Cloudinary, ShedLock, OpenAPI), `security/` (`JwtAuthFilter` + `JwtService` for stateless JWT auth), `exception/` (global exception handling), `filter/` (request-level filters, e.g. correlation IDs), `util/`, and shared `dto/`/`entity/` base types.

**API surface** is split by auth requirement, enforced in `SecurityConfig`:
- `/api/v1/public/**` — no auth
- `/api/v1/auth/**` — no auth (login/register)
- `/api/v1/customer/**` — Bearer, `ROLE_CUSTOMER`
- `/api/v1/admin/**` — Bearer, `ROLE_ADMIN`/`ROLE_STAFF`
- `/api/v1/cart/**`, `/api/v1/checkout` — no auth (guest + authenticated both supported)
- `/api/v1/orders/track/**` — no auth
- `/api/v1/payments/**` — varies by endpoint

**Scheduled jobs** live in `jobs/` (one class per job: abandoned-cart reminders, low-stock alerts, Riseller catalog/stock sync, refresh-token sweep, tax-document cleanup, document-bundle cleanup, temp-password expiry, monthly counter resets, lead digest emails, product-click roll-up). These run under ShedLock (`ShedLockConfig`, JDBC-backed) so they're safe with multiple instances.

**Integrations:**
- **PayHero** — M-Pesa STK push payment provider (`payment/`); env-configured, sandbox vs production via `PAYHERO_ENV`.
- **Africa's Talking** — SMS, currently stubbed/disabled by default (`AT_ENABLED=false`); email (Brevo SMTP) is the primary notification channel (`email/`, `notification/`).
- **Cloudinary** — product/media image storage.
- **DigitalOcean Spaces** (via AWS S3 SDK) — file uploads (`upload/`).
- **openhtmltopdf** — renders Thymeleaf HTML templates to PDF, so tax invoices/receipts reuse the same templating engine as emails instead of a second one.

**Caching** is Caffeine (in-memory, `CacheConfig`). **Rate limiting** is Bucket4j (`RateLimitConfig`).

**Riseller** is a third-party catalog the platform syncs against (see `RisellerCatalogSyncJob`, `RisellerStockSyncJob`, `RisellerProperties`) — treat product/stock data as partially externally-owned when touching that flow.

Seeded accounts (both ADMIN + STAFF): `pkihara2008@gmail.com`, `mdaucodes@gmail.com` — passwords are set via `SUPERADMIN_PASSWORD` / `DEV_ADMIN_PASSWORD` env vars, no hardcoded default in source you should rely on.

## Current focus (as of 2026-07-23)

Building an analytics dashboard in phases (`analytics/` package + matching frontend pages): revenue/payment KPIs, order funnel, tax reporting, reward coupons/referral economics, products & inventory, profitability, monthly projections, customer analytics, geographic/delivery breakdowns, and alerts — each phase adds endpoints here paired with a frontend page in the sibling `moments-non-lovable-project` repo.
