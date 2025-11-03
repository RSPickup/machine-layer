package com.example.parcellocker.db.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.example.parcellocker.db.MachineDatabase;
import com.example.parcellocker.db.dao.DoorDao;
import com.example.parcellocker.db.entities.DoorEntity;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for Door operations.
 * Manages door states and availability in locker machines.
 */
public class DoorRepository {

    private DoorDao doorDao;
    private ExecutorService executor;

    public DoorRepository(Application application) {
        MachineDatabase database = MachineDatabase.getInstance(application);
        doorDao = database.doorDao();
        executor = Executors.newFixedThreadPool(4);
    }

    // Insert operations
    public void insert(DoorEntity door) {
        executor.execute(() -> doorDao.insert(door));
    }

    public void insertAll(List<DoorEntity> doors) {
        executor.execute(() -> doorDao.insertAll(doors));
    }

    // Update operations
    public void update(DoorEntity door) {
        executor.execute(() -> doorDao.update(door));
    }

    public void updateSyncStatus(UUID id, String syncStatus) {
        executor.execute(() -> doorDao.updateSyncStatus(id, syncStatus));
    }

    // Delete operations
    public void delete(DoorEntity door) {
        executor.execute(() -> doorDao.delete(door));
    }

    public void deleteById(UUID id) {
        executor.execute(() -> doorDao.deleteById(id));
    }

    // Query operations
    public DoorEntity getById(UUID id) {
        return doorDao.getById(id);
    }

    public List<DoorEntity> getByMachine(UUID lockerMachineId) {
        return doorDao.getByMachine(lockerMachineId);
    }

    public DoorEntity getByMachineAndIndex(UUID lockerMachineId, int index) {
        return doorDao.getByMachineAndIndex(lockerMachineId, index);
    }

    public List<DoorEntity> getAvailableDoors(UUID lockerMachineId) {
        return doorDao.getAvailableDoors(lockerMachineId);
    }

    public List<DoorEntity> getOccupiedDoors(UUID lockerMachineId) {
        return doorDao.getOccupiedDoors(lockerMachineId);
    }

    public List<DoorEntity> getAll() {
        return doorDao.getAll();
    }

    public List<DoorEntity> getBySyncStatus(String syncStatus) {
        return doorDao.getBySyncStatus(syncStatus);
    }

    public int getAvailableDoorsCount(UUID lockerMachineId) {
        return doorDao.getAvailableDoorsCount(lockerMachineId);
    }

    // LiveData operations
    public LiveData<List<DoorEntity>> getDoorsLive(UUID lockerMachineId) {
        return doorDao.getDoorsLive(lockerMachineId);
    }

    public LiveData<List<DoorEntity>> getAllLive() {
        return doorDao.getAllLive();
    }

    public void shutdown() {
        executor.shutdown();
    }
}
