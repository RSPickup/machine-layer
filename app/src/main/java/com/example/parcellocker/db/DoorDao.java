package com.example.parcellocker.db;



import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface DoorDao {

    @Query("SELECT * FROM door WHERE machine_id = :machineId ORDER BY door_index")
    List<DoorEntity> getByMachine(String machineId);

    @Query("SELECT * FROM door")
    List<DoorEntity> getAll();

    @Query("SELECT * FROM door WHERE machine_id = :machineId AND door_index = :index LIMIT 1")
    DoorEntity getByMachineAndIndex(String machineId, int index);

    @Insert
    void insert(DoorEntity door);

    @Update
    void update(DoorEntity door);

    @Query("SELECT * FROM door WHERE machine_id = :machineId ORDER BY door_index")
    LiveData<List<DoorEntity>> getDoorsLive(String machineId);
}
