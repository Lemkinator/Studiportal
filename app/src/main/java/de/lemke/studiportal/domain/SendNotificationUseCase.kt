package de.lemke.studiportal.domain

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import de.lemke.studiportal.R
import de.lemke.studiportal.domain.model.Exam
import de.lemke.studiportal.domain.utils.AlarmReceiver
import de.lemke.studiportal.ui.ExamActivity
import java.util.*
import javax.inject.Inject

class SendNotificationUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private val channelId = context.getString(R.string.exam_notification_channel_id)
    private val notificationRequestCode = 55

    operator fun invoke(exam: Exam, showGrade: Boolean) {
        createNotificationChannel()
        initNotificationBuilder(exam, showGrade)
        // notificationId is a unique int for each notification that you must define
        NotificationManagerCompat.from(context).notify(exam.examNumber.hashCode(), notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        val name = context.getString(R.string.exam_notification_channel_name)
        val descriptionText = context.getString(R.string.exam_notification_channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system
        val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }


    private fun initNotificationBuilder(exam: Exam, showGrade: Boolean) {
        // Create an explicit intent for an Activity in your app
        // Create an Intent for the activity you want to start
        val resultIntent = Intent(context, ExamActivity::class.java)
        resultIntent.putExtra("examNumber", exam.examNumber)
        // Create the TaskStackBuilder
        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(resultIntent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(exam.name)
            .setContentText(
                if (showGrade) {
                    if (exam.grade != Exam.UNDEFINED) exam.grade + " - " + exam.state.getLocalString(context)
                    else exam.state.getLocalString(context)
                } else context.getString(R.string.touch_to_show_grade)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // Set the intent that will fire when the user taps the notification
            .setContentIntent(resultPendingIntent)
            // Automatically removes the notification when the user taps it.
            .setAutoCancel(true)
    }

    fun setExamNotification(enable: Boolean) =
        if (enable) enableExamNotification() else disableExamNotification()

    private fun enableExamNotification() {
        createNotificationChannel()
        val alarmIntent = Intent(context.applicationContext, AlarmReceiver::class.java).let { intent ->
            PendingIntent.getBroadcast(
                context,
                notificationRequestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val calendar: Calendar = Calendar.getInstance().apply {
            //set(Calendar.HOUR_OF_DAY, hour)
            //set(Calendar.MINUTE, minute)
        }
        // If the trigger time you specify is in the past, the alarm triggers immediately. if soo just add one day to required calendar
        // Note: also adding 1 min cuz if user clicks on notification as soon as received it it will reschedule the alarm to
        // fire another notification immediately
        if (Calendar.getInstance().apply { add(Calendar.MINUTE, 1) }.timeInMillis - calendar.timeInMillis > 0) {
            calendar.add(Calendar.DATE, 1)
        }
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, alarmIntent)
    }

    private fun disableExamNotification() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).let { intent ->
            PendingIntent.getBroadcast(context, notificationRequestCode, intent, 0 or PendingIntent.FLAG_IMMUTABLE)
        }
        alarmManager.cancel(intent)
    }
}