package de.lemke.studiportal.domain.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.util.SeslRoundedCorner
import androidx.appcompat.util.SeslSubheaderRoundedCorner
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import de.lemke.studiportal.R
import de.lemke.studiportal.domain.MakeSectionOfTextBoldUseCase
import de.lemke.studiportal.domain.model.Exam
import de.lemke.studiportal.ui.ExamActivity
import dev.oneuiproject.oneui.widget.Separator

class ExamAdapter(
    val context: Context,
    private val exams: MutableList<Pair<Exam?, String>>,
    private val search: String? = null,
) : RecyclerView.Adapter<ExamAdapter.ViewHolder>() {

    private val makeSectionOfTextBold: MakeSectionOfTextBoldUseCase = MakeSectionOfTextBoldUseCase()
    override fun getItemCount(): Int = exams.size
    override fun getItemId(position: Int): Long = position.toLong()
    override fun getItemViewType(position: Int): Int = when {
        exams[position].first == null -> 2
        exams[position].first?.isSeparator == true -> 1
        else -> 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = when (viewType) {
        0 -> ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.listview_item, parent, false), viewType)
        else -> ViewHolder(Separator(context), viewType)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder.isItem) {
            val exam = exams[position].first!!
            val color = MaterialColors.getColor(context, androidx.appcompat.R.attr.colorPrimary, context.getColor(R.color.primary_color_themed))
            holder.listItemTitle.text = makeSectionOfTextBold(exam.name, search, color)
            holder.listItemSubtitle1.text = makeSectionOfTextBold(exam.getSubtitle1(context), search, color)
            val subtitle2 = exam.getSubtitle2(context)
            if (subtitle2.isNotBlank()) {
                holder.listItemSubtitle2.text = makeSectionOfTextBold(subtitle2, search, color)
                holder.listItemSubtitle2.visibility = View.VISIBLE
            } else holder.listItemSubtitle2.visibility = View.GONE
            holder.listItemImg.setColorFilter(exam.getDrawableColor(context))
            holder.listItemImg.setImageResource(exam.getDrawableRessource())
            holder.parentView.setOnClickListener {
                context.startActivity(
                    Intent(context, ExamActivity::class.java)
                        .putExtra("examNumber", exam.examNumber)
                        .putExtra("semester", exam.semester)
                        .putExtra("boldText", search)
                )
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
        lateinit var parentView: LinearLayout
        lateinit var listItemImg: ImageView
        lateinit var listItemTitle: TextView
        lateinit var listItemSubtitle1: TextView
        lateinit var listItemSubtitle2: TextView

        init {
            when {
                isItem -> {
                    parentView = itemView as LinearLayout
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

class ItemDecoration(
    context: Context,
    private val examList: RecyclerView,
) : RecyclerView.ItemDecoration() {
    private val divider: Drawable?
    private val roundedCorner: SeslSubheaderRoundedCorner
    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val holder: ExamAdapter.ViewHolder = examList.getChildViewHolder(child) as ExamAdapter.ViewHolder
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
            val holder: ExamAdapter.ViewHolder = examList.getChildViewHolder(child) as ExamAdapter.ViewHolder
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
