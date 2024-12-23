package com.example.taskflow;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import org.mindrot.jbcrypt.BCrypt;

import java.util.ArrayList;

public class DataBase extends SQLiteOpenHelper {
    public static final String DBNAME = "login.db";

    public DataBase(@Nullable Context context) {
        super(context, "login.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE adminUser (email TEXT PRIMARY KEY, password TEXT, name TEXT, surname TEXT)");
        db.execSQL("CREATE TABLE tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT, task TEXT, FOREIGN KEY(email) REFERENCES adminUser(email))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS adminUser");
        db.execSQL("DROP TABLE IF EXISTS tasks");
        onCreate(db);
    }

    public String getUserNameByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name, surname FROM adminUser WHERE email=?", new String[]{email});
        if (cursor != null && cursor.moveToFirst()) {
            String userName = cursor.getString(0) + " " + cursor.getString(1);
            cursor.close();
            return userName;
        }
        if (cursor != null) cursor.close();
        return null;
    }

    public Boolean validateUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT password FROM adminUser WHERE email=?", new String[]{email});
            if (cursor != null && cursor.moveToFirst()) {
                String storedHashedPassword = cursor.getString(0);
                return BCrypt.checkpw(password, storedHashedPassword);
            }
            return false;
        } catch (SQLiteException e) {
            Log.e("DataBase", "Error validating user: " + e.getMessage());
            return false;
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
    }
    public Boolean insertAdminUser(String name, String surname, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues contentValues = new ContentValues();
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            contentValues.put("name", name);
            contentValues.put("surname", surname);
            contentValues.put("email", email);
            contentValues.put("password", hashedPassword);
            long result = db.insert("adminUser", null, contentValues);
            return result != -1;
        } catch (SQLiteException e) {
            Log.e("DataBase", "Error inserting admin user: " + e.getMessage());
            return false;
        } finally {
            db.close();
        }
    }

    public Boolean checkAdminEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM adminUser WHERE email=?", new String[]{email});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public boolean insertTask(String email, String task) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put("email", email);
            contentValues.put("task", task);
            long result = db.insert("tasks", null, contentValues);
            return result != -1;
        } catch (SQLiteException e) {
            Log.e("DataBase", "Error inserting task: " + e.getMessage());
            return false;
        } finally {
            db.close();
        }
    }

    public ArrayList<String> getTasks(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<String> tasks = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT task FROM tasks WHERE email=?", new String[]{email});
        while (cursor.moveToNext()) {
            tasks.add(cursor.getString(0));
        }
        cursor.close();
        return tasks;
    }

    public boolean updatePassword(String email, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues contentValues = new ContentValues();
            String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            contentValues.put("password", hashedPassword);
            int rowsAffected = db.update("adminUser", contentValues, "email=?", new String[]{email});
            return rowsAffected > 0;
        } catch (SQLiteException e) {
            Log.e("DataBase", "Error updating password: " + e.getMessage());
            return false;
        } finally {
            db.close();
        }
    }

    public boolean deleteTask(String email, String task) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            int rowsDeleted = db.delete("tasks", "email=? AND task=?", new String[]{email, task});
            return rowsDeleted > 0;
        } catch (SQLiteException e) {
            Log.e("DataBase", "Error deleting task: " + e.getMessage());
            return false;
        } finally {
            db.close();
        }
    }
}
