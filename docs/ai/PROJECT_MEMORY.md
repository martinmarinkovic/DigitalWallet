# PROJECT_MEMORY

## 1. Product Overview
Digital Wallet is a secondary wallet for everything except payment cards. Its purpose is to reduce clutter in Apple Wallet and Google Wallet by moving non-payment cards into a dedicated app with fast, category-based access.

## 2. Core Product Principles
- Offline-first by default.
- Category-first organization.
- Every card must belong to a physical category.
- Fast access is more important than feature density.
- UI must stay minimal, clean, and uncluttered.
- No payment flows or payment-card concepts.
- Avoid wallet clutter and avoid generic “store everything” behavior.

## 3. MVP Scope
- Home screen with category grid only.
- Pinned `Favorites` collection on Home, implemented as a virtual category backed by `WalletCard.isFavorite`.
- Default categories plus custom category creation.
- Inline animated search on Home only.
- Category Details screen.
- Add Card entry point and method flows.
- Card Preview / Edit flow before save when needed.
- Card Details screen.
- Fullscreen Code View.
- Reordering for categories and cards.
- Theme support: Light / Dark / System.
- Auto brightness option for fullscreen code view.
- Expiration reminders.
- Backup, Restore, and Export.
- Google Wallet import for supported cases only.
- Real release-ready Privacy Policy and Terms destinations.

## 4. Explicit Non-Goals
- Tags.
- Filters.
- Separate Search screen.
- Nearby or location-based features.
- Recently used.
- Usage-based sorting.
- Auto organization or smart categorization.
- Full-wallet scanner.
- Attachments.
- Card sharing.
- Wear OS in MVP.
- Cloud Sync in the current MVP release.
- Payment cards or payment flows.

## 5. Navigation Structure
- Bottom bar destinations must be exactly:
  - Home
  - Add Card
  - Settings
- Bottom bar must be visible only on the three top-level destinations above.
- Bottom bar must be hidden on secondary flows such as:
  - Category Details
  - Card Details
  - Card Preview / Edit
  - Fullscreen Code View
  - Add Card method subflows
- Additional routes may exist for:
  - Category Details
  - Add Card Method Flows
  - Card Preview / Edit
  - Card Details
  - Fullscreen Code View
- Home is the primary browsing surface.
- Search is inline on Home and must not become a separate destination.

## 6. Screen Inventory
- Home
  - Search icon in top bar on the left.
  - Add-category icon in top bar on the right.
  - Search expands inline with animation.
  - Previous searches are shown when search is expanded.
  - No filters.
  - No nearby.
  - No recently used.
  - No add-card shortcut on Home.
  - Content is category grid only.
  - First category is `Favorites`.
  - `Favorites` is virtual and derived from `isFavorite`; it is not a physical category users assign cards into.
  - `+ New Category` tile is always last.
- Category Details
  - Back button on the left.
  - Title centered.
  - Add-card action on the right.
  - Cards shown in a 2-column grid.
  - Long press enables reorder and context actions.
- Add Card
  - Entry point from bottom bar.
- Add Card Method Flows
  - Scan barcode / QR code.
  - Scan card photo.
  - Manual entry.
  - Smart scanning.
  - Import from Google Wallet.
- Card Preview / Edit
  - Final review/edit step before save when applicable.
- Card Details
  - Read-only primary detail view with actions to edit, show code, or manage reminders.
- Fullscreen Code View
  - Focused full-screen presentation for barcode / QR access.
- Settings
  - Light / Dark / System
  - Auto brightness
  - Backup
  - Restore
  - Export
  - Expiration reminders
  - Clear search history
  - Privacy policy
  - Terms
  - App version
- Create Category Dialog / Bottom Sheet
  - User creates custom categories with required name and color.

## 7. Data Model Overview
- `Category`
  - Physical organizational bucket for cards.
  - Fields should support stable id, name, color, default/custom flag, and persistent sort order.
- `Card`
  - Represents a non-payment wallet item only.
  - Must always reference a physical category.
  - Should support stable id, category id, title, code data, code type, optional descriptive fields, optional expiration data, and persistent sort order.
  - `isFavorite` is the source of truth for membership in the virtual `Favorites` collection.
- `SearchHistoryEntry`
  - Stores recent Home searches for inline recall.
- `ReminderConfig`
  - Stores reminder behavior for expiration-aware cards.
- `AppSettings`
  - Stores theme, auto brightness, reminder preferences, and related persistent settings.

## 8. Architecture Rules
- Kotlin + Jetpack Compose.
- Room local database.
- Repository Pattern.
- Hilt DI.
- MVI state management per screen/feature.
- Feature-focused packages where possible.
- Stable models and clean unidirectional state flow.
- Room is the local source of truth; repositories abstract local and remote sources.
- Keep feature boundaries clean and avoid cross-feature leakage.
- `Favorites` must be implemented as a derived virtual collection from `WalletCard.isFavorite`, not as stored category membership.
- Cards must always persist against a non-virtual physical category.

## 9. UX Rules
- Optimize for fast retrieval, not dense browsing.
- Keep surfaces minimal and visually calm.
- Home is category-first, not card-first.
- `Favorites` must behave like a pinned smart collection for quick access, not like a separate saved category assignment.
- Do not add shortcut actions that compete with the bottom navigation contract.
- Do not introduce clutter features such as tags, filters, nearby, or recent activity in MVP.
- Reorder interactions must feel intentional and persistent.
- Fullscreen code access should be frictionless and focused.

## 10. Persistence Rules
- Room is the primary local source of truth.
- Reorder state must persist.
- Search history must persist.
- Reminder settings must persist.
- No card may exist without a category.
- No card may persist with `Favorites` as its stored category; `Favorites` is derived from `isFavorite`.
- Local behavior must work without network access.

## 11. Cloud Sync Scope
- Deferred to V1.1.
- Not part of the current MVP release.
- Any existing local sync foundation must not be presented as working release sync until a real backend is connected.
- When implemented later, Room must remain the primary local source of truth.
- Future sync scope should cover categories, cards, ordering, and syncable settings only.

## 12. Reminder Scope
- Reminders apply only to expiration-aware cards.
- Supported offsets:
  - On day
  - 1 day before
  - 3 days before
  - 7 days before
- MVP reminder flow must include notification-permission handling on supported Android versions.

## 13. Google Wallet Import Scope
- Support only importable and explicitly supported cases.
- Do not assume full or unrestricted import for all wallet items.
- Import must stay aligned with the non-payment-card scope.

## 14. V1.1 Deferred Features
- Cloud Sync with a real backend-connected implementation.
- Wear OS.
- Card sharing.
- Optional icons for custom categories.
- Possible attachments only if later validated.

## Current Status
- The app has a working local-first shell and implemented MVP feature set for Home, Add Card flows, Category Details, Card Details, Fullscreen Code View, Settings, backup/restore/export, theme persistence, and expiration reminders.
- Release triage decisions now define `Cloud Sync` as V1.1 scope, not current MVP scope.
- Release triage decisions now define `Favorites` as a pinned virtual collection backed by `WalletCard.isFavorite`, not a physical category membership.
- Release triage decisions now require bottom navigation to appear only on `Home`, `Add Card`, and `Settings`.
- Release triage decisions now require notification-permission handling for expiration reminders and real Privacy Policy / Terms destinations before release.
- Current code still needs alignment with those release decisions in a follow-up implementation pass.
