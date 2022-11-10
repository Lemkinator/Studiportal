package de.lemke.studiportal.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    version = 1,
    entities = [
        ExamDb::class,
    ],
    exportSchema = true,
    autoMigrations = [],

)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun examDao(): ExamDao
}
