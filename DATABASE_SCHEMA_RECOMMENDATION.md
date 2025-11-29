# Database Schema Recommendation Report
## Job Card Management System

---

## 1. Executive Summary

This report proposes a streamlined, well-structured database schema for the job card management system. The design focuses on:
- **One technician user** (single-user mobile app)
- **Job cards** as the central entity
- **Fixed assets** (tools, equipment - reusable) with full status history tracking
- **Inventory assets** (consumables, parts - depletable)
- **New asset purchases** with receipt tracking and asset integration workflow
- **Minimal relational tables** for flexibility

---

## 2. Schema Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           CORE ENTITIES                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│  TECHNICIAN (1 record)                                                       │
│       │                                                                      │
│       ▼                                                                      │
│  JOB_CARDS ◄────────────────────────────────────────────────────────────┐   │
│       │                                                                  │   │
│       ├──────► JOB_FIXED_ASSETS (which fixed assets used on job)        │   │
│       │              │                                                   │   │
│       │              ▼                                                   │   │
│       │        FIXED_ASSETS (tools, equipment - status_history JSON)    │   │
│       │                                                                  │   │
│       ├──────► JOB_INVENTORY_USAGE (consumables used on job)            │   │
│       │              │                                                   │   │
│       │              ▼                                                   │   │
│       │        INVENTORY_ASSETS (parts, consumables - depletable)       │   │
│       │                                                                  │   │
│       └──────► JOB_PURCHASES (new items purchased for job) ─────────────┘   │
│                      │                                                       │
│                      ▼                                                       │
│                RECEIPT FIELDS on JOB_PURCHASES                               │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Detailed Table Definitions

---

### 3.1 TECHNICIAN

Stores the single logged-in technician. Always exactly 1 record.

```sql
CREATE TABLE technician (
    id                  INTEGER PRIMARY KEY DEFAULT 1,
    username            TEXT NOT NULL,
    name                TEXT NOT NULL,
    email               TEXT NOT NULL,
    phone               TEXT NOT NULL,
    auth_token          TEXT,
    last_sync_time      INTEGER,
    created_at          INTEGER NOT NULL,
    updated_at          INTEGER NOT NULL
);
```

| Field | Role |
|-------|------|
| `id` | Always 1 - enforces single technician |
| `username` | Login username |
| `name` | Display name on UI and reports |
| `email` | Contact email |
| `phone` | Contact phone number |
| `auth_token` | API authentication for server sync |
| `last_sync_time` | Track when data was last synced to server |

---

### 3.2 JOB_CARDS

Central entity - all work revolves around job cards.

```sql
CREATE TABLE job_cards (
    id                  INTEGER PRIMARY KEY,
    job_number          TEXT NOT NULL UNIQUE,

    -- Customer Info (denormalized for offline access)
    customer_name       TEXT NOT NULL,
    customer_phone      TEXT NOT NULL,
    customer_email      TEXT,

    -- Job Details
    title               TEXT NOT NULL,
    description         TEXT,
    job_type            TEXT NOT NULL,
    priority            TEXT DEFAULT 'NORMAL',

    -- Location
    service_address     TEXT NOT NULL,
    latitude            REAL,
    longitude           REAL,

    -- Scheduling
    scheduled_date      TEXT NOT NULL,
    scheduled_time      TEXT,
    estimated_duration  INTEGER,

    -- Status History (JSON) - all workflow transitions tracked here
    status_history      TEXT NOT NULL,

    -- Travel Metrics
    travel_distance     REAL,

    -- Work Completion
    work_performed      TEXT,
    technician_notes    TEXT,
    issues_encountered  TEXT,

    -- Evidence
    customer_signature  TEXT,
    before_photos       TEXT,
    after_photos        TEXT,
    other_photos        TEXT,

    -- Customer Feedback (after SIGNED)
    customer_rating     INTEGER,
    customer_feedback   TEXT,

    -- Follow-up
    requires_follow_up  INTEGER DEFAULT 0,
    follow_up_notes     TEXT,

    -- Sync Status
    is_synced           INTEGER DEFAULT 0,
    last_synced_at      INTEGER,

    -- Metadata
    created_at          INTEGER NOT NULL,
    updated_at          INTEGER NOT NULL
);

CREATE INDEX idx_job_cards_scheduled_date ON job_cards(scheduled_date);
CREATE INDEX idx_job_cards_job_number ON job_cards(job_number);
```

| Field | Role |
|-------|------|
| `id` | Server-side job ID (synced from backend) |
| `job_number` | Human-readable identifier (e.g., "JC-2024-001") |
| `customer_*` | Denormalized customer info for offline access |
| `job_type` | INSTALLATION \| REPAIR \| SERVICE \| INSPECTION |
| `priority` | LOW \| NORMAL \| HIGH \| URGENT |
| `scheduled_date` | YYYY-MM-DD format for date queries |
| `estimated_duration` | Expected minutes to complete |
| `status_history` | JSON array - full workflow audit trail (see below) |
| `travel_distance` | Distance traveled to job site (km) |
| `customer_signature` | Base64 encoded signature image |
| `before/after/other_photos` | JSON arrays: [{uri, notes}] |
| `customer_rating` | Customer rating 1-5 (after SIGNED) |
| `customer_feedback` | Customer comments/feedback (after SIGNED) |
| `is_synced` | 0=pending upload, 1=synced to server |

**Status History JSON Structure:**
```json
[
  { "status": "AVAILABLE", "timestamp": 1701000000000 },
  { "status": "AWAITING", "timestamp": 1701100000000 },
  { "status": "PENDING", "timestamp": 1701200000000 },
  { "status": "EN_ROUTE", "timestamp": 1701300000000 },
  { "status": "BUSY", "timestamp": 1701350000000 },
  { "status": "PAUSED", "timestamp": 1701400000000, "reason": "Waiting for parts" },
  { "status": "BUSY", "timestamp": 1701500000000 },
  { "status": "COMPLETED", "timestamp": 1701600000000 },
  { "status": "SIGNED", "timestamp": 1701650000000, "signed_by": "John Smith" }
]
```

**Status Values:**
| Status | Meaning |
|--------|---------|
| `AVAILABLE` | Unassigned, technician can claim |
| `AWAITING` | Assigned, not yet accepted |
| `PENDING` | Accepted, scheduled but not started |
| `EN_ROUTE` | Traveling to job site |
| `BUSY` | Actively working |
| `PAUSED` | Work temporarily stopped (reason required) |
| `COMPLETED` | Job finished, awaiting customer signature |
| `SIGNED` | Customer signed, job finalized (read-only) |
| `CANCELLED` | Job cancelled (reason required) |

**Status Workflow:**
```
AVAILABLE → AWAITING → PENDING → EN_ROUTE → BUSY → COMPLETED → SIGNED
                                    ↕         ↕         ↓
                                  PAUSED ←→ PAUSED    (if already signed,
                                                       skip to SIGNED)

Any status (except COMPLETED/SIGNED) → CANCELLED
```

**Note:** Once a job reaches `SIGNED` status, its details cannot be modified. If the customer has already signed (`customer_signature` is set) when the technician marks the job as COMPLETED, it automatically transitions to SIGNED.

**Derived Properties (in Domain Model):**
```kotlin
// Current status = last entry's status
val currentStatus: String
    get() = statusHistory.lastOrNull()?.status ?: "AVAILABLE"

// When was job accepted?
val acceptedAt: Long?
    get() = statusHistory.find { it.status == "PENDING" }?.timestamp

// When did technician start traveling?
val enRouteAt: Long?
    get() = statusHistory.find { it.status == "EN_ROUTE" }?.timestamp

// When did work start?
val startedAt: Long?
    get() = statusHistory.find { it.status == "BUSY" }?.timestamp

// When was job completed?
val completedAt: Long?
    get() = statusHistory.find { it.status == "COMPLETED" }?.timestamp

// When was job signed?
val signedAt: Long?
    get() = statusHistory.find { it.status == "SIGNED" }?.timestamp

// Who signed the job?
val signedBy: String?
    get() = statusHistory.find { it.status == "SIGNED" }?.signedBy

// When was job cancelled?
val cancelledAt: Long?
    get() = statusHistory.find { it.status == "CANCELLED" }?.timestamp

// Cancellation reason
val cancellationReason: String?
    get() = statusHistory.find { it.status == "CANCELLED" }?.reason

// Is job finalized (read-only)?
val isFinalized: Boolean
    get() = currentStatus == "SIGNED"

// Total time paused (sum of all pause durations)
val totalPausedMs: Long
    get() {
        var total = 0L
        val history = statusHistory
        for (i in history.indices) {
            if (history[i].status == "PAUSED" && i + 1 < history.size) {
                total += history[i + 1].timestamp - history[i].timestamp
            }
        }
        return total
    }

// All pause events with reasons
val pauseReasons: List<String>
    get() = statusHistory
        .filter { it.status == "PAUSED" && it.reason != null }
        .mapNotNull { it.reason }
```

---

### 3.3 FIXED_ASSETS

Reusable tools and equipment. Status derived from `status_history` JSON.

```sql
CREATE TABLE fixed_assets (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    asset_code          TEXT NOT NULL UNIQUE,
    asset_name          TEXT NOT NULL,
    asset_type          TEXT NOT NULL,

    -- Identification
    serial_number       TEXT,
    manufacturer        TEXT,
    model               TEXT,

    -- Status History (JSON) - all state changes tracked here
    status_history      TEXT NOT NULL,

    -- Purchase & Warranty
    purchase_date       INTEGER,
    purchase_cost       REAL,
    warranty_expiry     INTEGER,

    -- Maintenance Scheduling
    next_maintenance    INTEGER,

    -- Notes
    notes               TEXT,

    -- Sync Status
    is_synced           INTEGER DEFAULT 0,
    last_synced_at      INTEGER,

    -- Metadata
    created_at          INTEGER NOT NULL,
    updated_at          INTEGER NOT NULL
);

CREATE INDEX idx_fixed_assets_code ON fixed_assets(asset_code);
CREATE INDEX idx_fixed_assets_type ON fixed_assets(asset_type);
```

| Field | Role |
|-------|------|
| `asset_code` | Unique barcode/QR code label (e.g., "TOOL-001") |
| `asset_name` | Display name (e.g., "Manifold Gauge Set") |
| `asset_type` | TOOL \| EQUIPMENT \| VEHICLE \| METER \| LADDER |
| `serial_number` | Manufacturer's serial for warranty/tracking |
| `manufacturer` | Brand name |
| `model` | Model number/name |
| `status_history` | JSON array - full audit trail (see below) |
| `purchase_date` | When asset was acquired (timestamp) |
| `purchase_cost` | Original cost of the asset |
| `warranty_expiry` | Warranty end date (timestamp) |
| `next_maintenance` | Scheduled maintenance date (timestamp) |
| `notes` | Specs, capacity, special instructions |
| `is_synced` | 0=pending upload, 1=synced to server |
| `last_synced_at` | Last successful sync timestamp |

**Status History JSON Structure:**
```json
[
  { "status": "AVAILABLE", "timestamp": 1701000000000 },
  { "status": "CHECKED_OUT", "timestamp": 1701234567890, "job_id": 52, "condition": "GOOD" },
  { "status": "AVAILABLE", "timestamp": 1701298765432, "job_id": 52, "condition": "GOOD" },
  { "status": "IN_MAINTENANCE", "timestamp": 1701345678900 },
  { "status": "AVAILABLE", "timestamp": 1701432109876, "condition": "GOOD" }
]
```

**Status Values:**
| Status | Meaning |
|--------|---------|
| `AVAILABLE` | In stock, ready for checkout |
| `CHECKED_OUT` | Currently with a technician |
| `IN_MAINTENANCE` | Being serviced/repaired |
| `DAMAGED` | Needs repair before use |
| `RETIRED` | No longer in service |

**Derived Properties (in Domain Model):**
```kotlin
// Current status = last entry's status
val currentStatus: String
    get() = statusHistory.lastOrNull()?.status ?: "AVAILABLE"

// Current job (only if CHECKED_OUT)
val currentJobId: Int?
    get() = statusHistory.lastOrNull()
        ?.takeIf { it.status == "CHECKED_OUT" }?.jobId

// Last maintenance completed (when status changed FROM IN_MAINTENANCE)
val lastMaintenanceDate: Long?
    get() = statusHistory
        .zipWithNext()
        .filter { (prev, curr) -> prev.status == "IN_MAINTENANCE" && curr.status == "AVAILABLE" }
        .maxOfOrNull { (_, curr) -> curr.timestamp }

// Check if available
val isAvailable: Boolean
    get() = currentStatus == "AVAILABLE"
```

---

### 3.4 INVENTORY_ASSETS

Consumables and parts tracked by quantity.

```sql
CREATE TABLE inventory_assets (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    item_code           TEXT NOT NULL UNIQUE,
    item_name           TEXT NOT NULL,
    category            TEXT NOT NULL,

    -- Stock Levels
    current_stock       REAL NOT NULL DEFAULT 0,
    minimum_stock       REAL NOT NULL DEFAULT 0,
    unit_of_measure     TEXT NOT NULL,

    -- Pricing
    unit_cost           REAL,

    -- Storage
    storage_location    TEXT,

    -- Notes
    notes               TEXT,

    -- Sync Status
    is_synced           INTEGER DEFAULT 0,
    last_synced_at      INTEGER,

    -- Metadata
    created_at          INTEGER NOT NULL,
    updated_at          INTEGER NOT NULL
);

CREATE INDEX idx_inventory_code ON inventory_assets(item_code);
CREATE INDEX idx_inventory_category ON inventory_assets(category);
```

| Field | Role |
|-------|------|
| `item_code` | SKU/part number (e.g., "GAS-R410A") |
| `item_name` | Display name (e.g., "R410A Refrigerant Gas") |
| `category` | CONSUMABLE \| PART \| SUPPLY |
| `current_stock` | Current quantity (REAL for decimals: 2.5 kg) |
| `minimum_stock` | Reorder threshold for low-stock alerts |
| `unit_of_measure` | kg \| piece \| meter \| liter \| roll |
| `unit_cost` | Cost per unit for job costing |
| `storage_location` | Where item is stored (e.g., "Van", "Warehouse Shelf A3") |
| `is_synced` | 0=pending upload, 1=synced to server |
| `last_synced_at` | Last successful sync timestamp |

**Derived Property:**
```kotlin
val isLowStock: Boolean
    get() = currentStock <= minimumStock
```

**Examples:**
- R410A Refrigerant Gas (50 kg) - CONSUMABLE
- 35uF Capacitor (25 pieces) - PART
- Copper Pipe 1/4" (100 meters) - SUPPLY

---

### 3.5 JOB_FIXED_ASSETS

Links fixed assets to jobs. Checkout/return times and conditions are tracked in `fixed_assets.status_history`.

```sql
CREATE TABLE job_fixed_assets (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id              INTEGER NOT NULL,
    fixed_asset_id      INTEGER NOT NULL,
    created_at          INTEGER NOT NULL,

    FOREIGN KEY (job_id) REFERENCES job_cards(id) ON DELETE CASCADE,
    FOREIGN KEY (fixed_asset_id) REFERENCES fixed_assets(id) ON DELETE RESTRICT
);

CREATE INDEX idx_job_fixed_job ON job_fixed_assets(job_id);
CREATE INDEX idx_job_fixed_asset ON job_fixed_assets(fixed_asset_id);
```

| Field | Role |
|-------|------|
| `job_id` | Which job this asset is linked to |
| `fixed_asset_id` | Which asset was used |
| `created_at` | When the link was created (audit purposes) |

**Note:** Checkout time, return time, and condition are derived from `fixed_assets.status_history` entries that reference this `job_id`.

---

### 3.6 JOB_INVENTORY_USAGE

Tracks consumables/parts used per job.

```sql
CREATE TABLE job_inventory_usage (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id              INTEGER NOT NULL,
    inventory_asset_id  INTEGER NOT NULL,
    quantity_used       REAL NOT NULL,
    created_at          INTEGER NOT NULL,

    FOREIGN KEY (job_id) REFERENCES job_cards(id) ON DELETE CASCADE,
    FOREIGN KEY (inventory_asset_id) REFERENCES inventory_assets(id) ON DELETE RESTRICT
);

CREATE INDEX idx_job_inventory_job ON job_inventory_usage(job_id);
CREATE INDEX idx_job_inventory_asset ON job_inventory_usage(inventory_asset_id);
```

| Field | Role |
|-------|------|
| `job_id` | Which job consumed the item |
| `inventory_asset_id` | Which inventory item was used |
| `quantity_used` | Amount consumed (REAL for "1.5 kg") |
| `created_at` | When usage was recorded (audit purposes) |

**Business Logic:** On insert, decrement `inventory_assets.current_stock` by `quantity_used`.

---

### 3.7 JOB_PURCHASES

New items purchased during a job (not in existing inventory).

```sql
CREATE TABLE job_purchases (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id              INTEGER NOT NULL,

    -- Purchase Details
    item_name           TEXT NOT NULL,
    item_description    TEXT,
    quantity            REAL NOT NULL,
    unit_of_measure     TEXT NOT NULL,
    unit_cost           REAL NOT NULL,
    total_cost          REAL NOT NULL,

    -- Vendor Info
    vendor_name         TEXT,
    purchase_date       INTEGER NOT NULL,

    -- Asset Integration
    target_asset_type   TEXT NOT NULL,
    linked_asset_id     INTEGER,
    is_integrated       INTEGER DEFAULT 0,

    -- Approval (optional)
    approval_status     TEXT DEFAULT 'PENDING',
    approved_by         TEXT,
    approved_at         INTEGER,
    rejection_reason    TEXT,

    -- Notes
    notes               TEXT,

    -- Metadata
    created_at          INTEGER NOT NULL,
    updated_at          INTEGER NOT NULL,

    FOREIGN KEY (job_id) REFERENCES job_cards(id) ON DELETE CASCADE
);

CREATE INDEX idx_job_purchases_job ON job_purchases(job_id);
CREATE INDEX idx_job_purchases_integrated ON job_purchases(is_integrated);
CREATE INDEX idx_job_purchases_approval ON job_purchases(approval_status);
```

| Field | Role |
|-------|------|
| `job_id` | Which job required this purchase |
| `item_name` | What was purchased |
| `item_description` | Additional details/specs |
| `quantity` | How many/much (REAL for decimals) |
| `unit_of_measure` | piece \| kg \| meter \| liter |
| `unit_cost` | Price per unit |
| `total_cost` | quantity × unit_cost |
| `vendor_name` | Store/supplier name |
| `purchase_date` | When purchased (timestamp) |
| `target_asset_type` | FIXED \| INVENTORY \| NONE |
| `linked_asset_id` | FK to created/updated asset after integration |
| `is_integrated` | 0=pending, 1=added to assets |
| `approval_status` | PENDING \| APPROVED \| REJECTED |
| `approved_by` | Manager name who approved |
| `rejection_reason` | Why rejected (if applicable) |

**Target Asset Type:**
| Value | Meaning |
|-------|---------|
| `FIXED` | Becomes a new fixed asset (tool, equipment) |
| `INVENTORY` | Adds to inventory stock |
| `NONE` | One-time expense (not tracked as asset) |

**Integration Workflow:**
1. Technician purchases item → creates `job_purchases` record
2. Uploads receipt → updates receipt fields on `job_purchases`
3. Manager reviews → sets `approval_status`
4. If APPROVED + INVENTORY: increment existing stock or create new item
5. If APPROVED + FIXED: create new fixed_assets record
6. Set `linked_asset_id` and `is_integrated = 1`

---

### 3.8 PURCHASE RECEIPTS (embedded)

Receipt metadata is stored directly on `job_purchases` (one receipt per purchase).

**Receipt fields on `job_purchases`:**
- `receipt_uri` (TEXT)
- `receipt_mime_type` (TEXT)
- `receipt_captured_at` (INTEGER)

---

## 4. Relationship Summary

| Relationship | Type | Description |
|--------------|------|-------------|
| technician → job_cards | 1:N | One technician handles many jobs |
| job_cards → job_fixed_assets | 1:N | One job uses multiple fixed assets |
| fixed_assets → job_fixed_assets | 1:N | One asset used across many jobs |
| job_cards → job_inventory_usage | 1:N | One job consumes multiple items |
| inventory_assets → job_inventory_usage | 1:N | One item used across many jobs |
| job_cards → job_purchases | 1:N | One job may have multiple purchases |
| job_purchases | stores one receipt directly on the row |

---

## 5. Visual Schema Diagram

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                                                                              │
│  ┌─────────────┐         ┌──────────────────────────────────────────────┐   │
│  │ TECHNICIAN  │         │              JOB_CARDS                        │   │
│  │─────────────│         │──────────────────────────────────────────────│   │
│  │ id (PK)     │────────►│ id (PK)                                      │   │
│  │ username    │         │ job_number (UNIQUE)                          │   │
│  │ name        │         │ customer_name, phone, email                   │   │
│  │ email       │         │ title, description, job_type, priority        │   │
│  │ phone       │         │ service_address, lat, lng                     │   │
│  │ auth_token  │         │ scheduled_date, status_history (JSON)         │   │
│  └─────────────┘         │ travel_distance, work details, photos         │   │
│                          │ customer_rating, customer_feedback            │   │
│                          └──────────────────────────────────────────────┘   │
│                                          │                                   │
│                    ┌─────────────────────┼─────────────────────┐            │
│                    │                     │                     │            │
│                    ▼                     ▼                     ▼            │
│  ┌─────────────────────────┐ ┌─────────────────────────┐ ┌─────────────────┐│
│  │  JOB_FIXED_ASSETS       │ │  JOB_INVENTORY_USAGE    │ │  JOB_PURCHASES  ││
│  │─────────────────────────│ │─────────────────────────│ │─────────────────││
│  │ id (PK)                 │ │ id (PK)                 │ │ id (PK)         ││
│  │ job_id (FK)             │ │ job_id (FK)             │ │ job_id (FK)     ││
│  │ fixed_asset_id (FK)     │ │ inventory_asset_id (FK) │ │ item_name       ││
│  │ created_at              │ │ quantity_used           │ │ quantity, cost  ││
│  │                         │ │ created_at              │ │ target_type     ││
│  │                         │ │                         │ │ is_integrated   ││
│  └───────────┬─────────────┘ └───────────┬─────────────┘ └────────┬────────┘│
│              │                           │                        │         │
│              ▼                           ▼                        ▼         │
│  ┌─────────────────────────┐ ┌─────────────────────────┐ ┌─────────────────┐│
│  │    FIXED_ASSETS         │ │   INVENTORY_ASSETS      │ │PURCHASE_RECEIPTS││
│  │─────────────────────────│ │─────────────────────────│ │─────────────────││
│  │ id (PK)                 │ │ id (PK)                 │ │ id (PK)         ││
│  │ asset_code (UNIQUE)     │ │ item_code (UNIQUE)      │ │ purchase_id(FK) ││
│  │ asset_name, asset_type  │ │ item_name, category     │ │ file_path       ││
│  │ serial, manufacturer    │ │ current_stock           │ │ file_type       ││
│  │ status_history (JSON)   │ │ minimum_stock           │ │ uploaded_at     ││
│  │ purchase_cost, warranty │ │ storage_location        │ └─────────────────┘│
│  │ next_maintenance        │ │ unit_of_measure         │                    │
│  │ is_synced               │ │ is_synced               │                    │
│  └─────────────────────────┘ └─────────────────────────┘                    │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 6. Sample Queries

**Get all resources used on a job:**
```sql
-- Fixed assets used (checkout/return times from status_history JSON)
SELECT fa.asset_name, fa.asset_code, fa.status_history
FROM job_fixed_assets jf
JOIN fixed_assets fa ON jf.fixed_asset_id = fa.id
WHERE jf.job_id = ?;

-- Inventory consumed
SELECT ia.item_name, ia.item_code, ji.quantity_used, ia.unit_of_measure
FROM job_inventory_usage ji
JOIN inventory_assets ia ON ji.inventory_asset_id = ia.id
WHERE ji.job_id = ?;

-- Purchases made
SELECT item_name, quantity, total_cost, approval_status
FROM job_purchases
WHERE job_id = ?;
```

**Low stock alert:**
```sql
SELECT item_name, item_code, current_stock, minimum_stock, unit_of_measure
FROM inventory_assets
WHERE current_stock <= minimum_stock;
```

**Pending purchase integrations:**
```sql
SELECT p.*, j.job_number
FROM job_purchases p
JOIN job_cards j ON p.job_id = j.id
WHERE p.is_integrated = 0 AND p.approval_status = 'APPROVED';
```

**Assets due for maintenance (next 7 days):**
```sql
SELECT asset_name, asset_code, next_maintenance
FROM fixed_assets
WHERE next_maintenance <= strftime('%s', 'now') * 1000 + 604800000
ORDER BY next_maintenance ASC;
```

**Job cost summary:**
```sql
SELECT
    j.job_number,
    COALESCE(SUM(ji.quantity_used * ia.unit_cost), 0) AS inventory_cost,
    COALESCE((SELECT SUM(total_cost) FROM job_purchases WHERE job_id = j.id), 0) AS purchase_cost
FROM job_cards j
LEFT JOIN job_inventory_usage ji ON j.id = ji.job_id
LEFT JOIN inventory_assets ia ON ji.inventory_asset_id = ia.id
WHERE j.id = ?
GROUP BY j.id;
```

---

## 7. Table Summary

| Table | Purpose | Expected Records |
|-------|---------|------------------|
| technician | Current user | 1 (always) |
| job_cards | All jobs | Hundreds over time |
| fixed_assets | Reusable tools/equipment | 10-50 |
| inventory_assets | Consumable parts/supplies | 20-100 |
| job_fixed_assets | Asset checkouts per job | 1-5 per job |
| job_inventory_usage | Consumables per job | 2-10 per job |
| job_purchases | Ad-hoc purchases | 0-3 per job |
| purchase_receipts | Receipt files | 1 per purchase |

**Total: 8 tables**

---

## 8. Key Design Decisions

### Why separate Fixed vs Inventory?

| Aspect | Fixed Assets | Inventory Assets |
|--------|--------------|------------------|
| Nature | Reusable, tracked individually | Consumable, tracked by quantity |
| Tracking | Checkout/return with history | Deduct from stock |
| Identity | Serial number, unique item | Fungible (any unit is same) |
| Examples | Multimeter, ladder | Refrigerant gas, filters |

### Why JSON for status_history?

- Single source of truth - no redundant current status fields
- Complete audit trail in one place
- Flexible schema for different status types
- Easy to query last entry for current state
- Derive maintenance history without separate table

### Why Job Purchases as separate entity?

- Audit trail: Every purchase tied to specific job
- Receipt storage: Proper documentation for expenses
- Integration workflow: Review before adding to inventory
- Cost tracking: Know exactly what each job cost

---

## 9. Conclusion

This schema provides:

1. **Simplicity**: 8 focused tables, minimal redundancy
2. **Flexibility**: JSON `status_history` for both job cards and fixed assets
3. **Traceability**: Every asset usage and purchase linked to job
4. **Full History**: All status changes tracked with timestamp (and reason/condition where applicable)
5. **Integration Workflow**: Purchases reviewed before adding to inventory
6. **Maintenance Tracking**: `next_maintenance` date + last maintenance derived from history
7. **Offline-First**: All data local with sync flags for server

---

*Report generated: November 2024*
*For: Job Card Management System*
