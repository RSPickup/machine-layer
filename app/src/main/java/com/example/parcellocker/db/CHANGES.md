# Database Changes - Parcel Locker System

**Date**: October 1, 2025  
**Purpose**: Updated database architecture to reflect correct business logic for parcel locker system

## Summary of Changes

The database has been restructured to properly reflect the business requirements where:
- **Users** are backoffice staff only (admin, delivery) - no authentication needed
- **Packages** use a 3-PIN system for access control instead of user authentication
- **No RFID/NFC** card functionality needed
- Database is machine-local and will sync with cloud later

---

## 1. User Entity Changes (`/entities/User.java`)

### **REMOVED Fields:**
```java
// REMOVED - No longer needed
@ColumnInfo(name = "pin_code")
public String pinCode; // Encrypted PIN for locker access

@ColumnInfo(name = "card_id")
public String cardId; // RFID/NFC card identifier
```

### **ADDED Fields:**
```java
// NEW - Identifies user role
@ColumnInfo(name = "user_role")
public String userRole; // "admin" or "delivery"
```

### **NEW Methods:**
```java
// NEW utility methods
public boolean isAdmin() {
    return "admin".equals(userRole);
}

public boolean isDelivery() {
    return "delivery".equals(userRole);
}
```

### **Business Logic Changes:**
- Users are managed via backoffice system only
- No authentication at machine level required
- Only admin and delivery staff roles supported
- Removed all PIN/card authentication functionality

---

## 2. Package Entity Changes (`/entities/Package.java`)

### **ADDED Fields - 3-PIN System:**
```java
// NEW - Three PIN codes for different access levels
@ColumnInfo(name = "delivery_pin")
public String deliveryPin; // PIN for delivery person to place package

@ColumnInfo(name = "client_pin")
public String clientPin; // PIN for recipient to collect package

@ColumnInfo(name = "return_pin")
public String returnPin; // PIN for backoffice to return package after 72h
```

### **ADDED Fields - Enhanced Tracking:**
```java
// NEW - Enhanced package lifecycle tracking
@ColumnInfo(name = "delivered_by")
public UUID deliveredBy; // Foreign key to User.id (delivery person)

@ColumnInfo(name = "expiry_timestamp")
public Long expiryTimestamp; // 72 hours after delivery

@ColumnInfo(name = "return_timestamp")
public Long returnTimestamp; // When package was returned to backoffice

@ColumnInfo(name = "delivery_pin_used")
public Boolean deliveryPinUsed; // Track if delivery PIN has been used
```

### **MODIFIED Fields:**
```java
// CHANGED semantic meaning
// OLD: assignedTo (user assignment)
// NEW: deliveredBy (delivery person tracking)
@ColumnInfo(name = "delivered_by")
public UUID deliveredBy;

// ENHANCED status values
public String status; // "pending", "delivered", "collected", "expired", "returned"
```

### **NEW Methods - PIN Validation:**
```java
// NEW - PIN usage validation methods
public boolean canUseDeliveryPin() {
    return !deliveryPinUsed && "pending".equals(status);
}

public boolean canUseClientPin() {
    return "delivered".equals(status) && !isExpired();
}

public boolean canUseReturnPin() {
    return ("delivered".equals(status) && isExpired()) || "expired".equals(status);
}

public boolean isExpired() {
    return expiryTimestamp != null && System.currentTimeMillis() > expiryTimestamp;
}
```

### **ENHANCED Methods:**
```java
// ENHANCED - Auto-calculates 72-hour expiry
public void setDeliveryTimestamp(Long deliveryTimestamp) { 
    this.deliveryTimestamp = deliveryTimestamp;
    // Set expiry to 72 hours after delivery
    if (deliveryTimestamp != null) {
        this.expiryTimestamp = deliveryTimestamp + (72 * 60 * 60 * 1000); // 72 hours in milliseconds
    }
}
```

---

## 3. UserDao Changes (`/dao/UserDao.java`)

### **REMOVED Methods:**
```java
// REMOVED - No authentication needed
@Query("SELECT * FROM users WHERE pin_code = :pinCode LIMIT 1")
User getByPinCode(String pinCode);

@Query("SELECT * FROM users WHERE card_id = :cardId LIMIT 1")
User getByCardId(String cardId);
```

### **NEW Methods - Role-based Queries:**
```java
// NEW - Role-based user management
@Query("SELECT * FROM users WHERE user_role = :userRole AND is_active = 1")
List<User> getByRole(String userRole);

@Query("SELECT * FROM users WHERE user_role = 'admin' AND is_active = 1")
List<User> getActiveAdmins();

@Query("SELECT * FROM users WHERE user_role = 'delivery' AND is_active = 1")
List<User> getActiveDeliveryStaff();

@Query("SELECT * FROM users WHERE user_role = 'delivery' AND is_active = 1")
LiveData<List<User>> getActiveDeliveryStaffLive();

@Query("SELECT COUNT(*) FROM users WHERE user_role = 'delivery' AND is_active = 1")
int getActiveDeliveryStaffCount();
```

---

## 4. PackageDao Changes (`/dao/PackageDao.java`)

### **NEW Methods - PIN Authentication:**
```java
// NEW - PIN-based authentication system
@Query("SELECT * FROM packages WHERE delivery_pin = :deliveryPin AND delivery_pin_used = 0 AND status = 'pending' LIMIT 1")
Package getByDeliveryPin(String deliveryPin);

@Query("SELECT * FROM packages WHERE client_pin = :clientPin AND status = 'delivered' LIMIT 1")
Package getByClientPin(String clientPin);

@Query("SELECT * FROM packages WHERE return_pin = :returnPin AND (status = 'delivered' OR status = 'expired') LIMIT 1")
Package getByReturnPin(String returnPin);
```

### **NEW Methods - Enhanced Queries:**
```java
// NEW - Enhanced package querying
@Query("SELECT * FROM packages WHERE delivered_by = :userId")
List<Package> getByDeliveryPerson(UUID userId);

@Query("SELECT * FROM packages WHERE recipient_email = :recipientEmail")
List<Package> getByRecipientEmail(String recipientEmail);

@Query("SELECT * FROM packages WHERE status = 'delivered' AND expiry_timestamp < :currentTime")
List<Package> getExpiredPackages(Long currentTime);

@Query("SELECT * FROM packages WHERE status = 'delivered' AND expiry_timestamp > :currentTime")
List<Package> getActiveDeliveredPackages(Long currentTime);

@Query("SELECT COUNT(*) FROM packages WHERE status = 'delivered' AND expiry_timestamp < :currentTime")
int getExpiredPackagesCount(Long currentTime);
```

### **NEW Methods - Package Lifecycle Management:**
```java
// NEW - Package lifecycle operations
@Query("UPDATE packages SET door_id = :doorId, delivery_timestamp = :deliveryTimestamp, expiry_timestamp = :expiryTimestamp, status = 'delivered', delivery_pin_used = 1, updated_at = :updatedAt WHERE id = :id")
void markAsDelivered(UUID id, UUID doorId, Long deliveryTimestamp, Long expiryTimestamp, Long updatedAt);

@Query("UPDATE packages SET collection_timestamp = :collectionTimestamp, status = 'collected', updated_at = :updatedAt WHERE id = :id")
void markAsCollected(UUID id, Long collectionTimestamp, Long updatedAt);

@Query("UPDATE packages SET return_timestamp = :returnTimestamp, status = 'returned', door_id = NULL, updated_at = :updatedAt WHERE id = :id")
void markAsReturned(UUID id, Long returnTimestamp, Long updatedAt);

@Query("UPDATE packages SET status = 'expired', updated_at = :updatedAt WHERE id = :id")
void markAsExpired(UUID id, Long updatedAt);
```

---

## 5. Repository Changes

### **UserRepository (`/repository/UserRepository.java`)**

### **REMOVED Methods:**
```java
// REMOVED - No authentication needed
public User authenticateByPin(String pinCode);
public User authenticateByCard(String cardId);
```

### **NEW Methods:**
```java
// NEW - Role-based management
public List<User> getByRole(String userRole) {
    return userDao.getByRole(userRole);
}

public List<User> getActiveAdmins() {
    return userDao.getActiveAdmins();
}

public List<User> getActiveDeliveryStaff() {
    return userDao.getActiveDeliveryStaff();
}

public LiveData<List<User>> getActiveDeliveryStaffLive() {
    return userDao.getActiveDeliveryStaffLive();
}

public int getActiveDeliveryStaffCount() {
    return userDao.getActiveDeliveryStaffCount();
}
```

### **PackageRepository (`/repository/PackageRepository.java`)**

### **NEW Methods - PIN Authentication:**
```java
// NEW - PIN-based authentication system
public Package authenticateDeliveryPin(String deliveryPin) {
    return packageDao.getByDeliveryPin(deliveryPin);
}

public Package authenticateClientPin(String clientPin) {
    return packageDao.getByClientPin(clientPin);
}

public Package authenticateReturnPin(String returnPin) {
    return packageDao.getByReturnPin(returnPin);
}
```

### **NEW Methods - Package Lifecycle Management:**
```java
// NEW - Package lifecycle operations
public void markAsDelivered(UUID packageId, UUID doorId, UUID deliveryPersonId) {
    executor.execute(() -> {
        long now = System.currentTimeMillis();
        long expiry = now + (72 * 60 * 60 * 1000); // 72 hours
        packageDao.markAsDelivered(packageId, doorId, now, expiry, now);
    });
}

public void markAsCollected(UUID packageId) {
    executor.execute(() -> {
        long now = System.currentTimeMillis();
        packageDao.markAsCollected(packageId, now, now);
    });
}

public void markAsReturned(UUID packageId) {
    executor.execute(() -> {
        long now = System.currentTimeMillis();
        packageDao.markAsReturned(packageId, now, now);
    });
}

public void markAsExpired(UUID packageId) {
    executor.execute(() -> packageDao.markAsExpired(packageId, System.currentTimeMillis()));
}
```

### **NEW Methods - Enhanced Queries:**
```java
// NEW - Enhanced package querying
public List<Package> getByDeliveryPerson(UUID userId) {
    return packageDao.getByDeliveryPerson(userId);
}

public List<Package> getByRecipientEmail(String recipientEmail) {
    return packageDao.getByRecipientEmail(recipientEmail);
}

public List<Package> getExpiredPackages() {
    return packageDao.getExpiredPackages(System.currentTimeMillis());
}

public List<Package> getActiveDeliveredPackages() {
    return packageDao.getActiveDeliveredPackages(System.currentTimeMillis());
}

public int getExpiredPackagesCount() {
    return packageDao.getExpiredPackagesCount(System.currentTimeMillis());
}

public LiveData<List<Package>> getPackagesInLockersLive() {
    return packageDao.getPackagesInLockersLive();
}
```

---

## 6. Database Schema Changes (`MachineDatabase.java`)

### **REMOVED Entities:**
```java
// REMOVED - Logic moved to Package entity
Delivery.class
```

### **REMOVED DAOs:**
```java
// REMOVED - No longer needed
public abstract DeliveryDao deliveryDao();
```

### **UPDATED Entity List:**
```java
@Database(
    entities = {
        User.class,
        LockerMachine.class,
        DoorEntity.class,
        com.example.parcellocker.db.entities.Package.class, // Fully qualified to avoid conflict
        AuditLog.class,
        MachineEvent.class
        // Delivery.class - REMOVED
    },
    version = 1,
    exportSchema = false
)
```

### **UPDATED Documentation:**
```java
/**
 * Entities:
 * - User: Backoffice users (admin, delivery staff) - no PIN authentication
 * - LockerMachine: Physical locker machines with status tracking
 * - DoorEntity: Individual doors/compartments in machines
 * - Package: Packages with 3-PIN authentication system (delivery/client/return)
 * - AuditLog: System audit trail with flexible JSON details
 * - MachineEvent: Real-time hardware events with JSON metadata
 */
```

---

## 7. Removed Files

### **Files Deleted:**
- `/entities/Delivery.java` - Package handles all delivery logic now
- `/dao/DeliveryDao.java` - No separate delivery tracking needed
- `/repository/DeliveryRepository.java` - Functionality moved to PackageRepository

---

## 8. New Architecture Overview

### **Entity Relationships (Updated ERD):**
```
[Users] ----< [Packages] 
   |             |   |              
   |             |   |              
   |             |   O----[Doors] ---- [LockerMachines]
   |             |        |               |
   |             |        |               |
   O----< [AuditLogs] ----+               |
                          |               |
                          O----< [MachineEvents]

Legend:
----< : One-to-Many relationship
O---- : Optional (nullable foreign key)
```

### **Key Relationships:**
1. **Users** → **Packages**: One delivery person can deliver many packages (`delivered_by`)
2. **LockerMachines** → **Doors**: One machine has many doors (`locker_machine_id`)
3. **Doors** → **Packages**: One door contains one package (`door_id`)
4. **Users** → **AuditLogs**: User actions tracked in audit logs (`user_id`)
5. **LockerMachines** → **MachineEvents**: Machine events linked to machines (`locker_machine_id`)

---

## 9. New Business Workflows

### **1. Package Delivery Workflow:**
```java
// Step 1: Delivery person enters delivery PIN
Package pkg = packageRepository.authenticateDeliveryPin(enteredPin);

if (pkg != null && pkg.canUseDeliveryPin()) {
    // Step 2: Find available door
    List<DoorEntity> availableDoors = doorRepository.getAvailableDoors(machineId);
    DoorEntity door = availableDoors.get(0);
    
    // Step 3: Complete delivery (marks PIN as used, sets 72h expiry)
    packageRepository.markAsDelivered(pkg.getId(), door.getId(), deliveryPersonId);
    
    // Step 4: Update door status
    door.setIsOccupied(true);
    doorRepository.update(door);
    
    // Package is now "delivered" with 72-hour countdown
}
```

### **2. Package Collection Workflow:**
```java
// Step 1: Client enters their collection PIN
Package pkg = packageRepository.authenticateClientPin(enteredPin);

if (pkg != null && pkg.canUseClientPin()) {
    // Step 2: Open door and mark as collected
    packageRepository.markAsCollected(pkg.getId());
    
    // Step 3: Update door status
    DoorEntity door = doorRepository.getById(pkg.getDoorId());
    door.setIsOccupied(false);
    doorRepository.update(door);
    
    // Package is now "collected", door is available
}
```

### **3. Package Return Workflow (After 72 Hours):**
```java
// Step 1: Check for expired packages
List<Package> expiredPackages = packageRepository.getExpiredPackages();

// Step 2: Backoffice staff enters return PIN
Package pkg = packageRepository.authenticateReturnPin(enteredPin);

if (pkg != null && pkg.canUseReturnPin()) {
    // Step 3: Return package to backoffice
    packageRepository.markAsReturned(pkg.getId());
    
    // Step 4: Free up the door
    DoorEntity door = doorRepository.getById(pkg.getDoorId());
    door.setIsOccupied(false);
    doorRepository.update(door);
    
    // Package is now "returned", door is available
}
```

### **4. Automatic Expiry Management:**
```java
// Background service to handle package expiry
public void checkExpiredPackages() {
    long currentTime = System.currentTimeMillis();
    List<Package> expiredPackages = packageRepository.getExpiredPackages();
    
    for (Package pkg : expiredPackages) {
        if ("delivered".equals(pkg.getStatus()) && pkg.isExpired()) {
            packageRepository.markAsExpired(pkg.getId());
            
            // Notify backoffice of expired package
            // Package can now be retrieved using return PIN
        }
    }
}
```

---

## 10. Key Business Rules Implemented

### **PIN Authentication Rules:**
1. **Delivery PIN**: 
   - Single use only
   - Only valid for "pending" status packages
   - Becomes invalid after successful delivery
   - Links package to specific delivery person

2. **Client PIN**: 
   - Valid only for "delivered" status packages
   - Must be used within 72 hours
   - Cannot be used on expired packages

3. **Return PIN**: 
   - Valid only for expired packages (after 72 hours)
   - Can be used by backoffice staff
   - Frees up locker door for new packages

### **Package Lifecycle States:**
```
pending → delivered → collected (normal flow)
       → delivered → expired → returned (unclaimed flow)
```

### **72-Hour Rule:**
- Automatic expiry calculation when package is delivered
- Timer starts when `markAsDelivered()` is called
- Expired packages can only be retrieved with return PIN
- Automatic status change to "expired" after timeout

### **User Management:**
- No authentication at machine level
- Users managed via backoffice system
- Role-based access (admin vs delivery)
- All user actions tracked in audit logs

---

## 11. Database Size Impact

### **Reduced Complexity:**
- Removed Delivery entity (1 less table)
- Simplified user authentication (removed PIN/card fields)
- Consolidated delivery logic into Package entity

### **Enhanced Functionality:**
- Added 3-PIN authentication system
- Enhanced package lifecycle tracking
- Better expiry management
- Improved audit capabilities

### **Maintained Features:**
- All sync capabilities preserved
- Audit trail functionality intact
- Machine event monitoring unchanged
- UUID-based architecture maintained

---

## 12. Migration Notes

### **From Old to New Architecture:**
1. **Data Migration**: Existing packages need delivery/client/return PINs generated
2. **User Updates**: Remove PIN/card data, add user roles
3. **Delivery Records**: Migrate to package-based tracking
4. **Status Updates**: Map old statuses to new lifecycle states

### **Backward Compatibility:**
- Database version should be incremented
- Migration scripts needed for production deployment
- Existing audit logs remain intact
- Machine configuration unchanged

This new architecture correctly reflects the business requirements where packages are controlled by a 3-PIN system rather than user authentication, while maintaining all the robustness and sync capabilities of the original design.
