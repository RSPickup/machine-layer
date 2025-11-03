package com.example.parcellocker.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.example.parcellocker.db.entities.AuditLog;

import java.util.List;
import java.util.UUID;

@Dao
public interface AuditLogDao {

    @Query("SELECT * FROM audit_logs WHERE id = :id LIMIT 1")
    AuditLog getById(UUID id);

    @Query("SELECT * FROM audit_logs WHERE entity_type = :entityType AND entity_id = :entityId ORDER BY timestamp DESC")
    List<AuditLog> getByEntity(String entityType, UUID entityId);

    @Query("SELECT * FROM audit_logs WHERE user_id = :userId ORDER BY timestamp DESC")
    List<AuditLog> getByUserId(UUID userId);

    @Query("SELECT * FROM audit_logs WHERE action = :action ORDER BY timestamp DESC")
    List<AuditLog> getByAction(String action);

    @Query("SELECT * FROM audit_logs WHERE entity_type = :entityType ORDER BY timestamp DESC")
    List<AuditLog> getByEntityType(String entityType);

    @Query("SELECT * FROM audit_logs WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    List<AuditLog> getByTimeRange(Long startTime, Long endTime);

    @Query("SELECT * FROM audit_logs WHERE source = :source ORDER BY timestamp DESC")
    List<AuditLog> getBySource(String source);

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    List<AuditLog> getRecent(int limit);

    @Query("SELECT * FROM audit_logs")
    List<AuditLog> getAll();

    @Query("SELECT * FROM audit_logs WHERE sync_status = :syncStatus")
    List<AuditLog> getBySyncStatus(String syncStatus);

    @Insert
    void insert(AuditLog auditLog);

    @Insert
    void insertAll(List<AuditLog> auditLogs);

    @Update
    void update(AuditLog auditLog);

    @Delete
    void delete(AuditLog auditLog);

    @Query("DELETE FROM audit_logs WHERE id = :id")
    void deleteById(UUID id);

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<AuditLog>> getRecentLive(int limit);

    @Query("SELECT * FROM audit_logs WHERE entity_type = :entityType AND entity_id = :entityId ORDER BY timestamp DESC")
    LiveData<List<AuditLog>> getByEntityLive(String entityType, UUID entityId);

    @Query("UPDATE audit_logs SET sync_status = :syncStatus WHERE id = :id")
    void updateSyncStatus(UUID id, String syncStatus);

    @Query("DELETE FROM audit_logs WHERE timestamp < :cutoffTime")
    void deleteOlderThan(Long cutoffTime);

    @Query("SELECT COUNT(*) FROM audit_logs WHERE sync_status = 'local_only'")
    int getUnsyncedCount();
}
