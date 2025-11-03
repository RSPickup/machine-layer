package com.example.parcellocker.service;

import android.content.Context;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.example.parcellocker.db.repository.*;
import com.example.parcellocker.db.entities.*;

/**
 * CORRECTED: Workflow Service implementing the corrected business logic
 * - Package reference + PIN authentication
 * - Algeria-specific payment processing (DZD currency)
 * - Pre-assigned door validation
 * - Insufficient payment handling (return all money)
 */
public class WorkflowService {

    private PackageRepository packageRepository;
    private PaymentRepository paymentRepository;
    private DoorRepository doorRepository;
    private UserRepository userRepository;
    private AuditLogRepository auditLogRepository;
    private SyncService syncService;

    public WorkflowService(Context context) {
        // Initialize repositories and sync service
        // In real implementation, these would be dependency injected
        this.syncService = new SyncService(context);
    }

    // CORRECTED: Delivery workflow with package reference + PIN + pre-assigned door
    public DeliveryResult processDelivery(String packageReference, String deliveryPin, UUID deliveryPersonId) {
        try {
            // Step 1: Authenticate with package reference + delivery PIN
            Package pkg = packageRepository.authenticateDeliveryPin(packageReference, deliveryPin);

            if (pkg == null) {
                return new DeliveryResult(false, "Invalid package reference or delivery PIN");
            }

            // Step 2: Validate door is pre-assigned by backoffice
            if (!pkg.hasDoorAssigned()) {
                return new DeliveryResult(false, "Door assignment error. Please contact support.");
            }

            DoorEntity assignedDoor = doorRepository.getById(pkg.getDoorId());
            if (assignedDoor == null) {
                return new DeliveryResult(false, "Assigned door not found. Please contact support.");
            }

            // Step 3: Complete delivery using pre-assigned door
            packageRepository.markAsDelivered(pkg.getId(), deliveryPersonId);

            // Update door status
            assignedDoor.setIsOccupied(true);
            doorRepository.update(assignedDoor);

            // Create audit log
            createAuditLog("Package", pkg.getId(), "deliver", deliveryPersonId,
                          Map.of("door_id", assignedDoor.getId().toString(),
                                 "tracking_number", pkg.getTrackingNumber()));

            // Sync immediately if online
            syncService.syncPackageImmediately(pkg);

            return new DeliveryResult(true,
                "Package delivered successfully to compartment " + assignedDoor.getLabel() +
                ". 72-hour pickup timer started.");

        } catch (Exception e) {
            return new DeliveryResult(false, "Delivery failed: " + e.getMessage());
        }
    }

    // CORRECTED: Client collection workflow with package reference + PIN + Algeria payment
    public CollectionResult processCollection(String packageReference, String clientPin) {
        try {
            // Step 1: Authenticate with package reference + client PIN
            Package pkg = packageRepository.authenticateClientPin(packageReference, clientPin);

            if (pkg == null) {
                return new CollectionResult(false, "Invalid package reference or collection PIN");
            }

            if (pkg.isExpired()) {
                return new CollectionResult(false, "Package has expired. Please contact customer service.");
            }

            // Step 2: Check if payment is required
            Payment existingPayment = paymentRepository.getPaidPaymentForPackage(pkg.getId());

            if (existingPayment == null) {
                // Payment required - return payment options based on internet connectivity
                boolean hasInternet = paymentRepository.isOnlinePaymentAvailable();

                PaymentOptionsResult paymentOptions = new PaymentOptionsResult();
                paymentOptions.hasInternet = hasInternet;
                paymentOptions.cashAvailable = true;
                paymentOptions.onlineAvailable = hasInternet;
                paymentOptions.message = hasInternet ?
                    "Payment required. Choose: Cash or Online Payment" :
                    "Payment required. Only Cash payment available (no internet connection)";

                return new CollectionResult(false, "Payment required", paymentOptions);
            }

            // Step 3: Payment completed - proceed with collection
            return completePackageCollection(pkg);

        } catch (Exception e) {
            return new CollectionResult(false, "Collection failed: " + e.getMessage());
        }
    }

    // CORRECTED: Cash payment processing with Algeria DZD and insufficient payment handling
    public PaymentResult processCashPaymentDZD(UUID packageId, double requiredAmount, double receivedAmount) {
        try {
            // Create payment record in DZD
            Payment cashPayment = paymentRepository.createCashPaymentDZD(packageId, requiredAmount);

            // Check if payment is sufficient
            if (cashPayment.isSufficientPayment(receivedAmount)) {
                // Sufficient payment - calculate change
                double change = cashPayment.calculateChange(receivedAmount);

                // Complete payment with machine cash balance update
                String denominations = createDenominationsJson(receivedAmount);
                double newBalance = getCurrentMachineCashBalance() + receivedAmount - change;

                boolean success = paymentRepository.processCashPaymentDZD(
                    cashPayment.getId(), receivedAmount, denominations, newBalance);

                if (success) {
                    // Sync payment immediately if online
                    syncService.syncPaymentImmediately(cashPayment);

                    return new PaymentResult(true,
                        "Payment completed successfully. Change: " + String.format("%.0f", change) + " DZD",
                        change);
                } else {
                    return new PaymentResult(false, "Payment processing failed");
                }

            } else {
                // CORRECTED: Insufficient payment - return all money and cancel operation
                double shortfall = requiredAmount - receivedAmount;

                // Mark as insufficient and failed
                cashPayment.markAsInsufficientAndFailed(receivedAmount);
                paymentRepository.update(cashPayment);

                // Create audit log for insufficient payment
                createAuditLog("Payment", cashPayment.getId(), "insufficient_payment", null,
                              Map.of("required", String.valueOf(requiredAmount),
                                     "received", String.valueOf(receivedAmount),
                                     "shortfall", String.valueOf(shortfall),
                                     "money_returned", String.valueOf(receivedAmount)));

                return new PaymentResult(false,
                    "Insufficient payment. " + String.format("%.0f", shortfall) +
                    " DZD missing. All money returned. Operation cancelled.",
                    receivedAmount); // Amount returned
            }

        } catch (Exception e) {
            return new PaymentResult(false, "Payment processing error: " + e.getMessage());
        }
    }

    // CORRECTED: Online payment processing for Algeria
    public PaymentResult processOnlinePaymentDZD(UUID packageId, double requiredAmount, String gateway) {
        try {
            if (!paymentRepository.isOnlinePaymentAvailable()) {
                return new PaymentResult(false, "Online payment unavailable - no internet connection");
            }

            // Create payment record in DZD
            Payment onlinePayment = paymentRepository.createOnlinePaymentDZD(packageId, requiredAmount, gateway);

            // Call Algerian payment API (CIB, SATIM, etc.)
            PaymentGatewayResult gatewayResult = callAlgerianPaymentAPI(onlinePayment, requiredAmount);

            if (gatewayResult.isSuccessful()) {
                paymentRepository.completeOnlinePayment(onlinePayment.getId(), gatewayResult.getTransactionId());

                // Create audit log
                createAuditLog("Payment", onlinePayment.getId(), "online_payment_success", null,
                              Map.of("amount", String.valueOf(requiredAmount),
                                     "currency", "DZD",
                                     "gateway", gateway,
                                     "transaction_id", gatewayResult.getTransactionId()));

                // Sync immediately if online
                syncService.syncPaymentImmediately(onlinePayment);

                return new PaymentResult(true, "Online payment completed successfully");

            } else {
                paymentRepository.markAsFailed(onlinePayment.getId());
                return new PaymentResult(false, "Online payment failed: " + gatewayResult.getErrorMessage());
            }

        } catch (Exception e) {
            return new PaymentResult(false, "Online payment error: " + e.getMessage());
        }
    }

    // CORRECTED: Return workflow with assigned staff
    public ReturnResult processReturn(String packageReference, String returnPin, UUID returnStaffId) {
        try {
            // Step 1: Authenticate with package reference + return PIN
            Package pkg = packageRepository.authenticateReturnPin(packageReference, returnPin);

            if (pkg == null) {
                return new ReturnResult(false, "Invalid package reference or return PIN");
            }

            // Step 2: Complete return process
            DoorEntity door = doorRepository.getById(pkg.getDoorId());
            if (door == null) {
                return new ReturnResult(false, "Door not found for package");
            }

            // Mark package as returned and track who returned it
            packageRepository.markAsReturned(pkg.getId(), returnStaffId);

            // Update door status - now available
            door.setIsOccupied(false);
            doorRepository.update(door);

            // Create audit log
            User returnStaff = userRepository.getById(returnStaffId);
            createAuditLog("Package", pkg.getId(), "return", returnStaffId,
                          Map.of("door_id", door.getId().toString(),
                                 "return_reason", "expired",
                                 "staff_name", returnStaff != null ? returnStaff.getName() : "Unknown",
                                 "expiry_time", pkg.getExpiryTimestamp().toString()));

            // Sync immediately if online
            syncService.syncPackageImmediately(pkg);

            return new ReturnResult(true,
                "Package returned to backoffice successfully by " +
                (returnStaff != null ? returnStaff.getName() : "staff"));

        } catch (Exception e) {
            return new ReturnResult(false, "Return failed: " + e.getMessage());
        }
    }

    // Helper methods
    private CollectionResult completePackageCollection(Package pkg) {
        try {
            DoorEntity door = doorRepository.getById(pkg.getDoorId());
            if (door == null) {
                return new CollectionResult(false, "Door not found for package");
            }

            // Mark package as picked and door as available
            packageRepository.markAsPicked(pkg.getId());
            door.setIsOccupied(false);
            doorRepository.update(door);

            // Create audit log
            Payment completedPayment = paymentRepository.getPaidPaymentForPackage(pkg.getId());
            createAuditLog("Package", pkg.getId(), "collect", null,
                          Map.of("door_id", door.getId().toString(),
                                 "payment_method", completedPayment != null ? completedPayment.getPaymentMethod() : "none"));

            // Sync immediately if online
            syncService.syncPackageImmediately(pkg);

            return new CollectionResult(true,
                "Payment successful! Package collected from compartment " + door.getLabel());

        } catch (Exception e) {
            return new CollectionResult(false, "Collection completion failed: " + e.getMessage());
        }
    }

    private void createAuditLog(String entityType, UUID entityId, String action, UUID userId, Map<String, Object> details) {
        // Implementation would create audit log entry
        System.out.println("Audit: " + action + " on " + entityType + " " + entityId);
    }

    private String createDenominationsJson(double amount) {
        // Simplified - would create actual denominations JSON
        return "{\"" + String.valueOf((int)amount) + "\": 1}";
    }

    private double getCurrentMachineCashBalance() {
        // Would get actual machine cash balance
        return 5000.0; // Placeholder
    }

    private PaymentGatewayResult callAlgerianPaymentAPI(Payment payment, double amount) {
        // Placeholder for actual Algerian payment gateway integration
        return new PaymentGatewayResult(true, "TXN" + System.currentTimeMillis());
    }

    // Result classes
    public static class DeliveryResult {
        public boolean success;
        public String message;

        public DeliveryResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    public static class CollectionResult {
        public boolean success;
        public String message;
        public PaymentOptionsResult paymentOptions;

        public CollectionResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public CollectionResult(boolean success, String message, PaymentOptionsResult paymentOptions) {
            this.success = success;
            this.message = message;
            this.paymentOptions = paymentOptions;
        }
    }

    public static class PaymentResult {
        public boolean success;
        public String message;
        public double changeOrRefund;

        public PaymentResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public PaymentResult(boolean success, String message, double changeOrRefund) {
            this.success = success;
            this.message = message;
            this.changeOrRefund = changeOrRefund;
        }
    }

    public static class ReturnResult {
        public boolean success;
        public String message;

        public ReturnResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    public static class PaymentOptionsResult {
        public boolean hasInternet;
        public boolean cashAvailable;
        public boolean onlineAvailable;
        public String message;
    }

    public static class PaymentGatewayResult {
        private boolean successful;
        private String transactionId;
        private String errorMessage;

        public PaymentGatewayResult(boolean successful, String transactionId) {
            this.successful = successful;
            this.transactionId = transactionId;
        }

        public PaymentGatewayResult(boolean successful, String errorMessage, boolean isError) {
            this.successful = successful;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccessful() { return successful; }
        public String getTransactionId() { return transactionId; }
        public String getErrorMessage() { return errorMessage; }
    }
}
