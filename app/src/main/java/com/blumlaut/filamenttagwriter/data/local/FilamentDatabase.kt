package com.blumlaut.filamenttagwriter.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [FilamentEntity::class, PrinterEntity::class], version = 5, exportSchema = false)
abstract class FilamentDatabase : RoomDatabase() {

    abstract fun filamentDao(): FilamentDao
    abstract fun printerDao(): PrinterDao

    companion object {
        // Retain installed filament profiles on upgrade from schema v3.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE filaments ADD COLUMN brand TEXT NOT NULL DEFAULT ''")
            }
        }
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE filaments ADD COLUMN bedMinTemp INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE filaments ADD COLUMN bedMaxTemp INTEGER NOT NULL DEFAULT 0")
            }
        }
        @Volatile
        private var INSTANCE: FilamentDatabase? = null

        fun getInstance(context: Context): FilamentDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    FilamentDatabase::class.java,
                    "filament_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
