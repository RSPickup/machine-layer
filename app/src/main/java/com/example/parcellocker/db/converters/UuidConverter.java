package com.example.parcellocker.db.converters;

import androidx.room.TypeConverter;
import java.util.UUID;

/**
 * Type converter for UUID fields in Room database.
 * Converts UUID objects to String for SQLite storage and back to UUID when reading.
 *
 * SQLite doesn't have a native UUID type, so we store UUIDs as TEXT (strings).
 * This converter handles the conversion automatically for all UUID fields in entities.
 */
public class UuidConverter {

    /**
     * Converts UUID to String for database storage
     * @param uuid The UUID object to convert
     * @return String representation of the UUID, or null if input is null
     */
    @TypeConverter
    public String fromUUID(UUID uuid) {
        return uuid == null ? null : uuid.toString();
    }

    /**
     * Converts String back to UUID when reading from database
     * @param value String representation of UUID from database
     * @return UUID object, or null if input is null
     */
    @TypeConverter
    public UUID toUUID(String value) {
        return value == null ? null : UUID.fromString(value);
    }
}
