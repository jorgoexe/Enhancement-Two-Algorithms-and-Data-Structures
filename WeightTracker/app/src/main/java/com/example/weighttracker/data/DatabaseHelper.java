package com.example.weighttracker.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.weighttracker.model.WeightEntry;

import org.mindrot.jbcrypt.BCrypt;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "WeightTracker.db";
    private static final int DATABASE_VERSION = 3;

    // Table and column names
    private static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD = "password";
    public static final String COLUMN_CREATED_AT = "created_at";

    private static final String TABLE_WEIGHTS = "weights";
    public static final String COLUMN_WEIGHT_ID = "weight_id";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_WEIGHT = "weight";
    public static final String COLUMN_GOAL = "goal";
    public static final String COLUMN_USER_FK = "user_id"; // FK from weights → users
    public static final String COLUMN_WEIGHT_CREATED_AT = "created_at";

    private static final String INDEX_USER_WEIGHTS = "idx_user_weights_date";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        enableForeignKeys(db);
        db.beginTransaction();
        try {
            db.execSQL("CREATE TABLE " + TABLE_USERS + "(" +
                    COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_USERNAME + " TEXT UNIQUE NOT NULL CHECK(length(" + COLUMN_USERNAME + ") >= 4)," +
                    COLUMN_PASSWORD + " TEXT NOT NULL CHECK(length(" + COLUMN_PASSWORD + ") >= 8)," +
                    COLUMN_CREATED_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            db.execSQL("CREATE TABLE " + TABLE_WEIGHTS + "(" +
                    COLUMN_WEIGHT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_DATE + " TEXT NOT NULL CHECK(" + COLUMN_DATE + " <= date('now'))," +
                    COLUMN_WEIGHT + " REAL NOT NULL CHECK(" + COLUMN_WEIGHT + " >= 20 AND " + COLUMN_WEIGHT + " <= 300)," +
                    COLUMN_GOAL + " REAL CHECK(" + COLUMN_GOAL + " IS NULL OR (" + COLUMN_GOAL + " >= 20 AND " + COLUMN_GOAL + " <= 300))," +
                    COLUMN_USER_FK + " INTEGER NOT NULL," +
                    COLUMN_WEIGHT_CREATED_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY(" + COLUMN_USER_FK + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + ") ON DELETE CASCADE)");

            db.execSQL("CREATE INDEX " + INDEX_USER_WEIGHTS + " ON " + TABLE_WEIGHTS +
                    "(" + COLUMN_USER_FK + ", " + COLUMN_DATE + " DESC)");

            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TAG, "Error creating tables: " + e.getMessage());
            throw e;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        enableForeignKeys(db);
        db.beginTransaction();
        try {
            if (oldVersion < 2) {
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEIGHTS);
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
                onCreate(db);
            }
            if (oldVersion < 3) {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_CREATED_AT +
                        " TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
                db.execSQL("ALTER TABLE " + TABLE_WEIGHTS + " ADD COLUMN " + COLUMN_WEIGHT_CREATED_AT +
                        " TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
                db.execSQL("CREATE INDEX IF NOT EXISTS " + INDEX_USER_WEIGHTS + " ON " + TABLE_WEIGHTS +
                        "(" + COLUMN_USER_FK + ", " + COLUMN_DATE + " DESC)");
            }
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TAG, "Error upgrading database: " + e.getMessage());
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        enableForeignKeys(db);
    }

    public void enableForeignKeys(SQLiteDatabase db) {
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys = ON;");
        }
    }

    public boolean addUser(String username, String password) {
        if (!isPasswordValid(password) || !isUsernameValid(username)) return false;

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, BCrypt.hashpw(password, BCrypt.gensalt(12)));

        try {
            return db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_ABORT) != -1;
        } catch (SQLException e) {
            Log.e(TAG, "Error adding user: " + e.getMessage());
            return false;
        }
    }

    public boolean validateUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_USERS,
                new String[]{COLUMN_PASSWORD},
                COLUMN_USERNAME + " = ?",
                new String[]{username},
                null, null, null)) {

            if (cursor.moveToFirst()) {
                String storedHash = cursor.getString(0);
                return BCrypt.checkpw(password, storedHash);
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error validating user: " + e.getMessage());
            return false;
        }
    }

    public long getUserId(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_USERS,
                new String[]{COLUMN_USER_ID},
                COLUMN_USERNAME + " = ?",
                new String[]{username},
                null, null, null)) {
            return cursor.moveToFirst() ? cursor.getLong(0) : -1;
        } catch (Exception e) {
            Log.e(TAG, "Error getting user ID: " + e.getMessage());
            return -1;
        }
    }

    public boolean addWeight(long userId, String date, double weight, Double goal) {
        if (!isValidWeight(weight) || !isValidDate(date) || (goal != null && !isValidWeight(goal))) {
            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_FK, userId);
        values.put(COLUMN_DATE, date);
        values.put(COLUMN_WEIGHT, weight);
        if (goal != null) values.put(COLUMN_GOAL, goal);

        try {
            return db.insertWithOnConflict(TABLE_WEIGHTS, null, values, SQLiteDatabase.CONFLICT_ABORT) != -1;
        } catch (SQLException e) {
            Log.e(TAG, "Error adding weight: " + e.getMessage());
            return false;
        }
    }

    public Cursor getWeightsByUser(long userId) {
        return this.getReadableDatabase().query(
                TABLE_WEIGHTS,
                new String[]{COLUMN_WEIGHT_ID, COLUMN_DATE, COLUMN_WEIGHT, COLUMN_GOAL},
                COLUMN_USER_FK + " = ?",
                new String[]{String.valueOf(userId)},
                null, null,
                COLUMN_DATE + " DESC");
    }

    public boolean deleteWeight(long weightId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            return db.delete(TABLE_WEIGHTS, COLUMN_WEIGHT_ID + " = ?", new String[]{String.valueOf(weightId)}) > 0;
        } catch (SQLException e) {
            Log.e(TAG, "Error deleting weight: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteUserAndWeights(long userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        enableForeignKeys(db);
        db.beginTransaction();
        try {
            int deleted = db.delete(TABLE_USERS, COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)});
            db.setTransactionSuccessful();
            return deleted > 0;
        } catch (SQLException e) {
            Log.e(TAG, "Error deleting user: " + e.getMessage());
            return false;
        } finally {
            db.endTransaction();
        }
    }

    public static boolean isPasswordValid(String password) {
        return password != null && password.length() >= 8 &&
                password.matches(".*\\d.*") && password.matches(".*[!@#$%^&*].*");
    }

    private boolean isUsernameValid(String username) {
        return username != null && username.length() >= 4;
    }

    private boolean isValidWeight(double weight) {
        return weight >= 20 && weight <= 300;
    }

    private boolean isValidDate(String date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date inputDate = sdf.parse(date);
            return inputDate != null && !inputDate.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public boolean addWeightEntries(List<WeightEntry> entries) {
        SQLiteDatabase db = this.getWritableDatabase();
        enableForeignKeys(db);
        db.beginTransaction();
        try {
            for (WeightEntry entry : entries) {
                if (!isValidWeight(entry.getWeight()) || !isValidDate(entry.getDate())) {
                    throw new SQLException("Invalid weight or date");
                }

                ContentValues values = new ContentValues();
                values.put(COLUMN_USER_FK, entry.getUserId());
                values.put(COLUMN_DATE, entry.getDate());
                values.put(COLUMN_WEIGHT, entry.getWeight());
                if (entry.getGoal() != null) values.put(COLUMN_GOAL, entry.getGoal());

                if (db.insertWithOnConflict(TABLE_WEIGHTS, null, values, SQLiteDatabase.CONFLICT_ABORT) == -1) {
                    throw new SQLException("Insert failed");
                }
            }
            db.setTransactionSuccessful();
            return true;
        } catch (SQLException e) {
            Log.e(TAG, "Batch insert failed: " + e.getMessage());
            return false;
        } finally {
            db.endTransaction();
        }
    }
}
