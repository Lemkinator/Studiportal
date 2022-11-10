package de.lemke.studiportal.ui.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.util.SeslRoundedCorner
import androidx.appcompat.util.SeslSubheaderRoundedCorner
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import de.lemke.studiportal.R
import de.lemke.studiportal.databinding.FragmentExamBinding
import de.lemke.studiportal.domain.GetExamsWithSeparatorUseCase
import de.lemke.studiportal.domain.GetStudiportalDataUseCase
import de.lemke.studiportal.domain.GetUserSettingsUseCase
import de.lemke.studiportal.domain.StudiportalListener
import de.lemke.studiportal.domain.model.Exam
import dev.oneuiproject.oneui.layout.ToolbarLayout
import dev.oneuiproject.oneui.utils.internal.ReflectUtils
import dev.oneuiproject.oneui.widget.Separator
import dev.oneuiproject.oneui.widget.Toast
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class MainActivityExamFragment : Fragment() {
    private lateinit var binding: FragmentExamBinding
    private lateinit var exams: MutableList<Pair<Exam?, String>>
    private lateinit var examAdapter: ExamAdapter
    private lateinit var toolbarLayout: ToolbarLayout

    @Inject
    lateinit var getUserSettings: GetUserSettingsUseCase

    @Inject
    lateinit var getStudiportalData: GetStudiportalDataUseCase

    @Inject
    lateinit var getExamsWithSeparator: GetExamsWithSeparatorUseCase


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentExamBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbarLayout = requireActivity().findViewById(R.id.toolbar_layout_main)
        toolbarLayout.appBarLayout.addOnOffsetChangedListener { layout: AppBarLayout, verticalOffset: Int ->
            val totalScrollRange = layout.totalScrollRange
            val inputMethodWindowVisibleHeight = ReflectUtils.genericInvokeMethod(
                InputMethodManager::class.java,
                requireActivity().getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE),
                "getInputMethodWindowVisibleHeight"
            ) as Int
            if (totalScrollRange != 0) binding.examNoEntryView.translationY = (abs(verticalOffset) - totalScrollRange).toFloat() / 2.0f
            else binding.examNoEntryView.translationY = (abs(verticalOffset) - inputMethodWindowVisibleHeight).toFloat() / 2.0f
        }
        exams = mutableListOf() //TODO
        initList()
        val studiportalListener = object : StudiportalListener {
            override fun onSuccess(newExams: List<Exam>) {
                exams = getExamsWithSeparator(newExams)
                initList()
                binding.swipeRefreshLayout.isRefreshing = false
            }

            override fun onError(message: String) {
                binding.swipeRefreshLayout.isRefreshing = false
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch { getStudiportalData(studiportalListener) }
        }
    }

    fun setSubtitle(subtitle: String) {
        toolbarLayout.setExpandedSubtitle(subtitle)
        toolbarLayout.setCollapsedSubtitle(subtitle)
    }

    private fun initList() {
        if (!this::binding.isInitialized) return
        if (exams.isEmpty()) {
            Log.d("MainActivityExamFragment", "initList: no exams")
            binding.examListLayout.visibility = View.GONE
            binding.examListLottie.cancelAnimation()
            binding.examListLottie.progress = 0f
            binding.examNoEntryScrollView.visibility = View.VISIBLE
            binding.examListLottie.postDelayed({ binding.examListLottie.playAnimation() }, 400)
        } else {
            Log.d("MainActivityExamFragment", "initList: ${exams.size} exams")
            binding.examNoEntryScrollView.visibility = View.GONE
            binding.examListLayout.visibility = View.VISIBLE
        }
        binding.examList.layoutManager = LinearLayoutManager(context)
        examAdapter = ExamAdapter()
        binding.examList.adapter = examAdapter
        binding.examList.itemAnimator = null
        binding.examList.addItemDecoration(ItemDecoration(requireContext()))
        binding.examList.seslSetFastScrollerEnabled(true)
        binding.examList.seslSetFillBottomEnabled(true)
        binding.examList.seslSetGoToTopEnabled(true)
        binding.examList.seslSetLastRoundedCorner(true)
        binding.examList.seslSetSmoothScrollEnabled(true)
    }

    inner class ExamAdapter : RecyclerView.Adapter<ExamAdapter.ViewHolder>() {
        override fun getItemCount(): Int = exams.size
        override fun getItemId(position: Int): Long = position.toLong()
        override fun getItemViewType(position: Int): Int = when {
            exams[position].first == null -> 2
            exams[position].first?.isSeparator == true -> 1
            else -> 0
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = when (viewType) {
            0 -> ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.listview_item, parent, false), viewType)
            else -> ViewHolder(Separator(requireContext()), viewType)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (holder.isItem) {
                val exam = exams[position].first!!
                holder.listItemTitle.text = exam.name
                when (val kind = exam.kind.uppercase()) {
                    "KO" -> {
                        holder.listItemSubtitle2.visibility = View.GONE
                        when {
                            exam.malus == "-" && exam.bonus == "-" -> holder.listItemSubtitle1.text = getString(R.string.no_ects)
                            exam.bonus != "-" -> holder.listItemSubtitle1.text = getString(R.string.bonus, exam.bonus)
                            else -> holder.listItemSubtitle1.text = getString(R.string.malus, exam.malus)
                        }
                    }
                    "PL", "SL", "P", "G" -> {
                        holder.listItemSubtitle2.visibility = View.VISIBLE
                        when {
                            exam.isResignated -> {
                                //If e is resignated, shw special info on the topic
                                holder.listItemSubtitle1.text = getString(R.string.state, getString(R.string.state_resignated))
                                holder.listItemSubtitle2.text = getString(
                                    R.string.note,
                                    exam.note.getLocalString(requireContext()) + " (" + exam.semester + ")"
                                )
                            }
                            kind == "SL" -> {
                                holder.listItemSubtitle1.text =
                                    getString(
                                        R.string.state,
                                        exam.state.getLocalString(requireContext()) + getString(R.string.ects, exam.ects)
                                    )
                                holder.listItemSubtitle2.text = getString(R.string.attempt, exam.tryCount + " (" + exam.semester + ")")
                            }
                            else -> {
                                if (exam.state == Exam.State.AN) holder.listItemSubtitle1.text = getString(
                                    R.string.state,
                                    exam.state.getLocalString(requireContext()) + getString(R.string.ects, exam.ects)
                                )
                                else holder.listItemSubtitle1.text =
                                    getString(R.string.grade, exam.grade + getString(R.string.ects, exam.ects))
                                if (kind == "G") holder.listItemSubtitle2.text = getString(R.string.semester, exam.semester)
                                else holder.listItemSubtitle2.text = getString(R.string.attempt, exam.tryCount + " (" + exam.semester + ")")
                            }
                        }
                    }
                    else -> { //kind == "VL", ...
                        holder.listItemSubtitle2.visibility = View.VISIBLE
                        holder.listItemSubtitle1.text =
                            getString(
                                R.string.state,
                                exam.state.getLocalString(requireContext()) + getString(R.string.ects, exam.ects)
                            )
                        holder.listItemSubtitle2.text = exam.kind
                    }
                }
                holder.listItemImg.setColorFilter(requireContext().getColor(dev.oneuiproject.oneui.R.color.oui_primary_icon_color))
                when (exam.state) {
                    Exam.State.AN -> holder.listItemImg.setImageResource(dev.oneuiproject.oneui.R.drawable.ic_oui_timer)
                    Exam.State.BE -> {
                        holder.listItemImg.setColorFilter(requireContext().getColor(dev.oneuiproject.oneui.design.R.color.oui_functional_green_color))
                        holder.listItemImg.setImageResource(dev.oneuiproject.oneui.R.drawable.ic_oui_checkbox_checked)
                    }
                    Exam.State.NB -> {
                        holder.listItemImg.setColorFilter(requireContext().getColor(dev.oneuiproject.oneui.design.R.color.oui_functional_red_color))
                        holder.listItemImg.setImageResource(dev.oneuiproject.oneui.R.drawable.ic_oui_disturb)
                    }
                    Exam.State.EN -> {
                        holder.listItemImg.setColorFilter(requireContext().getColor(dev.oneuiproject.oneui.design.R.color.oui_functional_red_color))
                        holder.listItemImg.setImageResource(dev.oneuiproject.oneui.R.drawable.ic_oui_error_filled)
                    }
                    Exam.State.UNDEFINED -> holder.listItemImg.setImageResource(dev.oneuiproject.oneui.R.drawable.ic_oui_info_outline)
                }
                if (exam.isResignated) {
                    holder.listItemImg.setColorFilter(requireContext().getColor(dev.oneuiproject.oneui.design.R.color.oui_primary_icon_color))
                    holder.listItemImg.setImageResource(dev.oneuiproject.oneui.R.drawable.ic_oui_arrow_to_left)
                }
            }
            if (holder.isSeparator) {
                holder.listItemTitle.layoutParams =
                    ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            if (holder.isCategorySeparator) {
                holder.listItemTitle.layoutParams =
                    ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                holder.listItemTitle.text = exams[position].second
            }
        }

        inner class ViewHolder internal constructor(itemView: View, viewType: Int) : RecyclerView.ViewHolder(itemView) {
            var isItem: Boolean = viewType == 0
            var isSeparator: Boolean = viewType == 1
            var isCategorySeparator: Boolean = viewType == 2
            lateinit var parentView: RelativeLayout
            lateinit var listItemImg: ImageView
            lateinit var listItemTitle: TextView
            lateinit var listItemSubtitle1: TextView
            lateinit var listItemSubtitle2: TextView

            init {
                when {
                    isItem -> {
                        parentView = itemView as RelativeLayout
                        listItemImg = itemView.findViewById(R.id.listview_item_img)
                        listItemTitle = parentView.findViewById(R.id.list_item_title)
                        listItemSubtitle1 = parentView.findViewById(R.id.list_item_subtitle1)
                        listItemSubtitle2 = parentView.findViewById(R.id.list_item_subtitle2)
                    }
                    isSeparator || isCategorySeparator -> listItemTitle = itemView as TextView
                }
            }
        }
    }

    inner class ItemDecoration(context: Context) : RecyclerView.ItemDecoration() {
        private val divider: Drawable?
        private val roundedCorner: SeslSubheaderRoundedCorner
        override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            super.onDraw(c, parent, state)
            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                val holder: ExamAdapter.ViewHolder = binding.examList.getChildViewHolder(child) as ExamAdapter.ViewHolder
                if (holder.isItem) {
                    val top = (child.bottom + (child.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin)
                    val bottom = divider!!.intrinsicHeight + top
                    divider.setBounds(parent.left, top, parent.right, bottom)
                    divider.draw(c)
                }
            }
        }

        override fun seslOnDispatchDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                val holder: ExamAdapter.ViewHolder = binding.examList.getChildViewHolder(child) as ExamAdapter.ViewHolder
                if (!holder.isItem) roundedCorner.drawRoundedCorner(child, c)
            }
        }

        init {
            val outValue = TypedValue()
            context.theme.resolveAttribute(androidx.appcompat.R.attr.isLightTheme, outValue, true)
            divider = context.getDrawable(
                if (outValue.data == 0) androidx.appcompat.R.drawable.sesl_list_divider_dark
                else androidx.appcompat.R.drawable.sesl_list_divider_light
            )!!
            roundedCorner = SeslSubheaderRoundedCorner(context)
            roundedCorner.roundedCorners = SeslRoundedCorner.ROUNDED_CORNER_ALL
        }
    }
}