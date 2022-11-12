package de.lemke.studiportal.domain.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import de.lemke.studiportal.domain.GetStudiportalDataUseCase
import de.lemke.studiportal.domain.SendNotificationUseCase
import de.lemke.studiportal.domain.UpdateExamsUseCase
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var getStudiportalData: GetStudiportalDataUseCase

    @Inject
    lateinit var updateExams: UpdateExamsUseCase

    @Inject
    lateinit var sendNotification: SendNotificationUseCase
    /**
     * sends notification when receives alarm
     * and then reschedule the reminder again
     * */
    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        GlobalScope.launch {
            getStudiportalData(
                successCallback = { exams ->
                    GlobalScope.launch {
                        //demo?
                        updateExams(exams, true)
                    }
                },
            )
            //TODO reschedule the reminder
        }
    }
}
