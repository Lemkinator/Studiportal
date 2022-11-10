package de.lemke.studiportal.domain

import de.lemke.studiportal.data.ExamsRepository
import de.lemke.studiportal.domain.model.Exam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FindChangedExamsUseCase @Inject constructor(
    private val examsRepository: ExamsRepository,
) {
    suspend operator fun invoke(newExams: List<Exam>): List<Exam> = withContext(Dispatchers.Default) {
        val oldExams = examsRepository.getExams()
        return@withContext newExams.filter { newExam ->
            !newExam.isSeparator && oldExams.find { it == newExam } == null
        }
    }
}
