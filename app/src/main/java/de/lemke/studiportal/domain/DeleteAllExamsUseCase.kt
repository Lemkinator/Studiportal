package de.lemke.studiportal.domain

import de.lemke.studiportal.data.ExamsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DeleteAllExamsUseCase @Inject constructor(
    private val examsRepository: ExamsRepository,
) {
    suspend operator fun invoke() = withContext(Dispatchers.Default) {
        examsRepository.deleteAllExams()
    }
}
