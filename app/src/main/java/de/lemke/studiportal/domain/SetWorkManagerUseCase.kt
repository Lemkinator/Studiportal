package de.lemke.studiportal.domain

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ActivityContext
import de.lemke.studiportal.data.RefreshInterval
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SetWorkManagerUseCase @Inject constructor(
    @ActivityContext private val context: Context,
    private val getUserSettings: GetUserSettingsUseCase
) {
    suspend operator fun invoke(replaceExisting: Boolean = true) = withContext(Dispatchers.Default) {
        val userSettings = getUserSettings()
        val workManager = WorkManager.getInstance(context)
        if (userSettings.username.isEmpty() || userSettings.refreshInterval == RefreshInterval.NEVER) cancelStudiportalWork()
        else workManager.enqueueUniquePeriodicWork(
            "checkStudiportalData",
            if (replaceExisting) ExistingPeriodicWorkPolicy.REPLACE
            else ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<CheckStudiportalWork>(userSettings.refreshInterval.minutes.toLong(), TimeUnit.MINUTES)
                .setConstraints(
                    with(Constraints.Builder()) {
                        setRequiredNetworkType(NetworkType.CONNECTED)
                        if (userSettings.useMeteredNetwork) setRequiredNetworkType(NetworkType.UNMETERED)
                        build()
                    }
                )
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    PeriodicWorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .setInitialDelay(5, TimeUnit.MINUTES)
                .build()
        )
    }

    fun cancelStudiportalWork() {
        WorkManager.getInstance(context).cancelUniqueWork("checkStudiportalData")
    }
}


@HiltWorker
class CheckStudiportalWork @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val getStudiportalData: GetStudiportalDataUseCase,
    private val updateExams: UpdateExamsUseCase,
    private val getUserSettings: GetUserSettingsUseCase,
    private val demo: DemoUseCase,
) : CoroutineWorker(appContext, workerParams) {
    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        val userSettings = getUserSettings()
        if (userSettings.username == demo.username) demo.updateExams(userSettings.notificationsEnabled)
        else getStudiportalData(
            successCallback = { exams -> GlobalScope.launch { updateExams(exams, userSettings.notificationsEnabled) } }
        )
        Result.success()
    }
}
