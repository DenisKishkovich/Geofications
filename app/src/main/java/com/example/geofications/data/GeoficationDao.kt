package com.example.geofications.data

import androidx.room.*

@Dao
interface GeoficationDao {

    /**
     * Select all geofications from table
     */
    @Query("SELECT * FROM geofications_table")
    suspend fun getAllGeofications(): List<Geofication>

    /**
     * Select geofication by id
     */
    @Query("Select * FROM geofications_table WHERE id = :geoficationId")
    suspend fun getGeoficationById(geoficationId: Long): Geofication?

    /**
     * Insert geof. in database. If it exists, replace.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeofication(geofication: Geofication)

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
}