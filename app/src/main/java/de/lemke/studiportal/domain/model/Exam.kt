package de.lemke.studiportal.domain.model

import android.content.Context
import de.lemke.studiportal.R

data class Exam(
    val examNumber: String,
    val name: String,
    val bonus: String,
    val malus: String,
    val ects: String,
    val sws: String,
    val semester: String,
    val kind: String,
    val tryCount: String,
    val grade: String,
    val state: State,
    val comment: String,
    val isResignated: Boolean,
    val note: Note,
    val category: String,
) {

    enum class State {
        AN, BE, NB, EN, UNDEFINED;

        fun getLocalString(context: Context): String = when (this) {
            AN -> context.getString(R.string.an)
            BE -> context.getString(R.string.be)
            NB -> context.getString(R.string.nb)
            EN -> context.getString(R.string.en)
            UNDEFINED -> context.getString(R.string.undefined)
        }

        companion object {
            fun fromString(string: String?): State = when (string) {
                "AN", "an" -> AN
                "BE", "be" -> BE
                "NB", "nb" -> NB
                "EN", "en" -> EN
                else -> UNDEFINED
            }

            fun fromString(context: Context, string: String?): State = when (string) {
                context.getString(R.string.an), "AN", "an" -> AN
                context.getString(R.string.be), "BE", "be" -> BE
                context.getString(R.string.nb), "NB", "nb" -> NB
                context.getString(R.string.en), "EN", "en" -> EN
                else -> UNDEFINED
            }
        }
    }

    enum class Note {
        GR, K, SA, U, VF, UNDEFINED;

        fun getLocalString(context: Context): String = when (this) {
            GR -> context.getString(R.string.gr)
            K -> context.getString(R.string.k)
            SA -> context.getString(R.string.sa)
            U -> context.getString(R.string.u)
            VF -> context.getString(R.string.vf)
            UNDEFINED -> context.getString(R.string.undefined)
        }

        companion object {
            fun fromString(string: String?): Note = when (string) {
                "GR", "gr" -> GR
                "K", "k" -> K
                "SA", "sa" -> SA
                "U", "u" -> U
                "VF", "vf" -> VF
                else -> UNDEFINED
            }

            fun fromString(context: Context, string: String?): Note = when (string) {
                context.getString(R.string.gr), "GR", "gr" -> GR
                context.getString(R.string.k), "K", "k" -> K
                context.getString(R.string.sa), "SA", "sa" -> SA
                context.getString(R.string.u), "U", "u" -> U
                context.getString(R.string.vf), "VF", "vf" -> VF
                else -> UNDEFINED
            }
        }
    }

    val isSeparator: Boolean
        get() = examNumber == "separator"

    companion object {
        fun create(
            examNumber: String,
            name: String?,
            bonus: String?,
            malus: String?,
            ects: String?,
            sws: String?,
            semester: String?,
            kind: String?,
            tryCount: String?,
            grade: String?,
            state: String?,
            comment: String?,
            resignation: String?,
            note: String?,
            category: String,
        ): Exam {
            return Exam(
                examNumber = examNumber.replace(" +".toRegex(), " "),
                name = name ?: "-",
                bonus = bonus ?: "-",
                malus = malus ?: "-",
                ects = ects ?: "-",
                sws = sws ?: "-",
                semester = semester ?: "-",
                kind = kind ?: "-",
                tryCount = tryCount ?: "-",
                grade = grade ?: "-",
                state = State.fromString(state),
                comment = comment ?: "-",
                isResignated = resignation == "1",
                note = Note.fromString(note),
                category = category,
            )
        }

        fun createSeparator(index: Int, category: String): Exam {
            return Exam(
                examNumber = "separator",
                name = "separator $index",
                bonus = "-",
                malus = "-",
                ects = "-",
                sws = "-",
                semester = "-",
                kind = "-",
                tryCount = "-",
                grade = "-",
                state = State.UNDEFINED,
                comment = "-",
                isResignated = false,
                note = Note.UNDEFINED,
                category = category,
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Exam

        if (examNumber != other.examNumber) return false
        if (name != other.name) return false
        if (bonus != other.bonus) return false
        if (malus != other.malus) return false
        if (ects != other.ects) return false
        if (sws != other.sws) return false
        if (semester != other.semester) return false
        if (kind != other.kind) return false
        if (tryCount != other.tryCount) return false
        if (grade != other.grade) return false
        if (state != other.state) return false
        if (comment != other.comment) return false
        if (isResignated != other.isResignated) return false
        if (note != other.note) return false
        if (category != other.category) return false

        return true
    }

    override fun hashCode(): Int {
        var result = examNumber.hashCode()
        result = 31 * result + semester.hashCode()
        result = 31 * result + grade.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + comment.hashCode()
        result = 31 * result + note.hashCode()
        result = 31 * result + category.hashCode()
        return result
    }
}