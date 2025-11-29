# Database Design (v22, schema recommendation applied)

## Overview
- **Database**: `jobcard_database`
- **Version**: 23 (clean reset/reseed)
- **ORM**: Room (AndroidX) with `fallbackToDestructiveMigration` (add migrations before production)
- **Workflow source of truth**: `statusHistory` JSON on `job_cards` and `fixed_assets`
- **Media**: Job photos stored as JSON arrays of `{ uri, notes }`; receipts stored on `job_purchases` as single receipt fields. Picked images are copied into app storage (receipts under `.../files/Pictures/receipts`).
- **Single-tech app**: `technician` table holds the single logged-in user (id=1)

## Tables

| Table | Purpose | Key Fields | Notes |
|-------|---------|------------|-------|
| `technician` | Singleton technician profile | `id=1`, `username`, `authToken`, `lastSyncTime` | Always exactly one row |
| `customers` | Customer master data | `name`, `phone`, `email`, `address`, `area` | Job cards denormalize these for offline use |
| `job_cards` | Core jobs | `jobNumber` (UQ), `statusHistory`, `priority`, photos, feedback | Status values: AVAILABLE, AWAITING, PENDING, EN_ROUTE, BUSY, PAUSED, COMPLETED, SIGNED, CANCELLED |
| `inventory_assets` | Consumables/parts | `itemCode` (UQ), `currentStock`, `minimumStock`, `unitOfMeasure` | Low stock when `currentStock <= minimumStock` |
| `fixed_assets` | Tools/equipment | `fixedCode` (UQ), `fixedType`, `statusHistory`, `isAvailable`, `currentHolder` | Availability comes from latest status event + open checkouts |
| `job_inventory_usage` | Consumables used per job | `jobId`, `inventoryId`, `quantity`, `unitOfMeasure`, `recordedAt` | Drives stock deductions |
| `job_fixed_assets` | Fixed assets checked out per job | `jobId`, `fixedId`, `reason`, `checkoutTime`, `returnTime`, `condition` | Tracks who used what and when |
| `job_purchases` | Purchases made for a job | `jobId`, `vendor`, `totalAmount`, `notes`, `purchasedAt`, `receiptUri`, `receiptMimeType`, `receiptCapturedAt` | Receipt fields live on the purchase row (one-to-one) |

## Schema snippets

```sql
CREATE TABLE job_cards (
    id INTEGER PRIMARY KEY,
    job_number TEXT NOT NULL UNIQUE,
    customer_name TEXT NOT NULL,
    customer_phone TEXT NOT NULL,
    customer_email TEXT,
    title TEXT NOT NULL,
    description TEXT,
    job_type TEXT NOT NULL,
    priority TEXT NOT NULL DEFAULT 'NORMAL',
    status_history TEXT NOT NULL DEFAULT '[]',
    scheduled_date TEXT,
    scheduled_time TEXT,
    estimated_duration INTEGER,
    service_address TEXT NOT NULL,
    latitude REAL,
    longitude REAL,
    travel_distance REAL,
    work_performed TEXT,
    technician_notes TEXT,
    issues_encountered TEXT,
    customer_signature TEXT,
    before_photos TEXT,
    after_photos TEXT,
    other_photos TEXT,
    requires_follow_up INTEGER NOT NULL DEFAULT 0,
    follow_up_notes TEXT,
    customer_rating INTEGER,
    customer_feedback TEXT,
    is_synced INTEGER NOT NULL DEFAULT 0,
    last_synced_at INTEGER,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
CREATE INDEX idx_job_cards_status_date ON job_cards(scheduled_date);
```

```sql
CREATE TABLE inventory_assets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_code TEXT NOT NULL UNIQUE,
    item_name TEXT NOT NULL,
    category TEXT NOT NULL,
    current_stock REAL NOT NULL,
    minimum_stock REAL NOT NULL,
    unit_of_measure TEXT NOT NULL,
    is_synced INTEGER NOT NULL DEFAULT 0,
    last_synced_at INTEGER,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
CREATE INDEX idx_inventory_assets_category ON inventory_assets(category);
```

```sql
CREATE TABLE fixed_assets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    fixed_code TEXT NOT NULL UNIQUE,
    fixed_name TEXT NOT NULL,
    fixed_type TEXT NOT NULL,
    serial_number TEXT,
    manufacturer TEXT,
    model TEXT,
    status_history TEXT NOT NULL DEFAULT '[]',
    is_available INTEGER NOT NULL DEFAULT 1,
    current_holder TEXT,
    last_maintenance_date INTEGER,
    next_maintenance_date INTEGER,
    notes TEXT,
    is_synced INTEGER NOT NULL DEFAULT 0,
    last_synced_at INTEGER,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
CREATE INDEX idx_fixed_assets_type ON fixed_assets(fixedType);
```

```sql
CREATE TABLE job_inventory_usage (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id INTEGER NOT NULL,
    inventory_id INTEGER NOT NULL,
    item_code TEXT NOT NULL,
    item_name TEXT NOT NULL,
    quantity REAL NOT NULL,
    unit_of_measure TEXT NOT NULL,
    recorded_at INTEGER NOT NULL,
    FOREIGN KEY(job_id) REFERENCES job_cards(id) ON DELETE CASCADE,
    FOREIGN KEY(inventory_id) REFERENCES inventory_assets(id) ON DELETE CASCADE
);
CREATE INDEX idx_job_inventory_usage_job ON job_inventory_usage(job_id);
```

```sql
CREATE TABLE job_fixed_assets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id INTEGER,
    fixed_id INTEGER NOT NULL,
    fixed_code TEXT NOT NULL,
    fixed_name TEXT NOT NULL,
    reason TEXT NOT NULL,
    technician_id INTEGER NOT NULL,
    technician_name TEXT NOT NULL,
    checkout_time INTEGER NOT NULL,
    return_time INTEGER,
    condition TEXT NOT NULL DEFAULT 'Good',
    return_condition TEXT,
    notes TEXT,
    FOREIGN KEY(job_id) REFERENCES job_cards(id) ON DELETE CASCADE,
    FOREIGN KEY(fixed_id) REFERENCES fixed_assets(id) ON DELETE CASCADE
);
CREATE INDEX idx_job_fixed_assets_job ON job_fixed_assets(job_id);
```

```sql
CREATE TABLE job_purchases (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id INTEGER NOT NULL,
    vendor TEXT NOT NULL,
    total_amount REAL NOT NULL,
    notes TEXT,
    purchased_at INTEGER NOT NULL,
    receipt_uri TEXT,
    receipt_mime_type TEXT,
    receipt_captured_at INTEGER,
    FOREIGN KEY(job_id) REFERENCES job_cards(id) ON DELETE CASCADE
);
```

## Notes / Policies
- App logic enforces one receipt per purchase by clearing previous rows before insert/update.
- Gallery selections for receipts are copied into app storage; URIs persisted as FileProvider strings to avoid broken content links.
- Job timeline pulls from `statusHistory`; resume/pause/start events append to that JSON trail.
- Fixed-asset availability is derived from `statusHistory` plus open `job_fixed_assets` rows; the sample seed sets explicit status history.

## Current Gaps / Improvement Ideas
- Add Room migrations (currently destructive) and enable `exportSchema=true` for migration tests.
- Normalize photos into a `job_photos` table (category + notes) to remove JSON handling and simplify sync/conflict resolution.
- Add a unique index on `job_purchases(id)` is implicit; receipt fields are single-instance. If multiple receipts are needed later, split into a dedicated table.
- Store relative media paths (vs. full `file://` URIs) plus metadata (size/hash) to support backup/restore and sync validation.
- Consider triggers or app-level validation to keep `statusHistory` monotonic and to reject out-of-order status changes.
