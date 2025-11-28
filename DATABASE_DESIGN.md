# Database Design (v20, normalized reset)

## Overview
- **Database**: `jobcard_database`
- **Version**: 20 (clean reset/reseed)
- **ORM**: Room (AndroidX)
- **Architecture**: MVVM + Repository, offline-first
- **Sync flags**: `isSynced`/`lastSyncedAt` on mutable tables
- **Status history**: Stored as JSON on `job_cards` and `fixed_assets` for full workflow/audit

## Tables

| Table | Purpose | Key Fields | Notes |
|-------|---------|------------|-------|
| `technician` | Singleton current technician | `id=1`, `username`, `authToken` | Replaces legacy current_technician/technicians |
| `customers` | Master customers | `name`, `phone`, `email`, `address` | Denormalized copies live in job cards |
| `job_cards` | Core jobs | `jobNumber` (UQ), `status`, `priority`, `statusHistory`, photos, feedback | Status values: AVAILABLE, AWAITING, PENDING, EN_ROUTE, BUSY, PAUSED, COMPLETED, SIGNED, CANCELLED |
| `inventory_assets` | Consumables/parts | `itemCode` (UQ), `currentStock`, `minimumStock`, `unitOfMeasure` | Low-stock based on `currentStock <= minimumStock` |
| `fixed_assets` | Fixed assets/tools/equipment | `fixedCode` (UQ), `fixedType`, `statusHistory`, `isAvailable`, `currentHolder` | Availability derived from last status event |
| `job_inventory_usage` | Consumables used per job | `jobId`, `inventoryId`, `quantity`, `unitOfMeasure` | Drives stock deductions |
| `job_fixed_assets` | Fixed assets checked out per job | `jobId?`, `fixedId`, `reason`, `checkoutTime`, `returnTime`, `condition` | Tracks holder + return condition |
| `job_purchases` | Purchases for a job | `jobId`, `vendor`, `totalAmount`, `purchasedAt` | |
| `purchase_receipts` | Receipts attached to purchases | `purchaseId`, `uri`, `mimeType`, `notes`, `capturedAt` | |

## Schema snippets

```sql
-- job_cards
CREATE TABLE job_cards (
    id INTEGER PRIMARY KEY,
    jobNumber TEXT NOT NULL UNIQUE,
    customerId INTEGER NOT NULL,
    customerName TEXT NOT NULL,
    customerPhone TEXT NOT NULL,
    customerEmail TEXT,
    customerAddress TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    jobType TEXT NOT NULL,
    priority TEXT NOT NULL DEFAULT 'NORMAL',
    status TEXT NOT NULL,
    statusHistory TEXT NOT NULL DEFAULT '[]',
    scheduledDate TEXT,
    scheduledTime TEXT,
    estimatedDuration INTEGER,
    serviceAddress TEXT NOT NULL,
    latitude REAL,
    longitude REAL,
    travelDistance REAL,
    navigationUri TEXT,
    acceptedAt INTEGER,
    enRouteStartTime INTEGER,
    startTime INTEGER,
    endTime INTEGER,
    pausedTime INTEGER,
    pauseHistory TEXT,
    cancelledAt INTEGER,
    cancellationReason TEXT,
    workPerformed TEXT,
    technicianNotes TEXT,
    issuesEncountered TEXT,
    customerSignature TEXT,
    beforePhotos TEXT,
    afterPhotos TEXT,
    otherPhotos TEXT,
    resourcesUsed TEXT,
    requiresFollowUp INTEGER NOT NULL DEFAULT 0,
    followUpNotes TEXT,
    customerRating INTEGER,
    customerFeedback TEXT,
    isSynced INTEGER NOT NULL DEFAULT 0,
    lastSyncedAt INTEGER,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL
);
CREATE INDEX idx_job_cards_status_date ON job_cards(status, scheduledDate);
```

```sql
-- inventory_assets
CREATE TABLE inventory_assets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    itemCode TEXT NOT NULL UNIQUE,
    itemName TEXT NOT NULL,
    category TEXT NOT NULL,
    currentStock REAL NOT NULL,
    minimumStock REAL NOT NULL,
    unitOfMeasure TEXT NOT NULL,
    isSynced INTEGER NOT NULL DEFAULT 0,
    lastSyncedAt INTEGER,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL
);
CREATE INDEX idx_inventory_assets_category ON inventory_assets(category);
```

```sql
-- fixed_assets
CREATE TABLE fixed_assets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    fixedCode TEXT NOT NULL UNIQUE,
    fixedName TEXT NOT NULL,
    fixedType TEXT NOT NULL,
    serialNumber TEXT,
    manufacturer TEXT,
    model TEXT,
    statusHistory TEXT NOT NULL DEFAULT '[]',
    isAvailable INTEGER NOT NULL DEFAULT 1,
    currentHolder TEXT,
    lastMaintenanceDate INTEGER,
    nextMaintenanceDate INTEGER,
    notes TEXT,
    isSynced INTEGER NOT NULL DEFAULT 0,
    lastSyncedAt INTEGER,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL
);
CREATE INDEX idx_fixed_assets_type ON fixed_assets(fixedType);
```

```sql
-- job_inventory_usage
CREATE TABLE job_inventory_usage (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    jobId INTEGER NOT NULL,
    inventoryId INTEGER NOT NULL,
    itemCode TEXT NOT NULL,
    itemName TEXT NOT NULL,
    quantity REAL NOT NULL,
    unitOfMeasure TEXT NOT NULL,
    recordedAt INTEGER NOT NULL,
    FOREIGN KEY(jobId) REFERENCES job_cards(id) ON DELETE CASCADE,
    FOREIGN KEY(inventoryId) REFERENCES inventory_assets(id) ON DELETE CASCADE
);
CREATE INDEX idx_job_inventory_usage_job ON job_inventory_usage(jobId);
```

```sql
-- job_fixed_assets
CREATE TABLE job_fixed_assets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    jobId INTEGER,
    fixedId INTEGER NOT NULL,
    fixedCode TEXT NOT NULL,
    fixedName TEXT NOT NULL,
    reason TEXT NOT NULL,
    technicianId INTEGER NOT NULL,
    technicianName TEXT NOT NULL,
    checkoutTime INTEGER NOT NULL,
    returnTime INTEGER,
    condition TEXT NOT NULL DEFAULT 'Good',
    returnCondition TEXT,
    notes TEXT,
    FOREIGN KEY(jobId) REFERENCES job_cards(id) ON DELETE CASCADE,
    FOREIGN KEY(fixedId) REFERENCES fixed_assets(id) ON DELETE CASCADE
);
CREATE INDEX idx_job_fixed_assets_job ON job_fixed_assets(jobId);
```

```sql
-- job_purchases / purchase_receipts
CREATE TABLE job_purchases (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    jobId INTEGER NOT NULL,
    vendor TEXT NOT NULL,
    totalAmount REAL NOT NULL,
    notes TEXT,
    purchasedAt INTEGER NOT NULL,
    FOREIGN KEY(jobId) REFERENCES job_cards(id) ON DELETE CASCADE
);

CREATE TABLE purchase_receipts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    purchaseId INTEGER NOT NULL,
    uri TEXT NOT NULL,
    mimeType TEXT,
    notes TEXT,
    capturedAt INTEGER NOT NULL,
    FOREIGN KEY(purchaseId) REFERENCES job_purchases(id) ON DELETE CASCADE
);
```

## Notes / Policies
- Clean reset is enabled via `fallbackToDestructiveMigration` (approved for current build); add migrations before production.
- statusHistory is the source of truth for workflow; `status` is cached for fast filtering.
- Inventory deductions happen on job completion when `job_inventory_usage` rows are inserted.
- Fixed availability comes from `statusHistory` on `fixed_assets` plus open `job_fixed_assets` rows.
