package com.tpsk.tj.drive.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import com.tpsk.tj.drive.domain.repository.DriveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class AreaPresenceViewModel @Inject constructor(
    private val repository: DriveRepository
) : ViewModel() {
    private var job: Job? = null

    fun start(context: Context) {
        if (job?.isActive == true) return
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) return
        val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)
        job = viewModelScope.launch {
            while (isActive) {
                runCatching {
                    val location = withContext(Dispatchers.IO) {
                        val last = Tasks.await(client.lastLocation)
                        if (last != null && System.currentTimeMillis() - last.time < 15 * 60 * 1000) last
                        else Tasks.await(client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null))
                    }
                    if (location != null) {
                        repository.reportPresence(location.latitude, location.longitude).collect { }
                    }
                }
                delay(60_000)
            }
        }
    }
}
