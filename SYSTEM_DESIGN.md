# Moments Packaging Kenya — Backend System Design

This document is for a new developer joining the project. It explains *why* the system is shaped the way it is, not just *what* is in each file — the goal is that after reading this, you can find your way to the right code for a given change without a guided tour.

For day-to-day commands (build/run/test) see [CLAUDE.md](CLAUDE.md) and [README.md](README.md). This document is the deeper architectural picture.

## 1. What this system is

A self-serve B2C/B2B e-commerce platform for a Nairobi packaging wholesaler. A customer (individual shopper or business account) browses a catalogue, configures an order (quantities, tiers), checks out, and pays via M-Pesa (Safaricom Daraja STK push), bank transfer, or cash on delivery. Staff then move the order through a fulfilment pipeline (verify payment → produce → dispatch → deliver) from an internal admin dashboard. Enterprise orders (10,000+ units) skip checkout entirely and go to a sales-quote flow.

Two live surfaces consume this API: the public storefront + customer account area, and the staff-only admin dashboard — both are the sibling frontend repo (`moments-non-lovable-project`), a single React app with routes split by auth requirement.

## 2. Tech stack and why

| Layer | Choice | Notes |
|---|---|---|
| Framework | Spring Boot 3 / Java 21 | |
| Auth | Stateless JWT (jjwt) | No server-side session; the token itself carries roles/permissions/staffRole so the frontend can gate UI without a round-trip |
| DB | PostgreSQL, Hibernate `ddl-auto: update` | Schema is *derived from entities*, not hand-written migrations — there is no Flyway/Liquibase. This matters: the schema only exists reproducibly because the entity classes are in git. |
| Scheduling | Spring `@Scheduled` + ShedLock (JDBC-backed) | ShedLock exists specifically so a job doesn't double-run if this is ever scaled to multiple instances |
| Caching | Caffeine (in-memory) | Single-instance assumption — see §7 |
| Rate limiting | Bucket4j, per-IP, in-memory | Same single-instance caveat |
| File storage | Cloudinary (images) + DigitalOcean Spaces via S3 SDK (uploads) | Two different object stores for historical/vendor reasons — check which one a given feature actually uses before assuming |
| PDF generation | openhtmltopdf, rendering Thymeleaf templates | Chosen so tax invoices/receipts reuse the *same* templating engine as transactional emails, instead of a second PDF library with its own template syntax |
| Payments | PayHero (M-Pesa Daraja wrapper) | The only M-Pesa integration; Daraja-direct credentials exist but aren't wired in (see README "Pending") |

## 3. Architecture: feature packages, not technical layers

Code under `src/main/java/.../momentspackagingbackendjavafirstclient/` is organized **by business domain**, each a vertical slice with its own `controller/dto/entity/repository/service`:

```
order/  payment/  product/  cart/  customer/  auth/  referral/
taxdocument/  documentbundle/  analytics/  receipt/  enquiry/
lead/  business/  blog/  industry/  taxonomy/  tag/  settings/
audit/  user/  backup/
```

If you're changing "how orders work," everything you need is almost always inside `order/` plus shared code in `common/`. This is deliberate — it optimizes for "I need to change one feature" over "I need to see all controllers."

`common/` holds what every domain needs: `config/` (Spring `@Configuration` — security, CORS, caching, rate limiting, Cloudinary, ShedLock, OpenAPI), `security/` (JWT filter + service), `exception/` (global handler), `filter/` (correlation IDs), `util/`.

## 4. Request lifecycle (authenticated request)

```
Client                JwtAuthFilter                 SecurityConfig            Controller
  │  Authorization: Bearer <jwt>                                                  │
  ├──────────────────────►│                                                       │
  │                       │ extract username from JWT                            │
  │                       │ load UserDetails (DB lookup)                         │
  │                       │ validate signature + expiry + username match         │
  │                       │ set SecurityContext (roles, authorities)             │
  │                       ├──────────────────────►│                              │
  │                       │                        │ authorizeHttpRequests rules │
  │                       │                        │ (path prefix + @IsAdmin /   │
  │                       │                        │  @IsStaffOrAdmin method     │
  │                       │                        │  annotations)               │
  │                       │                        ├─────────────────────────────►│
  │                       │                        │                             │  handles request
  │◄──────────────────────┴────────────────────────┴─────────────────────────────┤
```

The JWT itself carries `userId`, `roles`, `staffRole`, `permissions`, `accountType` — minted once at login (`JwtService.generateAccessToken`) and never re-verified against the DB's *current* permission set until the token expires. If you change someone's role, they keep their old permissions until their token expires or they log in again. There's no server-side revocation of an issued access token (refresh tokens are separately revocable — see `RefreshTokenSweepJob`).

**Admin impersonation** ("Preview dashboard" for support) mints a separate, deliberately short-lived token flagged with `impersonatedBy`, reusing the exact same claim shape so the frontend's auth/routing code doesn't need a separate code path — see `JwtService.generateImpersonationToken`.

## 5. The order lifecycle — the most important state machine in the system

```
PENDING_PAYMENT → PAID → PAYMENT_VERIFIED → IN_PRODUCTION → READY_FOR_DISPATCH → DISPATCHED → DELIVERED
       │                                                                                │
       └──────────────────────────► CANCELLED                          REFUNDED ◄──────┘
```

- **PENDING_PAYMENT → PAID**: automatic for M-Pesa via the Daraja callback webhook (`/api/v1/payments/daraja/callback/**`, public — Safaricom can't send a bearer token); manual for bank transfer/COD (staff confirm in the admin Payments queue).
- **PAID → PAYMENT_VERIFIED**: staff action in the admin Payment Queue (`AdminOrderController`, `PATCH /{id}/status`) — this exists as a distinct step from "paid" because M-Pesa confirmation isn't treated as fully trusted until a human looks at it.
- **PAYMENT_VERIFIED → IN_PRODUCTION → READY_FOR_DISPATCH**: staff actions in the Preparation Queue.
- **READY_FOR_DISPATCH → DISPATCHED**: gated behind a *checklist* (`PATCH /{id}/dispatch-confirm`) — every line item must be ticked verified before dispatch is allowed, tracked per-item, not just a single confirm click.
- **DISPATCHED → DELIVERED**: manual admin action on the order detail page ("Mark delivered") — there's no customer-facing delivery confirmation or courier webhook driving this.
- **Cancel** and **Refund** are side-paths, not part of the linear chain, and refund itself is two-gated: `refund-request`/`refund-request/resolve` are staff-or-admin (just logs the complaint, no side effects), while `mark-payment-refunded` and `restore-inventory` are `@IsAdmin`-only, since those actually move money/stock.

Every transition is audit-logged (actor + timestamp) and visible in the order detail drawer's status history — this has been verified working end-to-end, including the checklist-gated dispatch step.

**Known gap**: there is no job that expires or flags orders stuck in `PENDING_PAYMENT` indefinitely (failed M-Pesa attempts, abandoned bank-transfer intents). Orders from weeks ago with real customer contact info can sit forever with no cleanup, unlike `AbandonedCartJob` which does exist for carts. Worth adding if this becomes a real op.

## 6. Rewards / Coupons — the other system with real money implications

Every account earns 1,000 coupons (worth KES 100) on signup, coupons per KES spent on paid orders, and coupons per product review. Conversion rate, redemption cap, and welcome-code terms are **all admin-editable at runtime** (`settings` domain), not hardcoded — see `AdminAnalyticsRevenueController`'s sibling settings endpoints and the admin Settings page. A promo code and a coupon redemption **cannot both apply to one order** — this is enforced both in the checkout UI (hides whichever isn't in use) and independently in the backend (rejects the combination), so don't assume the frontend guard is the only protection when touching checkout pricing logic.

Business Accounts additionally get a one-time 5%-off welcome code once their business profile is completed, single-use, tied to a minimum order amount and validity window — also admin-configurable.

## 7. Scheduled jobs

All in `jobs/`, all ShedLock-guarded:

| Job | Cadence | Purpose |
|---|---|---|
| `AbandonedCartJob` | — | Reminder emails for abandoned carts |
| `DatabaseBackupJob` | Daily 2am | Data-only backup of every table to Cloudinary (added — see §9) |
| `DocumentBundleCleanupJob` | — | Expires old receipt/tax-invoice/ETR bundles |
| `LeadDigestEmailJob` | — | Digest email of new sales leads |
| `LowStockAlertJob` | — | Alerts on low/out-of-stock inventory |
| `RefreshTokenSweepJob` | — | Purges expired refresh tokens |
| `ResetMonthlyCountersJob` | — | Resets monthly usage counters (referral caps, etc.) |
| `RisellerCatalogSyncJob` / `RisellerStockSyncJob` | — | Syncs product catalogue/stock against the third-party Riseller feed — treat that data as partially externally-owned |
| `RollUpProductClicksJob` | — | Aggregates product-click analytics |
| `TaxDocumentCleanupJob` | Weekly (Fri 3am) | Deletes Cloudinary PDFs for tax invoices sent 2+ weeks ago |
| `TempPasswordExpiryJob` | — | Expires unused temporary passwords |

ShedLock means these are safe under multiple instances *for the lock itself*, but Caffeine caching and Bucket4j rate limiting are still per-instance in-memory state — if this ever scales horizontally, those two need to move to a shared store (Redis) first.

## 8. Security posture (see also the repo-level security review from 2026-07-23)

- BCrypt password hashing, stateless JWT, method-level `@IsAdmin`/`@IsStaffOrAdmin` annotations on sensitive actions.
- Per-IP rate limiting on login/checkout/payment/cart/enquiry endpoints (`RateLimitConfig`) — note it trusts `X-Forwarded-For` at face value with no trusted-proxy validation; confirm the deployment's edge actually sets this header correctly, or it's spoofable.
- **`src/main/resources/.env` was tracked in git** with real secrets (JWT signing key, DB password, Cloudinary keys) — untracked going forward as of 2026-07-23, but the values were live in history and must still be rotated (new JWT secret already generated; DB password and Cloudinary keys need rotating via Railway/Cloudinary dashboards) and the history purged (needs collaborator coordination first — force-push territory).
- `README.md` documents the seeded superadmin accounts' real emails and default password pattern — also worth scrubbing once rotation is done.
- **Order references are sequential and guessable** (`ORD-2026-07-0001`, `-0002`, ...) — `GET /orders/track/{reference}` used to return full financials/PII on reference alone. Fixed 2026-07-23: reference-only now returns status/progress only; full details (items, pricing, contact name, delivery address) require an `?email=` param matching the order's own email (`OrderService.getTrackingInfo`), itself rate-limited the same as `/by-email` since it's now a match/no-match oracle.

## 9. Backups

`DatabaseBackupJob` (daily, 2am Africa/Nairobi) dumps every table in the `public` schema (except ShedLock's own bookkeeping table) to one JSON file per table, zips them with a manifest, and uploads the archive to Cloudinary as a raw resource under `system-backups/`. Filenames spell out the full moment down to the second plus UTC offset and zone name (e.g. `backup-2026-07-23T02-00-00+03-00-Africa-Nairobi`) so the Cloudinary console alone tells you exactly when a backup ran, no need to open the archive.

**Retention**: every run keeps only the 2 most recent backups (the one just created plus the one immediately before it) and deletes anything older, via the Cloudinary Admin API (`DatabaseBackupService.enforceRetention`). This self-corrects even if a run is missed for a few days — it'll just delete more than one stale backup at once rather than leaving them.

This is **data-only, not a full pg_dump-equivalent** — schema isn't included, because the schema is fully reproducible from the JPA entity classes already in git (`ddl-auto: update`). To restore: deploy the app fresh (schema gets created), then re-insert each table's JSON rows in the manifest's table order (parent tables before dependents). There is no automated restore tool yet — writing one is a natural next step if this is ever needed for real.

## 10. Deployment topology

- App: Railway (production URL referenced in `enrich-products.mjs` as `moments-packaging-latest-backend-production.up.railway.app`).
- DB: PostgreSQL, also presumably Railway-hosted in production (local dev points at `localhost:5432`).
- Cloudinary: product images, raw document uploads (tax invoices, receipts, DB backups).
- DigitalOcean Spaces (S3-compatible): general file uploads, separate from Cloudinary.
- Email: Brevo SMTP (transactional emails — order confirmations, receipts, staff digests).
- SMS: Africa's Talking, currently disabled by default (`AT_ENABLED=false`) — email is the only live notification channel.
- Frontend: separate Vite/React static build, calling this API's `/api/v1/**` routes over CORS (`app.cors.allowed-origins`).

## 11. Where to look for X

- "Why does this order have this status" → `order/entity/OrderStatus.java` + `order/controller/AdminOrderController.java` + the order detail drawer's status history in the frontend.
- "How do coupons/discounts actually get calculated" → `referral/` domain + the admin Settings-editable values referenced in §6.
- "Why is this analytics number wrong/empty" → `analytics/controller/AdminAnalyticsRevenueController.java` — note the separate legacy `settings/controller/AdminAnalyticsController.java` (`/analytics/overview`) is still live and intentionally kept alongside the newer per-metric endpoints.
- "Something to do with images/files" → check both `upload/` (Cloudinary + S3) and whether the feature you're touching uses images (Cloudinary, validated) vs raw documents (Cloudinary raw, unvalidated) vs general uploads (S3).
