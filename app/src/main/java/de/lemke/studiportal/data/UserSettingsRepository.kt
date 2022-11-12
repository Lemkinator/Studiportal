package de.lemke.studiportal.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import de.lemke.studiportal.data.database.Converters.localDateTimeFromDb
import de.lemke.studiportal.data.database.Converters.localDateTimeToDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
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
            it[KEY_USE_MOBILE_DATA] = newSettings.useMobileData
            it[KEY_IS_REFRESH_OVERDUE] = newSettings.isRefreshOverdue
            it[KEY_REFRESH_INTERVAL] = newSettings.refreshInterval
            it[KEY_LAST_REFRESH] = localDateTimeToDb(newSettings.lastRefresh)
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
        useMobileData = prefs[KEY_USE_MOBILE_DATA] ?: true,
        isRefreshOverdue = prefs[KEY_IS_REFRESH_OVERDUE] ?: true,
        refreshInterval = prefs[KEY_REFRESH_INTERVAL] ?: 60L,
        lastRefresh = localDateTimeFromDb(prefs[KEY_LAST_REFRESH] ?: localDateTimeToDb(LocalDateTime.MIN)),
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
        private val KEY_USE_MOBILE_DATA = booleanPreferencesKey("useMobileData")
        private val KEY_IS_REFRESH_OVERDUE = booleanPreferencesKey("isRefreshOverdue")
        private val KEY_REFRESH_INTERVAL = longPreferencesKey("refreshInterval")
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
    /** use mobile data */
    val useMobileData: Boolean,
    /** is refresh overdue */
    val isRefreshOverdue: Boolean,
    /** refresh interval */
    val refreshInterval: Long,
    /** last updated */
    val lastRefresh: LocalDateTime,
    /** exam category filter */
    val categoryFilter: String,
)
