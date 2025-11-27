# Database Design Document

## Overview

This document describes the database schema for the Job Card Management application. The application uses **Room Database** (SQLite) for local data persistence on Android devices.

**Database Name**: `jobcard_database`
**Current Version**: 13
**ORM**: Room (AndroidX)
**Architecture**: MVVM with Repository pattern

---

## Table of Contents

1. [Current Schema Overview](#current-schema-overview)
2. [Entity Relationship Diagram](#entity-relationship-diagram)
3. [Table Specifications](#table-specifications)
4. [Design Principles](#design-principles)
5. [Proposed Schema Improvements](#proposed-schema-improvements)
6. [Migration Strategy](#migration-strategy)
7. [Indexing Strategy](#indexing-strategy)
8. [Data Integrity Rules](#data-integrity-rules)

---

## Current Schema Overview

### Tables Summary

| Table Name | Purpose | Record Count (Typical) | Growth Rate |
|------------|---------|------------------------|-------------|
| `job_cards` | Job card tracking | 50-500 | High |
| `current` | Consumable inventory | 20-100 | Low |
| `customers` | Customer information | 100-1000 | Medium |
| `technicians` | Technician profiles | 1-50 | Low |
| `current_technician` | Active session | 1 (singleton) | None |
| `fixed` | Fixed assets | 20-200 | Low |
| `fixed_checkouts` | Asset checkout history | 100-5000 | High |
| `tool_checkouts` | Consumable checkouts | 100-5000 | High |

---

## Entity Relationship Diagram

```
┌─────────────────────┐
│  current_technician │ (Singleton - Active Session)
│  ─────────────────  │
│  PK: id (always 1)  │
│  technicianId       │
│  authToken          │
│  currentActiveJobId │─────┐
└─────────────────────┘     │
                            │
                            │
┌─────────────────────┐     │    ┌─────────────────────┐
│    technicians      │     │    │     customers       │
│  ─────────────────  │     │    │  ─────────────────  │
│  PK: id             │     │    │  PK: id             │
│  employeeNumber     │     │    │  name               │
│  name               │     │    │  phone              │
│  email              │     │    │  email              │
└─────────────────────┘     │    │  address            │
                            │    └──────────┬──────────┘
                            │               │
                            │               │ (denormalized)
                            ↓               ↓
                    ┌──────────────────────────────┐
                    │        job_cards             │
                    │  ──────────────────────────  │
                    │  PK: id                      │
                    │  jobNumber (unique)          │
                    │  customerId (soft FK)        │
                    │  customerName (denorm)       │
                    │  status                      │
                    │  isMyJob                     │
                    │  beforePhotos (JSON)         │
                    │  afterPhotos (JSON)          │
                    │  otherPhotos (JSON)          │
                    │  resourcesUsed (JSON)        │
                    │  pauseHistory (JSON)         │
                    └───────────┬──────────────────┘
                                │
                                │
                    ┌───────────┴──────────────────┐
                    │                              │
                    ↓                              ↓
        ┌────────────────────┐      ┌─────────────────────────┐
        │  fixed_checkouts   │      │    tool_checkouts       │
        │  ────────────────  │      │  ─────────────────────  │
        │  PK: id            │      │  PK: id                 │
        │  FK: fixedId       │      │  technicianId           │
        │  FK: jobId         │      │  resourceId             │
        │  technicianId      │      │  checkedOutAt           │
        │  checkoutTime      │      │  returnedAt             │
        │  returnTime        │      │  isReturned             │
        └────────┬───────────┘      └────────┬────────────────┘
                 │                           │
                 │                           │
                 ↓                           ↓
        ┌────────────────────┐      ┌─────────────────────────┐
        │      fixed         │      │       current           │
        │  ────────────────  │      │  ─────────────────────  │
        │  PK: id            │      │  PK: id                 │
        │  fixedCode (UQ)    │      │  itemCode               │
        │  fixedName         │      │  itemName               │
        │  fixedType         │      │  currentStock (REAL)    │
        │  isAvailable       │      │  minimumStock (REAL)    │
        └────────────────────┘      └─────────────────────────┘
```

**Legend**:
- `PK` = Primary Key
- `FK` = Foreign Key
- `UQ` = Unique Index
- `(denorm)` = Denormalized data
- `(JSON)` = JSON stored in TEXT column
- `REAL` = Double/Float type

---

## Table Specifications

### 1. job_cards

**Purpose**: Core table storing all job card information, tracking complete job lifecycle from assignment to completion.

**Schema**:

```sql
CREATE TABLE job_cards (
    -- Identity
    id INTEGER PRIMARY KEY NOT NULL,
    jobNumber TEXT NOT NULL,

    -- Customer Information (Denormalized for query performance)
    customerId INTEGER NOT NULL,
    customerName TEXT NOT NULL,
    customerPhone TEXT NOT NULL,
    customerEmail TEXT,
    customerAddress TEXT NOT NULL,

    -- Assignment
    isMyJob INTEGER NOT NULL DEFAULT 0, -- Boolean: 1=assigned to current tech, 0=available

    -- Job Details
    title TEXT NOT NULL,
    description TEXT,
    jobType TEXT NOT NULL, -- ENUM: INSTALLATION, REPAIR, SERVICE, INSPECTION
    status TEXT NOT NULL,  -- ENUM: AWAITING, PENDING, EN_ROUTE, BUSY, PAUSED, COMPLETED, CANCELLED

    -- Scheduling
    scheduledDate TEXT,
    scheduledTime TEXT,
    estimatedDuration INTEGER, -- minutes

    -- Location
    serviceAddress TEXT NOT NULL,
    latitude REAL,
    longitude REAL,
    navigationUri TEXT,

    -- Time Tracking
    acceptedAt INTEGER,        -- Unix timestamp (ms)
    enRouteStartTime INTEGER,  -- When EN_ROUTE status started
    startTime INTEGER,         -- When BUSY status started
    endTime INTEGER,           -- When COMPLETED
    pausedTime INTEGER,        -- Total pause duration (ms)
    pauseHistory TEXT,         -- JSON array of pause events
    cancelledAt INTEGER,       -- When CANCELLED
    cancellationReason TEXT,

    -- Work Details
    workPerformed TEXT,
    technicianNotes TEXT,
    issuesEncountered TEXT,
    customerSignature TEXT,    -- Base64 encoded signature

    -- Photos (JSON arrays)
    beforePhotos TEXT,         -- [{"uri": "...", "notes": "..."}]
    afterPhotos TEXT,          -- [{"uri": "...", "notes": "..."}]
    otherPhotos TEXT,          -- [{"uri": "...", "notes": "..."}]

    -- Resources
    resourcesUsed TEXT,        -- JSON array of resources with quantities

    -- Follow-up
    requiresFollowUp INTEGER NOT NULL DEFAULT 0,
    followUpNotes TEXT,

    -- Sync tracking
    isSynced INTEGER NOT NULL DEFAULT 0,
    lastSyncedAt INTEGER,

    -- Audit
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL
);

-- Indexes for performance
CREATE INDEX idx_job_cards_status ON job_cards(status);
CREATE INDEX idx_job_cards_scheduled_date ON job_cards(scheduledDate);
CREATE INDEX idx_job_cards_is_my_job ON job_cards(isMyJob);
CREATE INDEX idx_job_cards_my_job_status ON job_cards(isMyJob, status);
```

**JSON Formats**:

```json
// beforePhotos, afterPhotos, otherPhotos
[
  {"uri": "file:///storage/photo1.jpg", "notes": "Front view showing damage"},
  {"uri": "file:///storage/photo2.jpg"}
]

// pauseHistory
[
  {
    "timestamp": 1699564800000,
    "reason": "Waiting for parts delivery",
    "duration": 1800000,
    "resumeStatus": "BUSY"
  }
]

// resourcesUsed
[
  {
    "resourceId": 1,
    "itemName": "R410A Refrigerant Gas",
    "itemCode": "GAS-R410A",
    "quantity": 2.5,
    "unit": "kg"
  }
]
```

**Business Rules**:
- Only ONE job per technician can have `status = 'BUSY'` at a time
- `isMyJob = 1` means job is assigned to current logged-in technician
- Customer data is denormalized for offline-first operation
- Photos stored as file paths (JSON array with optional notes)
- Resources deducted from inventory upon job completion

---

### 2. current (Assets/Consumables)

**Purpose**: Tracks consumable inventory items (parts, materials, supplies) with stock levels.

**Schema**:

```sql
CREATE TABLE current (
    id INTEGER PRIMARY KEY NOT NULL,
    itemCode TEXT NOT NULL,
    itemName TEXT NOT NULL,
    category TEXT NOT NULL,
    currentStock REAL NOT NULL,    -- Changed from INTEGER to REAL in v13
    minimumStock REAL NOT NULL,    -- Minimum threshold
    unitOfMeasure TEXT NOT NULL    -- kg, piece, meter, liter, roll, bottle, set
);
```

**Examples**:
- `R410A Refrigerant Gas` - 25.5 kg
- `Capacitor 35uF` - 10 pieces
- `Copper Pipe 1/4"` - 150 meters

**Business Rules**:
- Stock can go negative (tracked as backorder)
- Low stock alert when `currentStock <= minimumStock`
- Stock decremented automatically when job completed

---

### 3. customers

**Purpose**: Master customer database with contact and service information.

**Schema**:

```sql
CREATE TABLE customers (
    id INTEGER PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    phone TEXT NOT NULL,
    email TEXT,
    address TEXT NOT NULL,
    area TEXT,
    notes TEXT,  -- Special instructions (e.g., "Gate code: 1234")
    isSynced INTEGER NOT NULL DEFAULT 1
);
```

**Relationship**: Soft foreign key to `job_cards.customerId` (denormalized in job_cards)

---

### 4. technicians

**Purpose**: Stores technician profiles and statistics.

**Schema**:

```sql
CREATE TABLE technicians (
    id INTEGER PRIMARY KEY NOT NULL DEFAULT 1,
    employeeNumber TEXT NOT NULL,
    name TEXT NOT NULL,
    email TEXT NOT NULL,
    phone TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'Technician',
    specialization TEXT,           -- e.g., "AC Installation", "Refrigeration"
    isActive INTEGER NOT NULL DEFAULT 1,
    currentJobId INTEGER,          -- Current active job ID
    totalJobsCompleted INTEGER NOT NULL DEFAULT 0,
    rating REAL NOT NULL DEFAULT 0.0,
    authToken TEXT NOT NULL DEFAULT 'sample_token_12345',
    profilePhotoPath TEXT,
    lastLoginAt INTEGER
);
```

---

### 5. current_technician (Session Singleton)

**Purpose**: Stores the currently logged-in technician's session. **Only ONE record exists** (id=1).

**Schema**:

```sql
CREATE TABLE current_technician (
    id INTEGER PRIMARY KEY NOT NULL,  -- Always 1 (singleton)
    technicianId INTEGER NOT NULL,
    employeeNumber TEXT NOT NULL,
    name TEXT NOT NULL,
    email TEXT NOT NULL,
    phone TEXT NOT NULL,
    specialization TEXT,
    profilePhotoUrl TEXT,
    authToken TEXT NOT NULL,
    refreshToken TEXT,
    lastSyncTime INTEGER,
    isOnDuty INTEGER NOT NULL DEFAULT 1,
    currentActiveJobId INTEGER,       -- Job currently being worked (BUSY only)
    totalJobsCompletedToday INTEGER NOT NULL DEFAULT 0,
    loginTime INTEGER NOT NULL
);
```

**Business Rules**:
- Always exactly one record (id=1)
- Deleted on logout
- `currentActiveJobId` only set when job status is BUSY or PAUSED
- Reset `totalJobsCompletedToday` daily (handled by app logic)

---

### 6. fixed

**Purpose**: Tracks fixed/durable assets (tools, equipment, AC units, vehicles).

**Schema**:

```sql
CREATE TABLE fixed (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    fixedCode TEXT NOT NULL UNIQUE,
    fixedName TEXT NOT NULL,
    fixedType TEXT NOT NULL,       -- TOOL, AIR_CONDITIONER, LADDER, EQUIPMENT, VEHICLE, METER, PUMP
    serialNumber TEXT,
    manufacturer TEXT,
    model TEXT,
    isAvailable INTEGER NOT NULL DEFAULT 1,
    currentHolder TEXT,            -- Technician name
    lastMaintenanceDate INTEGER,
    nextMaintenanceDate INTEGER,
    notes TEXT,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL
);

CREATE UNIQUE INDEX idx_fixed_code ON fixed(fixedCode);
```

**Business Rules**:
- Cannot checkout if `isAvailable = 0`
- `currentHolder` updated on checkout/return
- Maintenance tracking for preventive maintenance

---

### 7. fixed_checkouts

**Purpose**: Audit trail for fixed asset checkout/return transactions.

**Schema**:

```sql
CREATE TABLE fixed_checkouts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    fixedId INTEGER NOT NULL,
    fixedCode TEXT NOT NULL,       -- Cached for display
    fixedName TEXT NOT NULL,       -- Cached for display
    technicianId INTEGER NOT NULL,
    technicianName TEXT NOT NULL,
    checkoutTime INTEGER NOT NULL,
    returnTime INTEGER,            -- NULL = still checked out
    reason TEXT NOT NULL,          -- Purpose of checkout
    jobId INTEGER,                 -- Associated job (optional)
    jobNumber TEXT,                -- Cached job number
    checkoutCondition TEXT NOT NULL DEFAULT 'Good',
    returnCondition TEXT,
    checkoutNotes TEXT,
    returnNotes TEXT,
    FOREIGN KEY(fixedId) REFERENCES fixed(id) ON DELETE CASCADE
);

CREATE INDEX idx_fixed_checkouts_fixed_id ON fixed_checkouts(fixedId);
CREATE INDEX idx_fixed_checkouts_technician_id ON fixed_checkouts(technicianId);
CREATE INDEX idx_fixed_checkouts_job_id ON fixed_checkouts(jobId);
```

**Business Rules**:
- `returnTime = NULL` means asset still checked out
- Cascade delete: If fixed asset deleted, all history deleted
- Cached fields (fixedCode, fixedName, jobNumber) for historical integrity

---

### 8. tool_checkouts

**Purpose**: Tracks checkout/return of consumable tools from inventory.

**Schema**:

```sql
CREATE TABLE tool_checkouts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    technicianId INTEGER NOT NULL,
    resourceId INTEGER NOT NULL,
    itemName TEXT NOT NULL,        -- Cached
    itemCode TEXT NOT NULL,        -- Cached
    checkedOutAt INTEGER NOT NULL,
    returnedAt INTEGER,            -- NULL = not returned yet
    isReturned INTEGER NOT NULL DEFAULT 0
);
```

**Business Rules**:
- No foreign key to `current` table (soft reference)
- `isReturned = 0` means tool still with technician
- Cached fields maintain history even if asset deleted

---

## Design Principles

### 1. Offline-First Architecture
- All data stored locally with sync flags
- Denormalization for performance (customer data in job_cards)
- Optimistic UI updates

### 2. Audit Trail
- Timestamps for all major operations (acceptedAt, startTime, endTime, etc.)
- Checkout history preserved permanently
- Pause history tracked in JSON

### 3. Flexible Schema with JSON
- Photos stored as JSON for flexibility (avoid separate photo table)
- Resources stored as JSON (avoid join complexity)
- Pause events stored as JSON array

### 4. Data Integrity
- Foreign keys used where appropriate (fixed_checkouts → fixed)
- Soft foreign keys where cascade not desired (job_cards → customers)
- Unique constraints on business keys (fixedCode)

### 5. Performance Optimization
- Strategic indexes on frequently queried columns
- Denormalization to reduce joins
- Composite indexes for common query patterns

---

## Proposed Schema Improvements

While the current schema is functional, here are recommended improvements for better consistency and scalability:

### Improvement 1: Add Photos Table (Normalized)

**Problem**: Photos stored as JSON makes complex queries difficult.

**Solution**: Create separate photo table with foreign key to job_cards.

```sql
CREATE TABLE job_photos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    jobId INTEGER NOT NULL,
    category TEXT NOT NULL,  -- BEFORE, AFTER, OTHER
    uri TEXT NOT NULL,
    notes TEXT,
    capturedAt INTEGER NOT NULL,
    FOREIGN KEY(jobId) REFERENCES job_cards(id) ON DELETE CASCADE
);

CREATE INDEX idx_job_photos_job_id ON job_photos(jobId);
CREATE INDEX idx_job_photos_category ON job_photos(category);
```

**Benefits**:
- Easier to query photos across jobs
- Better data integrity
- Simpler photo management logic

**Trade-offs**:
- More complex queries (requires JOIN)
- Migration complexity

---

### Improvement 2: Add Job Resources Table (Normalized)

**Problem**: Resources stored as JSON prevents inventory queries.

**Solution**: Create separate resources table.

```sql
CREATE TABLE job_resources (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    jobId INTEGER NOT NULL,
    resourceId INTEGER NOT NULL,
    itemName TEXT NOT NULL,
    itemCode TEXT NOT NULL,
    quantity REAL NOT NULL,
    unit TEXT NOT NULL,
    addedAt INTEGER NOT NULL,
    FOREIGN KEY(jobId) REFERENCES job_cards(id) ON DELETE CASCADE,
    FOREIGN KEY(resourceId) REFERENCES current(id) ON DELETE RESTRICT
);

CREATE INDEX idx_job_resources_job_id ON job_resources(jobId);
CREATE INDEX idx_job_resources_resource_id ON job_resources(resourceId);
```

**Benefits**:
- Query total resource usage across jobs
- Better inventory tracking
- Easier reporting

---

### Improvement 3: Add Foreign Key for Customer

**Problem**: Soft foreign key between job_cards and customers can lead to orphaned references.

**Solution**: Add proper foreign key with ON DELETE RESTRICT.

```sql
-- Modify job_cards table
ALTER TABLE job_cards
ADD CONSTRAINT fk_job_cards_customer
FOREIGN KEY(customerId) REFERENCES customers(id)
ON DELETE RESTRICT;
```

**Benefits**:
- Cannot delete customer with active jobs
- Data integrity enforced at database level

**Trade-offs**:
- Requires customer deletion logic to check for jobs first

---

### Improvement 4: Add Pause Events Table

**Problem**: JSON pause history is hard to query and analyze.

**Solution**: Normalize pause events.

```sql
CREATE TABLE job_pause_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    jobId INTEGER NOT NULL,
    pausedAt INTEGER NOT NULL,
    resumedAt INTEGER,
    duration INTEGER,  -- Calculated on resume (milliseconds)
    reason TEXT NOT NULL,
    resumeStatus TEXT,  -- EN_ROUTE or BUSY
    FOREIGN KEY(jobId) REFERENCES job_cards(id) ON DELETE CASCADE
);

CREATE INDEX idx_job_pause_events_job_id ON job_pause_events(jobId);
```

**Benefits**:
- Analyze pause patterns
- Calculate total pause time with SQL
- Better pause history reporting

---

### Improvement 5: Add Stock Transaction Log

**Problem**: No audit trail for stock changes.

**Solution**: Create stock transaction table.

```sql
CREATE TABLE stock_transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    resourceId INTEGER NOT NULL,
    transactionType TEXT NOT NULL,  -- CHECKOUT, RETURN, ADJUSTMENT, JOB_USAGE
    quantity REAL NOT NULL,         -- Positive = increase, Negative = decrease
    previousStock REAL NOT NULL,
    newStock REAL NOT NULL,
    jobId INTEGER,                  -- If related to job
    technicianId INTEGER,           -- Who made the transaction
    reason TEXT,
    createdAt INTEGER NOT NULL,
    FOREIGN KEY(resourceId) REFERENCES current(id) ON DELETE CASCADE
);

CREATE INDEX idx_stock_transactions_resource_id ON stock_transactions(resourceId);
CREATE INDEX idx_stock_transactions_job_id ON stock_transactions(jobId);
```

**Benefits**:
- Complete audit trail of stock changes
- Identify stock discrepancies
- Track technician resource usage patterns

---

### Improvement 6: Add Job Status History

**Problem**: Cannot track when status changes occurred.

**Solution**: Create status history table.

```sql
CREATE TABLE job_status_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    jobId INTEGER NOT NULL,
    fromStatus TEXT,
    toStatus TEXT NOT NULL,
    changedAt INTEGER NOT NULL,
    technicianId INTEGER NOT NULL,
    notes TEXT,
    FOREIGN KEY(jobId) REFERENCES job_cards(id) ON DELETE CASCADE
);

CREATE INDEX idx_job_status_history_job_id ON job_status_history(jobId);
```

**Benefits**:
- Track complete job lifecycle
- Performance metrics (time in each status)
- Identify bottlenecks in workflow

---

## Migration Strategy

### Current State (v13)
- 8 tables
- JSON-based storage for complex relationships
- Denormalized customer data
- Destructive migration fallback

### Recommended Migration Path

**Phase 1: Add Audit Tables** (v14)
1. Add `stock_transactions` table
2. Add `job_status_history` table
3. Keep existing JSON fields for backward compatibility

**Phase 2: Normalize Photos** (v15)
1. Add `job_photos` table
2. Migrate existing JSON photos to table
3. Keep JSON fields populated for rollback capability

**Phase 3: Normalize Resources** (v16)
1. Add `job_resources` table
2. Migrate JSON resources to table
3. Deprecate (but keep) resourcesUsed JSON field

**Phase 4: Add Foreign Keys** (v17)
1. Add foreign key constraints
2. Update repository logic to handle constraints

**Migration Script Template**:

```kotlin
// Example: v13 to v14 migration
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add stock_transactions table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS stock_transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                resourceId INTEGER NOT NULL,
                transactionType TEXT NOT NULL,
                quantity REAL NOT NULL,
                previousStock REAL NOT NULL,
                newStock REAL NOT NULL,
                jobId INTEGER,
                technicianId INTEGER,
                reason TEXT,
                createdAt INTEGER NOT NULL
            )
        """)

        database.execSQL("""
            CREATE INDEX idx_stock_transactions_resource_id
            ON stock_transactions(resourceId)
        """)
    }
}
```

---

## Indexing Strategy

### Current Indexes

| Table | Index | Columns | Purpose |
|-------|-------|---------|---------|
| job_cards | idx_job_cards_status | status | Filter by status |
| job_cards | idx_job_cards_scheduled_date | scheduledDate | Sort by schedule |
| job_cards | idx_job_cards_is_my_job | isMyJob | My jobs vs available |
| job_cards | idx_job_cards_my_job_status | isMyJob, status | Combined filter |
| fixed | idx_fixed_code | fixedCode (UNIQUE) | Lookup by code |
| fixed_checkouts | idx_fixed_checkouts_fixed_id | fixedId | History lookup |
| fixed_checkouts | idx_fixed_checkouts_technician_id | technicianId | Tech's checkouts |
| fixed_checkouts | idx_fixed_checkouts_job_id | jobId | Job's assets |

### Recommended Additional Indexes

```sql
-- For job search queries
CREATE INDEX idx_job_cards_customer_name ON job_cards(customerName);
CREATE INDEX idx_job_cards_job_number ON job_cards(jobNumber);

-- For time-based queries
CREATE INDEX idx_job_cards_created_at ON job_cards(createdAt);
CREATE INDEX idx_job_cards_completed_at ON job_cards(endTime);

-- For sync operations
CREATE INDEX idx_job_cards_sync_status ON job_cards(isSynced);

-- For asset lookups
CREATE INDEX idx_current_item_code ON current(itemCode);
CREATE INDEX idx_current_category ON current(category);

-- For checkout queries
CREATE INDEX idx_tool_checkouts_technician_id ON tool_checkouts(technicianId);
CREATE INDEX idx_tool_checkouts_resource_id ON tool_checkouts(resourceId);
CREATE INDEX idx_tool_checkouts_returned ON tool_checkouts(isReturned);
```

---

## Data Integrity Rules

### 1. Job Status Constraints
- Only ONE job per technician can have `status = 'BUSY'`
- Status transitions must follow state machine:
  - `AVAILABLE → AWAITING → PENDING → EN_ROUTE → BUSY → COMPLETED`
  - `BUSY ↔ PAUSED` (bidirectional)
  - Any state → `CANCELLED`

**Enforcement**: Application-level (enforced in Repository)

### 2. Stock Integrity
- Stock can be negative (backorder scenario)
- Stock changes must be logged if audit table exists
- Job completion must deduct resources atomically

**Enforcement**: Transaction-based (Room @Transaction)

### 3. Technician Session Integrity
- Only one `current_technician` record (id=1)
- `currentActiveJobId` must reference valid job
- Job referenced must have `status IN ('BUSY', 'PAUSED', 'EN_ROUTE')`

**Enforcement**: DAO-level with suspend functions

### 4. Asset Checkout Integrity
- Cannot checkout unavailable asset (`isAvailable = 0`)
- Must provide checkout reason
- Return must include condition assessment

**Enforcement**: Repository business logic

### 5. Referential Integrity
- `fixed_checkouts.fixedId` → `fixed.id` (CASCADE DELETE)
- Soft references maintained with cached fields

---

## Performance Considerations

### Query Optimization
1. **Use Flows for reactive data** - Automatic UI updates without polling
2. **Batch operations** - Use `@Transaction` for multi-step operations
3. **Selective loading** - Avoid loading photos/large JSON in list queries
4. **Pagination** - Implement paging for large job lists

### Storage Optimization
1. **Photo compression** - Compress before storing file paths
2. **JSON field limits** - Consider moving to separate tables if JSON exceeds 1MB
3. **Cleanup policies** - Archive old completed jobs periodically

### Indexing Best Practices
1. Index columns used in WHERE, ORDER BY, GROUP BY
2. Composite indexes for multi-column filters
3. Avoid over-indexing (slows writes)

---

## Conclusion

This database design provides a solid foundation for the Job Card Management application with:
- **Offline-first** capabilities for field technicians
- **Audit trails** for accountability
- **Flexible schema** with JSON for rapid development
- **Performance optimization** through strategic indexing
- **Migration path** for future normalization improvements

The current schema (v13) is production-ready but can be enhanced with the proposed improvements for better scalability and analytics capabilities.

---

## Appendix: Schema Evolution History

| Version | Changes | Date |
|---------|---------|------|
| v1 | Initial schema | - |
| v13 | Changed asset quantities from INT to DOUBLE | Current |
| v14 (Proposed) | Add audit tables | Future |
| v15 (Proposed) | Normalize photos | Future |
| v16 (Proposed) | Normalize resources | Future |
| v17 (Proposed) | Add foreign key constraints | Future |

---

**Document Version**: 1.0
**Last Updated**: 2025-11-12
**Author**: System Analysis
