package com.example.parcellocker.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.example.parcellocker.db.entities.LockerMachine;

import java.util.List;
import java.util.UUID;

@Dao
public interface LockerMachineDao {

    @Query("SELECT * FROM locker_machines WHERE id = :id LIMIT 1")
    LockerMachine getById(UUID id);

    @Query("SELECT * FROM locker_machines WHERE machine_serial = :machineSerial LIMIT 1")
    LockerMachine getBySerial(String machineSerial);

    @Query("SELECT * FROM locker_machines WHERE is_active = 1")
    List<LockerMachine> getActiveMachines();

    @Query("SELECT * FROM locker_machines WHERE is_online = 1")
    List<LockerMachine> getOnlineMachines();

    @Query("SELECT * FROM locker_machines WHERE is_active = 1 AND is_online = 1")
    List<LockerMachine> getActiveOnlineMachines();

    @Query("SELECT * FROM locker_machines")
    List<LockerMachine> getAll();

    @Query("SELECT * FROM locker_machines WHERE sync_status = :syncStatus")
    List<LockerMachine> getBySyncStatus(String syncStatus);

    @Insert
    void insert(LockerMachine machine);

    @Insert
    void insertAll(List<LockerMachine> machines);

    @Update
    void update(LockerMachine machine);

    @Delete
    void delete(LockerMachine machine);

    @Query("DELETE FROM locker_machines WHERE id = :id")
    void deleteById(UUID id);

    @Query("SELECT * FROM locker_machines WHERE is_active = 1")
    LiveData<List<LockerMachine>> getActiveMachinesLive();

    @Query("SELECT * FROM locker_machines")
    LiveData<List<LockerMachine>> getAllLive();

    @Query("UPDATE locker_machines SET sync_status = :syncStatus WHERE id = :id")
    void updateSyncStatus(UUID id, String syncStatus);

    @Query("UPDATE locker_machines SET is_online = :isOnline, last_heartbeat = :lastHeartbeat WHERE id = :id")
    void updateOnlineStatus(UUID id, Boolean isOnline, Long lastHeartbeat);

    @Query("UPDATE locker_machines SET is_active = :isActive WHERE id = :id")
    void updateActiveStatus(UUID id, Boolean isActive);

    @Query("SELECT COUNT(*) FROM locker_machines WHERE is_active = 1 AND is_online = 1")
    int getActiveOnlineMachinesCount();
}
