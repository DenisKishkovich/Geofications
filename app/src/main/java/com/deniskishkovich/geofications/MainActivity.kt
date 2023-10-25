package com.deniskishkovich.geofications

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Lay out app behind the system bars (for App Bar styling)
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    /*TODO 1.request notification permission
      TODO 2. check alarmManager if device restarts
      TODO 3. adjust style material3
      TODO 4. time selection dialog rotation bug
    */
}