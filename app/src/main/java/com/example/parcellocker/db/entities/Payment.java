package com.example.parcellocker.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.ColumnInfo;
import java.util.UUID;

/**
 * Payment entity for tracking package payment transactions.
 * Handles both online payments (API-based) and cash payments (machine-local).
 */
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
    public UUID packageId; // Foreign key to Package.id

    @ColumnInfo(name = "payment_method")
    public String paymentMethod; // "online", "cash"

    @ColumnInfo(name = "payment_status")
    public String paymentStatus; // "pending", "paid", "failed", "refunded"

    @ColumnInfo(name = "amount_required")
    public Double amountRequired; // Amount that needs to be paid

    @ColumnInfo(name = "amount_paid")
    public Double amountPaid; // Actual amount paid (for cash, might be more than required)

    @ColumnInfo(name = "change_given")
    public Double changeGiven; // Change dispensed for cash payments

    @ColumnInfo(name = "currency")
    public String currency; // "USD", "EUR", etc.

    // Online payment fields
    @ColumnInfo(name = "transaction_id")
    public String transactionId; // External payment gateway transaction ID

    @ColumnInfo(name = "payment_gateway")
    public String paymentGateway; // "stripe", "paypal", etc.

    // Cash payment fields (machine-local)
    @ColumnInfo(name = "cash_denominations")
    public String cashDenominations; // JSON of bills/coins received e.g., {"20": 1, "10": 2, "5": 1}

    @ColumnInfo(name = "machine_cash_balance")
    public Double machineCashBalance; // Machine's cash balance after this transaction

    // Timestamps
    @ColumnInfo(name = "initiated_at")
    public Long initiatedAt; // When payment was initiated

    @ColumnInfo(name = "completed_at")
    public Long completedAt; // When payment was completed

    @ColumnInfo(name = "created_at")
    public Long createdAt;

    @ColumnInfo(name = "updated_at")
    public Long updatedAt;

    @ColumnInfo(name = "sync_status")
    public String syncStatus; // "local_only", "synced", "pending_sync"

    // Constructors
    public Payment() {
        this.id = UUID.randomUUID();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.syncStatus = "local_only";
        this.paymentStatus = "pending";
        this.initiatedAt = System.currentTimeMillis();
    }

    // Constructor for cash payment
    public Payment(UUID packageId, Double amountRequired, String currency) {
        this();
        this.packageId = packageId;
        this.amountRequired = amountRequired;
        this.currency = currency;
        this.paymentMethod = "cash";
    }

    // Constructor for online payment
    public Payment(UUID packageId, Double amountRequired, String currency, String paymentGateway) {
        this();
        this.packageId = packageId;
        this.amountRequired = amountRequired;
        this.currency = currency;
        this.paymentMethod = "online";
        this.paymentGateway = paymentGateway;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPackageId() { return packageId; }
    public void setPackageId(UUID packageId) { this.packageId = packageId; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
        this.updatedAt = System.currentTimeMillis();
    }

    public Double getAmountRequired() { return amountRequired; }
    public void setAmountRequired(Double amountRequired) { this.amountRequired = amountRequired; }

    public Double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(Double amountPaid) { this.amountPaid = amountPaid; }

    public Double getChangeGiven() { return changeGiven; }
    public void setChangeGiven(Double changeGiven) { this.changeGiven = changeGiven; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getPaymentGateway() { return paymentGateway; }
    public void setPaymentGateway(String paymentGateway) { this.paymentGateway = paymentGateway; }

    public String getCashDenominations() { return cashDenominations; }
    public void setCashDenominations(String cashDenominations) { this.cashDenominations = cashDenominations; }

    public Double getMachineCashBalance() { return machineCashBalance; }
    public void setMachineCashBalance(Double machineCashBalance) { this.machineCashBalance = machineCashBalance; }

    public Long getInitiatedAt() { return initiatedAt; }
    public void setInitiatedAt(Long initiatedAt) { this.initiatedAt = initiatedAt; }

    public Long getCompletedAt() { return completedAt; }
    public void setCompletedAt(Long completedAt) { this.completedAt = completedAt; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    // Business logic methods
    public boolean isPaid() {
        return "paid".equals(paymentStatus);
    }

    public boolean isCashPayment() {
        return "cash".equals(paymentMethod);
    }

    public boolean isOnlinePayment() {
        return "online".equals(paymentMethod);
    }

    public boolean isCompleted() {
        return completedAt != null && isPaid();
    }

    public void markAsPaid() {
        this.paymentStatus = "paid";
        this.completedAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public void markAsFailed() {
        this.paymentStatus = "failed";
        this.updatedAt = System.currentTimeMillis();
    }

    // NEW: Algeria-specific payment methods
    public boolean isDZDCurrency() {
        return "DZD".equals(currency);
    }

    public boolean isSufficientPayment(Double receivedAmount) {
        return receivedAmount != null && amountRequired != null &&
               receivedAmount >= amountRequired;
    }

    public Double calculateChange(Double receivedAmount) {
        if (receivedAmount != null && amountRequired != null && receivedAmount > amountRequired) {
            return receivedAmount - amountRequired;
        }
        return 0.0;
    }

    // NEW: Insufficient payment handling (return all money)
    public boolean isInsufficientPayment(Double receivedAmount) {
        return !isSufficientPayment(receivedAmount);
    }

    public void markAsInsufficientAndFailed(Double receivedAmount) {
        this.paymentStatus = "failed";
        this.amountPaid = receivedAmount;
        this.updatedAt = System.currentTimeMillis();
        // Note: All money should be returned to customer
    }
}
