package com.example.geofications

import android.app.job.JobParameters
import android.app.job.JobService
import com.example.geofications.data.GeoficationDao
import com.example.geofications.data.GeoficationDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Job service to update Geofication's parameters in database
 */
class UpdateGeoficationJobService : JobService() {

    // Params to pass to jobFinished()
    private lateinit var params: JobParameters

    // Scope for coroutine in which database is updating
    private val supervisorJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + supervisorJob)

    private val INTENT_ACTION_DATE_TIME = "datetime"
    private val INTENT_ACTION_COMPLETED = "completed"

    override fun onStartJob(jobParameters: JobParameters): Boolean {
        params = jobParameters

        val intentAction = jobParameters.extras.getString("intentAction")
        val id = jobParameters.extras.getLong("id")

        // DAO
        val dataSource = GeoficationDatabase.getInstance(applicationContext).geoficationDAO

        when (intentAction) {
            // Update isDateTimeAlarmSet
            INTENT_ACTION_DATE_TIME -> {
                val isNotificationSet = jobParameters.extras.getBoolean("isNotificationSet")
                // Updating the database
                serviceScope.launch {
                    updateIsDateTimeAlarmSetInDb(dataSource, id, isNotificationSet)
                }

                // True because database updates in background
                return true
            }
            // Update isCompleted state of geofication
            INTENT_ACTION_COMPLETED -> {
                val isCompleted = jobParameters.extras.getBoolean("isCompleted")
                // Updating the database
                serviceScope.launch {
                    updateIsCompletedInDb(dataSource, id, isCompleted)
                }

                // True because database updates in background
                return true
            }
            else -> return false
        }
    }

    // Returning false means we want to end this job entirely right now and onStartJob() won't be called again
    override fun onStopJob(jobParameters: JobParameters): Boolean {
        return false
    }

    /**
     * Set isTimeNotificationSet in database
     */
    private suspend fun updateIsDateTimeAlarmSetInDb(data: GeoficationDao, id: Long, isTimeNotificationSet: Boolean) {
        withContext(Dispatchers.IO) {
            data.updateIsTimeNotificationSetStatus(id, isTimeNotificationSet)
            // Notify the system when our work is finished, so that it can release the resources. It is used when "true" is returned from onStartJob
            jobFinished(params, false)
        }
    }

    private suspend fun updateIsCompletedInDb(data: GeoficationDao, id: Long, isCompleted: Boolean) {
        withContext(Dispatchers.IO) {
            data.updateCompleted(id, isCompleted)
            // Notify the system when our work is finished, so that it can release the resources. It is used when "true" is returned from onStartJob
            jobFinished(params, false)
        }
    }

    /**
     * Cancel coroutine when called
     */
    override fun onDestroy() {
        super.onDestroy()
        supervisorJob.cancel()
    }
}