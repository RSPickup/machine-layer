package com.example.parcellocker.db.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.example.parcellocker.db.MachineDatabase;
import com.example.parcellocker.db.dao.AuditLogDao;
import com.example.parcellocker.db.entities.AuditLog;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for AuditLog operations.
 * Manages system audit trails and compliance logging.
 */
public class AuditLogRepository {

    private AuditLogDao auditLogDao;
    private ExecutorService executor;

    public AuditLogRepository(Application application) {
        MachineDatabase database = MachineDatabase.getInstance(application);
        auditLogDao = database.auditLogDao();
        executor = Executors.newFixedThreadPool(4);
    }

    // Insert operations
    public void insert(AuditLog auditLog) {
        executor.execute(() -> auditLogDao.insert(auditLog));
    }

    public void insertAll(List<AuditLog> auditLogs) {
        executor.execute(() -> auditLogDao.insertAll(auditLogs));
    }

    // Update operations
    public void update(AuditLog auditLog) {
        executor.execute(() -> auditLogDao.update(auditLog));
    }

    public void updateSyncStatus(UUID id, String syncStatus) {
        executor.execute(() -> auditLogDao.updateSyncStatus(id, syncStatus));
    }

    // Delete operations
    public void delete(AuditLog auditLog) {
        executor.execute(() -> auditLogDao.delete(auditLog));
    }

    public void deleteById(UUID id) {
        executor.execute(() -> auditLogDao.deleteById(id));
    }

    public void deleteOlderThan(Long cutoffTime) {
        executor.execute(() -> auditLogDao.deleteOlderThan(cutoffTime));
    }

    // Query operations
    public AuditLog getById(UUID id) {
        return auditLogDao.getById(id);
    }

    public List<AuditLog> getByEntity(String entityType, UUID entityId) {
        return auditLogDao.getByEntity(entityType, entityId);
    }

    public List<AuditLog> getByUserId(UUID userId) {
        return auditLogDao.getByUserId(userId);
    }

    public List<AuditLog> getByAction(String action) {
        return auditLogDao.getByAction(action);
    }

    public List<AuditLog> getByEntityType(String entityType) {
        return auditLogDao.getByEntityType(entityType);
    }

    public List<AuditLog> getByTimeRange(Long startTime, Long endTime) {
        return auditLogDao.getByTimeRange(startTime, endTime);
    }

    public List<AuditLog> getBySource(String source) {
        return auditLogDao.getBySource(source);
    }

    public List<AuditLog> getRecent(int limit) {
        return auditLogDao.getRecent(limit);
    }

    public List<AuditLog> getAll() {
        return auditLogDao.getAll();
    }

    public List<AuditLog> getBySyncStatus(String syncStatus) {
        return auditLogDao.getBySyncStatus(syncStatus);
    }

    public int getUnsyncedCount() {
        return auditLogDao.getUnsyncedCount();
    }

    // LiveData operations
    public LiveData<List<AuditLog>> getRecentLive(int limit) {
        return auditLogDao.getRecentLive(limit);
    }

    public LiveData<List<AuditLog>> getByEntityLive(String entityType, UUID entityId) {
        return auditLogDao.getByEntityLive(entityType, entityId);
    }

    public void shutdown() {
        executor.shutdown();
    }
}
