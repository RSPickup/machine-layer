package com.example.parcellocker.db;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import android.content.Context;

import com.example.parcellocker.db.dao.*;
import com.example.parcellocker.db.entities.*;
import com.example.parcellocker.db.converters.*;

/**
 * Main Room database class for the Parcel Locker System.
 *
 * This database uses UUID primary keys for all entities to support offline-first
 * architecture and seamless sync with cloud services. Type converters handle
 * UUID and JSON field conversions for SQLite compatibility.
 *
 * Entities:
 * - User: Minimal user info (id, name) for package tracking
 * - LockerMachine: Physical locker machines with status tracking
 * - DoorEntity: Individual doors/compartments in machines
 * - Package: Packages with 3-PIN authentication system
 * - Payment: Payment transactions (online and cash) for packages
 * - AuditLog: System audit trail with flexible JSON details
 * - MachineEvent: Real-time hardware events with JSON metadata
 */
@Database(
    entities = {
        User.class,
        LockerMachine.class,
        DoorEntity.class,
        com.example.parcellocker.db.entities.Package.class,
        Payment.class,
        AuditLog.class,
        MachineEvent.class
    },
    version = 1,
    exportSchema = false
)
@TypeConverters({UuidConverter.class, JsonConverter.class})
public abstract class MachineDatabase extends RoomDatabase {

    // DAO abstract methods
    public abstract UserDao userDao();
    public abstract LockerMachineDao lockerMachineDao();
    public abstract DoorDao doorDao();
    public abstract PackageDao packageDao();
    public abstract PaymentDao paymentDao();
    public abstract AuditLogDao auditLogDao();
    public abstract MachineEventDao machineEventDao();

    private static volatile MachineDatabase INSTANCE;

    public static MachineDatabase getInstance(Context ctx) {
        if (INSTANCE == null) {
            synchronized (MachineDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(ctx.getApplicationContext(),
                                    MachineDatabase.class, "parcel-locker-db")
                            .allowMainThreadQueries() // Remove in production
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Closes the database instance.
     * Call this when the application is shutting down.
     */
    public static void closeDatabase() {
        if (INSTANCE != null) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}
