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
}