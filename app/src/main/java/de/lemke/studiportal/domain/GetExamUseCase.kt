package de.lemke.studiportal.domain

import de.lemke.studiportal.data.ExamsRepository
import de.lemke.studiportal.domain.model.Exam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetExamUseCase @Inject constructor(
    private val examsRepository: ExamsRepository,
) {
    suspend operator fun invoke(examNumber: String, semester: String): Exam? = withContext(Dispatchers.Default) {
        examsRepository.getExam(examNumber, semester)
    }
}
