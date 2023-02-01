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
    suspend operator fun invoke(newExams: List<Exam>, notifyAboutChanges: Boolean): Boolean = withContext(Dispatchers.Default) {
        updateUserSettings { it.copy(lastRefresh = ZonedDateTime.now()) }
        if (newExams.isEmpty()) return@withContext false
        val oldExams = examsRepository.getExams()
        val changedExams = newExams.filter { exam -> oldExams.none { it == exam } }
        if (changedExams.isEmpty()) return@withContext false
        examsRepository.updateExams(newExams)
        if (notifyAboutChanges && oldExams.isNotEmpty())
            changedExams.forEach { if (!it.isSeparator) sendNotification(it, getUserSettings().showGradeInNotification) }
        return@withContext changedExams.isNotEmpty()
    }
}
