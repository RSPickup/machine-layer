package com.example.parcellocker.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.example.parcellocker.db.entities.User;

import java.util.List;
import java.util.UUID;

@Dao
public interface UserDao {

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    User getById(UUID id);

    @Query("SELECT * FROM users")
    List<User> getAll();

    @Query("SELECT * FROM users WHERE sync_status = :syncStatus")
    List<User> getBySyncStatus(String syncStatus);

    @Insert
    void insert(User user);

    @Insert
    void insertAll(List<User> users);

    @Update
    void update(User user);

    @Delete
    void delete(User user);

    @Query("DELETE FROM users WHERE id = :id")
    void deleteById(UUID id);

    @Query("SELECT * FROM users")
    LiveData<List<User>> getAllLive();

    @Query("UPDATE users SET sync_status = :syncStatus WHERE id = :id")
    void updateSyncStatus(UUID id, String syncStatus);

    @Query("SELECT COUNT(*) FROM users")
    int getUsersCount();
}
