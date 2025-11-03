# Parcel Locker Machine Workflows

**Based on Database Architecture Implementation**  
**Date**: October 2, 2025

## System Overview

The parcel locker machine operates with a **3-PIN authentication system** and **separate Payment entity** for transaction management. The machine handles local operations while syncing with cloud services for comprehensive management.

### Architecture Components Used:
- **Package Entity**: 3-PIN system (delivery_pin, client_pin, return_pin)
- **Payment Entity**: Separate transaction tracking (online/cash)
- **User Entity**: Minimal tracking (id, name) for delivery/return attribution
- **Door Entity**: Physical compartment management
- **Sync System**: All entities support cloud synchronization

---

## 1. Package Delivery Workflow (Delivery Person)

### **Step 1: Delivery Person Arrives**
```java
// Machine UI displays: "Select Service" -> User selects "DELIVERY"
// Machine displays: "Enter Package Reference"
String packageRef = getInputFromKeypad();

// Machine displays: "Enter Delivery PIN" 
String deliveryPin = getInputFromKeypad();

// Authenticate using PackageRepository with tracking number and PIN
Package pkg = packageRepository.getByTrackingNumber(packageRef);

if (pkg != null && deliveryPin.equals(pkg.getDeliveryPin()) && pkg.canUseDeliveryPin()) {
    // Valid package and PIN, package is in "pending" status
    displayPackageInfo(pkg.getTrackingNumber(), pkg.getRecipientName());
    
    // Door is pre-assigned by backoffice system
    DoorEntity assignedDoor = doorRepository.getById(pkg.getDoorId());
    displayMessage("Package assigned to compartment: " + assignedDoor.getLabel());
} else {
    displayError("Invalid package reference or delivery PIN");
    return;
}
```

### **Step 2: Open Pre-assigned Door**
```java
// Door is already assigned by backoffice, no need to find available door
DoorEntity assignedDoor = doorRepository.getById(pkg.getDoorId());

if (assignedDoor == null) {
    displayError("Door assignment error. Please contact support.");
    return;
}

// Open the pre-assigned door
openDoor(assignedDoor.getId());
displayMessage("Please place package in compartment " + assignedDoor.getLabel());
```

### **Step 3: Complete Delivery**
```java
// Wait for door to close (hardware sensor)
waitForDoorClose(assignedDoor.getId());

// Mark package as delivered in database
User deliveryPerson = userRepository.getById(getCurrentDeliveryPersonId());
packageRepository.markAsDelivered(pkg.getId(), assignedDoor.getId(), deliveryPerson.getId());

// Update door status
assignedDoor.setIsOccupied(true);
doorRepository.update(assignedDoor);

// Create audit log
createAuditLog("Package", pkg.getId(), "deliver", deliveryPerson.getId(), 
               Map.of("door_id", assignedDoor.getId().toString(), 
                      "tracking_number", pkg.getTrackingNumber()));

displaySuccess("Package delivered successfully to compartment " + assignedDoor.getLabel() + 
               ". 72-hour pickup timer started.");
```

**Result**: Package status changes from `"pending"` → `"delivered"` with 72-hour expiry timer.

---

## 2. Package Collection Workflow (Client)

### **Step 1: Client Authentication**
```java
// Machine UI displays: "Select Service" -> User selects "CLIENT"
// Machine displays: "Enter Package Reference"
String packageRef = getInputFromKeypad();

// Machine displays: "Enter Collection PIN"
String clientPin = getInputFromKeypad();

// Authenticate using PackageRepository with tracking number and PIN
Package pkg = packageRepository.getByTrackingNumber(packageRef);

if (pkg != null && clientPin.equals(pkg.getClientPin()) && pkg.canUseClientPin()) {
    // Valid package and PIN, package is "delivered" and not expired
    displayPackageInfo(pkg.getTrackingNumber(), pkg.getRecipientName());
} else if (pkg != null && pkg.isExpired()) {
    displayError("Package has expired. Please contact customer service.");
    return;
} else {
    displayError("Invalid package reference or collection PIN");
    return;
}
```

### **Step 2: Payment Options (Algeria-specific)**
```java
// Check if payment is required for this package
Payment existingPayment = paymentRepository.getPaidPaymentForPackage(pkg.getId());

if (existingPayment == null) {
    // Payment required - check internet connectivity for payment options
    boolean hasInternet = checkInternetConnectivity();
    
    if (hasInternet) {
        displayPaymentOptions("Cash", "Online Payment"); // Both options available
    } else {
        displayPaymentOptions("Cash"); // Only cash available offline
        displayMessage("Online payment unavailable - no internet connection");
    }
    
    String paymentMethod = getPaymentMethodChoice();
    
    if ("cash".equals(paymentMethod)) {
        processCashPaymentDZD(pkg.getId());
    } else if ("online".equals(paymentMethod) && hasInternet) {
        processOnlinePaymentDZD(pkg.getId());
    }
} else {
    displayMessage("Package already paid. Proceeding to pickup...");
}
```

### **Step 3: Cash Payment Processing (Algeria - DZD)**
```java
private void processCashPaymentDZD(UUID packageId) {
    // Create payment record in DZD
    double requiredAmount = 1000.0; // 1000 DZD example
    Payment cashPayment = paymentRepository.createCashPayment(packageId, requiredAmount, "DZD");
    
    displayMessage("Payment required: " + requiredAmount + " DZD");
    displayMessage("Current machine balance: " + getCurrentMachineCashBalance() + " DZD");
    displayMessage("Please insert cash for package collection");
    
    // Collect cash from machine hardware
    CashCollection result = collectCashFromMachine(requiredAmount);
    
    if (result.getTotalReceived() >= requiredAmount) {
        // Sufficient payment received
        double change = result.getTotalReceived() - requiredAmount;
        
        // Complete payment in database
        String denominations = result.getDenominationsAsJson(); // e.g., {"2000": 1}
        double newBalance = getCurrentMachineCashBalance() + result.getTotalReceived() - change;
        
        paymentRepository.completeCashPayment(
            cashPayment.getId(), 
            result.getTotalReceived(), 
            change, 
            denominations, 
            newBalance
        );
        
        if (change > 0) {
            dispenseChange(change);
            displayMessage("Change dispensed: " + String.format("%.0f", change) + " DZD");
        }
        
        displaySuccess("Payment completed successfully");
        
    } else {
        // Insufficient payment - return all money and cancel operation
        paymentRepository.markAsFailed(cashPayment.getId());
        
        // Return all inserted money
        returnAllCash(result.getTotalReceived());
        
        displayError("Insufficient payment. " + String.format("%.0f", requiredAmount - result.getTotalReceived()) + 
                    " DZD missing. All money returned. Operation cancelled.");
        
        // Create audit log for failed payment
        createAuditLog("Payment", cashPayment.getId(), "insufficient_payment", null,
                       Map.of("required", String.valueOf(requiredAmount),
                              "received", String.valueOf(result.getTotalReceived()),
                              "money_returned", String.valueOf(result.getTotalReceived())));
        return;
    }
}
```

### **Step 4: Online Payment Processing (Algeria - DZD)**
```java
private void processOnlinePaymentDZD(UUID packageId) {
    // Create payment record in DZD
    double requiredAmount = 1000.0; // 1000 DZD example
    Payment onlinePayment = paymentRepository.createOnlinePayment(packageId, requiredAmount, "DZD", "algeria_gateway");
    
    displayMessage("Processing online payment: " + requiredAmount + " DZD");
    
    try {
        // Call Algerian payment API (CIB, SATIM, etc.)
        PaymentResult result = callAlgerianPaymentAPI(onlinePayment, requiredAmount);
        
        if (result.isSuccessful()) {
            paymentRepository.completeOnlinePayment(onlinePayment.getId(), result.getTransactionId());
            displaySuccess("Payment completed successfully");
            
            // Create audit log
            createAuditLog("Payment", onlinePayment.getId(), "online_payment_success", null,
                           Map.of("amount", String.valueOf(requiredAmount),
                                  "currency", "DZD",
                                  "transaction_id", result.getTransactionId()));
        } else {
            paymentRepository.markAsFailed(onlinePayment.getId());
            displayError("Online payment failed: " + result.getErrorMessage());
            return;
        }
    } catch (Exception e) {
        paymentRepository.markAsFailed(onlinePayment.getId());
        displayError("Online payment error. Please try cash payment.");
        return;
    }
}
```

### **Step 5: Package Pickup After Successful Payment**
```java
// Verify payment is completed
if (paymentRepository.isPackagePaid(pkg.getId())) {
    // Find the door containing the package
    DoorEntity door = doorRepository.getById(pkg.getDoorId());
    
    // Open door for collection
    openDoor(door.getId());
    displayMessage("Payment successful! Please collect your package from compartment " + door.getLabel());
    
    // Wait for door to close
    waitForDoorClose(door.getId());
    
    // Mark package as picked and door as available
    packageRepository.markAsPicked(pkg.getId());
    door.setIsOccupied(false);
    doorRepository.update(door);
    
    // Create audit log
    Payment completedPayment = paymentRepository.getPaidPaymentForPackage(pkg.getId());
    createAuditLog("Package", pkg.getId(), "collect", null,
                   Map.of("door_id", door.getId().toString(),
                          "payment_method", completedPayment.getPaymentMethod(),
                          "amount_paid", completedPayment.getAmountPaid().toString(),
                          "currency", completedPayment.getCurrency()));
    
    displaySuccess("Package collected successfully. Thank you!");
} else {
    displayError("Payment not completed. Please complete payment first.");
}
```

---

## 3. Package Return Workflow (Backoffice/Admin)

### **Step 1: Expired Package Identification (Automatic)**
```java
// Background service running on machine
public void checkExpiredPackages() {
    List<Package> expiredPackages = packageRepository.getExpiredPackages();
    
    for (Package pkg : expiredPackages) {
        if ("delivered".equals(pkg.getStatus()) && pkg.isExpired()) {
            // Mark as expired for return processing
            packageRepository.updateStatus(pkg.getId(), "expired");
            
            // Log expiry event
            createAuditLog("Package", pkg.getId(), "expire", null,
                           Map.of("expiry_time", pkg.getExpiryTimestamp().toString()));
        }
    }
    
    // Display expired packages on machine UI for backoffice visibility
    displayExpiredPackagesBoard();
}

// Machine UI shows expired packages board
private void displayExpiredPackagesBoard() {
    List<Package> expiredPackages = packageRepository.getByStatus("expired");
    
    displayMessage("EXPIRED PACKAGES REQUIRING RETURN:");
    for (Package pkg : expiredPackages) {
        DoorEntity door = doorRepository.getById(pkg.getDoorId());
        displayMessage("- " + pkg.getTrackingNumber() + " in compartment " + door.getLabel() + 
                      " (expired " + formatTime(pkg.getExpiryTimestamp()) + ")");
    }
}
```

### **Step 2: Return Process (Similar to Delivery)**
```java
// Machine UI displays: "Select Service" -> User selects "RETURN"
// Backoffice staff selects which delivery person will collect expired package
// Machine displays: "Enter Package Reference"
String packageRef = getInputFromKeypad();

// Machine displays: "Enter Return PIN"
String returnPin = getInputFromKeypad();

// Authenticate using PackageRepository with tracking number and PIN
Package pkg = packageRepository.getByTrackingNumber(packageRef);

if (pkg != null && returnPin.equals(pkg.getReturnPin()) && pkg.canUseReturnPin()) {
    // Valid package and PIN, package is expired or being manually returned
    displayPackageInfo(pkg.getTrackingNumber(), "EXPIRED - Ready for return");
    
    DoorEntity door = doorRepository.getById(pkg.getDoorId());
    displayMessage("Package location: compartment " + door.getLabel());
} else {
    displayError("Invalid package reference or return PIN");
    return;
}
```

### **Step 3: Package Retrieval by Assigned Staff**
```java
// Same operation as delivery but in reverse - open door for retrieval
DoorEntity door = doorRepository.getById(pkg.getDoorId());

// Open door for staff retrieval
openDoor(door.getId());
displayMessage("Please retrieve expired package from compartment " + door.getLabel());

// Wait for door to close (confirms package removed)
waitForDoorClose(door.getId());

// Mark package as returned and track who returned it
User returnStaff = userRepository.getById(getCurrentReturnStaffId()); // Assigned delivery person
packageRepository.markAsReturned(pkg.getId(), returnStaff.getId());

// Update door status - now available
door.setIsOccupied(false);
doorRepository.update(door);

// Create audit log
createAuditLog("Package", pkg.getId(), "return", returnStaff.getId(),
               Map.of("door_id", door.getId().toString(),
                      "return_reason", "expired",
                      "staff_name", returnStaff.getName(),
                      "expiry_time", pkg.getExpiryTimestamp().toString()));

displaySuccess("Package returned to backoffice successfully by " + returnStaff.getName());
```

---

## 4. Cloud Synchronization Workflow

### **Step 1: Smart Sync Strategy (Internet-Dependent)**
```java
public class SyncService {
    private static final long OFFLINE_SYNC_INTERVAL = 15 * 60 * 1000; // 15 minutes when offline
    private boolean isOnlineMode = false;
    
    public void initializeSyncService() {
        // Check internet connectivity and set sync mode
        this.isOnlineMode = checkInternetConnectivity();
        
        if (isOnlineMode) {
            // Real-time sync mode - sync on each operation
            enableRealTimeSyncMode();
        } else {
            // Offline mode - periodic sync every 15 minutes
            schedulePeriodicSync();
        }
    }
    
    // Real-time sync when internet is available
    private void enableRealTimeSyncMode() {
        displayMessage("Online mode: Real-time sync enabled");
        
        // Sync immediately on each operation
        packageRepository.setOnOperationCallback(this::syncPackageImmediately);
        paymentRepository.setOnOperationCallback(this::syncPaymentImmediately);
        auditLogRepository.setOnOperationCallback(this::syncAuditLogImmediately);
    }
    
    // Periodic sync when offline
    private void schedulePeriodicSync() {
        displayMessage("Offline mode: 15-minute periodic sync enabled");
        
        Timer syncTimer = new Timer();
        syncTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (checkInternetConnectivity()) {
                    // Internet is back - switch to online mode
                    isOnlineMode = true;
                    enableRealTimeSyncMode();
                    syncTimer.cancel(); // Stop periodic sync
                    
                    // Sync all pending data immediately
                    performFullSync();
                } else {
                    // Still offline - continue periodic sync attempts
                    performOfflineSync();
                }
            }
        }, OFFLINE_SYNC_INTERVAL, OFFLINE_SYNC_INTERVAL);
    }
}
```

### **Step 2: Real-time Sync Operations (When Online)**
```java
// Immediate sync after each operation when internet is available
public void syncPackageImmediately(Package pkg) {
    if (isOnlineMode) {
        try {
            CloudResponse response = cloudAPI.syncPackage(pkg);
            if (response.isSuccessful()) {
                packageRepository.updateSyncStatus(pkg.getId(), "synced");
            }
        } catch (Exception e) {
            // Internet lost - switch to offline mode
            isOnlineMode = false;
            schedulePeriodicSync();
        }
    }
}

public void syncPaymentImmediately(Payment payment) {
    if (isOnlineMode) {
        try {
            // Critical: Cash payments must sync immediately for accounting
            if (payment.isCashPayment() && payment.isPaid()) {
                CashTransactionData cashData = new CashTransactionData(
                    payment.getId(), payment.getPackageId(), 
                    payment.getAmountPaid(), payment.getChangeGiven(),
                    payment.getCashDenominations(), payment.getMachineCashBalance(),
                    currentMachineId
                );
                
                CloudResponse response = cloudAPI.syncCashPayment(cashData);
                if (response.isSuccessful()) {
                    paymentRepository.updateSyncStatus(payment.getId(), "synced");
                }
            }
        } catch (Exception e) {
            isOnlineMode = false;
            schedulePeriodicSync();
        }
    }
}
```

### **Step 3: Offline Sync Operations (15-minute intervals)**
```java
private void performOfflineSync() {
    try {
        // Attempt to sync pending data every 15 minutes
        syncPendingPackages();
        syncPendingPayments();
        syncPendingAuditLogs();
        syncPendingMachineEvents();
        
        // Receive updates from cloud
        receiveCloudUpdates();
        
    } catch (Exception e) {
        logError("Offline sync failed, will retry in 15 minutes", e);
    }
}

private void syncPendingPackages() {
    List<Package> unsyncedPackages = packageRepository.getBySyncStatus("local_only");
    List<Package> pendingSyncPackages = packageRepository.getBySyncStatus("pending_sync");
    
    // Combine both lists
    List<Package> allPendingPackages = new ArrayList<>();
    allPendingPackages.addAll(unsyncedPackages);
    allPendingPackages.addAll(pendingSyncPackages);
    
    for (Package pkg : allPendingPackages) {
        try {
            CloudResponse response = cloudAPI.syncPackage(pkg);
            if (response.isSuccessful()) {
                packageRepository.updateSyncStatus(pkg.getId(), "synced");
            }
        } catch (Exception e) {
            packageRepository.updateSyncStatus(pkg.getId(), "pending_sync");
        }
    }
}

private void syncPendingPayments() {
    // Priority: Cash payments for accounting accuracy
    List<Payment> unsyncedCashPayments = paymentRepository.getBySyncStatus("local_only")
        .stream()
        .filter(p -> p.isCashPayment() && p.isPaid())
        .collect(Collectors.toList());
    
    for (Payment payment : unsyncedCashPayments) {
        try {
            CashTransactionData cashData = new CashTransactionData(
                payment.getId(), payment.getPackageId(),
                payment.getAmountPaid(), payment.getChangeGiven(),
                payment.getCashDenominations(), payment.getMachineCashBalance(),
                currentMachineId
            );
            
            CloudResponse response = cloudAPI.syncCashPayment(cashData);
            if (response.isSuccessful()) {
                paymentRepository.updateSyncStatus(payment.getId(), "synced");
            }
        } catch (Exception e) {
            logError("Failed to sync cash payment: " + payment.getId(), e);
        }
    }
}
```

### **Step 4: Bidirectional Cloud Updates**
```java
private void receiveCloudUpdates() {
    try {
        // Get new packages assigned to this machine
        List<CloudPackage> newPackages = cloudAPI.getNewPackages(currentMachineId);
        
        for (CloudPackage cloudPkg : newPackages) {
            // Convert and insert local package with pre-assigned door
            Package localPackage = convertFromCloudPackage(cloudPkg);
            localPackage.setDoorId(cloudPkg.getAssignedDoorId()); // Door pre-assigned by backoffice
            packageRepository.insert(localPackage);
            
            // Create payment record if required
            if (cloudPkg.requiresPayment()) {
                Payment payment = new Payment(localPackage.getId(), 
                                            cloudPkg.getPaymentAmount(), 
                                            "DZD"); // Algeria currency
                paymentRepository.insert(payment);
            }
        }
        
        // Get user updates (delivery staff assignments)
        List<CloudUser> updatedUsers = cloudAPI.getUserUpdates();
        for (CloudUser cloudUser : updatedUsers) {
            User localUser = new User(cloudUser.getId(), cloudUser.getName());
            userRepository.insert(localUser);
        }
        
        // Get machine configuration updates
        MachineConfig config = cloudAPI.getMachineConfig(currentMachineId);
        if (config != null) {
            updateMachineConfiguration(config);
        }
        
    } catch (Exception e) {
        logError("Failed to receive cloud updates", e);
    }
}
```
