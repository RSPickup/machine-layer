package com.example.parcellocker.db.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.example.parcellocker.db.MachineDatabase;
import com.example.parcellocker.db.dao.UserDao;
import com.example.parcellocker.db.entities.User;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for User operations.
 * Minimal user management for machine operation only.
 * Users are just for tracking who delivered packages.
 */
public class UserRepository {

    private UserDao userDao;
    private ExecutorService executor;

    public UserRepository(Application application) {
        MachineDatabase database = MachineDatabase.getInstance(application);
        userDao = database.userDao();
        executor = Executors.newFixedThreadPool(4);
    }

    // Insert operations
    public void insert(User user) {
        executor.execute(() -> userDao.insert(user));
    }

    public void insertAll(List<User> users) {
        executor.execute(() -> userDao.insertAll(users));
    }

    // Update operations
    public void update(User user) {
        executor.execute(() -> userDao.update(user));
    }

    public void updateSyncStatus(UUID id, String syncStatus) {
        executor.execute(() -> userDao.updateSyncStatus(id, syncStatus));
    }

    // Delete operations
    public void delete(User user) {
        executor.execute(() -> userDao.delete(user));
    }

    public void deleteById(UUID id) {
        executor.execute(() -> userDao.deleteById(id));
    }

    // Query operations
    public User getById(UUID id) {
        return userDao.getById(id);
    }

    public List<User> getAll() {
        return userDao.getAll();
    }

    public List<User> getBySyncStatus(String syncStatus) {
        return userDao.getBySyncStatus(syncStatus);
    }

    public int getUsersCount() {
        return userDao.getUsersCount();
    }

    // LiveData operations
    public LiveData<List<User>> getAllLive() {
        return userDao.getAllLive();
    }

    public void shutdown() {
        executor.shutdown();
    }
}
