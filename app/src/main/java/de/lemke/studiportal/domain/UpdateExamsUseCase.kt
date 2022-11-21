package de.lemke.studiportal.domain

import de.lemke.studiportal.data.ExamsRepository
import de.lemke.studiportal.domain.model.Exam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import javax.inject.Inject

class UpdateExamsUseCase @Inject constructor(
    private val examsRepository: ExamsRepository,
    private val sendNotification: SendNotificationUseCase,
    private val getUserSettings: GetUserSettingsUseCase,
    private val updateUserSettings: UpdateUserSettingsUseCase,
) {
    suspend operator fun invoke(exams: List<Exam>, notifyAboutChanges: Boolean): Boolean = withContext(Dispatchers.Default) {
        updateUserSettings { it.copy(lastRefresh = ZonedDateTime.now()) }
        val oldExams = examsRepository.getExams()
        examsRepository.updateExams(exams)
        val changedExams = exams.filter { exam -> oldExams.find { it == exam } == null }
        if (notifyAboutChanges && oldExams.isNotEmpty())
            changedExams.forEach { sendNotification(it, getUserSettings().showGradeInNotification) }
        return@withContext changedExams.isNotEmpty()
    }
}
