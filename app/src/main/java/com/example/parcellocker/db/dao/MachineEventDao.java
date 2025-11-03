package com.example.parcellocker.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.example.parcellocker.db.entities.MachineEvent;

import java.util.List;
import java.util.UUID;

@Dao
public interface MachineEventDao {

    @Query("SELECT * FROM machine_events WHERE id = :id LIMIT 1")
    MachineEvent getById(UUID id);

    @Query("SELECT * FROM machine_events WHERE locker_machine_id = :lockerMachineId ORDER BY timestamp DESC")
    List<MachineEvent> getByMachineId(UUID lockerMachineId);

    @Query("SELECT * FROM machine_events WHERE door_id = :doorId ORDER BY timestamp DESC")
    List<MachineEvent> getByDoorId(UUID doorId);

    @Query("SELECT * FROM machine_events WHERE event_type = :eventType ORDER BY timestamp DESC")
    List<MachineEvent> getByEventType(String eventType);

    @Query("SELECT * FROM machine_events WHERE severity = :severity ORDER BY timestamp DESC")
    List<MachineEvent> getBySeverity(String severity);

    @Query("SELECT * FROM machine_events WHERE is_resolved = 0 ORDER BY timestamp DESC")
    List<MachineEvent> getUnresolvedEvents();

    @Query("SELECT * FROM machine_events WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    List<MachineEvent> getByTimeRange(Long startTime, Long endTime);

    @Query("SELECT * FROM machine_events WHERE locker_machine_id = :lockerMachineId AND event_type = :eventType ORDER BY timestamp DESC LIMIT :limit")
    List<MachineEvent> getByMachineAndEventType(UUID lockerMachineId, String eventType, int limit);

    @Query("SELECT * FROM machine_events ORDER BY timestamp DESC LIMIT :limit")
    List<MachineEvent> getRecent(int limit);

    @Query("SELECT * FROM machine_events")
    List<MachineEvent> getAll();

    @Query("SELECT * FROM machine_events WHERE sync_status = :syncStatus")
    List<MachineEvent> getBySyncStatus(String syncStatus);

    @Insert
    void insert(MachineEvent machineEvent);

    @Insert
    void insertAll(List<MachineEvent> machineEvents);

    @Update
    void update(MachineEvent machineEvent);

    @Delete
    void delete(MachineEvent machineEvent);

    @Query("DELETE FROM machine_events WHERE id = :id")
    void deleteById(UUID id);

    @Query("SELECT * FROM machine_events WHERE is_resolved = 0 ORDER BY timestamp DESC")
    LiveData<List<MachineEvent>> getUnresolvedEventsLive();

    @Query("SELECT * FROM machine_events WHERE locker_machine_id = :lockerMachineId ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<MachineEvent>> getByMachineIdLive(UUID lockerMachineId, int limit);

    @Query("SELECT * FROM machine_events ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<MachineEvent>> getRecentLive(int limit);

    @Query("UPDATE machine_events SET sync_status = :syncStatus WHERE id = :id")
    void updateSyncStatus(UUID id, String syncStatus);

    @Query("UPDATE machine_events SET is_resolved = :isResolved, resolved_at = :resolvedAt WHERE id = :id")
    void markAsResolved(UUID id, Boolean isResolved, Long resolvedAt);

    @Query("DELETE FROM machine_events WHERE timestamp < :cutoffTime")
    void deleteOlderThan(Long cutoffTime);

    @Query("SELECT COUNT(*) FROM machine_events WHERE is_resolved = 0 AND severity IN ('error', 'critical')")
    int getCriticalUnresolvedCount();
}
