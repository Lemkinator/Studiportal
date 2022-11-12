package de.lemke.studiportal.domain

import de.lemke.studiportal.data.ExamsRepository
import de.lemke.studiportal.domain.model.Exam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject

class UpdateExamsUseCase @Inject constructor(
    private val examsRepository: ExamsRepository,
    private val sendNotification: SendNotificationUseCase,
    private val updateUserSettings: UpdateUserSettingsUseCase,
) {
    suspend operator fun invoke(exams: List<Exam>, notifyAboutChanges: Boolean): Boolean = withContext(Dispatchers.Default) {
        updateUserSettings { it.copy(lastRefresh = LocalDateTime.now()) }
        val oldExams = examsRepository.getExams()
        examsRepository.updateExams(exams)
        val changedExams = exams.filter { exam ->
            oldExams.find { it == exam } == null
        }
        if (notifyAboutChanges) {
            changedExams.forEach { sendNotification(it, notifyAboutChanges) }
        }
        return@withContext changedExams.isNotEmpty()
    }
}
