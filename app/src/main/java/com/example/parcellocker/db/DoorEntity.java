package com.example.parcellocker.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "door")
public class DoorEntity {
    @PrimaryKey(autoGenerate = true) public int id;
    @ColumnInfo(name = "machine_id") public String machineId;
    @ColumnInfo(name = "cu_id") public int cuId;
    @ColumnInfo(name = "door_index") public int doorIndex; // 0-based
    @ColumnInfo(name = "label") public String label;
    @ColumnInfo(name = "locked") public int locked; // 1 locked, 0 unlocked
    @ColumnInfo(name = "occupied") public int occupied; // 1 present, 0 empty
}