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

    override fun onStartJob(jobParameters: JobParameters): Boolean {
        params = jobParameters

        val id = jobParameters.extras.getLong("id")
        val isNotificationSet = jobParameters.extras.getBoolean("isNotificationSet")

        // DAO
        val dataSource = GeoficationDatabase.getInstance(applicationContext).geoficationDAO

        // Updating the database
        serviceScope.launch {
            updateIsDateTimeAlarmSetInDb(dataSource, id, isNotificationSet)
        }
        // True because database updates in background
        return true
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

    /**
     * Cancel coroutine when called
     */
    override fun onDestroy() {
        super.onDestroy()
        supervisorJob.cancel()
    }
}