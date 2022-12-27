package de.lemke.studiportal

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import de.lemke.studiportal.domain.GetUserSettingsUseCase
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Main entry point into the application process.
 * Registered in the AndroidManifest.xml file.
 */
@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject
    lateinit var getUserSettings: GetUserSettingsUseCase

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        runBlocking {
            val userSettings = getUserSettings()
            if (!userSettings.autoDarkMode) {
                if (userSettings.darkMode) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }

    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.VERBOSE)
            .build()
}
