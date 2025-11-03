package com.example.parcellocker.db.converters;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Type converter for JSON fields in Room database.
 * Converts Map<String, Object> to JSON string for SQLite storage and back to Map when reading.
 *
 * SQLite stores JSON as TEXT, so this converter uses Gson to serialize/deserialize
 * Map objects to/from JSON strings. Used for flexible data fields like audit details.
 */
public class JsonConverter {

    private static final Gson gson = new Gson();
    private static final Type mapType = new TypeToken<Map<String, Object>>(){}.getType();

    /**
     * Converts Map to JSON string for database storage
     * @param json The Map object to convert
     * @return JSON string representation, or null if input is null
     */
    @TypeConverter
    public String fromJson(Map<String, Object> json) {
        return json == null ? null : gson.toJson(json);
    }

    /**
     * Converts JSON string back to Map when reading from database
     * @param value JSON string from database
     * @return Map<String, Object>, or null if input is null
     */
    @TypeConverter
    public Map<String, Object> toJson(String value) {
        return value == null ? null : gson.fromJson(value, mapType);
    }
}
