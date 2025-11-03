# Database Changes V2 - Payment & Simplified Architecture

**Date**: October 1, 2025  
**Purpose**: Added payment functionality and further simplified package lifecycle

## Summary of Changes V2

Further simplified the database architecture with:
- **Payment System**: Online and offline (cash) payment support
- **Simplified Package Entity**: Removed unnecessary fields, cleaner status flow
- **User Entity**: Minimal fields only (id, name, sync_status)
- **Status Logic**: pending → delivered (PIN used) → picked (paid & collected) OR returned (to office)
- **Removed Delivery Entity**: Everything now handled by Package entity

---

## 1. Package Entity - Major Overhaul (`/entities/Package.java`)

### **REMOVED Fields (Simplified):**
```java
// REMOVED - Not needed for machine operation
@ColumnInfo(name = "recipient_phone")
public String recipientPhone;

@ColumnInfo(name = "recipient_email") 
public String recipientEmail;

@ColumnInfo(name = "collection_timestamp")
public Long collectionTimestamp; // Replaced with picked_timestamp

@ColumnInfo(name = "delivery_pin_used")
public Boolean deliveryPinUsed; // Status "delivered" means PIN was used
```

### **ADDED Fields - Payment System:**
```java
// NEW - Payment functionality
@ColumnInfo(name = "payment_required")
public Boolean paymentRequired; // Does client need to pay?

@ColumnInfo(name = "payment_amount")
public Double paymentAmount; // Amount in local currency

@ColumnInfo(name = "payment_method")
public String paymentMethod; // "online", "cash", null

@ColumnInfo(name = "payment_status")
public String paymentStatus; // "pending", "paid", "failed"

@ColumnInfo(name = "cash_received")
public Double cashReceived; // For cash payments - machine tracking
```

### **ADDED Fields - Enhanced Tracking:**
```java
// NEW - Who returned the package
@ColumnInfo(name = "returned_by")
public UUID returnedBy; // Who returned the package to office

// RENAMED - Better semantics
@ColumnInfo(name = "picked_timestamp")
public Long pickedTimestamp; // When client collected (paid + picked)
```

### **SIMPLIFIED Status Logic:**
```java
// SIMPLIFIED - Only 4 states
public String status; // "pending", "delivered", "picked", "returned"

// Status meanings:
// "pending" = Package awaiting delivery
// "delivered" = PIN used, package in locker (72h timer starts)
// "picked" = Client paid (if required) and collected package
// "returned" = Package returned to office (expired or manual)
```

### **NEW Business Logic Methods:**
```java
// NEW - Payment validation
public boolean requiresPayment() {
    return paymentRequired != null && paymentRequired && !"paid".equals(paymentStatus);
}

public boolean canBePicked() {
    return "delivered".equals(status) && !isExpired() && 
           (!requiresPayment() || "paid".equals(paymentStatus));
}

// SIMPLIFIED - PIN validation
public boolean canUseDeliveryPin() {
    return "pending".equals(status); // No delivery_pin_used tracking
}
```

---

## 2. User Entity - Minimized (`/entities/User.java`)

### **REMOVED Fields (Extreme Simplification):**
```java
// REMOVED - Not needed for machine
@ColumnInfo(name = "first_name")
public String firstName;

@ColumnInfo(name = "last_name") 
public String lastName;

public String email;
public String phone;

@ColumnInfo(name = "user_role")
public String userRole; // Machine doesn't care about roles

@ColumnInfo(name = "is_active")
public Boolean isActive;

@ColumnInfo(name = "created_at")
public Long createdAt;

@ColumnInfo(name = "updated_at")
public Long updatedAt;
```

### **KEPT Fields (Essential Only):**
```java
// MINIMAL - Only what machine needs
@PrimaryKey
public UUID id; // Cloud user ID for sync and package tracking

public String name; // Display name for machine UI (e.g., "John Doe")

@ColumnInfo(name = "sync_status")
public String syncStatus; // "local_only", "synced", "pending_sync"
```

### **Purpose**: 
- Machine just needs to track who delivered/returned packages
- All user management handled in cloud/backoffice
- No authentication at machine level

---

## 3. PackageDao - Payment & Simplified Logic (`/dao/PackageDao.java`)

### **NEW Methods - Payment Queries:**
```java
// NEW - Payment management
@Query("SELECT * FROM packages WHERE payment_required = 1 AND payment_status = 'pending'")
List<Package> getUnpaidPackages();

@Query("SELECT * FROM packages WHERE payment_method = 'cash' AND payment_status = 'paid'")
List<Package> getCashPaidPackages();

@Query("SELECT * FROM packages WHERE payment_required = 1 AND payment_status = 'paid'")
List<Package> getPaidPackages();

@Query("SELECT COUNT(*) FROM packages WHERE payment_required = 1 AND payment_status = 'pending'")
int getUnpaidPackagesCount();
```

### **NEW Methods - Payment Updates:**
```java
// NEW - Payment status updates
@Query("UPDATE packages SET payment_status = 'paid', payment_method = :paymentMethod, updated_at = :updatedAt WHERE id = :id")
void markPaymentAsPaid(UUID id, String paymentMethod, Long updatedAt);

@Query("UPDATE packages SET payment_status = 'paid', payment_method = 'cash', cash_received = :cashReceived, updated_at = :updatedAt WHERE id = :id")
void markCashPaymentAsPaid(UUID id, Double cashReceived, Long updatedAt);

@Query("UPDATE packages SET payment_status = 'failed', updated_at = :updatedAt WHERE id = :id")
void markPaymentAsFailed(UUID id, Long updatedAt);
```

### **SIMPLIFIED Methods - Lifecycle Updates:**
```java
// SIMPLIFIED - No more delivery_pin_used tracking
@Query("UPDATE packages SET door_id = :doorId, delivery_timestamp = :deliveryTimestamp, expiry_timestamp = :expiryTimestamp, status = 'delivered', delivered_by = :deliveredBy, updated_at = :updatedAt WHERE id = :id")
void markAsDelivered(UUID id, UUID doorId, Long deliveryTimestamp, Long expiryTimestamp, UUID deliveredBy, Long updatedAt);

// NEW - picked instead of collected
@Query("UPDATE packages SET picked_timestamp = :pickedTimestamp, status = 'picked', updated_at = :updatedAt WHERE id = :id")
void markAsPicked(UUID id, Long pickedTimestamp, Long updatedAt);

// ENHANCED - Track who returned
@Query("UPDATE packages SET return_timestamp = :returnTimestamp, status = 'returned', returned_by = :returnedBy, door_id = NULL, updated_at = :updatedAt WHERE id = :id")
void markAsReturned(UUID id, Long returnTimestamp, UUID returnedBy, Long updatedAt);
```

### **ADDED Methods - Return Tracking:**
```java
// NEW - Track who returned packages
@Query("SELECT * FROM packages WHERE returned_by = :userId")
List<Package> getByReturnPerson(UUID userId);
```

---

## 4. UserDao - Minimized (`/dao/UserDao.java`)

### **REMOVED Methods (No Longer Needed):**
```java
// REMOVED - No role management
@Query("SELECT * FROM users WHERE user_role = :userRole AND is_active = 1")
List<User> getByRole(String userRole);

@Query("SELECT * FROM users WHERE user_role = 'admin' AND is_active = 1")
List<User> getActiveAdmins();

@Query("SELECT * FROM users WHERE user_role = 'delivery' AND is_active = 1")
List<User> getActiveDeliveryStaff();

@Query("UPDATE users SET is_active = :isActive WHERE id = :id")
void updateActiveStatus(UUID id, Boolean isActive);

// And all other role-based methods...
```

### **KEPT Methods (Essential Only):**
```java
// MINIMAL - Basic CRUD only
@Query("SELECT * FROM users WHERE id = :id LIMIT 1")
User getById(UUID id);

@Query("SELECT * FROM users")
List<User> getAll();

@Query("SELECT * FROM users WHERE sync_status = :syncStatus")
List<User> getBySyncStatus(String syncStatus);

// Basic insert/update/delete operations
// Sync status management
```

---

## 5. Repository Updates

### **PackageRepository - Payment Methods:**
```java
// NEW - Payment operations
public void markPaymentAsPaid(UUID packageId, String paymentMethod) {
    executor.execute(() -> packageDao.markPaymentAsPaid(packageId, paymentMethod, System.currentTimeMillis()));
}

public void markCashPaymentAsPaid(UUID packageId, Double cashReceived) {
    executor.execute(() -> packageDao.markCashPaymentAsPaid(packageId, cashReceived, System.currentTimeMillis()));
}

public void markPaymentAsFailed(UUID packageId) {
    executor.execute(() -> packageDao.markPaymentAsFailed(packageId, System.currentTimeMillis()));
}

// NEW - Payment queries
public List<Package> getUnpaidPackages() {
    return packageDao.getUnpaidPackages();
}

public List<Package> getCashPaidPackages() {
    return packageDao.getCashPaidPackages();
}

public int getUnpaidPackagesCount() {
    return packageDao.getUnpaidPackagesCount();
}

public LiveData<List<Package>> getUnpaidPackagesLive() {
    return packageDao.getUnpaidPackagesLive();
}
```

### **SIMPLIFIED Lifecycle Methods:**
```java
// SIMPLIFIED - markAsPicked instead of markAsCollected
public void markAsPicked(UUID packageId) {
    executor.execute(() -> {
        long now = System.currentTimeMillis();
        packageDao.markAsPicked(packageId, now, now);
    });
}

// ENHANCED - Track who returned
public void markAsReturned(UUID packageId, UUID returnedBy) {
    executor.execute(() -> {
        long now = System.currentTimeMillis();
        packageDao.markAsReturned(packageId, now, returnedBy, now);
    });
}
```

### **UserRepository - Simplified:**
```java
// REMOVED all role-based methods, authentication methods
// KEPT only basic CRUD and sync operations
```

---

## 6. Database Schema Changes

### **Entity List Update:**
```java
@Database(
    entities = {
        User.class,                    // Simplified (id, name, sync_status)
        LockerMachine.class,           // Unchanged
        DoorEntity.class,              // Unchanged  
        com.example.parcellocker.db.entities.Package.class, // Enhanced with payment
        AuditLog.class,                // Unchanged
        MachineEvent.class            // Unchanged
        // Delivery.class - COMPLETELY REMOVED
    },
    version = 1,
    exportSchema = false
)
```

### **Foreign Key Updates:**
```java
// ADDED - returnedBy foreign key in Package
@ForeignKey(entity = User.class, 
           parentColumns = "id", 
           childColumns = "returnedBy",
           onDelete = ForeignKey.SET_NULL)
```

---

## 7. New Workflows with Payment

### **1. Package Delivery Workflow (Unchanged):**
```java
// Step 1: Delivery person enters delivery PIN
Package pkg = packageRepository.authenticateDeliveryPin(enteredPin);

if (pkg != null && pkg.canUseDeliveryPin()) {
    // Step 2: Find available door and deliver
    packageRepository.markAsDelivered(pkg.getId(), doorId, deliveryPersonId);
    
    // Status is now "delivered" (PIN automatically marked as used)
}
```

### **2. Package Collection Workflow (WITH PAYMENT):**
```java
// Step 1: Client enters their collection PIN
Package pkg = packageRepository.authenticateClientPin(enteredPin);

if (pkg != null && pkg.canUseClientPin()) {
    
    // Step 2: Check if payment required
    if (pkg.requiresPayment()) {
        
        // Step 3a: Online payment (handled by API, not local)
        if ("online".equals(paymentMethod)) {
            // Payment processed online, just mark as paid
            packageRepository.markPaymentAsPaid(pkg.getId(), "online");
        }
        
        // Step 3b: Cash payment (vending machine style)
        else if ("cash".equals(paymentMethod)) {
            // Machine collects cash
            double cashReceived = collectCashFromMachine();
            packageRepository.markCashPaymentAsPaid(pkg.getId(), cashReceived);
        }
    }
    
    // Step 4: Verify payment completed
    if (pkg.canBePicked()) {
        // Open door and mark as picked
        packageRepository.markAsPicked(pkg.getId());
        
        // Status is now "picked"
    }
}
```

### **3. Package Return Workflow (ENHANCED):**
```java
// Step 1: Backoffice staff enters return PIN
Package pkg = packageRepository.authenticateReturnPin(enteredPin);

if (pkg != null && pkg.canUseReturnPin()) {
    // Step 2: Return package to office with staff tracking
    User returnStaff = userRepository.getById(currentStaffId);
    packageRepository.markAsReturned(pkg.getId(), returnStaff.getId());
    
    // Status is now "returned", door freed, staff tracked
}
```

### **4. Cash Payment Management:**
```java
// Machine cash tracking
public void processCashPayment(UUID packageId, double amount) {
    Package pkg = packageRepository.getById(packageId);
    
    if (pkg.getPaymentAmount() <= amount) {
        // Sufficient payment
        packageRepository.markCashPaymentAsPaid(packageId, amount);
        
        // Calculate change if needed
        double change = amount - pkg.getPaymentAmount();
        if (change > 0) {
            dispensChange(change);
        }
    }
}

// Sync cash payments to cloud
public void syncCashPayments() {
    List<Package> cashPaid = packageRepository.getCashPaidPackages();
    for (Package pkg : cashPaid) {
        if ("local_only".equals(pkg.getSyncStatus())) {
            // Sync to cloud with cash_received amount
            syncToCloud(pkg);
        }
    }
}
```

---

## 8. Removed Files & Components

### **Completely Removed:**
- `/entities/Delivery.java` - All functionality moved to Package
- `/dao/DeliveryDao.java` - No longer needed
- `/repository/DeliveryRepository.java` - Package handles everything

### **Reason for Removal:**
Package entity now contains everything needed:
- Delivery tracking via `delivered_by` field
- Collection tracking via `picked_timestamp` 
- Return tracking via `returned_by` field
- Payment tracking via payment fields
- No separate delivery record needed

---

## 9. Key Business Rules - Updated

### **Status Flow (Simplified):**
```
pending → delivered → picked (normal flow)
       → delivered → returned (expired/manual return)
```

### **Payment Rules:**
1. **No Payment Required**: Package can be picked immediately after PIN validation
2. **Online Payment**: Processed via API, machine just marks as paid when confirmed
3. **Cash Payment**: Machine acts like vending machine, collects cash locally
4. **Payment Failure**: Package remains in "delivered" state until paid or expired

### **PIN Rules (Simplified):**
1. **Delivery PIN**: Valid only for "pending" packages
2. **Client PIN**: Valid only for "delivered" packages (not expired)
3. **Return PIN**: Valid only for expired "delivered" packages

### **72-Hour Rule (Unchanged):**
- Automatic expiry after delivery
- Expired packages can only be returned with return PIN
- Payment not required for returns

---

## 10. Machine Operation Summary

### **What Machine Handles Locally:**
- Package status tracking (pending → delivered → picked/returned)
- Cash payment collection and validation
- PIN authentication for all 3 PIN types
- Door control and occupancy tracking
- User tracking for delivery/return attribution

### **What Cloud Handles:**
- Online payment processing
- Detailed user management and authentication
- Package creation and PIN generation
- Sync of all local data for reporting
- Business logic and administration

### **Sync Points:**
- Cash payments sync to cloud with amounts
- Package status changes sync to cloud
- User actions tracked in audit logs
- All data prepared for cloud synchronization

---

## 11. Benefits of New Architecture

### **Simplified:**
- Removed unnecessary User fields (roles, detailed info)
- Eliminated separate Delivery entity
- Cleaner status flow (4 states instead of 5+)
- Single Package entity handles entire lifecycle

### **Enhanced:**
- Payment system supports online and cash
- Better tracking of who returned packages  
- Cash payment handling for offline operation
- Cleaner business logic methods

### **Machine-Focused:**
- Only essential data for machine operation
- Minimal sync requirements
- Ready for vending machine-style cash handling
- Simple status tracking for UI

This architecture is now perfectly suited for a machine-local operation with the ability to handle payments both online (via cloud API) and offline (cash collection), while maintaining full audit trails and sync capabilities for when cloud connectivity is available.
