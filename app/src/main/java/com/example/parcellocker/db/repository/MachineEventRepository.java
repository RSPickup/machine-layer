package com.example.parcellocker.db.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.example.parcellocker.db.MachineDatabase;
import com.example.parcellocker.db.dao.MachineEventDao;
import com.example.parcellocker.db.entities.MachineEvent;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for MachineEvent operations.
 * Manages real-time hardware events and system monitoring.
 */
public class MachineEventRepository {

    private MachineEventDao machineEventDao;
    private ExecutorService executor;

    public MachineEventRepository(Application application) {
        MachineDatabase database = MachineDatabase.getInstance(application);
        machineEventDao = database.machineEventDao();
        executor = Executors.newFixedThreadPool(4);
    }

    // Insert operations
    public void insert(MachineEvent machineEvent) {
        executor.execute(() -> machineEventDao.insert(machineEvent));
    }

    public void insertAll(List<MachineEvent> machineEvents) {
        executor.execute(() -> machineEventDao.insertAll(machineEvents));
    }

    // Update operations
    public void update(MachineEvent machineEvent) {
        executor.execute(() -> machineEventDao.update(machineEvent));
    }

    public void updateSyncStatus(UUID id, String syncStatus) {
        executor.execute(() -> machineEventDao.updateSyncStatus(id, syncStatus));
    }

    public void markAsResolved(UUID id) {
        executor.execute(() -> machineEventDao.markAsResolved(id, true, System.currentTimeMillis()));
    }

    // Delete operations
    public void delete(MachineEvent machineEvent) {
        executor.execute(() -> machineEventDao.delete(machineEvent));
    }

    public void deleteById(UUID id) {
        executor.execute(() -> machineEventDao.deleteById(id));
    }

    public void deleteOlderThan(Long cutoffTime) {
        executor.execute(() -> machineEventDao.deleteOlderThan(cutoffTime));
    }

    // Query operations
    public MachineEvent getById(UUID id) {
        return machineEventDao.getById(id);
    }

    public List<MachineEvent> getByMachineId(UUID lockerMachineId) {
        return machineEventDao.getByMachineId(lockerMachineId);
    }

    public List<MachineEvent> getByDoorId(UUID doorId) {
        return machineEventDao.getByDoorId(doorId);
    }

    public List<MachineEvent> getByEventType(String eventType) {
        return machineEventDao.getByEventType(eventType);
    }

    public List<MachineEvent> getBySeverity(String severity) {
        return machineEventDao.getBySeverity(severity);
    }

    public List<MachineEvent> getUnresolvedEvents() {
        return machineEventDao.getUnresolvedEvents();
    }

    public List<MachineEvent> getByTimeRange(Long startTime, Long endTime) {
        return machineEventDao.getByTimeRange(startTime, endTime);
    }

    public List<MachineEvent> getByMachineAndEventType(UUID lockerMachineId, String eventType, int limit) {
        return machineEventDao.getByMachineAndEventType(lockerMachineId, eventType, limit);
    }

    public List<MachineEvent> getRecent(int limit) {
        return machineEventDao.getRecent(limit);
    }

    public List<MachineEvent> getAll() {
        return machineEventDao.getAll();
    }

    public List<MachineEvent> getBySyncStatus(String syncStatus) {
        return machineEventDao.getBySyncStatus(syncStatus);
    }

    public int getCriticalUnresolvedCount() {
        return machineEventDao.getCriticalUnresolvedCount();
    }

    // LiveData operations
    public LiveData<List<MachineEvent>> getUnresolvedEventsLive() {
        return machineEventDao.getUnresolvedEventsLive();
    }

    public LiveData<List<MachineEvent>> getByMachineIdLive(UUID lockerMachineId, int limit) {
        return machineEventDao.getByMachineIdLive(lockerMachineId, limit);
    }

    public LiveData<List<MachineEvent>> getRecentLive(int limit) {
        return machineEventDao.getRecentLive(limit);
    }

    public void shutdown() {
        executor.shutdown();
    }
}
