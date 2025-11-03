# Parcel Locker System - Database Architecture

This document explains the complete Room database implementation for the Android parcel locker system, including entities, DAOs, repositories, and type converters.

## Overview

The database uses **Room Persistence Library** with **UUID-based primary keys** to support offline-first architecture and seamless synchronization with cloud services. All entities include `sync_status` fields to track synchronization state between the tablet and cloud backend.

## Architecture Components

### 1. Type Converters (`/converters/`)

#### UuidConverter.java
- **Purpose**: Converts UUID objects to/from String for SQLite storage
- **Why Needed**: SQLite doesn't have native UUID support, stores as TEXT
- **Usage**: Applied to all UUID fields (primary keys, foreign keys)

```java
@TypeConverter
public String fromUUID(UUID uuid) // UUID → String for storage
public UUID toUUID(String value)   // String → UUID for retrieval
```

#### JsonConverter.java
- **Purpose**: Converts Map<String, Object> to/from JSON strings
- **Why Needed**: SQLite stores JSON as TEXT, uses Gson for serialization
- **Usage**: Applied to `details` fields in AuditLog and MachineEvent

```java
@TypeConverter
public String fromJson(Map<String, Object> json) // Map → JSON string
public Map<String, Object> toJson(String value)  // JSON string → Map
```

### 2. Entities (`/entities/`)

All entities use UUID primary keys and include common fields:
- `id`: UUID primary key
- `created_at`, `updated_at`: Timestamps
- `sync_status`: "local_only", "synced", "pending_sync"

#### User.java
- **Purpose**: System users with authentication capabilities
- **Key Fields**: `pin_code`, `card_id`, `email`, `is_active`
- **Authentication**: Supports PIN and RFID/NFC card access

#### LockerMachine.java
- **Purpose**: Physical locker machines
- **Key Fields**: `machine_serial`, `ip_address`, `is_online`, `total_doors`
- **Features**: Heartbeat monitoring, firmware version tracking

#### DoorEntity.java
- **Purpose**: Individual doors/compartments within machines
- **Key Fields**: `locker_machine_id`, `door_index`, `is_locked`, `is_occupied`
- **Relationships**: Foreign key to LockerMachine

#### Package.java
- **Purpose**: Packages with tracking and delivery information
- **Key Fields**: `tracking_number`, `recipient_email`, `assigned_to`, `door_id`
- **Status**: "pending", "delivered", "collected"
- **Relationships**: Foreign keys to User and DoorEntity

#### Delivery.java
- **Purpose**: Delivery events linking packages to users and doors
- **Key Fields**: `package_id`, `pickup_code`, `delivery_timestamp`, `is_collected`
- **Features**: Pickup code generation, expiry tracking

#### AuditLog.java
- **Purpose**: System audit trail for compliance and monitoring
- **Key Fields**: `entity_type`, `entity_id`, `action`, `details` (JSON)
- **JSON Usage**: Flexible audit data storage (e.g., `{"action": "deliver", "door_id": "uuid"}`)

#### MachineEvent.java
- **Purpose**: Real-time hardware events and system monitoring
- **Key Fields**: `locker_machine_id`, `event_type`, `severity`, `details` (JSON)
- **JSON Usage**: Event-specific metadata (e.g., `{"sensor_reading": 1.23, "temperature": 22.5}`)

### 3. Data Access Objects (`/dao/`)

Each DAO provides comprehensive CRUD operations and specialized queries:

#### Common DAO Patterns
- **Basic CRUD**: `insert()`, `update()`, `delete()`, `getById()`
- **Sync Support**: `getBySyncStatus()`, `updateSyncStatus()`
- **LiveData**: `getAllLive()`, `getByStatusLive()` for UI observation
- **Batch Operations**: `insertAll()`, `deleteOlderThan()`

#### Specialized Queries
- **UserDao**: Authentication by PIN/card, active users
- **DoorDao**: Available/occupied doors, door availability counts
- **PackageDao**: Packages by status, user assignments, locker contents
- **DeliveryDao**: Uncollected deliveries, expired packages
- **AuditLogDao**: Audit trails by entity, time ranges, recent events
- **MachineEventDao**: Unresolved events, critical alerts, machine-specific events

### 4. Repository Layer (`/repository/`)

Repositories provide clean APIs for UI components and handle background threading:

#### Key Features
- **Threading**: ExecutorService for background database operations
- **Caching**: LiveData for automatic UI updates
- **Business Logic**: Utility methods (e.g., `markAsCollected()`, `updateOnlineStatus()`)
- **Sync Management**: Batch sync status updates

#### Repository Examples
```java
// PackageRepository - Core locker functionality
packageRepository.assignToDoor(packageId, doorId);
packageRepository.updateStatus(packageId, "delivered");

// UserRepository - Authentication
User user = userRepository.authenticateByPin(pinCode);

// DeliveryRepository - Package pickup
deliveryRepository.markAsCollected(deliveryId);
```

### 5. Main Database Class

#### MachineDatabase.java
- **Configuration**: All entities, DAOs, and type converters
- **Singleton Pattern**: Thread-safe instance management
- **Database Name**: "parcel-locker-db"
- **Version**: 1 (increment for schema changes)

```java
@Database(entities = {User.class, LockerMachine.class, ...}, version = 1)
@TypeConverters({UuidConverter.class, JsonConverter.class})
```

## Entity Relationship Diagram (ERD)

```
[Users] ----< [Packages] >---- [Deliveries]
   |             |   |              |
   |             |   |              |
   |             |   O----[Doors] ---- [LockerMachines]
   |             |        |               |
   |             |        |               |
   O----< [AuditLogs] ----+               |
                          |               |
                          O----< [MachineEvents]

Legend:
----< : One-to-Many relationship
>---- : Many-to-One relationship
O---- : Optional (nullable foreign key)
```

### Relationships
1. **Users** → **Packages**: One user can have many packages (`assigned_to`)
2. **LockerMachines** → **Doors**: One machine has many doors (`locker_machine_id`)
3. **Doors** → **Packages**: One door contains one package (`door_id`)
4. **Packages** → **Deliveries**: One package has one delivery (`package_id`)
5. **Users** → **AuditLogs**: User actions tracked in audit logs (`user_id`)
6. **LockerMachines** → **MachineEvents**: Machine events linked to machines (`locker_machine_id`)

## Database Schema Features

### UUID Primary Keys
- **Benefits**: Globally unique, offline-friendly, sync-compatible
- **Storage**: Stored as TEXT in SQLite via UuidConverter
- **Generation**: Auto-generated in entity constructors

### JSON Fields
- **Purpose**: Flexible metadata storage for audit and event details
- **Implementation**: Map<String, Object> converted to JSON strings
- **Examples**:
  - AuditLog.details: `{"package_id": "uuid", "action": "delivered"}`
  - MachineEvent.details: `{"door_index": 5, "sensor_value": 1.23}`

### Sync Status Tracking
All entities include `sync_status` field:
- **"local_only"**: Created locally, not yet synced
- **"pending_sync"**: Modified locally, needs sync
- **"synced"**: Synchronized with cloud

### Timestamps
- **created_at**: Entity creation time
- **updated_at**: Last modification time (auto-updated in setters)
- **Specialized**: `delivery_timestamp`, `pickup_timestamp`, `last_heartbeat`

## Usage Examples

### 1. Package Delivery Workflow
```java
// 1. Create package
Package pkg = new Package();
pkg.setTrackingNumber("TRK123456");
pkg.setRecipientEmail("user@example.com");
packageRepository.insert(pkg);

// 2. Assign to user
User user = userRepository.getByEmail("user@example.com");
pkg.setAssignedTo(user.getId());
packageRepository.update(pkg);

// 3. Find available door
List<DoorEntity> availableDoors = doorRepository.getAvailableDoors(machineId);
DoorEntity door = availableDoors.get(0);

// 4. Deliver package
pkg.setDoorId(door.getId());
pkg.setStatus("delivered");
packageRepository.update(pkg);

// 5. Create delivery record
Delivery delivery = new Delivery();
delivery.setPackageId(pkg.getId());
delivery.setDoorId(door.getId());
delivery.setPickupCode(generatePickupCode());
deliveryRepository.insert(delivery);
```

### 2. User Authentication
```java
// PIN authentication
User user = userRepository.authenticateByPin(enteredPin);
if (user != null && user.getIsActive()) {
    // Access granted
    List<Package> userPackages = packageRepository.getByUserId(user.getId());
}

// Card authentication
User user = userRepository.authenticateByCard(scannedCardId);
```

### 3. Machine Monitoring
```java
// Log machine event
MachineEvent event = new MachineEvent();
event.setLockerMachineId(machineId);
event.setEventType("door_opened");
event.setSeverity("info");
Map<String, Object> details = new HashMap<>();
details.put("door_index", 5);
details.put("timestamp", System.currentTimeMillis());
event.setDetails(details);
machineEventRepository.insert(event);

// Get unresolved critical events
List<MachineEvent> criticalEvents = machineEventRepository.getUnresolvedEvents()
    .stream()
    .filter(e -> "critical".equals(e.getSeverity()))
    .collect(Collectors.toList());
```

### 4. Audit Logging
```java
// Log package delivery
AuditLog auditLog = new AuditLog();
auditLog.setEntityType("Package");
auditLog.setEntityId(packageId);
auditLog.setAction("deliver");
auditLog.setUserId(deliveryPersonId);
Map<String, Object> details = new HashMap<>();
details.put("door_id", doorId.toString());
details.put("tracking_number", trackingNumber);
auditLog.setDetails(details);
auditLogRepository.insert(auditLog);
```

## Testing the Database

### Unit Testing
```java
@Test
public void testPackageDelivery() {
    // Create test package
    Package pkg = new Package();
    pkg.setTrackingNumber("TEST123");
    packageDao.insert(pkg);
    
    // Verify insertion
    Package retrieved = packageDao.getByTrackingNumber("TEST123");
    assertEquals("TEST123", retrieved.getTrackingNumber());
    assertNotNull(retrieved.getId()); // UUID generated
}
```

### Database Inspector
- View SQLite tables in Android Studio
- Verify UUID fields stored as TEXT
- Check JSON fields stored as TEXT strings

## Best Practices

### 1. Repository Usage
- Always use repositories in UI components, not DAOs directly
- Use LiveData for automatic UI updates
- Handle threading properly (repositories use ExecutorService)

### 2. Sync Management
- Update `sync_status` when data changes locally
- Batch sync operations for efficiency
- Handle sync conflicts appropriately

### 3. JSON Fields
- Keep JSON simple and flat when possible
- Validate JSON structure before storage
- Consider performance impact of JSON parsing

### 4. Database Maintenance
- Implement data retention policies (delete old audit logs)
- Monitor database size and performance
- Plan for schema migrations

## Dependencies

```kotlin
// Room dependencies (already added to build.gradle.kts)
implementation("androidx.room:room-runtime:2.8.1")
annotationProcessor("androidx.room:room-compiler:2.8.1")

// JSON conversion
implementation("com.google.code.gson:gson:2.10.1")
```

## Migration Planning

When schema changes are needed:
1. Increment database version
2. Create migration classes
3. Test migrations thoroughly
4. Update entity classes
5. Update corresponding DAOs

This database architecture provides a solid foundation for the parcel locker system with offline-first capabilities, comprehensive audit trails, and real-time event monitoring.
