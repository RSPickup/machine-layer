package com.example.parcellocker.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import java.util.UUID;

/**
 * LockerMachine entity representing physical locker machines.
 * Each machine contains multiple doors and has location/status information.
 */
@Entity(tableName = "locker_machines")
public class LockerMachine {

    @PrimaryKey
    public UUID id;

    @ColumnInfo(name = "machine_serial")
    public String machineSerial; // Unique hardware serial number

    public String name; // Human-readable name (e.g., "Main Lobby Locker")

    public String location; // Physical location description

    @ColumnInfo(name = "ip_address")
    public String ipAddress; // Network IP address

    @ColumnInfo(name = "is_online")
    public Boolean isOnline; // Connection status

    @ColumnInfo(name = "is_active")
    public Boolean isActive; // Operational status

    @ColumnInfo(name = "total_doors")
    public Integer totalDoors; // Number of doors in this machine

    @ColumnInfo(name = "firmware_version")
    public String firmwareVersion;

    @ColumnInfo(name = "last_heartbeat")
    public Long lastHeartbeat; // Last communication timestamp

    @ColumnInfo(name = "created_at")
    public Long createdAt;

    @ColumnInfo(name = "updated_at")
    public Long updatedAt;

    @ColumnInfo(name = "sync_status")
    public String syncStatus; // e.g., "local_only", "synced", "pending_sync"

    // Constructors
    public LockerMachine() {
        this.id = UUID.randomUUID();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.syncStatus = "local_only";
        this.isOnline = false;
        this.isActive = true;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getMachineSerial() { return machineSerial; }
    public void setMachineSerial(String machineSerial) { this.machineSerial = machineSerial; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public Boolean getIsOnline() { return isOnline; }
    public void setIsOnline(Boolean isOnline) {
        this.isOnline = isOnline;
        this.updatedAt = System.currentTimeMillis();
        if (isOnline) {
            this.lastHeartbeat = System.currentTimeMillis();
        }
    }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
        this.updatedAt = System.currentTimeMillis();
    }

    public Integer getTotalDoors() { return totalDoors; }
    public void setTotalDoors(Integer totalDoors) { this.totalDoors = totalDoors; }

    public String getFirmwareVersion() { return firmwareVersion; }
    public void setFirmwareVersion(String firmwareVersion) { this.firmwareVersion = firmwareVersion; }

    public Long getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(Long lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
}
