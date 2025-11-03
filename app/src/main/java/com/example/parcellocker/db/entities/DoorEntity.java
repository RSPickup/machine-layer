package com.example.parcellocker.db.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import java.util.UUID;

/**
 * Door entity representing individual doors/compartments in locker machines.
 * Each door belongs to a locker machine and can contain packages.
 */
@Entity(tableName = "doors",
        foreignKeys = {
            @ForeignKey(entity = LockerMachine.class,
                       parentColumns = "id",
                       childColumns = "lockerMachineId",
                       onDelete = ForeignKey.CASCADE)
        })
public class DoorEntity {

    @PrimaryKey
    public UUID id;

    @ColumnInfo(name = "locker_machine_id")
    public UUID lockerMachineId; // Foreign key to LockerMachine.id

    @ColumnInfo(name = "cu_id")
    public int cuId; // Control unit ID

    @ColumnInfo(name = "door_index")
    public int doorIndex; // 0-based door index within the machine

    public String label; // Human-readable door label (e.g., "A1", "B3")

    @ColumnInfo(name = "is_locked")
    public Boolean isLocked; // true = locked, false = unlocked

    @ColumnInfo(name = "is_occupied")
    public Boolean isOccupied; // true = contains package, false = empty

    @ColumnInfo(name = "size_category")
    public String sizeCategory; // e.g., "small", "medium", "large"

    @ColumnInfo(name = "created_at")
    public Long createdAt;

    @ColumnInfo(name = "updated_at")
    public Long updatedAt;

    @ColumnInfo(name = "sync_status")
    public String syncStatus; // e.g., "local_only", "synced", "pending_sync"

    // Constructors
    public DoorEntity() {
        this.id = UUID.randomUUID();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.syncStatus = "local_only";
        this.isLocked = true;
        this.isOccupied = false;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getLockerMachineId() { return lockerMachineId; }
    public void setLockerMachineId(UUID lockerMachineId) { this.lockerMachineId = lockerMachineId; }

    public int getCuId() { return cuId; }
    public void setCuId(int cuId) { this.cuId = cuId; }

    public int getDoorIndex() { return doorIndex; }
    public void setDoorIndex(int doorIndex) { this.doorIndex = doorIndex; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public Boolean getIsLocked() { return isLocked; }
    public void setIsLocked(Boolean isLocked) {
        this.isLocked = isLocked;
        this.updatedAt = System.currentTimeMillis();
    }

    public Boolean getIsOccupied() { return isOccupied; }
    public void setIsOccupied(Boolean isOccupied) {
        this.isOccupied = isOccupied;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getSizeCategory() { return sizeCategory; }
    public void setSizeCategory(String sizeCategory) { this.sizeCategory = sizeCategory; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
}