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
    suspend operator fun invoke() = withContext(Dispatchers.Default) {
        val userSettings = getUserSettings()
        val workManager = WorkManager.getInstance(context)
        if (userSettings.username.isEmpty() || userSettings.refreshInterval == RefreshInterval.NEVER) cancelStudiportalWork()
        else workManager.enqueueUniquePeriodicWork(
            "checkStudiportalData",
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<CheckStudiportalWork>(userSettings.refreshInterval.minutes.toLong(), TimeUnit.MINUTES)
                .setConstraints(
                    with(Constraints.Builder()) {
                        setRequiredNetworkType(NetworkType.CONNECTED)
                        if (!userSettings.allowMeteredConnection) setRequiredNetworkType(NetworkType.UNMETERED)
                        build()
                    }
                )
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
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
    private val updateUserSettings: UpdateUserSettingsUseCase,
    private val demo: DemoUseCase,
) : CoroutineWorker(appContext, workerParams) {
    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        if (getUserSettings().username == demo.username) demo.updateDemoExams()
        else getStudiportalData(successCallback = { data ->
            GlobalScope.launch {
                updateUserSettings {
                    it.copy(studentName = data.first?.first ?: it.studentName, studentInfo = data.first?.second ?: it.studentInfo)
                }
                updateExams(data.second)
            }
        })
        Result.success()
    }
}
