package com.example.mintyminutes

data class Notification(
    val id: Int = 0,
    val title: String = "",
    val message: String = ""
) {
    // No-argument constructor for Firebase
    constructor() : this(0, "", "")
}