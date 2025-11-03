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
 * Simplified lifecycle: pending → delivered (PIN used) → picked (collected) OR returned (to office)
 * Payment is handled by separate PaymentRepository.
 */
public class PackageRepository {

    private PackageDao packageDao;
    private ExecutorService executor;

    public PackageRepository(Application application) {
        MachineDatabase database = MachineDatabase.getInstance(application);
        packageDao = database.packageDao();
        executor = Executors.newFixedThreadPool(4);
    }

    // Insert operations
    public void insert(Package packageEntity) {
        executor.execute(() -> packageDao.insert(packageEntity));
    }

    public void insertAll(List<Package> packages) {
        executor.execute(() -> packageDao.insertAll(packages));
    }

    // Update operations
    public void update(Package packageEntity) {
        executor.execute(() -> packageDao.update(packageEntity));
    }

    public void updateStatus(UUID id, String status) {
        executor.execute(() -> packageDao.updateStatus(id, status, System.currentTimeMillis()));
    }

    public void updateSyncStatus(UUID id, String syncStatus) {
        executor.execute(() -> packageDao.updateSyncStatus(id, syncStatus));
    }

    // CORRECTED: PIN-based authentication operations with package reference validation
    public Package authenticateDeliveryPin(String packageReference, String deliveryPin) {
        Package pkg = packageDao.getByTrackingNumber(packageReference);
        if (pkg != null && pkg.authenticateDelivery(packageReference, deliveryPin)) {
            return pkg;
        }
        return null;
    }

    public Package authenticateClientPin(String packageReference, String clientPin) {
        Package pkg = packageDao.getByTrackingNumber(packageReference);
        if (pkg != null && pkg.authenticateClient(packageReference, clientPin)) {
            return pkg;
        }
        return null;
    }

    public Package authenticateReturnPin(String packageReference, String returnPin) {
        Package pkg = packageDao.getByTrackingNumber(packageReference);
        if (pkg != null && pkg.authenticateReturn(packageReference, returnPin)) {
            return pkg;
        }
        return null;
    }

    // CORRECTED: Delivery workflow - door must be pre-assigned by backoffice
    public boolean canDeliverPackage(UUID packageId) {
        Package pkg = getById(packageId);
        return pkg != null && pkg.isReadyForDelivery();
    }

    // Package lifecycle operations - with door validation
    public void markAsDelivered(UUID packageId, UUID deliveryPersonId) {
        executor.execute(() -> {
            Package pkg = packageDao.getById(packageId);
            if (pkg != null && pkg.hasDoorAssigned()) {
                long now = System.currentTimeMillis();
                long expiry = now + (72 * 60 * 60 * 1000); // 72 hours
                packageDao.markAsDelivered(packageId, pkg.getDoorId(), now, expiry, deliveryPersonId, now);
            }
        });
    }

    public void markAsPicked(UUID packageId) {
        executor.execute(() -> {
            long now = System.currentTimeMillis();
            packageDao.markAsPicked(packageId, now, now);
        });
    }

    public void markAsReturned(UUID packageId, UUID returnedBy) {
        executor.execute(() -> {
            long now = System.currentTimeMillis();
            packageDao.markAsReturned(packageId, now, returnedBy, now);
        });
    }

    // Delete operations
    public void delete(Package packageEntity) {
        executor.execute(() -> packageDao.delete(packageEntity));
    }

    public void deleteById(UUID id) {
        executor.execute(() -> packageDao.deleteById(id));
    }

    // Query operations (synchronous)
    public Package getById(UUID id) {
        return packageDao.getById(id);
    }

    public Package getByTrackingNumber(String trackingNumber) {
        return packageDao.getByTrackingNumber(trackingNumber);
    }

    public List<Package> getByDeliveryPerson(UUID userId) {
        return packageDao.getByDeliveryPerson(userId);
    }

    public List<Package> getByReturnPerson(UUID userId) {
        return packageDao.getByReturnPerson(userId);
    }

    public Package getByDoorId(UUID doorId) {
        return packageDao.getByDoorId(doorId);
    }

    public List<Package> getByStatus(String status) {
        return packageDao.getByStatus(status);
    }

    public List<Package> getAll() {
        return packageDao.getAll();
    }

    public List<Package> getBySyncStatus(String syncStatus) {
        return packageDao.getBySyncStatus(syncStatus);
    }

    public List<Package> getPackagesInLockers() {
        return packageDao.getPackagesInLockers();
    }

    public List<Package> getPendingPackages() {
        return packageDao.getPendingPackages();
    }

    public List<Package> getExpiredPackages() {
        return packageDao.getExpiredPackages(System.currentTimeMillis());
    }

    public List<Package> getActiveDeliveredPackages() {
        return packageDao.getActiveDeliveredPackages(System.currentTimeMillis());
    }

    public int getCountByStatus(String status) {
        return packageDao.getCountByStatus(status);
    }

    public int getExpiredPackagesCount() {
        return packageDao.getExpiredPackagesCount(System.currentTimeMillis());
    }

    // LiveData operations (for UI)
    public LiveData<List<Package>> getByStatusLive(String status) {
        return packageDao.getByStatusLive(status);
    }

    public LiveData<List<Package>> getPackagesInLockersLive() {
        return packageDao.getPackagesInLockersLive();
    }

    public LiveData<List<Package>> getAllLive() {
        return packageDao.getAllLive();
    }

    public void shutdown() {
        executor.shutdown();
    }
}
