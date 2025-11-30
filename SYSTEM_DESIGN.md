# System Design – UI & Flows (Job Card Management)

This document describes the current user-facing surfaces (screens, cards, dialogs) and what they do. The app is single-technician, offline-first; statuses are driven by `statusHistory` on jobs and fixed assets.

## Screens

- **Dashboard**
  - **Stats row**: Available, Awaiting, Active, Pending counts; taps navigate to Jobs with the corresponding status filter.
  - **Current Job card**: Shows the one active job (EN_ROUTE/BUSY/PAUSED). Actions: Arrived/Begin (EN_ROUTE→BUSY), Pause (BUSY→PAUSED with reason dialog), En Route (PAUSED/PENDING→EN_ROUTE), Resume (PAUSED→BUSY), Cancel (with reason), Complete (opens detail), Add Photo (camera/gallery), Add Material (opens material dialog).
  - **Paused Jobs list**: Paused jobs with Resume/En Route and detail navigation.
  - **Awaiting/Pending lists**: Awaiting jobs with Accept (to PENDING), Pending jobs with Start (to EN_ROUTE) and detail navigation.
  - **Asset dialogs**: Photo capture dialog; Add Material dialog (inventory to job, fixed checkout).
- **Jobs**
  - Search bar with clear; horizontal status chips (All or specific status).
  - Job list rows: job number, title, customer, address, scheduled date/time, status chip; taps open Job Detail.
  - No create/plus action (legacy create removed).
- **Job Detail**
  - Job info card: title, job type, schedule, duration, location link; status chip.
  - Customer info card: name, phone (tap-to-dial), email (mailto), addresses.
  - Photos card: unified before/during/after with add (camera/gallery), tag/notes/edit/remove.
  - Materials Used card: list of inventory usage for the job.
  - Purchases card: list/add purchases, single receipt per purchase (capture/gallery/view/remove).
  - Job Timeline: status history.
  - Work & Notes card (last): work performed, technician notes, issues, travel distance (km), follow-up flag/notes, complete job button. Autosaves with debounce.
- **Inventory Assets**
  - Tabs: Inventory / Fixed.
  - Search and category filters (inventory) or type filters (fixed).
  - Inventory list (no grouping): each item opens “Add to Job” dialog to select an active job (EN_ROUTE/BUSY/PAUSED) and quantity; records usage and decrements stock; low-stock badge shown.
  - Fixed list: card per asset with checkout button (opens fixed checkout dialog) and details dialog.
- **Settings**
  - Minimal About card: version (local build), support contact.

## Dialogs / Components

- **PhotoCaptureDialog**: Camera/gallery selection, category/notes capture.
- **AddJobMaterialDialog**: Add inventory to current job or checkout fixed asset; shows available inventory/fixed filtered by search.
- **AddAssetToJobDialog** (Inventory screen): Quantity + active job picker (EN_ROUTE/BUSY/PAUSED) to log usage.
- **FixedCheckoutDialog**: Checkout fixed asset with reason, optional job number entry, condition dropdown, notes.
- **ReasonDialog**: Reusable text-input dialog for pause/cancel.
- **Receipt options**: View/capture/pick/remove single receipt per purchase.

## Status Rules (frontend expectations)

- Only one job can be EN_ROUTE or BUSY at a time (repository enforces).
- Accept (AVAILABLE/AWAITING) → PENDING; Start from PENDING → EN_ROUTE; Arrive (EN_ROUTE) → BUSY; Pause BUSY → PAUSED; Resume PAUSED → BUSY; En Route from PAUSED/PENDING/AWAITING → EN_ROUTE; Cancel/Complete update status history.

## Data Notes (UI-facing)

- Photos and receipts are FileProvider URIs copied into app storage; photos are JSON arrays on jobs, receipts are fields on purchases.
- Travel distance is technician-entered in Work & Notes (not shown in header).
- Inventory usage writes to `job_inventory_usage` and deducts stock; fixed checkout writes status history and checkout rows.
