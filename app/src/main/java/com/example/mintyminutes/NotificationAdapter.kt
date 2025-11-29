package com.example.mintyminutes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotificationAdapter(
    private val notifications: MutableList<Notification>,
    private val onDeleteClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val notificationTitle: TextView = view.findViewById(R.id.notificationTitle)
        val notificationMessage: TextView = view.findViewById(R.id.notificationMessage)
        val deleteBtn: ImageButton = view.findViewById(R.id.deleteBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.notificationTitle.text = notification.title
        holder.notificationMessage.text = notification.message

        holder.deleteBtn.setOnClickListener {
            onDeleteClick(notification)
        }
    }

    override fun getItemCount() = notifications.size

    fun removeItem(notification: Notification) {
        val position = notifications.indexOf(notification)
        if (position != -1) {
            notifications.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}