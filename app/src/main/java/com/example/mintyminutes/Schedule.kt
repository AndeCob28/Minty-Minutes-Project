package com.example.mintyminutes

data class Schedule(
    val id: Int = 0,
    val title: String = "",
    val time: String = ""
) {
    // No-argument constructor for Firebase
    constructor() : this(0, "", "")
}