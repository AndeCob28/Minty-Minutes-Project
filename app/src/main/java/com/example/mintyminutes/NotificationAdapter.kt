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
        if (position >= notifications.size) {
            println("DEBUG NotificationAdapter: Invalid position $position, list size: ${notifications.size}")
            return
        }

        val notification = notifications[position]
        println("DEBUG NotificationAdapter: Binding notification at position $position - ${notification.title}: ${notification.message}")

        holder.notificationTitle.text = notification.title
        holder.notificationMessage.text = notification.message

        holder.deleteBtn.setOnClickListener {
            println("DEBUG NotificationAdapter: Delete clicked for notification: ${notification.title}")
            onDeleteClick(notification)
        }
    }

    override fun getItemCount(): Int {
        println("DEBUG NotificationAdapter: getItemCount called, returning ${notifications.size}")
        return notifications.size
    }

    fun removeItem(notification: Notification) {
        val position = notifications.indexOf(notification)
        if (position != -1) {
            notifications.removeAt(position)
            notifyItemRemoved(position)
            println("DEBUG NotificationAdapter: Removed item at position $position")
        }
    }
}