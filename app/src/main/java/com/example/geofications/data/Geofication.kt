package com.example.geofications.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "geofications_table")
data class Geofication(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,

    @ColumnInfo(name = "geof_title")
    var title: String = "",

    @ColumnInfo(name = "geof_description")
    var description: String = "",

    @ColumnInfo
    var isCompleted: Boolean = false
) {
    val isEmpty
        get() = title.isEmpty() && description.isEmpty()
}