package com.example.geofications

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup app bar navigate up
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return when (navController.currentDestination?.id) {
            // NavigateUp behavior as back pressed in GeoficationDetailsFragment
            R.id.geoficationDetailsFragment -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
        }
    }

    /*TODO 1.request notification permission
      TODO 2. check alarmManager if device restarts
      TODO 3. переделать диалоги в material
    */
}