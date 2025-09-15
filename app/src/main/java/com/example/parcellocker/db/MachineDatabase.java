package com.example.parcellocker.db;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

@Database(entities = {DoorEntity.class}, version = 1, exportSchema = false)
public abstract class MachineDatabase extends RoomDatabase {
    public abstract DoorDao doorDao();

    private static volatile MachineDatabase INSTANCE;

    public static MachineDatabase getInstance(Context ctx) {
        if (INSTANCE == null) {
            synchronized (MachineDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(ctx.getApplicationContext(),
                                    MachineDatabase.class, "machine-db")
                            .allowMainThreadQueries() // for simple testing only; remove in prod
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
