package com.example.geofications

import android.app.job.JobParameters
import android.app.job.JobService
import android.widget.Toast
import com.example.geofications.data.GeoficationDao
import com.example.geofications.data.GeoficationDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UpdateGeoficationJobService : JobService() {

    private val supervisorJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + supervisorJob)
    private lateinit var params: JobParameters

    override fun onStartJob(jobParameters: JobParameters): Boolean {
        params = jobParameters
        val id = jobParameters.extras.getLong("id")
        val isNotificationSet = jobParameters.extras.getBoolean("isNotificationSet")

        val dataSource = GeoficationDatabase.getInstance(applicationContext).geoficationDAO

        serviceScope.launch {
            updateIsDateTimeAlarmSetInDb(dataSource, id, isNotificationSet)
        }

        //Toast.makeText(applicationContext, id.toString(), Toast.LENGTH_SHORT).show()
        return false
    }

    override fun onStopJob(jobParameters: JobParameters): Boolean {
        return false
    }

    private suspend fun updateIsDateTimeAlarmSetInDb(data: GeoficationDao, id: Long, isTimeNotificationSet: Boolean) {
        withContext(Dispatchers.IO) {
            data.updateIsTimeNotificationSetStatus(id, isTimeNotificationSet)
            jobFinished(params, false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        supervisorJob.cancel()
    }
}