package com.example.mintyminutes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ScheduleAdapter(
    private val schedules: MutableList<Schedule>,
    private val onEditClick: (Schedule) -> Unit,
    private val onDeleteClick: (Schedule) -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    class ScheduleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val scheduleTitle: TextView = view.findViewById(R.id.scheduleTitle)
        val scheduleTime: TextView = view.findViewById(R.id.scheduleTime)
        val editBtn: ImageButton = view.findViewById(R.id.editBtn)
        val deleteBtn: ImageButton = view.findViewById(R.id.deleteBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val schedule = schedules[position]
        holder.scheduleTitle.text = schedule.title
        holder.scheduleTime.text = schedule.time

        holder.editBtn.setOnClickListener {
            onEditClick(schedule)
        }

        holder.deleteBtn.setOnClickListener {
            onDeleteClick(schedule)
        }
    }

    override fun getItemCount() = schedules.size

    fun removeItem(schedule: Schedule) {
        val position = schedules.indexOf(schedule)
        if (position != -1) {
            schedules.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun addItem(schedule: Schedule) {
        schedules.add(schedule)
        notifyItemInserted(schedules.size - 1)
    }
}