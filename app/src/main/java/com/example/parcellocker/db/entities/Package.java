package com.example.parcellocker.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.ColumnInfo;
import java.util.UUID;

/**
 * Package entity with simplified lifecycle.
 * Status flow: pending → delivered (PIN used) → picked (collected) OR returned (to office)
 * Payment is handled by separate Payment entity.
 */
@Entity(tableName = "packages",
        foreignKeys = {
            @ForeignKey(entity = User.class,
                       parentColumns = "id",
                       childColumns = "deliveredBy",
                       onDelete = ForeignKey.SET_NULL),
            @ForeignKey(entity = User.class,
                       parentColumns = "id",
                       childColumns = "returnedBy",
                       onDelete = ForeignKey.SET_NULL),
            @ForeignKey(entity = DoorEntity.class,
                       parentColumns = "id",
                       childColumns = "doorId",
                       onDelete = ForeignKey.SET_NULL)
        })
public class Package {

    @PrimaryKey
    public UUID id;

    @ColumnInfo(name = "tracking_number")
    public String trackingNumber;

    // Simplified recipient info - just name for machine display
    @ColumnInfo(name = "recipient_name")
    public String recipientName;

    // PIN codes for different access levels
    @ColumnInfo(name = "delivery_pin")
    public String deliveryPin; // PIN for delivery person to place package

    @ColumnInfo(name = "client_pin")
    public String clientPin; // PIN for recipient to collect package

    @ColumnInfo(name = "return_pin")
    public String returnPin; // PIN for backoffice to return package after 72h

    // User tracking
    @ColumnInfo(name = "delivered_by")
    public UUID deliveredBy; // Who delivered the package

    @ColumnInfo(name = "returned_by")
    public UUID returnedBy; // Who returned the package to office

    @ColumnInfo(name = "door_id")
    public UUID doorId; // Which door contains the package

    // Package lifecycle - simplified status
    public String status; // "pending", "delivered", "picked", "returned"

    // Timestamps
    @ColumnInfo(name = "delivery_timestamp")
    public Long deliveryTimestamp;

    @ColumnInfo(name = "picked_timestamp")
    public Long pickedTimestamp; // When client collected

    @ColumnInfo(name = "expiry_timestamp")
    public Long expiryTimestamp; // 72 hours after delivery

    @ColumnInfo(name = "return_timestamp")
    public Long returnTimestamp; // When returned to office

    @ColumnInfo(name = "created_at")
    public Long createdAt;

    @ColumnInfo(name = "updated_at")
    public Long updatedAt;

    @ColumnInfo(name = "sync_status")
    public String syncStatus;

    // Constructors
    public Package() {
        this.id = UUID.randomUUID();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.syncStatus = "local_only";
        this.status = "pending";
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getDeliveryPin() { return deliveryPin; }
    public void setDeliveryPin(String deliveryPin) { this.deliveryPin = deliveryPin; }

    public String getClientPin() { return clientPin; }
    public void setClientPin(String clientPin) { this.clientPin = clientPin; }

    public String getReturnPin() { return returnPin; }
    public void setReturnPin(String returnPin) { this.returnPin = returnPin; }

    public UUID getDeliveredBy() { return deliveredBy; }
    public void setDeliveredBy(UUID deliveredBy) { this.deliveredBy = deliveredBy; }

    public UUID getReturnedBy() { return returnedBy; }
    public void setReturnedBy(UUID returnedBy) { this.returnedBy = returnedBy; }

    public UUID getDoorId() { return doorId; }
    public void setDoorId(UUID doorId) { this.doorId = doorId; }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = System.currentTimeMillis();
    }

    // Timestamp getters/setters
    public Long getDeliveryTimestamp() { return deliveryTimestamp; }
    public void setDeliveryTimestamp(Long deliveryTimestamp) {
        this.deliveryTimestamp = deliveryTimestamp;
        if (deliveryTimestamp != null) {
            this.expiryTimestamp = deliveryTimestamp + (72 * 60 * 60 * 1000); // 72 hours
        }
    }

    public Long getPickedTimestamp() { return pickedTimestamp; }
    public void setPickedTimestamp(Long pickedTimestamp) { this.pickedTimestamp = pickedTimestamp; }

    public Long getExpiryTimestamp() { return expiryTimestamp; }
    public void setExpiryTimestamp(Long expiryTimestamp) { this.expiryTimestamp = expiryTimestamp; }

    public Long getReturnTimestamp() { return returnTimestamp; }
    public void setReturnTimestamp(Long returnTimestamp) { this.returnTimestamp = returnTimestamp; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    // Business logic methods - CORRECTED for package reference + PIN authentication
    public boolean isExpired() {
        return expiryTimestamp != null && System.currentTimeMillis() > expiryTimestamp;
    }

    public boolean canUseDeliveryPin() {
        return "pending".equals(status);
    }

    public boolean canUseClientPin() {
        return "delivered".equals(status) && !isExpired();
    }

    public boolean canUseReturnPin() {
        return ("delivered".equals(status) && isExpired()) || "expired".equals(status);
    }

    // NEW: Authentication helper methods for corrected workflow
    public boolean authenticateDelivery(String trackingRef, String pin) {
        return trackingRef.equals(this.trackingNumber) &&
               pin.equals(this.deliveryPin) &&
               canUseDeliveryPin();
    }

    public boolean authenticateClient(String trackingRef, String pin) {
        return trackingRef.equals(this.trackingNumber) &&
               pin.equals(this.clientPin) &&
               canUseClientPin();
    }

    public boolean authenticateReturn(String trackingRef, String pin) {
        return trackingRef.equals(this.trackingNumber) &&
               pin.equals(this.returnPin) &&
               canUseReturnPin();
    }

    // NEW: Door assignment validation (must be pre-assigned by backoffice)
    public boolean hasDoorAssigned() {
        return doorId != null;
    }

    public boolean isReadyForDelivery() {
        return "pending".equals(status) && hasDoorAssigned();
    }
}
