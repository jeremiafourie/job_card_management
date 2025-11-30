# System Design Document - Job Card Management Application

## Executive Summary

The **Job Card Management Application** is a mobile field service management solution designed for HVAC/refrigeration technicians to manage, track, and complete service job cards from their Android devices. The application enables field workers to efficiently handle job assignments, track work progress, document service activities, manage inventory, and provide comprehensive job completion records with photos, signatures, and resource usage tracking.

**Target Users**: Field service technicians, HVAC installers, maintenance workers
**Platform**: Android (Minimum SDK 26 / Android 8.0)
**Architecture**: Offline-first mobile application with backend synchronization
**Primary Use Case**: Field service job execution and tracking

### Current Implementation Snapshot (v22)
- Single-technician app; `technician` table is a singleton.
- Job status, job timelines, and fixed-asset availability are driven by `statusHistory` JSON stored on `job_cards` and `fixed_assets`.
- Photos are stored locally as JSON-encoded URIs pointing to app-managed copies (camera or gallery copied into app storage via FileProvider); purchase receipts are single-file fields on each `job_purchase` stored the same way.
- Job creation is available from the Jobs list (“+”); new jobs start with a seeded status history per schema.
- Consumables usage is tracked via `job_inventory_usage`; fixed-asset checkouts/returns append status events and maintain per-job checkout rows.
- Migrations are destructive in this build (`fallbackToDestructiveMigration`); add real migrations before production.

---

## Table of Contents

1. [System Overview](#system-overview)
2. [User Personas](#user-personas)
3. [Core Features](#core-features)
4. [User Workflows](#user-workflows)
5. [Screen-by-Screen User Guide](#screen-by-screen-user-guide)
6. [Business Processes](#business-processes)
7. [Use Cases and Scenarios](#use-cases-and-scenarios)
8. [User Experience Design](#user-experience-design)
9. [Integration Points](#integration-points)
10. [Success Metrics](#success-metrics)

---

## System Overview

### Purpose

The Job Card Management Application solves critical problems in field service operations:

**Problems Solved**:
1. **Paper-based inefficiency** - Eliminates paper job cards, clipboards, and manual forms
2. **Poor visibility** - Provides real-time job status to technicians and dispatch
3. **Resource tracking gaps** - Automatically tracks parts/materials used on jobs
4. **Documentation challenges** - Captures photos, signatures, and notes digitally
5. **Time tracking issues** - Automatically records time spent on each job stage
6. **Inventory discrepancies** - Links resource usage to specific jobs
7. **Compliance gaps** - Ensures all required information is captured

### Key Capabilities

- **Job Management**: View, claim, accept, and execute service jobs
- **Time Tracking**: Automatic tracking of job lifecycle timestamps
- **Photo Documentation**: Before/after/during photos with notes
- **Digital Signatures**: Customer signature capture for job completion
- **Resource Tracking**: Record parts, materials, and tools used
- **Inventory Management**: Real-time inventory tracking and low-stock alerts
- **Asset Checkout**: Track checkout/return of tools and equipment
- **Offline Operation**: Full functionality without internet connection
- **Sync Capability**: Background synchronization with backend when online

### Technology Highlights

- **Modern UI**: Material Design 3 with Jetpack Compose
- **Offline-First**: Local SQLite database with Room
- **Real-time Updates**: Reactive UI with Kotlin Flows
- **Photo Capture**: Native Android camera integration
- **Location Services**: GPS integration for job site navigation
- **Data Security**: Encrypted local storage for sensitive data

---

## User Personas

### Primary Persona: Field Technician (Mike)

**Demographics**:
- Age: 28-45
- Role: HVAC/Refrigeration Technician
- Experience: 3-10 years in field service
- Tech Savvy: Moderate (uses smartphone daily)

**Goals**:
- Complete jobs efficiently
- Minimize paperwork and data entry
- Have all job information readily available
- Track time accurately for billing
- Avoid running out of parts during jobs
- Provide professional service documentation

**Pain Points**:
- Forgets to document work performed
- Paper forms get lost or damaged
- Unclear job details from dispatch
- Doesn't know what parts are in the van
- Time tracking is manual and error-prone
- Customer signature collection is awkward

**How This App Helps Mike**:
- All job info in one place on his phone
- Automatic time tracking - no manual logging
- Camera makes documentation easy
- Digital signature is quick and professional
- Real-time inventory of what's in the van
- Offline mode works even in basements/remote areas

---

### Secondary Persona: Senior Technician (Sarah)

**Demographics**:
- Age: 35-55
- Role: Lead Technician / Supervisor
- Experience: 10+ years
- Tech Savvy: Moderate (prefers proven tools)

**Goals**:
- Maintain high quality standards
- Mentor junior technicians
- Ensure proper documentation
- Track team resource usage
- Monitor job progress
- Minimize callbacks and rework

**Pain Points**:
- Junior techs forget to document issues
- Hard to verify work was done properly
- Resource usage not tracked consistently
- Photos are inconsistent or missing
- Follow-up items get lost

**How This App Helps Sarah**:
- Required fields ensure complete documentation
- Before/after photos provide visual proof of work
- Resource tracking shows exactly what was used
- Notes field captures issues encountered
- Follow-up flag ensures nothing is forgotten

---

## Core Features

### 1. Job Management

**What It Does**: Allows technicians to view, claim, and manage service job assignments.

**User Benefits**:
- See all available jobs and assigned jobs in one place
- Understand job details before accepting
- Track progress through job lifecycle
- Never lose track of what needs to be done

**Key Functionality**:
- View available jobs (not yet assigned)
- Claim jobs from available pool
- Accept assigned jobs
- Filter jobs by status (Pending, En Route, Busy, Paused, Completed)
- Search jobs by customer, address, or job number
- View scheduled time and estimated duration
- Navigate to job site with maps integration

---

### 2. Time Tracking

**What It Does**: Automatically records timestamps for every stage of the job lifecycle.

**User Benefits**:
- No manual time entry required
- Accurate billing data
- Proof of when work was performed
- Track pause time (lunch, parts runs, etc.)

**Tracked Time Points**:
- **Accepted At**: When technician accepts the job
- **En Route**: Travel time to job site
- **Start Time**: When actual work begins
- **Pause Time**: Total time paused (with reasons)
- **End Time**: When work is completed

**Automatic Calculations**:
- Total job duration (start to end)
- Actual work time (minus pauses)
- Travel time (en route duration)
- Pause duration with breakdown by reason

---

### 3. Photo Documentation

**What It Does**: Enables capture and organization of photos throughout the job.

**User Benefits**:
- Visual proof of work performed
- Document conditions before and after
- Capture issues or damages
- Professional service documentation

**Photo Categories**:
1. **Before Photos**: Initial condition, existing issues, starting point
2. **After Photos**: Completed work, final condition, quality verification
3. **Other Photos**: During work, issues found, additional documentation

**Photo Features**:
- Add notes to any photo (e.g., "Refrigerant leak at compressor")
- Move photos between categories if needed
- Delete photos before submitting job
- Multiple photos per category
- Photos stored locally (no data usage)

**User Workflow**:
```
1. Arrive at job → Take "Before" photos
2. Discover issue → Take "Other" photo with notes
3. Complete work → Take "After" photos
4. Submit job → All photos included in completion record
```

---

### 4. Customer Signature

**What It Does**: Digital signature capture for customer approval and acceptance.

**User Benefits**:
- Professional completion process
- Immediate customer acknowledgment
- No paper forms to lose
- Legally binding signature record

**Capture Method**:
- Touch screen signature pad
- Clear button to retry if needed
- Signature stored as image (Base64)
- Included in job completion package

---

### 5. Resource Tracking

**What It Does**: Record parts, materials, and supplies used on each job.

**User Benefits**:
- Accurate billing for materials
- Automatic inventory deduction
- Proof of what was used and why
- Helps with restocking decisions

**Resource Types**:
- **Refrigerants**: R410A, R32, etc. (measured in kg)
- **Parts**: Capacitors, contactors, filters (pieces)
- **Materials**: Copper pipe, insulation (meters/rolls)
- **Consumables**: Nitrogen, oil, sealants (liters/bottles)

**How It Works**:
1. During or after job, add resources used
2. Specify quantity and unit of measure
3. Resources linked to job card
4. Upon job completion, inventory automatically decremented
5. Low stock alerts trigger when below minimum

**Example**:
```
Job #12345 - AC Repair
Resources Used:
- R410A Refrigerant: 1.5 kg
- Capacitor 35uF: 1 piece
- Filter 16x20: 1 piece
→ On completion: Stock levels updated automatically
```

---

### 6. Inventory Management

**What It Does**: Track consumable inventory items in real-time.

**User Benefits**:
- Know what's available before starting job
- Get alerts when stock is low
- Never run out of critical parts mid-job
- Track usage patterns

**Inventory Features**:
- Current stock levels with units (kg, pieces, meters, etc.)
- Minimum stock thresholds
- Low stock indicators
- Category organization (Refrigerants, Parts, Tools, Materials)
- Search and filter capabilities

**Consumable Assets Screen**:
```
Refrigerants
├── R410A: 25.5 kg (Min: 10 kg) ✅
├── R32: 5 kg (Min: 10 kg) ⚠️ LOW STOCK
└── R22: 0 kg (Min: 5 kg) 🔴 OUT OF STOCK

Parts
├── Capacitor 35uF: 8 pieces (Min: 5) ✅
└── Contactor 3-pole 40A: 3 pieces (Min: 5) ⚠️ LOW STOCK
```

---

### 7. Fixed Asset Management

**What It Does**: Track durable tools, equipment, and assets with checkout/return system.

**User Benefits**:
- Know what tools are available
- Track who has what equipment
- Accountability for expensive tools
- Maintenance scheduling reminders

**Asset Types**:
- **Tools**: Power drills, vacuum pumps, manifold gauges, leak detectors
- **Air Conditioners**: Units being installed or serviced
- **Ladders**: Extension ladders, step ladders
- **Equipment**: Refrigerant recovery machines, pressure testers
- **Vehicles**: Service vans, trucks
- **Meters**: Multimeters, clamp meters, thermometers
- **Pumps**: Vacuum pumps, transfer pumps

**Checkout Process**:
1. Search for asset (e.g., "Vacuum Pump")
2. Select asset to checkout
3. Specify reason (e.g., "AC installation at 123 Main St")
4. Optional: Link to specific job
5. Record asset condition (Good/Fair/Poor)
6. Confirm checkout
7. Asset marked as "Unavailable" and assigned to technician

**Return Process**:
1. View "My Active Checkouts"
2. Select asset to return
3. Record return condition
4. Add notes if condition changed
5. Confirm return
6. Asset marked as "Available" again

**Maintenance Tracking**:
- Last maintenance date
- Next maintenance due date
- Alerts when maintenance is overdue
- Service history log

---

### 8. Job Notes and Documentation

**What It Does**: Capture comprehensive job details, issues, and observations.

**User Benefits**:
- Document unexpected issues
- Provide detailed work descriptions
- Flag follow-up needs
- Leave notes for future technicians

**Note Fields**:
1. **Work Performed**: What was done (required for completion)
2. **Technician Notes**: Additional observations or recommendations
3. **Issues Encountered**: Problems found during work
4. **Follow-up Notes**: What needs to be done next (if follow-up required)

**Example**:
```
Work Performed:
"Replaced faulty compressor capacitor (35uF). Tested system operation.
All parameters within normal range. Refrigerant levels good."

Issues Encountered:
"Electrical panel showing signs of corrosion. Recommended replacement
within 6 months to prevent future failures."

Follow-up Required: ☑️ Yes
Follow-up Notes:
"Schedule electrical panel replacement. Customer aware and interested.
Provided estimate of $450-600 for panel upgrade."
```

---

## User Workflows

### Workflow 1: Claiming and Accepting a Job

**Scenario**: Mike starts his day and wants to pick up new jobs.

**Steps**:
1. **Open App** → Dashboard screen shows overview
2. **Navigate to Jobs Tab** → See "Available Jobs" (4 jobs)
3. **Tap "Available" Tab** → Browse unassigned jobs
4. **Review Job Details**:
   - Customer: "ABC Corp"
   - Location: "123 Industrial Park, 5.2 miles away"
   - Job Type: "AC Repair"
   - Scheduled: "Today, 9:00 AM"
   - Estimated Duration: "2 hours"
5. **Tap "Claim Job" Button** → Job assigned to Mike
6. **Job Status Changes**: AVAILABLE → AWAITING
7. **Review Full Details** → Read description, customer notes
8. **Tap "Accept Job" Button** → Commit to doing the job
9. **Job Status Changes**: AWAITING → PENDING
10. **Ready to Start** → Job appears in "My Jobs - Pending"

**Result**: Job is now in Mike's queue, ready to start when he arrives.

---

### Workflow 2: Executing a Job (Complete Lifecycle)

**Scenario**: Mike executes a standard AC repair job from start to finish.

#### Phase 1: Preparation
```
1. Open job from "My Jobs - Pending"
2. Review customer info, job details, scheduled time
3. Check inventory for parts that might be needed
4. Tap "Start Job" → Choose "En Route"
5. Status: PENDING → EN_ROUTE (travel time tracked)
6. Optionally tap "Navigate" to open maps
```

#### Phase 2: Arrival and Assessment
```
7. Arrive at job site
8. Tap "Start Job" again → "Begin Work"
9. Status: EN_ROUTE → BUSY (work time tracked)
10. Take "Before Photos":
    - Photo of AC unit (exterior)
    - Photo of control panel
    - Photo of any visible issues
    Add notes: "Unit not cooling, fan running"
```

#### Phase 3: Work Execution
```
11. Diagnose issue → Bad compressor capacitor
12. Take "Other Photo": Close-up of capacitor with bulge
    Add note: "Capacitor shows physical damage (bulge)"
13. Replace capacitor with new one from van
14. Test system operation
```

#### Phase 4: Pause (if needed)
```
15. If need to get parts:
    - Tap "Pause Job"
    - Select reason: "Parts run"
    - Status: BUSY → PAUSED (pause time tracked)
16. When return:
    - Tap "Resume Job" → Choose "Continue Work"
    - Status: PAUSED → BUSY
```

#### Phase 5: Completion Documentation
```
17. After repair complete, take "After Photos":
    - Photo of installed capacitor
    - Photo of pressure readings (normal)
    - Photo of temperature differential (working)
    Add note: "System cooling properly, 18°F delta T"

18. Add resources used:
    - Tap "Add Resource"
    - Select "Capacitor 35uF"
    - Quantity: 1 piece
    - Tap "Add"

19. Fill in job details:
    - Work Performed: "Replaced faulty compressor capacitor..."
    - Issues Encountered: "Capacitor failed due to age..."
    - Technician Notes: "System running optimally..."

20. Get customer signature:
    - Hand phone to customer
    - Customer signs on screen
    - Tap "Save Signature"

21. Mark follow-up if needed:
    - Requires Follow-up: YES
    - Follow-up Notes: "Schedule preventive maintenance in 6 months"

22. Tap "Complete Job" button
```

#### Phase 6: Job Submission
```
23. System performs automatic actions:
    - Validates all required fields filled
    - Records completion timestamp
    - Deducts resources from inventory
    - Marks job as COMPLETED
    - Sets job as ready for sync

24. Success message shown
25. Job moves to "Completed" tab
26. Dashboard updates (active job count decreases)
27. Inventory updated (Capacitor stock: 8 → 7)
```

**Total Time Tracked**:
- En Route: 15 minutes
- Active Work: 1 hour 45 minutes
- Paused: 30 minutes (parts run)
- Total Job Time: 2 hours 30 minutes

**Result**: Complete job record with photos, signature, time tracking, resource usage, and documentation. Ready for billing and customer record.

---

### Workflow 3: Inventory Check and Restocking

**Scenario**: Mike wants to check what parts are in his van before heading out.

**Steps**:
1. **Open App** → Tap "Assets" tab
2. **View "Inventory Assets"** → See all consumable inventory
3. **Check Stock Levels**:
   - R410A: 25.5 kg ✅ (Good)
   - Capacitor 35uF: 3 pieces ⚠️ (LOW STOCK)
   - Filters 16x20: 0 pieces 🔴 (OUT OF STOCK)
4. **Use Category Filter** → "Filters" to see all filter types
5. **Search for specific item** → "cap" to find all capacitors
6. **Note low stock items** → Make mental note to restock
7. **Plan accordingly** → Avoid jobs requiring unavailable parts

**Result**: Mike knows he's low on 35uF capacitors and out of 16x20 filters. He'll swing by the warehouse before tackling those types of jobs.

---

### Workflow 4: Tool Checkout

**Scenario**: Mike needs a vacuum pump for an AC installation job.

**Steps**:
1. **Open App** → Tap "Assets" tab
2. **Select "Fixed Assets"** tab
3. **Search** → Type "vacuum"
4. **Find Asset**: "Vacuum Pump 6 CFM" (Available ✅)
5. **Tap Asset** → View details (serial #, condition, last maintenance)
6. **Tap "Checkout" Button**
7. **Fill Checkout Form**:
   - Reason: "AC installation"
   - Link to Job: #12345 (optional)
   - Condition: Good
   - Notes: (optional)
8. **Tap "Confirm Checkout"**
9. **Asset Status Updates**:
   - isAvailable: false
   - currentHolder: "Mike Wilson"
10. **Confirmation** → "Vacuum Pump checked out successfully"

**Later - Return Flow**:
1. **Open "My Active Checkouts"** (in Assets screen)
2. **See list**: Vacuum Pump 6 CFM (checked out 3 hours ago)
3. **Tap asset** → Tap "Return"
4. **Fill Return Form**:
   - Return Condition: Good
   - Notes: (optional, e.g., "Oil level low, recommend service")
5. **Tap "Confirm Return"**
6. **Asset Status Updates**:
   - isAvailable: true
   - currentHolder: null
7. **Checkout record closed** (returnTime recorded)

**Result**: Complete audit trail of who had the vacuum pump, when, and for what job. Accountability for expensive equipment.

---

### Workflow 5: Handling Unexpected Issues

**Scenario**: Mike discovers additional problems during a routine service call.

**Original Job**: Annual AC maintenance
**Discovery**: Refrigerant leak detected

**Steps**:
1. **During inspection**, detect hissing sound
2. **Take "Other Photo"**:
   - Photo of evaporator coil area
   - Note: "Suspected refrigerant leak at evaporator coil connection"
3. **Add to "Issues Encountered" field**:
   ```
   "During routine maintenance, detected refrigerant leak at evaporator
   coil brazed connection. System pressure low. Leak rate approx 1 lb/week
   based on customer report of gradually declining cooling performance."
   ```
4. **Discuss with customer** → Needs repair, beyond scope of maintenance
5. **Mark follow-up required**: YES
6. **Add follow-up notes**:
   ```
   "Requires leak repair and system recharge. Estimated 2-3 hours labor
   plus refrigerant. Customer wants quote before proceeding. Schedule
   follow-up after quote approval."
   ```
7. **Complete original maintenance work**
8. **Get customer signature** with verbal agreement on follow-up
9. **Submit job as COMPLETED** with follow-up flag

**Result**: Original job completed and documented. Follow-up need clearly flagged and documented for scheduling team. Customer aware and informed.

---

## Screen-by-Screen User Guide

### 1. Dashboard Screen

**Purpose**: Home screen providing at-a-glance overview of work status.

**Layout**:
```
┌────────────────────────────────────┐
│  Good morning, Mike! ☀️            │
│  Tuesday, November 12              │
├────────────────────────────────────┤
│  🔧 CURRENT ACTIVE JOB             │
│  ┌──────────────────────────────┐ │
│  │ #12345 - AC Repair           │ │
│  │ ABC Corp                     │ │
│  │ 🕐 In Progress: 1h 25m       │ │
│  │ [View Job Details]           │ │
│  └──────────────────────────────┘ │
├────────────────────────────────────┤
│  📊 TODAY'S OVERVIEW               │
│  ┌─────────┬─────────┬──────────┐ │
│  │ Awaiting│ Pending │ Paused   │ │
│  │    2    │    3    │    0     │ │
│  └─────────┴─────────┴──────────┘ │
│  ┌────────────────────────────┐   │
│  │ Available Jobs:  4         │   │
│  │ [View All]                 │   │
│  └────────────────────────────┘   │
├────────────────────────────────────┤
│ Bottom Navigation:                 │
│ [🏠 Dashboard] [📋 Jobs]           │
│ [📦 Assets] [⚙️ Settings]          │
└────────────────────────────────────┘
```

**User Interactions**:
- Tap current active job → Open job detail
- Tap "View All" (Available Jobs) → Jump to Jobs tab, Available section
- Tap stat cards → Filter jobs by that status
- Swipe to refresh → Reload latest data

**Information Displayed**:
- Greeting with current time of day
- Current date
- Active job (if BUSY or PAUSED status)
  - Job number and type
  - Customer name
  - Time in current status
- Job count summaries:
  - Awaiting (assigned but not accepted)
  - Pending (accepted, ready to start)
  - Paused (temporarily stopped)
  - Available (unassigned, claimable)

---

### 2. Jobs Screen

**Purpose**: View, search, filter, and manage all job cards.

**Layout**:
```
┌────────────────────────────────────┐
│  My Jobs  |  Available Jobs        │ ← Tabs
├────────────────────────────────────┤
│  🔍 [Search jobs...]               │
├────────────────────────────────────┤
│  Filter: [All] Pending En Route   │
│         [Busy] Paused Completed    │ ← Status chips
├────────────────────────────────────┤
│  📋 Job Cards List                 │
│  ┌──────────────────────────────┐ │
│  │ #12345 🔴 BUSY               │ │
│  │ AC Repair                    │ │
│  │ ABC Corp - 123 Main St       │ │
│  │ 📅 Today 9:00 AM • 2h est.   │ │
│  │ 🕐 In progress: 1h 25m       │ │
│  └──────────────────────────────┘ │
│  ┌──────────────────────────────┐ │
│  │ #12346 🟡 PENDING            │ │
│  │ AC Installation              │ │
│  │ XYZ Industries - 456 Oak Ave │ │
│  │ 📅 Today 1:00 PM • 3h est.   │ │
│  └──────────────────────────────┘ │
│  ┌──────────────────────────────┐ │
│  │ #12347 ⚫ PAUSED              │ │
│  │ AC Maintenance               │ │
│  │ Tech Solutions - 789 Elm St  │ │
│  │ ⏸ Waiting for parts         │ │
│  └──────────────────────────────┘ │
└────────────────────────────────────┘
```

**My Jobs Tab**:
- All jobs assigned to current technician
- Grouped/filtered by status
- Sorted by scheduled date/time
- Shows current status and time info

**Available Jobs Tab**:
- Unassigned jobs any technician can claim
- Shows distance from current location
- "Claim" button on each job card
- Filters by job type, urgency, location

**Job Card Information**:
- Job number (unique identifier)
- Status badge (color-coded)
- Job type (Installation, Repair, Service, Inspection)
- Customer name
- Service address
- Scheduled date and time
- Estimated duration
- Current elapsed time (if active)
- Pause reason (if paused)

**Status Color Coding**:
- 🟢 PENDING - Ready to start (Green)
- 🔵 EN_ROUTE - Traveling to job (Blue)
- 🔴 BUSY - Actively working (Red)
- ⚫ PAUSED - Temporarily stopped (Gray)
- ✅ COMPLETED - Finished (Green checkmark)
- ❌ CANCELLED - Cancelled (Red X)
- 🟡 AWAITING - Assigned, not accepted (Yellow)

**User Interactions**:
- Tap job card → Open job detail screen
- Swipe right → Quick actions (Call customer, Navigate)
- Pull down → Refresh list
- Tap status chip → Filter by that status
- Use search → Find jobs by customer, address, or job number

---

### 3. Job Detail Screen

**Purpose**: Complete job information and execution interface.

**Layout** (Scrollable):

```
┌────────────────────────────────────┐
│  ← Back          #12345      ⋮ Menu│
├────────────────────────────────────┤
│  🔴 BUSY • 1h 25m in progress      │
│                                    │
│  AC Repair                         │
│  ABC Corporation                   │
│  📞 (555) 123-4567                 │
│  📧 contact@abccorp.com            │
│  📍 123 Main Street, Suite 200     │
│      Springfield, IL 62701         │
│  [Navigate 🗺]                     │
├────────────────────────────────────┤
│  📅 SCHEDULE                       │
│  Scheduled: Today, 9:00 AM         │
│  Duration: 2 hours (estimated)     │
│                                    │
│  ⏱ TIME TRACKING                   │
│  Accepted:   8:30 AM               │
│  En Route:   8:45 AM (15 min)      │
│  Started:    9:00 AM               │
│  Now:        10:25 AM (1h 25m)     │
├────────────────────────────────────┤
│  📸 PHOTOS (5)                     │
│  Before (2) | After (0) | Other (3)│
│  [📷 Add Photo]                    │
│  ┌────┬────┬────┬────┐            │
│  │img │img │img │img │            │
│  └────┴────┴────┴────┘            │
├────────────────────────────────────┤
│  🔧 RESOURCES USED                 │
│  • Capacitor 35uF × 1 piece        │
│  • R410A Refrigerant × 0.5 kg      │
│  [+ Add Resource]                  │
├────────────────────────────────────┤
│  📝 WORK DETAILS                   │
│  Work Performed:                   │
│  [Text field - filled]             │
│                                    │
│  Issues Encountered:               │
│  [Text field - filled]             │
│                                    │
│  Technician Notes:                 │
│  [Text field]                      │
├────────────────────────────────────┤
│  ✍️ CUSTOMER SIGNATURE             │
│  [________signature________]       │
│  ✓ Signed by John Smith           │
├────────────────────────────────────┤
│  🔄 FOLLOW-UP                      │
│  [✓] Requires Follow-up            │
│  Notes: [Text field]               │
├────────────────────────────────────┤
│  [⏸ Pause Job]   [✅ Complete Job]│
└────────────────────────────────────┘
```

**Sections Explained**:

1. **Header**:
   - Current status with time in status
   - Job number for reference
   - Menu for additional actions (cancel, edit)

2. **Job Information**:
   - Job type and title
   - Customer contact info (tap to call/email)
   - Service address (tap to copy)
   - Navigate button (opens maps app)

3. **Schedule & Time Tracking**:
   - Scheduled date/time
   - Estimated duration
   - Actual time tracking:
     - When accepted
     - Travel time (en route)
     - When work started
     - Current elapsed time
   - Pause history (if applicable)

4. **Photos Section**:
   - Categorized tabs (Before/After/Other)
   - Photo count per category
   - Add Photo button (opens camera)
   - Thumbnail gallery
   - Tap photo to view full screen
   - Long-press to add/edit notes or delete

5. **Resources Used**:
   - List of parts/materials consumed
   - Quantity and unit of measure
   - Add Resource button
     - Search inventory
     - Select item
     - Enter quantity
     - Auto-suggests based on job type

6. **Work Details**:
   - **Work Performed** (required): Description of work done
   - **Issues Encountered**: Problems found during job
   - **Technician Notes**: Additional observations, recommendations

7. **Customer Signature**:
   - Signature pad for customer approval
   - Shows signer name and timestamp
   - Clear/retake option

8. **Follow-up**:
   - Checkbox to flag follow-up required
   - Notes field for follow-up details

9. **Action Buttons**:
   - **Pause Job**: Temporarily stop work
     - Select reason (Break, Parts run, Waiting for customer, etc.)
     - Tracks pause time
   - **Complete Job**: Finalize and submit
     - Validates required fields
     - Deducts resources from inventory
     - Records completion time
     - Marks for sync

**User Interactions**:
- Tap customer phone → Call customer
- Tap customer email → Send email
- Tap address → Copy to clipboard
- Tap Navigate → Open Google Maps / Waze
- Tap photo thumbnail → Full-screen view
- Long-press photo → Options (delete, move category, edit notes)
- Tap resource → Edit quantity or remove
- Tap text field → Edit with keyboard
- Tap signature pad → Draw signature
- Tap Pause → Show pause reason dialog
- Tap Complete → Validate and submit job

**State-Dependent Actions**:

| Current Status | Available Actions |
|---------------|-------------------|
| PENDING | Start Job (En Route or Begin Work) |
| EN_ROUTE | Arrive (Begin Work), Pause, Cancel |
| BUSY | Pause, Complete, Cancel |
| PAUSED | Resume (Continue Work or Return En Route) |

---

### 4. Assets Screen

**Purpose**: Manage inventory and fixed assets.

**Layout**:
```
┌────────────────────────────────────┐
│  Inventory Assets  |  Fixed Assets   │ ← Tabs
├────────────────────────────────────┤
│  🔍 [Search assets...]             │
├────────────────────────────────────┤
│  Category: [All ▾]                 │ ← Filter dropdown
│  Refrigerants | Parts | Materials  │ ← Quick filters
├────────────────────────────────────┤
│  📦 CONSUMABLE INVENTORY           │
│  ┌──────────────────────────────┐ │
│  │ R410A Refrigerant Gas        │ │
│  │ GAS-R410A                    │ │
│  │ Stock: 25.5 kg / Min: 10 kg │ │
│  │ ▓▓▓▓▓▓▓░░░ 75% ✅           │ │
│  └──────────────────────────────┘ │
│  ┌──────────────────────────────┐ │
│  │ Capacitor 35uF               │ │
│  │ CAPACITOR-35UF               │ │
│  │ Stock: 3 pc / Min: 5 pc      │ │
│  │ ▓▓░░░░░░░░ 60% ⚠️ LOW       │ │
│  └──────────────────────────────┘ │
│  ┌──────────────────────────────┐ │
│  │ Filter 16x20                 │ │
│  │ FILTER-16X20                 │ │
│  │ Stock: 0 pc / Min: 5 pc      │ │
│  │ ░░░░░░░░░░ 0% 🔴 OUT        │ │
│  └──────────────────────────────┘ │
└────────────────────────────────────┘
```

**Inventory Assets Tab** (Consumables):

**Asset Card Shows**:
- Item name
- Item code
- Current stock level with unit
- Minimum stock threshold
- Visual stock level indicator (progress bar)
- Status badge:
  - ✅ Good Stock (above minimum)
  - ⚠️ Low Stock (at or below minimum)
  - 🔴 Out of Stock (zero)

**User Interactions**:
- Search by name or code
- Filter by category
- Sort by stock level, name, or category
- Tap asset → View usage history
- No direct stock adjustment (updated via job completion)

---

**Fixed Assets Tab** (Tools/Equipment):

```
┌────────────────────────────────────┐
│  Inventory Assets  |  Fixed Assets   │
├────────────────────────────────────┤
│  🔍 [Search fixed assets...]       │
├────────────────────────────────────┤
│  Type: [All ▾]                     │
│  Tools | Equipment | Vehicles      │
├────────────────────────────────────┤
│  🔧 FIXED ASSETS                   │
│  ┌──────────────────────────────┐ │
│  │ Vacuum Pump 6 CFM            │ │
│  │ VP-6CFM-001                  │ │
│  │ ✅ Available                 │ │
│  │ SN: VP123456789              │ │
│  │ [Checkout]                   │ │
│  └──────────────────────────────┘ │
│  ┌──────────────────────────────┐ │
│  │ Manifold Gauge Set           │ │
│  │ GAUGE-4PORT-002              │ │
│  │ 🔴 Checked out               │ │
│  │ Holder: John Smith           │ │
│  │ [View Details]               │ │
│  └──────────────────────────────┘ │
│  ┌──────────────────────────────┐ │
│  │ Extension Ladder 24ft        │ │
│  │ LADDER-EXT-24-003            │ │
│  │ ✅ Available                 │ │
│  │ ⚠️ Maintenance due in 5 days│ │
│  │ [Checkout]                   │ │
│  └──────────────────────────────┘ │
├────────────────────────────────────┤
│  MY ACTIVE CHECKOUTS (3)           │
│  [View All]                        │
└────────────────────────────────────┘
```

**Fixed Asset Card Shows**:
- Asset name
- Asset code
- Availability status
- Serial number
- Current holder (if checked out)
- Maintenance status
- Action button (Checkout or View Details)

**User Interactions**:
- Search by name, code, or serial number
- Filter by type (Tool, Equipment, Vehicle, etc.)
- Filter by availability
- Tap available asset → Checkout dialog
- Tap checked out asset → View details and history
- View "My Active Checkouts" → Return dialog

**Checkout Dialog**:
```
┌────────────────────────────────────┐
│  Checkout: Vacuum Pump 6 CFM       │
├────────────────────────────────────┤
│  Reason:                           │
│  [Text field: "AC installation"]   │
│                                    │
│  Link to Job (optional):           │
│  [Dropdown: Select job or None]    │
│                                    │
│  Condition:                        │
│  ⦿ Good  ○ Fair  ○ Poor           │
│                                    │
│  Notes (optional):                 │
│  [Text field]                      │
│                                    │
│  [Cancel]         [Confirm]        │
└────────────────────────────────────┘
```

**Return Dialog**:
```
┌────────────────────────────────────┐
│  Return: Vacuum Pump 6 CFM         │
├────────────────────────────────────┤
│  Checked out: 3 hours ago          │
│  For: AC installation (Job #12345) │
│                                    │
│  Return Condition:                 │
│  ⦿ Good  ○ Fair  ○ Poor           │
│                                    │
│  Notes (optional):                 │
│  [e.g., "Oil level low"]           │
│                                    │
│  [Cancel]         [Confirm]        │
└────────────────────────────────────┘
```

---

### 5. Settings Screen

**Purpose**: App preferences, user profile, and system settings.

**Layout**:
```
┌────────────────────────────────────┐
│  ⚙️ Settings                       │
├────────────────────────────────────┤
│  👤 PROFILE                        │
│  ┌──────────────────────────────┐ │
│  │  [Photo]  Mike Wilson        │ │
│  │           EMP001             │ │
│  │           Technician         │ │
│  │           ✅ On Duty         │ │
│  └──────────────────────────────┘ │
├────────────────────────────────────┤
│  📊 TODAY'S STATS                  │
│  Jobs Completed: 2                 │
│  Total Time: 5h 30m                │
│  Last Sync: 5 minutes ago          │
├────────────────────────────────────┤
│  📄 INFO                           │
│  › About (local build)             │
│  › Contact Support                 │
└────────────────────────────────────┘
```

**Profile Section**:
- Photo, name, employee number, role
- On-duty status toggle
- Current stats

**Preferences**:
- Notification settings (new jobs, reminders)
- Theme selection
- Default map/navigation app
- Camera quality settings
- Auto-sync preferences

**Legal & Info**:
- Open source license attributions
- Privacy policy
- Terms of service
- App version and build info

---

## Business Processes

### Process 1: Daily Technician Workflow

**Morning**:
1. Login to app
2. Review dashboard for assigned jobs
3. Check "Awaiting" jobs and accept them
4. Review inventory levels
5. Plan route based on job locations
6. Checkout any needed tools/equipment

**During Day**:
7. For each job:
   - Start job (en route)
   - Navigate to location
   - Arrive and begin work (busy)
   - Take before photos
   - Perform service
   - Document issues
   - Add resources used
   - Take after photos
   - Get customer signature
   - Complete job
8. Pause jobs as needed (lunch, parts runs)
9. Claim available jobs if schedule permits

**End of Day**:
10. Complete any in-progress jobs
11. Return checked out tools
12. Review completed jobs
13. Check for follow-ups flagged
14. Sync all data with backend
15. Log out

---

### Process 2: Job Lifecycle (System Perspective)

```
Job Created (Backend/Dispatch)
        ↓
    AVAILABLE
        ↓ (Technician claims)
    AWAITING
        ↓ (Technician accepts)
    PENDING
        ↓ (Technician starts - "En Route")
    EN_ROUTE
        ↓ (Technician arrives - "Begin Work")
     BUSY ←──────────┐
        │            │
        ↓ (Pause)    │ (Resume)
    PAUSED ──────────┘
        │
        ↓ (Complete)
   COMPLETED
        ↓ (Sync)
 [Backend Updated]
```

**Alternative Paths**:
- Any state → CANCELLED (with reason)
- PENDING → EN_ROUTE → BUSY (skip states if on-site)
- Multiple BUSY ↔ PAUSED cycles allowed

---

### Process 3: Inventory Replenishment

**Trigger**: Low stock alert on asset

**Steps**:
1. System detects stock below minimum threshold
2. Asset marked with ⚠️ LOW STOCK indicator
3. Technician sees alert in Assets screen
4. Technician checks usage history
5. Technician requests restock from warehouse
6. Warehouse updates stock levels (backend)
7. App syncs and updates inventory
8. Alert clears when stock above minimum

---

### Process 4: Equipment Maintenance

**Trigger**: Maintenance due date approaching

**Steps**:
1. System checks nextMaintenanceDate for all fixed assets
2. Assets with maintenance due in <7 days show warning
3. Technician sees maintenance alert on asset
4. Technician reports asset for maintenance
5. Asset marked as "Unavailable"
6. Maintenance performed
7. Asset returned to service
8. lastMaintenanceDate and nextMaintenanceDate updated
9. Asset marked as "Available"

---

## Use Cases and Scenarios

### Use Case 1: Emergency Call

**Scenario**: Customer calls with urgent AC failure. Dispatch creates job and assigns to nearest available technician.

**Flow**:
1. Dispatch creates job, marks as URGENT
2. Job appears in technician's "Awaiting" list
3. Push notification: "New urgent job assigned"
4. Technician opens app, reviews job details
5. Accepts job immediately
6. Checks inventory for common AC repair parts
7. Starts job as "En Route"
8. Navigates to location (15 min away)
9. Arrives, begins work
10. Diagnoses issue quickly (blown fuse)
11. Replaces fuse from stock
12. Tests system
13. Takes after photo
14. Gets customer signature on phone
15. Completes job (45 min total time)
16. Job syncs to backend
17. Billing automatically generated

**Result**: Rapid response to emergency, complete documentation, automatic billing, happy customer.

---

### Use Case 2: Multi-Day Installation

**Scenario**: Large AC installation requiring 2 days of work.

**Day 1**:
1. Technician accepts installation job
2. Starts work
3. Completes preparation work
4. Takes photos of progress
5. Pauses job at end of day: "Multi-day job - Day 1 complete"
6. Leaves job in PAUSED state

**Day 2**:
1. Technician opens paused job
2. Reviews Day 1 photos and notes
3. Resumes job
4. Completes installation
5. Takes before/after comparison photos
6. Adds all resources used (both days)
7. Tests system thoroughly
8. Gets customer signature
9. Completes job

**Result**: Single job record with complete history across multiple days.

---

### Use Case 3: Parts Run During Job

**Scenario**: Technician needs a part not in van.

**Flow**:
1. During job, discover need for specific capacitor
2. Tap "Pause Job"
3. Select reason: "Parts run"
4. Job status: BUSY → PAUSED (pause time starts)
5. Check inventory app: Capacitor not in van stock
6. Navigate to supply house or warehouse
7. Pick up capacitor
8. Return to job site
9. Tap "Resume Job" → "Continue Work"
10. Job status: PAUSED → BUSY (pause time recorded)
11. Install new part
12. Complete job normally

**Time Tracking**:
- Active work: 1 hour before pause + 45 min after = 1h 45m
- Parts run (paused): 30 minutes
- Total job time: 2h 15m
- Billable time: 1h 45m (or full 2h 15m based on company policy)

**Result**: Accurate time tracking separates billable work time from parts acquisition time.

---

### Use Case 4: Discovering Safety Issue

**Scenario**: During routine maintenance, technician discovers dangerous electrical issue.

**Flow**:
1. Performing standard AC maintenance
2. Notice exposed wiring in electrical panel
3. Take "Other Photo" of safety issue
4. Add detailed note on photo: "Exposed 240V wiring - SAFETY HAZARD"
5. Add to "Issues Encountered":
   ```
   "Electrical panel cover loose, exposing live 240V wiring. Immediate
   safety hazard. Temporarily secured with tape. Customer advised not
   to touch panel. URGENT: Licensed electrician required."
   ```
6. Mark "Requires Follow-up": YES
7. Follow-up notes:
   ```
   "Electrical panel requires immediate professional repair. Customer
   wants to use their electrician. Provided photos and documentation.
   Follow up in 1 week to verify repair completed before next service."
   ```
8. Get customer signature acknowledging issue
9. Complete maintenance job

**Result**: Safety issue documented with photos, customer informed, follow-up flagged, company liability covered.

---

## User Experience Design

### Design Principles

1. **Offline-First**: App must work perfectly without internet
2. **One-Handed Operation**: Most tasks doable with one hand (for on-site use)
3. **Glove-Friendly**: Large touch targets for users wearing work gloves
4. **Minimal Input**: Auto-fill, suggestions, and defaults reduce typing
5. **Clear Status**: Always show what state job is in and what to do next
6. **Progressive Disclosure**: Show basics first, details on demand
7. **Undo Safety**: Confirm destructive actions, allow undo when possible

### Accessibility

- **Large Text Support**: Scales with system font size settings
- **High Contrast**: Material 3 ensures good contrast ratios
- **Touch Targets**: Minimum 48dp for buttons (glove-friendly)
- **Voice Input**: Supports voice-to-text for notes fields
- **Screen Reader**: Compatible with TalkBack (for visually impaired)

### Performance

- **Fast Launch**: App opens in <2 seconds
- **Instant Actions**: Button presses respond immediately
- **Smooth Scrolling**: 60 FPS list scrolling
- **Photo Optimization**: Compress photos automatically
- **Offline Queue**: Actions queued for sync when connection restored

---

## Integration Points

### Backend API

**Sync Operations**:
- **Download**: New jobs, customer updates, inventory adjustments
- **Upload**: Completed jobs, photos, signatures, time tracking
- **Conflict Resolution**: Server timestamp wins on conflicts

**Sync Triggers**:
- Manual sync button in settings
- Automatic sync when connected to WiFi
- Background sync every 30 minutes (when online)
- Job completion triggers immediate sync attempt

### External Apps

1. **Google Maps / Waze**: Navigation to job site
2. **Phone Dialer**: Call customer directly
3. **Email Client**: Email customer
4. **Camera App**: Take photos (alternative to in-app camera)

### Future Integrations (Planned)

- **Weather Service**: Show weather conditions at job site
- **Traffic Data**: Estimated travel time based on current traffic
- **Customer Portal**: Customer job status viewing
- **Accounting System**: Automatic invoice generation
- **Parts Suppliers**: Check part availability/pricing

---

## Success Metrics

### Operational Metrics

- **Jobs Completed per Technician per Day**: Target 6-8
- **Average Job Completion Time**: By job type
- **First-Time Fix Rate**: % jobs not requiring follow-up
- **Customer Signature Capture Rate**: Target 100%
- **Photo Documentation Rate**: Target 100% (before + after)

### User Adoption Metrics

- **Daily Active Users**: % of technicians using app daily
- **Job Completion via App**: Target 100% of jobs
- **Time to Complete Job Record**: Average < 5 minutes
- **User Satisfaction Score**: Target > 4.5/5

### Business Impact Metrics

- **Billing Accuracy**: % jobs with complete resource tracking
- **Inventory Accuracy**: % variance between system and physical count
- **Dispute Resolution**: % customer disputes resolved with app data
- **Warranty Claims**: Documentation completeness for claims

### Technical Metrics

- **App Crash Rate**: Target < 0.1%
- **Sync Success Rate**: Target > 99%
- **Offline Capability**: 100% core features work offline
- **Battery Impact**: < 10% battery drain per 8-hour shift

---

## Conclusion

The Job Card Management Application transforms field service operations by:

1. **Eliminating Paper**: All job documentation digital and organized
2. **Improving Accuracy**: Automatic time tracking and resource recording
3. **Enhancing Accountability**: Complete audit trail with photos and signatures
4. **Increasing Efficiency**: Faster job completion with less admin time
5. **Better Customer Service**: Professional documentation and quick turnaround
6. **Data-Driven Insights**: Rich data for performance analysis and improvement

### For Technicians

The app makes their job easier by:
- Putting all information at their fingertips
- Automating tedious paperwork
- Providing clear guidance on job execution
- Making documentation quick and professional

### For Management

The app provides:
- Real-time visibility into field operations
- Accurate time and resource tracking
- Complete job documentation for compliance
- Data for performance optimization
- Reduced administrative overhead

### For Customers

The app delivers:
- Professional service experience
- Visual proof of work performed
- Accurate billing with resource breakdown
- Faster service turnaround
- Better communication and follow-up

---

**Document Version**: 1.0
**Last Updated**: 2025-11-12
**Target Audience**: Stakeholders, Business Analysts, Product Owners, New Team Members
