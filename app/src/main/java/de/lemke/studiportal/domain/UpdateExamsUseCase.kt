package de.lemke.studiportal.domain

import de.lemke.studiportal.data.ExamsRepository
import de.lemke.studiportal.domain.model.Exam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UpdateExamsUseCase @Inject constructor(
    private val examsRepository: ExamsRepository,
) {
    suspend operator fun invoke(exams: List<Exam>) = withContext(Dispatchers.Default) {
        examsRepository.updateExams(exams)
    }
}
