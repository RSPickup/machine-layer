package com.example.parcellocker.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.ColumnInfo;
import java.util.UUID;
import java.util.Map;

/**
 * AuditLog entity for tracking all system actions and changes.
 * Uses JSON converter for flexible details storage.
 */
@Entity(tableName = "audit_logs",
        foreignKeys = {
            @ForeignKey(entity = User.class,
                       parentColumns = "id",
                       childColumns = "userId",
                       onDelete = ForeignKey.SET_NULL)
        })
public class AuditLog {

    @PrimaryKey
    public UUID id;

    @ColumnInfo(name = "entity_type")
    public String entityType; // e.g., "Package", "User", "Door"

    @ColumnInfo(name = "entity_id")
    public UUID entityId; // ID of the affected entity

    public String action; // e.g., "create", "update", "delete", "deliver", "collect"

    @ColumnInfo(name = "user_id")
    public UUID userId; // User who performed the action (nullable)

    public Long timestamp;

    public Map<String, Object> details; // JSON field for flexible audit data

    public String source; // e.g., "tablet", "web", "api"

    @ColumnInfo(name = "sync_status")
    public String syncStatus; // e.g., "local_only", "synced", "pending_sync"

    // Constructors
    public AuditLog() {
        this.id = UUID.randomUUID();
        this.timestamp = System.currentTimeMillis();
        this.syncStatus = "local_only";
        this.source = "tablet";
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
}
