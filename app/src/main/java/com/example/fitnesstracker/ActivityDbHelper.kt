package com.example.fitnesstracker

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ActivityDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION){

    companion object {
        // Database name
        val DATABASE_NAME = "loggedActivities.db"

        // Version
        const val DATABASE_VERSION = 1

        // Table
        val TABLE_NAME = "loggedActivity_table"

        // Columns
        val _ID = "id"
        val DISTANCE = "distance"
        val DATE = "date"
        val TOTALTIME = "totalTime"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // Creating the table
        val SQL_CREATE_TABLE =
            "CREATE TABLE ${TABLE_NAME} (" +
                    "${_ID} INTEGER PRIMARY KEY," +
                    "${DISTANCE} TEXT," +
                    "${DATE} TEXT," +
                    "${TOTALTIME} TEXT)"


        db?.execSQL(SQL_CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        val SQL_DELETE_TABLE = "DROP TABLE IF EXISTS ${TABLE_NAME}"
        db?.execSQL(SQL_DELETE_TABLE)
        onCreate(db)
    }

    fun insertData(distance: String, date: String, time: String) {
        val db = this.writableDatabase

        // Add the data
        val contentValues = ContentValues()
        contentValues.put(DISTANCE, distance)
        contentValues.put(DATE, date)
        contentValues.put(TOTALTIME, time)

        // Insert the new row
        db.insert(TABLE_NAME, null, contentValues)
    }

    fun updateData(id: String, distance: String, date: String, time: String): Boolean {
        val db = this.writableDatabase

        // New values
        val contentValues = ContentValues()
        contentValues.put(_ID, id)
        contentValues.put(DISTANCE, distance)
        contentValues.put(DATE, date)
        contentValues.put(TOTALTIME, time)

        // Update the selected row
        db.update(TABLE_NAME, contentValues, "ID = ?", arrayOf(id))
        return true
    }

    fun deleteData(id : String) : Int {
        val db = this.writableDatabase

        // Delete data at the row ID
        return db.delete(TABLE_NAME,"ID = ?", arrayOf(id))
    }

    fun deleteAllData() {
        // Get a reference to the writable database
        val db = this.writableDatabase

        // Delete all data from the table
        db.delete(TABLE_NAME, null, null)

        // Close the database connection
        db.close()
    }

    val viewAllData : Cursor
        get() {
            val db = this.writableDatabase
            // Return data from each row
            val cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null)
            return cursor
        }
}