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
    companion object {
        const val UNDEFINED = "-"

        fun create(
            examNumber: String,
            name: String?,
            bonus: String? = null,
            malus: String? = null,
            ects: String? = null,
            sws: String? = null,
            semester: String? = null,
            kind: String?,
            tryCount: String? = null,
            grade: String? = null,
            state: String? = null,
            comment: String? = null,
            resignation: String? = null,
            note: String? = null,
            category: String,
        ): Exam {
            return Exam(
                examNumber = examNumber.replace(" +".toRegex(), " "),
                name = name ?: examNumber.replace(" +".toRegex(), " "),
                bonus = bonus ?: UNDEFINED,
                malus = malus ?: UNDEFINED,
                ects = ects ?: UNDEFINED,
                sws = sws ?: UNDEFINED,
                semester = semester ?: UNDEFINED,
                kind = kind ?: UNDEFINED,
                tryCount = tryCount ?: UNDEFINED,
                grade = grade ?: UNDEFINED,
                state = State.fromString(state),
                comment = comment ?: UNDEFINED,
                isResignated = resignation == "1",
                note = Note.fromString(note),
                category = category,
            )
        }

        fun createSeparator(index: Int, category: String): Exam {
            return Exam(
                examNumber = "separator",
                name = "separator $index",
                bonus = UNDEFINED,
                malus = UNDEFINED,
                ects = UNDEFINED,
                sws = UNDEFINED,
                semester = UNDEFINED,
                kind = UNDEFINED,
                tryCount = UNDEFINED,
                grade = UNDEFINED,
                state = State.UNDEFINED,
                comment = UNDEFINED,
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

    val isSeparator: Boolean
        get() = examNumber == "separator"

    fun getSubtitle1(context: Context, showGrade: Boolean = true): String = when (val kind = kind.uppercase()) {
        "KO" -> {
            when {
                malus == "-" && bonus == "-" -> context.getString(R.string.no_ects)
                bonus != "-" -> context.getString(R.string.bonus_value, bonus)
                else -> context.getString(R.string.malus_value, malus)
            }
        }
        "PL", "SL", "P", "G" -> {
            when {
                isResignated -> context.getString(R.string.state_value, context.getString(R.string.resignated))
                state == State.AN || kind == "SL" -> context.getString(
                    R.string.state_value,
                    state.getLocalString(context) + context.getString(R.string.ects_value, ects)
                )
                showGrade -> context.getString(R.string.grade_value, grade + context.getString(R.string.ects_value, ects))
                else -> context.getString(
                    R.string.state_value,
                    state.getLocalString(context) + context.getString(R.string.ects_value, ects)
                )
            }
        }
        else -> context.getString(R.string.state_value, state.getLocalString(context) + context.getString(R.string.ects_value, ects))
    }

    fun getSubtitle2(context: Context): String = when (val kind = kind.uppercase()) {
        "KO" -> ""
        "PL", "SL", "P", "G" -> {
            when {
                isResignated -> context.getString(R.string.note_value, note.getLocalString(context) + " (" + semester + ")")
                kind == "G" -> context.getString(R.string.semester_value, semester)
                else -> context.getString(R.string.attempt_value, "$tryCount ($semester)")
            }
        }
        else -> kind
    }

    fun getInfoPairList(context: Context, includeUndefined: Boolean): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        if (examNumber != UNDEFINED || includeUndefined) list.add(context.getString(R.string.exam_number) to examNumber)
        if (name != UNDEFINED || includeUndefined) list.add(context.getString(R.string.name) to name)
        if (kind != UNDEFINED || includeUndefined) list.add(context.getString(R.string.kind) to getLocalKindString(context))
        if (category != UNDEFINED || includeUndefined) list.add(context.getString(R.string.category) to category)
        if (bonus != UNDEFINED || includeUndefined) list.add(context.getString(R.string.bonus) to bonus)
        if (malus != UNDEFINED || includeUndefined) list.add(context.getString(R.string.malus) to malus)
        if (ects != UNDEFINED || includeUndefined) list.add(context.getString(R.string.ects) to ects)
        if (sws != UNDEFINED || includeUndefined) list.add(context.getString(R.string.sws) to sws)
        if (semester != UNDEFINED || includeUndefined) list.add(context.getString(R.string.semester) to semester)
        if (tryCount != UNDEFINED || includeUndefined) list.add(context.getString(R.string.attempt) to tryCount)
        if (grade != UNDEFINED || includeUndefined) list.add(context.getString(R.string.grade) to grade)
        if (state != State.UNDEFINED || includeUndefined) list.add(context.getString(R.string.state) to state.getLocalString(context))
        if (comment != UNDEFINED || includeUndefined) list.add(context.getString(R.string.comment) to comment)
        if (isResignated) list.add(context.getString(R.string.resignation) to context.getString(R.string.resignated))
        else if (includeUndefined) list.add(context.getString(R.string.resignation) to UNDEFINED)
        if (note != Note.UNDEFINED || includeUndefined) list.add(context.getString(R.string.note) to note.getLocalString(context))
        return list
    }

    fun getDrawableRessource() =
        if (isResignated) dev.oneuiproject.oneui.R.drawable.ic_oui_arrow_to_left
        else when (state) {
            State.AN -> dev.oneuiproject.oneui.R.drawable.ic_oui_timer
            State.BE -> dev.oneuiproject.oneui.R.drawable.ic_oui_checkbox_checked
            State.NB -> dev.oneuiproject.oneui.R.drawable.ic_oui_disturb
            State.EN -> dev.oneuiproject.oneui.R.drawable.ic_oui_error_filled
            State.UNDEFINED -> dev.oneuiproject.oneui.R.drawable.ic_oui_info_outline
        }

    fun getDrawableColor(context: Context): Int =
        if (isResignated) context.getColor(dev.oneuiproject.oneui.design.R.color.oui_primary_icon_color)
        else when (state) {
            State.AN -> context.getColor(dev.oneuiproject.oneui.R.color.oui_primary_icon_color)
            State.BE -> context.getColor(dev.oneuiproject.oneui.design.R.color.oui_functional_green_color)
            State.NB -> context.getColor(dev.oneuiproject.oneui.design.R.color.oui_functional_red_color)
            State.EN -> context.getColor(dev.oneuiproject.oneui.design.R.color.oui_functional_red_color)
            State.UNDEFINED -> context.getColor(dev.oneuiproject.oneui.R.color.oui_primary_icon_color)
        }

    fun containsKeywords(context: Context, keywords: Set<String>): Boolean {
        return keywords.any {
            examNumber.contains(it, ignoreCase = true) ||
                    name.contains(it, ignoreCase = true) ||
                    bonus.contains(it, ignoreCase = true) ||
                    malus.contains(it, ignoreCase = true) ||
                    ects.contains(it, ignoreCase = true) ||
                    sws.contains(it, ignoreCase = true) ||
                    semester.contains(it, ignoreCase = true) ||
                    kind.contains(it, ignoreCase = true) ||
                    getLocalKindString(context).contains(it, ignoreCase = true) ||
                    tryCount.contains(it, ignoreCase = true) ||
                    grade.contains(it, ignoreCase = true) ||
                    state.name.contains(it, ignoreCase = true) ||
                    state.getLocalString(context).contains(it, ignoreCase = true) ||
                    comment.contains(it, ignoreCase = true) ||
                    note.getLocalString(context).contains(it, ignoreCase = true) ||
                    category.contains(it, ignoreCase = true)
        }
    }

    private fun getLocalKindString(context: Context) = when (kind.uppercase()) {
        "KO" -> context.getString(R.string.ko)
        "PL" -> context.getString(R.string.pl)
        "SL" -> context.getString(R.string.sl)
        "P" -> context.getString(R.string.p)
        "G" -> context.getString(R.string.g)
        else -> kind
    }

    enum class State {
        AN, BE, NB, EN, UNDEFINED;

        fun getLocalString(context: Context): String = when (this) {
            AN -> context.getString(R.string.an)
            BE -> context.getString(R.string.be)
            NB -> context.getString(R.string.nb)
            EN -> context.getString(R.string.en)
            UNDEFINED -> Exam.UNDEFINED
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
        GR, K, SA, U, VF, V, UNDEFINED;

        fun getLocalString(context: Context): String = when (this) {
            GR -> context.getString(R.string.gr)
            K -> context.getString(R.string.k)
            SA -> context.getString(R.string.sa)
            U -> context.getString(R.string.u)
            VF -> context.getString(R.string.vf)
            V -> context.getString(R.string.v)
            UNDEFINED -> Exam.UNDEFINED
        }

        companion object {
            fun fromString(string: String?): Note = when (string) {
                "GR", "gr" -> GR
                "K", "k" -> K
                "SA", "sa" -> SA
                "U", "u" -> U
                "VF", "vf" -> VF
                "V", "v" -> V
                else -> UNDEFINED
            }

            fun fromString(context: Context, string: String?): Note = when (string) {
                context.getString(R.string.gr), "GR", "gr" -> GR
                context.getString(R.string.k), "K", "k" -> K
                context.getString(R.string.sa), "SA", "sa" -> SA
                context.getString(R.string.u), "U", "u" -> U
                context.getString(R.string.vf), "VF", "vf" -> VF
                context.getString(R.string.v), "V", "v" -> V
                else -> UNDEFINED
            }
        }
    }
}