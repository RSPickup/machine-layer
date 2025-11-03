package com.example.parcellocker.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import java.util.UUID;

/**
 * User entity - minimal information for machine operation only.
 * Just tracks who delivered packages - no authentication or detailed management.
 * Full user details managed in cloud/backoffice system.
 */
@Entity(tableName = "users")
public class User {

    @PrimaryKey
    public UUID id; // Cloud user ID for sync and package tracking

    public String name; // Display name for machine UI (e.g., "John Doe")

    @ColumnInfo(name = "sync_status")
    public String syncStatus; // "local_only", "synced", "pending_sync"

    // Constructors
    public User() {
        this.id = UUID.randomUUID();
        this.syncStatus = "local_only";
    }

    // Constructor for cloud sync (when user comes from backoffice)
    public User(UUID cloudId, String name) {
        this.id = cloudId;
        this.name = name;
        this.syncStatus = "synced";
    }

    // Basic getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
}
