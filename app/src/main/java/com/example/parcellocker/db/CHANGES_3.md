# Database Changes V3 - Corrected Payment Architecture

**Date**: October 2, 2025  
**Purpose**: Fixed payment architecture with separate Payment entity (corrected from V2 mistake)

## Summary of Changes V3

**MAJOR CORRECTION**: In V2, I incorrectly added payment fields directly to the Package entity. This has been corrected by creating a proper separate **Payment entity** for better architecture and transaction tracking.

### Architecture Overview:
- **User**: Minimal fields (id, name, sync_status) for package tracking only
- **Package**: Clean entity with 3-PIN system and lifecycle tracking
- **Payment**: Separate entity for all payment transactions (online and cash)
- **Relationship**: Package → Payment (one-to-many for refunds/multiple attempts)

---

## 1. NEW Payment Entity (`/entities/Payment.java`)

### **Complete Payment Entity:**
```java
@Entity(tableName = "payments",
        foreignKeys = {
            @ForeignKey(entity = Package.class,
                       parentColumns = "id",
                       childColumns = "packageId",
                       onDelete = ForeignKey.CASCADE)
        })
public class Payment {
    @PrimaryKey
    public UUID id;
    
    @ColumnInfo(name = "package_id")
    public UUID packageId; // Foreign key to Package
    
    // Payment details
    @ColumnInfo(name = "payment_method")
    public String paymentMethod; // "online", "cash"
    
    @ColumnInfo(name = "payment_status") 
    public String paymentStatus; // "pending", "paid", "failed", "refunded"
    
    @ColumnInfo(name = "amount_required")
    public Double amountRequired; // Amount that needs to be paid
    
    @ColumnInfo(name = "amount_paid")
    public Double amountPaid; // Actual amount paid
    
    @ColumnInfo(name = "change_given")
    public Double changeGiven; // Change for cash payments
    
    public String currency; // "USD", "EUR", etc.
    
    // Online payment fields
    @ColumnInfo(name = "transaction_id")
    public String transactionId; // External payment gateway transaction ID
    
    @ColumnInfo(name = "payment_gateway")
    public String paymentGateway; // "stripe", "paypal", etc.
    
    // Cash payment fields (machine-local)
    @ColumnInfo(name = "cash_denominations")
    public String cashDenominations; // JSON of bills/coins received
    
    @ColumnInfo(name = "machine_cash_balance")
    public Double machineCashBalance; // Machine cash after transaction
    
    // Timestamps
    @ColumnInfo(name = "initiated_at")
    public Long initiatedAt;
    
    @ColumnInfo(name = "completed_at")
    public Long completedAt;
    
    @ColumnInfo(name = "sync_status")
    public String syncStatus;
}
```

### **Key Benefits of Separate Payment Entity:**
1. **Transaction Tracking**: Each payment attempt is recorded separately
2. **Refund Support**: Multiple payment records per package for refunds
3. **Cash Management**: Detailed tracking of machine cash balance
4. **Audit Trail**: Complete payment history for compliance
5. **Sync Optimization**: Only payment data syncs to cloud, not entire package

---

## 2. CORRECTED Package Entity (`/entities/Package.java`)

### **REMOVED Fields (From V2 Mistake):**
```java
// REMOVED - These were incorrectly added to Package in V2
@ColumnInfo(name = "payment_required")
public Boolean paymentRequired;

@ColumnInfo(name = "payment_amount")
public Double paymentAmount;

@ColumnInfo(name = "payment_method")
public String paymentMethod;

@ColumnInfo(name = "payment_status")
public String paymentStatus;

@ColumnInfo(name = "cash_received")
public Double cashReceived;
```

### **CLEAN Package Entity (Correct Architecture):**
```java
public class Package {
    @PrimaryKey
    public UUID id;
    
    @ColumnInfo(name = "tracking_number")
    public String trackingNumber;
    
    @ColumnInfo(name = "recipient_name")
    public String recipientName; // Minimal recipient info
    
    // 3-PIN system
    @ColumnInfo(name = "delivery_pin")
    public String deliveryPin;
    
    @ColumnInfo(name = "client_pin")
    public String clientPin;
    
    @ColumnInfo(name = "return_pin")
    public String returnPin;
    
    // User tracking
    @ColumnInfo(name = "delivered_by")
    public UUID deliveredBy;
    
    @ColumnInfo(name = "returned_by")
    public UUID returnedBy;
    
    @ColumnInfo(name = "door_id")
    public UUID doorId;
    
    // Simple lifecycle
    public String status; // "pending", "delivered", "picked", "returned"
    
    // Timestamps
    @ColumnInfo(name = "delivery_timestamp")
    public Long deliveryTimestamp;
    
    @ColumnInfo(name = "picked_timestamp")
    public Long pickedTimestamp;
    
    @ColumnInfo(name = "expiry_timestamp")
    public Long expiryTimestamp; // 72 hours
    
    @ColumnInfo(name = "return_timestamp")
    public Long returnTimestamp;
    
    @ColumnInfo(name = "sync_status")
    public String syncStatus;
}
```

---

## 3. NEW PaymentDao (`/dao/PaymentDao.java`)

### **Core Payment Queries:**
```java
@Dao
public interface PaymentDao {
    // Basic queries
    @Query("SELECT * FROM payments WHERE package_id = :packageId")
    List<Payment> getByPackageId(UUID packageId);
    
    @Query("SELECT * FROM payments WHERE package_id = :packageId AND payment_status = 'paid' LIMIT 1")
    Payment getPaidPaymentForPackage(UUID packageId);
    
    // Cash payment specific
    @Query("SELECT * FROM payments WHERE payment_method = 'cash' AND payment_status = 'paid'")
    List<Payment> getPaidCashPayments();
    
    @Query("SELECT SUM(amount_paid) FROM payments WHERE payment_method = 'cash' AND payment_status = 'paid'")
    Double getTotalCashCollected();
    
    @Query("SELECT SUM(change_given) FROM payments WHERE payment_method = 'cash' AND payment_status = 'paid'")
    Double getTotalChangeGiven();
    
    // Online payment specific
    @Query("SELECT * FROM payments WHERE transaction_id = :transactionId LIMIT 1")
    Payment getByTransactionId(String transactionId);
    
    // Payment lifecycle
    @Query("UPDATE payments SET payment_status = 'paid', completed_at = :completedAt, updated_at = :updatedAt WHERE id = :id")
    void markAsPaid(UUID id, Long completedAt, Long updatedAt);
    
    @Query("UPDATE payments SET amount_paid = :amountPaid, change_given = :changeGiven, cash_denominations = :cashDenominations, machine_cash_balance = :machineCashBalance, payment_status = 'paid', completed_at = :completedAt, updated_at = :updatedAt WHERE id = :id")
    void completeCashPayment(UUID id, Double amountPaid, Double changeGiven, String cashDenominations, Double machineCashBalance, Long completedAt, Long updatedAt);
}
```

---

## 4. NEW PaymentRepository (`/repository/PaymentRepository.java`)

### **Payment Management Methods:**
```java
public class PaymentRepository {
    // Create payments
    public Payment createCashPayment(UUID packageId, Double amount, String currency) {
        Payment payment = new Payment(packageId, amount, currency);
        insert(payment);
        return payment;
    }
    
    public Payment createOnlinePayment(UUID packageId, Double amount, String currency, String gateway) {
        Payment payment = new Payment(packageId, amount, currency, gateway);
        insert(payment);
        return payment;
    }
    
    // Complete payments
    public void completeCashPayment(UUID paymentId, Double amountPaid, Double changeGiven, 
                                   String cashDenominations, Double machineCashBalance) {
        // Updates payment with cash details and marks as paid
    }
    
    public void completeOnlinePayment(UUID paymentId, String transactionId) {
        // Updates payment with transaction ID and marks as paid
    }
    
    // Business logic
    public boolean isPackagePaid(UUID packageId) {
        Payment paidPayment = getPaidPaymentForPackage(packageId);
        return paidPayment != null;
    }
    
    // Cash management
    public Double getTotalCashCollected() {
        return paymentDao.getTotalCashCollected();
    }
}
```

---

## 5. CORRECTED PackageDao & Repository

### **REMOVED Methods (From V2 Mistake):**
```java
// REMOVED - Payment is now separate entity
@Query("SELECT * FROM packages WHERE payment_required = 1 AND payment_status = 'pending'")
List<Package> getUnpaidPackages();

@Query("UPDATE packages SET payment_status = 'paid', payment_method = :paymentMethod WHERE id = :id")
void markPaymentAsPaid(UUID id, String paymentMethod);

// All other payment-related methods removed from Package
```

### **CLEAN PackageRepository (Correct):**
```java
public class PackageRepository {
    // Pure package operations only
    public Package authenticateDeliveryPin(String deliveryPin);
    public Package authenticateClientPin(String clientPin);
    public Package authenticateReturnPin(String returnPin);
    
    public void markAsDelivered(UUID packageId, UUID doorId, UUID deliveryPersonId);
    public void markAsPicked(UUID packageId);
    public void markAsReturned(UUID packageId, UUID returnedBy);
    
    // No payment methods - handled by PaymentRepository
}
```

---

## 6. Updated Database Schema

### **MachineDatabase Entities:**
```java
@Database(
    entities = {
        User.class,          // Minimal user info
        LockerMachine.class, // Machine management
        DoorEntity.class,    // Door/compartment tracking
        Package.class,       // Package lifecycle
        Payment.class,       // Payment transactions (NEW)
        AuditLog.class,      // System audit trail
        MachineEvent.class   // Hardware events
    },
    version = 1,
    exportSchema = false
)
@TypeConverters({UuidConverter.class, JsonConverter.class})
public abstract class MachineDatabase extends RoomDatabase {
    public abstract PaymentDao paymentDao(); // NEW
    // ...other DAOs
}
```

---

## 7. New Workflows with Separate Payment Entity

### **1. Package Collection with Payment:**
```java
// Step 1: Authenticate client PIN
Package pkg = packageRepository.authenticateClientPin(enteredPin);

if (pkg != null && pkg.canUseClientPin()) {
    
    // Step 2: Check if payment required
    Payment paidPayment = paymentRepository.getPaidPaymentForPackage(pkg.getId());
    
    if (paidPayment == null) {
        // Step 3a: Create cash payment
        Payment cashPayment = paymentRepository.createCashPayment(
            pkg.getId(), 5.0, "USD");
        
        // Step 3b: Process cash in machine
        double cashReceived = collectCashFromMachine();
        double change = cashReceived - 5.0;
        
        // Step 3c: Complete cash payment
        paymentRepository.completeCashPayment(
            cashPayment.getId(), cashReceived, change, 
            "{\"5\": 1}", getCurrentMachineCashBalance());
    }
    
    // Step 4: Verify payment and allow pickup
    if (paymentRepository.isPackagePaid(pkg.getId())) {
        packageRepository.markAsPicked(pkg.getId());
        // Open door
    }
}
```

### **2. Online Payment Workflow:**
```java
// Step 1: Create online payment
Payment onlinePayment = paymentRepository.createOnlinePayment(
    packageId, 5.0, "USD", "stripe");

// Step 2: Process via API (not stored locally)
String transactionId = processOnlinePayment(onlinePayment);

// Step 3: Mark as completed
if (transactionId != null) {
    paymentRepository.completeOnlinePayment(onlinePayment.getId(), transactionId);
} else {
    paymentRepository.markAsFailed(onlinePayment.getId());
}
```

### **3. Cash Management:**
```java
// Machine cash tracking
public void syncCashToCloud() {
    List<Payment> cashPayments = paymentRepository.getPaidCashPayments();
    Double totalCash = paymentRepository.getTotalCashCollected();
    Double totalChange = paymentRepository.getTotalChangeGiven();
    
    // Sync cash payments to cloud for accounting
    for (Payment payment : cashPayments) {
        if ("local_only".equals(payment.getSyncStatus())) {
            syncPaymentToCloud(payment);
        }
    }
}
```

---

## 8. Entity Relationships (Corrected)

### **Database Relationships:**
```
[Users] ----< [Packages] >---- [Payments]
   |             |   |              |
   |             |   |              |
   |             |   O----[Doors] ---- [LockerMachines]
   |             |        |               |
   |             |        |               |
   O----< [AuditLogs] ----+               |
                          |               |
                          O----< [MachineEvents]

New Relationship:
[Packages] ----< [Payments] (One package can have multiple payment attempts)
```

### **Key Relationships:**
1. **Package** → **Payment**: One-to-many (refunds, multiple attempts)
2. **Package** → **User**: deliveredBy, returnedBy (who handled package)
3. **Package** → **Door**: Current location in machine
4. **Payment**: Standalone transaction record with complete audit trail

---

## 9. Benefits of Corrected Architecture

### **Separation of Concerns:**
- **Package**: Handles lifecycle, PINs, door assignment
- **Payment**: Handles all money transactions and cash management
- **User**: Minimal info for tracking only

### **Better Data Management:**
- **Multiple Payments**: Support refunds, failed attempts, partial payments
- **Cash Tracking**: Detailed machine cash balance management
- **Audit Trail**: Complete payment history separate from package lifecycle
- **Sync Efficiency**: Payment data syncs independently

### **Machine Operation:**
- **Cash Handling**: Like vending machine with change calculation
- **Online Payments**: API-based, minimal local storage
- **Payment Validation**: Simple check if package has paid payment record
- **Compliance**: Complete transaction records for auditing

---

## 10. Migration from V2 Mistake

### **If V2 Was Implemented:**
1. Remove payment fields from Package entity
2. Create Payment entity and migrate data
3. Update all payment-related queries to use Payment table
4. Fix foreign key relationships

### **Database Version:**
- Increment database version for schema changes
- Create migration scripts for production deployment

---

## 11. Machine Responsibilities (Clarified)

### **Local Machine Handles:**
- Package PIN authentication and lifecycle
- Cash payment collection and change dispensing
- Payment record creation and completion
- Machine cash balance tracking

### **Cloud/API Handles:**
- Online payment processing (Stripe, PayPal, etc.)
- User management and authentication
- Package creation and PIN generation
- Payment transaction reporting and reconciliation

### **Sync Process:**
- Cash payments sync with detailed denominations
- Payment statuses sync for reconciliation
- Machine cash balance syncs for accounting
- All data prepared for cloud integration

This corrected architecture now properly separates payment concerns into a dedicated Payment entity, providing better transaction tracking, cash management, and audit capabilities while keeping the Package entity clean and focused on its core lifecycle responsibilities.
