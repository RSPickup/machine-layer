package com.example.parcellocker.db.dao;



import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.parcellocker.db.entities.DoorEntity;

import java.util.List;
import java.util.UUID;

@Dao
public interface DoorDao {

    @Query("SELECT * FROM doors WHERE locker_machine_id = :lockerMachineId ORDER BY door_index")
    List<DoorEntity> getByMachine(UUID lockerMachineId);

    @Query("SELECT * FROM doors")
    List<DoorEntity> getAll();

    @Query("SELECT * FROM doors WHERE locker_machine_id = :lockerMachineId AND door_index = :index LIMIT 1")
    DoorEntity getByMachineAndIndex(UUID lockerMachineId, int index);

    @Query("SELECT * FROM doors WHERE id = :id LIMIT 1")
    DoorEntity getById(UUID id);

    @Query("SELECT * FROM doors WHERE is_occupied = 0 AND locker_machine_id = :lockerMachineId ORDER BY door_index")
    List<DoorEntity> getAvailableDoors(UUID lockerMachineId);

    @Query("SELECT * FROM doors WHERE is_occupied = 1 AND locker_machine_id = :lockerMachineId ORDER BY door_index")
    List<DoorEntity> getOccupiedDoors(UUID lockerMachineId);

    @Query("SELECT * FROM doors WHERE sync_status = :syncStatus")
    List<DoorEntity> getBySyncStatus(String syncStatus);

    @Insert
    void insert(DoorEntity door);

    @Insert
    void insertAll(List<DoorEntity> doors);

    @Update
    void update(DoorEntity door);

    @Delete
    void delete(DoorEntity door);

    @Query("DELETE FROM doors WHERE id = :id")
    void deleteById(UUID id);

    @Query("SELECT * FROM doors WHERE locker_machine_id = :lockerMachineId ORDER BY door_index")
    LiveData<List<DoorEntity>> getDoorsLive(UUID lockerMachineId);

    @Query("SELECT * FROM doors")
    LiveData<List<DoorEntity>> getAllLive();

    @Query("SELECT COUNT(*) FROM doors WHERE locker_machine_id = :lockerMachineId AND is_occupied = 0")
    int getAvailableDoorsCount(UUID lockerMachineId);

    @Query("UPDATE doors SET sync_status = :syncStatus WHERE id = :id")
    void updateSyncStatus(UUID id, String syncStatus);
}
