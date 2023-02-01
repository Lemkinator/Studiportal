package de.lemke.studiportal.data.database

import androidx.room.TypeConverter
import java.time.ZonedDateTime

/** Type converters to map between SQLite types and entity types. */
object Converters {
    /** Returns the string representation of the [zonedDateTime]. */
    @TypeConverter
    fun zonedDateTimeToDb(zonedDateTime: ZonedDateTime?): String = zonedDateTime.toString()

    /** Returns the [ZonedDateTime] represented by the [zonedDateTimeString]. */
    @TypeConverter
    fun zonedDateTimeFromDb(zonedDateTimeString: String?): ZonedDateTime? = try {
        ZonedDateTime.parse(zonedDateTimeString)
    } catch (e: Exception) {
        null
    }
}
