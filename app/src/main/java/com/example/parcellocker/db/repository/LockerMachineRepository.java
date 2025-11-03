package com.example.parcellocker.db.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.example.parcellocker.db.MachineDatabase;
import com.example.parcellocker.db.dao.LockerMachineDao;
import com.example.parcellocker.db.entities.LockerMachine;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for LockerMachine operations.
 * Manages locker machine status and heartbeat monitoring.
 */
public class LockerMachineRepository {

    private LockerMachineDao lockerMachineDao;
    private ExecutorService executor;

    public LockerMachineRepository(Application application) {
        MachineDatabase database = MachineDatabase.getInstance(application);
        lockerMachineDao = database.lockerMachineDao();
        executor = Executors.newFixedThreadPool(4);
    }

    // Insert operations
    public void insert(LockerMachine machine) {
        executor.execute(() -> lockerMachineDao.insert(machine));
    }

    public void insertAll(List<LockerMachine> machines) {
        executor.execute(() -> lockerMachineDao.insertAll(machines));
    }

    // Update operations
    public void update(LockerMachine machine) {
        executor.execute(() -> lockerMachineDao.update(machine));
    }

    public void updateSyncStatus(UUID id, String syncStatus) {
        executor.execute(() -> lockerMachineDao.updateSyncStatus(id, syncStatus));
    }

    public void updateOnlineStatus(UUID id, Boolean isOnline) {
        executor.execute(() -> lockerMachineDao.updateOnlineStatus(id, isOnline, System.currentTimeMillis()));
    }

    public void updateActiveStatus(UUID id, Boolean isActive) {
        executor.execute(() -> lockerMachineDao.updateActiveStatus(id, isActive));
    }

    // Delete operations
    public void delete(LockerMachine machine) {
        executor.execute(() -> lockerMachineDao.delete(machine));
    }

    public void deleteById(UUID id) {
        executor.execute(() -> lockerMachineDao.deleteById(id));
    }

    // Query operations
    public LockerMachine getById(UUID id) {
        return lockerMachineDao.getById(id);
    }

    public LockerMachine getBySerial(String machineSerial) {
        return lockerMachineDao.getBySerial(machineSerial);
    }

    public List<LockerMachine> getActiveMachines() {
        return lockerMachineDao.getActiveMachines();
    }

    public List<LockerMachine> getOnlineMachines() {
        return lockerMachineDao.getOnlineMachines();
    }

    public List<LockerMachine> getActiveOnlineMachines() {
        return lockerMachineDao.getActiveOnlineMachines();
    }

    public List<LockerMachine> getAll() {
        return lockerMachineDao.getAll();
    }

    public List<LockerMachine> getBySyncStatus(String syncStatus) {
        return lockerMachineDao.getBySyncStatus(syncStatus);
    }

    public int getActiveOnlineMachinesCount() {
        return lockerMachineDao.getActiveOnlineMachinesCount();
    }

    // LiveData operations
    public LiveData<List<LockerMachine>> getActiveMachinesLive() {
        return lockerMachineDao.getActiveMachinesLive();
    }

    public LiveData<List<LockerMachine>> getAllLive() {
        return lockerMachineDao.getAllLive();
    }

    public void shutdown() {
        executor.shutdown();
    }
}
