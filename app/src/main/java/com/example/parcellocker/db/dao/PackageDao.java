package com.example.parcellocker.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.example.parcellocker.db.entities.Package;

import java.util.List;
import java.util.UUID;

@Dao
public interface PackageDao {

    @Query("SELECT * FROM packages WHERE id = :id LIMIT 1")
    Package getById(UUID id);

    @Query("SELECT * FROM packages WHERE tracking_number = :trackingNumber LIMIT 1")
    Package getByTrackingNumber(String trackingNumber);

    // PIN-based authentication queries
    @Query("SELECT * FROM packages WHERE delivery_pin = :deliveryPin AND status = 'pending' LIMIT 1")
    Package getByDeliveryPin(String deliveryPin);

    @Query("SELECT * FROM packages WHERE client_pin = :clientPin AND status = 'delivered' LIMIT 1")
    Package getByClientPin(String clientPin);

    @Query("SELECT * FROM packages WHERE return_pin = :returnPin AND (status = 'delivered' OR status IS NULL) LIMIT 1")
    Package getByReturnPin(String returnPin);

    @Query("SELECT * FROM packages WHERE delivered_by = :userId")
    List<Package> getByDeliveryPerson(UUID userId);

    @Query("SELECT * FROM packages WHERE returned_by = :userId")
    List<Package> getByReturnPerson(UUID userId);

    @Query("SELECT * FROM packages WHERE door_id = :doorId LIMIT 1")
    Package getByDoorId(UUID doorId);

    @Query("SELECT * FROM packages WHERE status = :status")
    List<Package> getByStatus(String status);

    @Query("SELECT * FROM packages")
    List<Package> getAll();

    @Query("SELECT * FROM packages WHERE sync_status = :syncStatus")
    List<Package> getBySyncStatus(String syncStatus);

    @Query("SELECT * FROM packages WHERE door_id IS NOT NULL")
    List<Package> getPackagesInLockers();

    @Query("SELECT * FROM packages WHERE status = 'pending'")
    List<Package> getPendingPackages();

    @Query("SELECT * FROM packages WHERE status = 'delivered' AND expiry_timestamp < :currentTime")
    List<Package> getExpiredPackages(Long currentTime);

    @Query("SELECT * FROM packages WHERE status = 'delivered' AND expiry_timestamp > :currentTime")
    List<Package> getActiveDeliveredPackages(Long currentTime);

    @Insert
    void insert(Package packageEntity);

    @Insert
    void insertAll(List<Package> packages);

    @Update
    void update(Package packageEntity);

    @Delete
    void delete(Package packageEntity);

    @Query("DELETE FROM packages WHERE id = :id")
    void deleteById(UUID id);

    // Package lifecycle updates - simplified (no payment logic)
    @Query("UPDATE packages SET door_id = :doorId, delivery_timestamp = :deliveryTimestamp, expiry_timestamp = :expiryTimestamp, status = 'delivered', delivered_by = :deliveredBy, updated_at = :updatedAt WHERE id = :id")
    void markAsDelivered(UUID id, UUID doorId, Long deliveryTimestamp, Long expiryTimestamp, UUID deliveredBy, Long updatedAt);

    @Query("UPDATE packages SET picked_timestamp = :pickedTimestamp, status = 'picked', updated_at = :updatedAt WHERE id = :id")
    void markAsPicked(UUID id, Long pickedTimestamp, Long updatedAt);

    @Query("UPDATE packages SET return_timestamp = :returnTimestamp, status = 'returned', returned_by = :returnedBy, door_id = NULL, updated_at = :updatedAt WHERE id = :id")
    void markAsReturned(UUID id, Long returnTimestamp, UUID returnedBy, Long updatedAt);

    // LiveData queries
    @Query("SELECT * FROM packages WHERE status = :status")
    LiveData<List<Package>> getByStatusLive(String status);

    @Query("SELECT * FROM packages WHERE door_id IS NOT NULL")
    LiveData<List<Package>> getPackagesInLockersLive();

    @Query("SELECT * FROM packages")
    LiveData<List<Package>> getAllLive();

    @Query("UPDATE packages SET sync_status = :syncStatus WHERE id = :id")
    void updateSyncStatus(UUID id, String syncStatus);

    @Query("UPDATE packages SET status = :status, updated_at = :updatedAt WHERE id = :id")
    void updateStatus(UUID id, String status, Long updatedAt);

    @Query("SELECT COUNT(*) FROM packages WHERE status = :status")
    int getCountByStatus(String status);

    @Query("SELECT COUNT(*) FROM packages WHERE status = 'delivered' AND expiry_timestamp < :currentTime")
    int getExpiredPackagesCount(Long currentTime);
}
