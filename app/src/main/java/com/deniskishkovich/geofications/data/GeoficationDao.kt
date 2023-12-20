package com.deniskishkovich.geofications.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface GeoficationDao {

    /**
     * Select all geofications from table
     */
    @Query("SELECT * FROM geofications_table ORDER BY editedTimestamp DESC")
    fun getAllGeofications(): LiveData<List<Geofication>>

    /**
     * Select geofication by id
     */
    @Query("Select * FROM geofications_table WHERE id = :geoficationId")
    suspend fun getGeoficationById(geoficationId: Long): Geofication?

    /**
     * Insert geof. in database. If it exists, replace.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeofication(geofication: Geofication): Long

    /**
     * Update a geof.
     * @return the number of geof updated. This should always be 1.
     */
    @Update
    suspend fun updateGeofication(geofication: Geofication): Int

    /**
     * Delete a geof. by id
     * @return the number of geof. deleted. This should always be 1.
     */
    @Query("DELETE FROM geofications_table WHERE id = :geoficationId")
    suspend fun deleteGeoficationById(geoficationId: Long): Int

    /**
     * Delete all geof.
     * @return the number of geof. deleted
     */
    @Query("DELETE FROM geofications_table")
    suspend fun deleteAllGeofications()

    /**
     * Update completed state of a geof.
     */
    @Query("UPDATE geofications_table SET isCompleted = :isCompleted WHERE id = :geoficationId")
    suspend fun updateCompleted(geoficationId: Long, isCompleted: Boolean)

    /**
     * Update date and time notification's params
     */
    @Query("UPDATE geofications_table SET isTimeNotificationSet = :isTimeNotificationSet, timestampToNotify = :timeInMillis WHERE id = :geoficationId")
    suspend fun updateDateTimeNotificationStatus(
        geoficationId: Long,
        isTimeNotificationSet: Boolean,
        timeInMillis: Long?
    )

    /**
     * Update date and time notification's set status
     */
    @Query("UPDATE geofications_table SET isTimeNotificationSet = :isTimeNotificationSet WHERE id = :geoficationId")
    suspend fun updateIsTimeNotificationSetStatus(
        geoficationId: Long,
        isTimeNotificationSet: Boolean
    )

    /**
     * Update location notification's params
     */
    @Query("UPDATE geofications_table SET isLocationNotificationSet = :isLocationNotificationSet, latitude_to_notify = :latitude, longitude_to_notify = :longitude, locationString = :locationString WHERE id = :geoficationId")
    suspend fun updateLocationNotificationStatus(
        geoficationId: Long,
        isLocationNotificationSet: Boolean,
        latitude: Double?,
        longitude: Double?,
        locationString: String?
    )

    /**
     * Select all geofications from table
     */
    @Query("SELECT * FROM geofications_table ORDER BY editedTimestamp DESC")
    suspend fun getAllGeoficationsSuspend(): List<Geofication>
}