package com.example.parcellocker.db.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.example.parcellocker.db.MachineDatabase;
import com.example.parcellocker.db.dao.PackageDao;
import com.example.parcellocker.db.entities.Package;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for Package operations.
 * Manages package deliveries and pickup codes.
 */
public class DeliveryRepository {

    private PackageDao packageDao;
    private ExecutorService executor;

    public DeliveryRepository(Application application) {
        MachineDatabase database = MachineDatabase.getInstance(application);
        packageDao = database.packageDao();
        executor = Executors.newFixedThreadPool(4);
    }

    // Insert operations
    public void insert(Package aPackage) {
        executor.execute(() -> packageDao.insert(aPackage));
    }

    public void insertAll(List<Package> packages) {
        executor.execute(() -> packageDao.insertAll(packages));
    }

    // Update operations
    public void update(Package aPackage) {
        executor.execute(() -> packageDao.update(aPackage));
    }

    public void updateSyncStatus(UUID id, String syncStatus) {
        executor.execute(() -> packageDao.updateSyncStatus(id, syncStatus));
    }

    public void markAsCollected(UUID id) {
        executor.execute(() -> packageDao.markAsCollected(id, true, System.currentTimeMillis(), "collected"));
    }

    // Delete operations
    public void delete(Package aPackage) {
        executor.execute(() -> packageDao.delete(aPackage));
    }

    public void deleteById(UUID id) {
        executor.execute(() -> packageDao.deleteById(id));
    }

    // Query operations
    public Package getById(UUID id) {
        return packageDao.getById(id);
    }

    public Package getByPackageId(UUID packageId) {
        return packageDao.getByPackageId(packageId);
    }

    public Package getByPickupCode(String pickupCode) {
        return packageDao.getByPickupCode(pickupCode);
    }

    public List<Package> getByDeliveryPerson(UUID userId) {
        return packageDao.getByDeliveryPerson(userId);
    }

    public List<Package> getByStatus(String status) {
        return packageDao.getByStatus(status);
    }

    public List<Package> getUnCollectedDeliveries() {
        return packageDao.getUnCollectedDeliveries();
    }

    public List<Package> getCollectedDeliveries() {
        return packageDao.getCollectedDeliveries();
    }

    public List<Package> getExpiredDeliveries() {
        return packageDao.getExpiredDeliveries(System.currentTimeMillis());
    }

    public List<Package> getAll() {
        return packageDao.getAll();
    }

    public List<Package> getBySyncStatus(String syncStatus) {
        return packageDao.getBySyncStatus(syncStatus);
    }

    public int getUnCollectedCount() {
        return packageDao.getUnCollectedCount();
    }

    // LiveData operations
    public LiveData<List<Package>> getByStatusLive(String status) {
        return packageDao.getByStatusLive(status);
    }

    public LiveData<List<Package>> getUnCollectedDeliveriesLive() {
        return packageDao.getUnCollectedDeliveriesLive();
    }

    public LiveData<List<Package>> getAllLive() {
        return packageDao.getAllLive();
    }

    public void shutdown() {
        executor.shutdown();
    }
}
