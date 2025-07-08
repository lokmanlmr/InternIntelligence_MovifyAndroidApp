package com.example.Movify

import ActivityCommsViewModel
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.example.Movify.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    private lateinit var connectivityManager: ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val activityCommsViewModel: ActivityCommsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Apply window insets for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize ConnectivityManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Initial check
        updateNoNetworkLayoutVisibility(isNetworkAvailable())

        // Set up the retry button click listener
        binding.noNetworkLayout.findViewById<View>(R.id.btn_retry).setOnClickListener {
            val networkNowAvailable = isNetworkAvailable()
            updateNoNetworkLayoutVisibility(networkNowAvailable) // Update UI first

            if (networkNowAvailable) {
                Log.d("RetryButton", "Network is available. Triggering retry action.")
                activityCommsViewModel.triggerRetryNetworkAction()
            } else {
                Log.d("RetryButton", "Network still not available.")
                // Optionally show a Toast here if you want to give immediate feedback
                // Toast.makeText(this, "Network still unavailable", Toast.LENGTH_SHORT).show()
            }
        }


        // --- Navigation Component Setup ---
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragment_container) as NavHostFragment
        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_search, R.id.nav_saved, R.id.nav_profile
            )
        )

        NavigationUI.setupWithNavController(binding.bottomNavigation, navController)

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val currentGraphOwnerIdOnClick = findCurrentTopLevelDestination(
                navController,
                appBarConfiguration.topLevelDestinations
            )

            if (currentGraphOwnerIdOnClick == item.itemId && navController.currentDestination?.id != item.itemId) {

            }
            else if (navController.currentDestination?.id == item.itemId) {
                if (!item.isChecked) {
                    item.isChecked = true
                }
                return@setOnItemSelectedListener true
            }

            val navigated = NavigationUI.onNavDestinationSelected(item, navController)

            if (navigated) {
                Log.d(
                    "BNV_CLICK",
                    "NavigationUI.onNavDestinationSelected handled navigation for ${item.title}."
                )
                // The addOnDestinationChangedListener below will ultimately ensure the correct visual state.
            } else {
                Log.d(
                    "BNV_CLICK",
                    "NavigationUI.onNavDestinationSelected did NOT handle navigation for ${item.title}."
                )
                if (currentGraphOwnerIdOnClick == item.itemId && !item.isChecked) {
                    Log.d(
                        "BNV_CLICK",
                        "Item ${item.title} is owner, but not checked. DestinationChangedListener should fix."
                    )
                }
            }
            return@setOnItemSelectedListener true
        }

        navController.addOnDestinationChangedListener { controller, destination, _ ->

            var finalOwnerId = 0
            var tempDestination: NavDestination? = destination

            while (tempDestination != null) {
                if (appBarConfiguration.topLevelDestinations.contains(tempDestination.id)) {
                    finalOwnerId = tempDestination.id
                    break
                }
                tempDestination = tempDestination.parent
            }

            if (finalOwnerId != 0) {
                val menuItemToSelect = binding.bottomNavigation.menu.findItem(finalOwnerId)
                if (menuItemToSelect != null) {
                    if (!menuItemToSelect.isChecked) {
                        menuItemToSelect.isChecked = true
                    }
                }
            } else {
                if (appBarConfiguration.topLevelDestinations.contains(destination.id)) {
                    val directMenuItem = binding.bottomNavigation.menu.findItem(destination.id)
                    if (directMenuItem != null && !directMenuItem.isChecked) {
                        directMenuItem.isChecked = true
                    }
                }
            }
        }
    }


    private fun isNetworkAvailable(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    private fun updateNoNetworkLayoutVisibility(isConnected: Boolean) {
        if (isConnected) {
            binding.noNetworkLayout.visibility = View.GONE
            // Optional: Make your main content visible if it was hidden
            binding.fragmentContainer.visibility = View.VISIBLE
            binding.bottomNavigation.visibility = View.VISIBLE
        } else {
            binding.noNetworkLayout.visibility = View.VISIBLE
            // Optional: Hide your main content when no network layout is shown
            // to avoid overlaps or showing stale data prominently.
            binding.fragmentContainer.visibility = View.GONE
            binding.bottomNavigation.visibility = View.GONE

        }
    }

    private fun registerNetworkCallback() {
        if (networkCallback == null) { // Ensure callback is only registered once
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    runOnUiThread {
                        updateNoNetworkLayoutVisibility(true)
                    }
                }
                override fun onLost(network: Network) {
                    super.onLost(network)
                    runOnUiThread {
                        updateNoNetworkLayoutVisibility(false)
                    }
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    // You might want to re-evaluate connectivity here as well,
                    // for example, if the network changes from metered to unmetered.
                    // For simply showing/hiding the layout, onAvailable/onLost are often sufficient.
                    runOnUiThread {
                        updateNoNetworkLayoutVisibility(isNetworkAvailable())
                    }
                }
            }

            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
        }
    }

    private fun unregisterNetworkCallback() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: IllegalArgumentException) {
                // Handle the case where the callback might have already been unregistered
                // or was never registered. This can happen on configuration changes or quick lifecycle events.
                // Log.w("MainActivity", "Network callback already unregistered or not valid.")
            }
            networkCallback = null // Reset the callback
        }
    }


    private fun findCurrentTopLevelDestination(
        navController: NavController,
        topLevelDestinationIds: Set<Int>
    ): Int {
        var currentDestination: NavDestination? = navController.currentDestination
        while (currentDestination != null) {
            if (topLevelDestinationIds.contains(currentDestination.id)) {
                return currentDestination.id
            }
            currentDestination = currentDestination.parent
        }
        return 0
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(
            navController,
            appBarConfiguration
        ) || super.onSupportNavigateUp()
    }

    override fun onResume() {
        super.onResume()
        registerNetworkCallback()
        // Re-check when resuming, in case state changed while paused
        updateNoNetworkLayoutVisibility(isNetworkAvailable())
    }

    override fun onPause() {
        super.onPause()
        unregisterNetworkCallback()
    }
}