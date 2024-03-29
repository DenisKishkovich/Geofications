package com.deniskishkovich.geofications.data

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
    var isCompleted: Boolean = false,

    @ColumnInfo
    var createdTimestamp: Long = System.currentTimeMillis(),

    @ColumnInfo
    var editedTimestamp: Long = System.currentTimeMillis(),

    @ColumnInfo
    var timestampToNotify: Long? = null,

    @ColumnInfo
    var isTimeNotificationSet: Boolean = false,

    @ColumnInfo(name = "latitude_to_notify")
    var latitude: Double? = null,

    @ColumnInfo(name = "longitude_to_notify")
    var longitude: Double? = null,

    @ColumnInfo
    var locationString: String? = null,

    @ColumnInfo
    var isLocationNotificationSet: Boolean = false
) {
    val isEmpty
        get() = title.isEmpty() && description.isEmpty()
}