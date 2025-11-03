package com.example.parcellocker.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.ColumnInfo;
import java.util.UUID;
import java.util.Map;

/**
 * MachineEvent entity for tracking real-time hardware events.
 * Uses JSON converter for flexible event details storage.
 */
@Entity(tableName = "machine_events",
        foreignKeys = {
            @ForeignKey(entity = LockerMachine.class,
                       parentColumns = "id",
                       childColumns = "lockerMachineId",
                       onDelete = ForeignKey.CASCADE),
            @ForeignKey(entity = DoorEntity.class,
                       parentColumns = "id",
                       childColumns = "doorId",
                       onDelete = ForeignKey.SET_NULL)
        })
public class MachineEvent {

    @PrimaryKey
    public UUID id;

    @ColumnInfo(name = "locker_machine_id")
    public UUID lockerMachineId; // Foreign key to LockerMachine.id

    @ColumnInfo(name = "door_id")
    public UUID doorId; // Foreign key to DoorEntity.id (nullable for machine-level events)

    @ColumnInfo(name = "event_type")
    public String eventType; // e.g., "door_opened", "door_closed", "sensor_triggered", "error"

    public String severity; // e.g., "info", "warning", "error", "critical"

    public Long timestamp;

    public Map<String, Object> details; // JSON field for event-specific data

    @ColumnInfo(name = "is_resolved")
    public Boolean isResolved; // For error events

    @ColumnInfo(name = "resolved_at")
    public Long resolvedAt;

    @ColumnInfo(name = "sync_status")
    public String syncStatus; // e.g., "local_only", "synced", "pending_sync"

    // Constructors
    public MachineEvent() {
        this.id = UUID.randomUUID();
        this.timestamp = System.currentTimeMillis();
        this.syncStatus = "local_only";
        this.severity = "info";
        this.isResolved = true;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getLockerMachineId() { return lockerMachineId; }
    public void setLockerMachineId(UUID lockerMachineId) { this.lockerMachineId = lockerMachineId; }

    public UUID getDoorId() { return doorId; }
    public void setDoorId(UUID doorId) { this.doorId = doorId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }

    public Boolean getIsResolved() { return isResolved; }
    public void setIsResolved(Boolean isResolved) {
        this.isResolved = isResolved;
        if (isResolved && resolvedAt == null) {
            this.resolvedAt = System.currentTimeMillis();
        }
    }

    public Long getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Long resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
}
