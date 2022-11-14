package de.lemke.studiportal.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import de.lemke.studiportal.R
import de.lemke.studiportal.data.database.Converters.zonedDateTimeFromDb
import de.lemke.studiportal.data.database.Converters.zonedDateTimeToDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.ZonedDateTime
import javax.inject.Inject

/** Provides CRUD operations for user settings. */
class UserSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    /** Returns the current user settings. */
    suspend fun getSettings(): UserSettings = dataStore.data.map(::settingsFromPreferences).first()

    /**
     * Updates the current user settings and returns the new settings.
     * @param f Invoked with the current settings; The settings returned from this function will replace the current ones.
     */
    suspend fun updateSettings(f: (UserSettings) -> UserSettings): UserSettings {
        val prefs = dataStore.edit {
            val newSettings = f(settingsFromPreferences(it))
            it[KEY_LAST_VERSION_CODE] = newSettings.lastVersionCode
            it[KEY_LAST_VERSION_NAME] = newSettings.lastVersionName
            it[KEY_TOS_ACCEPTED] = newSettings.tosAccepted
            it[KEY_DEV_MODE_ENABLED] = newSettings.devModeEnabled
            it[KEY_CONFIRM_EXIT] = newSettings.confirmExit
            it[KEY_SEARCH] = newSettings.search
            it[KEY_USERNAME] = newSettings.username
            it[KEY_PASSWORD] = newSettings.password
            it[KEY_NOTIFICATIONS_ENABLED] = newSettings.notificationsEnabled
            it[KEY_SHOW_GRADE_IN_NOTIFICATION] = newSettings.showGradeInNotification
            it[KEY_USE_METERED_NETWORK] = newSettings.useMeteredNetwork
            it[KEY_REFRESH_INTERVAL] = newSettings.refreshInterval.minutes
            it[KEY_LAST_REFRESH] = zonedDateTimeToDb(newSettings.lastRefresh)
            it[KEY_CATEGORY_FILTER] = newSettings.categoryFilter
        }
        return settingsFromPreferences(prefs)
    }


    private fun settingsFromPreferences(prefs: Preferences) = UserSettings(
        lastVersionCode = prefs[KEY_LAST_VERSION_CODE] ?: -1,
        lastVersionName = prefs[KEY_LAST_VERSION_NAME] ?: "0.0",
        tosAccepted = prefs[KEY_TOS_ACCEPTED] ?: false,
        devModeEnabled = prefs[KEY_DEV_MODE_ENABLED] ?: false,
        confirmExit = prefs[KEY_CONFIRM_EXIT] ?: true,
        search = prefs[KEY_SEARCH] ?: "",
        username = prefs[KEY_USERNAME] ?: "",
        password = prefs[KEY_PASSWORD] ?: "",
        notificationsEnabled = prefs[KEY_NOTIFICATIONS_ENABLED] ?: false,
        showGradeInNotification = prefs[KEY_SHOW_GRADE_IN_NOTIFICATION] ?: false,
        useMeteredNetwork = prefs[KEY_USE_METERED_NETWORK] ?: true,
        refreshInterval = RefreshInterval.fromMinutes(prefs[KEY_REFRESH_INTERVAL]),
        lastRefresh = zonedDateTimeFromDb(prefs[KEY_LAST_REFRESH]),
        categoryFilter = prefs[KEY_CATEGORY_FILTER] ?: "",
    )

    private companion object {
        private val KEY_LAST_VERSION_CODE = intPreferencesKey("lastVersionCode")
        private val KEY_LAST_VERSION_NAME = stringPreferencesKey("lastVersionName")
        private val KEY_TOS_ACCEPTED = booleanPreferencesKey("tosAccepted")
        private val KEY_DEV_MODE_ENABLED = booleanPreferencesKey("devModeEnabled")
        private val KEY_CONFIRM_EXIT = booleanPreferencesKey("confirmExit")
        private val KEY_SEARCH = stringPreferencesKey("search")
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_PASSWORD = stringPreferencesKey("password")
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notificationsEnabled")
        private val KEY_SHOW_GRADE_IN_NOTIFICATION = booleanPreferencesKey("showGradeInNotification")
        private val KEY_USE_METERED_NETWORK = booleanPreferencesKey("useMeteredNetwork")
        private val KEY_REFRESH_INTERVAL = intPreferencesKey("refreshInterval")
        private val KEY_LAST_REFRESH = stringPreferencesKey("lastRefresh")
        private val KEY_CATEGORY_FILTER = stringPreferencesKey("categoryFilter")
    }
}

/** Settings associated with the current user. */
data class UserSettings(
    /** devMode enabled */
    val devModeEnabled: Boolean,
    /** confirm Exit*/
    val confirmExit: Boolean,
    /** Last App-Version-Code */
    val lastVersionCode: Int,
    /** Last App-Version-Name */
    val lastVersionName: String,
    /** terms of service accepted by user */
    val tosAccepted: Boolean,
    /** search */
    val search: String,
    /** username */
    val username: String,
    /** password */
    val password: String,
    /** Notifications enabled */
    val notificationsEnabled: Boolean,
    /** Notifications enabled */
    val showGradeInNotification: Boolean,
    /** use metered Networks */
    val useMeteredNetwork: Boolean,
    /** refresh interval */
    val refreshInterval: RefreshInterval,
    /** last updated */
    val lastRefresh: ZonedDateTime?,
    /** exam category filter */
    val categoryFilter: String,
)

enum class RefreshInterval(val minutes: Int) {
    NEVER(0),
    INTERVAL_FIFTEEN_MINUTES(15),
    INTERVAL_HALF_HOUR(30),
    INTERVAL_HOUR(60),
    INTERVAL_HALF_DAY(720),
    INTERVAL_DAY(1440);

    fun getLocalString(context: Context) = context.resources.getStringArray(R.array.array_refresh_interval)[ordinal]

    companion object {
        fun fromMinutes(minutes: Int?): RefreshInterval {
            return when (minutes) {
                0 -> NEVER
                15 -> INTERVAL_FIFTEEN_MINUTES
                30 -> INTERVAL_HALF_HOUR
                60 -> INTERVAL_HOUR
                720 -> INTERVAL_HALF_DAY
                1440 -> INTERVAL_DAY
                else -> INTERVAL_HOUR
            }
        }
    }
}
