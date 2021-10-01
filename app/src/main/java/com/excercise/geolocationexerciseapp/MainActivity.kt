package com.excercise.geolocationexerciseapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.IntentSender
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.excercise.geolocationexerciseapp.databinding.ActivityMainBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.permissionx.guolindev.PermissionX

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var mLocation: Location
    private var isAcceptPermission = false

    private val UPDATE_INTERVAL = (10 * 1000 /* 10 secs */).toLong()
    private val FASTEST_INTERVAL: Long = 2000 /* 2 sec */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupPermission()

        if (isAcceptPermission) {
            fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(binding.root.context)
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(p0: LocationResult) {
                    super.onLocationResult(p0)
                    onNewDataLocation(p0.lastLocation)
                }
            }
            setupLocationRequest()

            let {
                val builder = LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest)
                val taskService = LocationServices.getSettingsClient(binding.root.context)
                    .checkLocationSettings(builder.build())

                taskService.addOnCompleteListener { result ->
                    try {
                        startLocationUpdates()
                        getLastLocation()
                    } catch (e: Exception) {
                        try {
                            Log.d("Error->", e.cause.toString())
                            val resolvableException = e.cause as? ResolvableApiException
                            if (resolvableException === null) {
                                return@addOnCompleteListener
                            } else {
                                when (resolvableException.statusCode) {
                                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                                        try {
                                            resolvableException.startResolutionForResult(
                                                this,
                                                LOCATION_SETTING_REQUEST
                                            )
                                            resolutionForResult.launch(
                                                IntentSenderRequest.Builder(resolvableException.resolution)
                                                    .build()
                                            )
                                        } catch (sendEx: IntentSender.SendIntentException) {
                                            Log.d("BUG->", sendEx.message.toString())
                                        }
                                    }
                                    else -> {
                                        Log.d("ERROR->", "HELL NO")
                                    }
                                }
                            }
                        } catch (sendEx: IntentSender.SendIntentException) {
                            Log.d("BUG->", sendEx.message.toString())
                        }
                    }
                }
            }
        }
    }

    private fun setupPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PermissionX.init(this)
                .permissions(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
                .onExplainRequestReason { scope, deniedList ->
                    scope.showRequestReasonDialog(
                        deniedList,
                        "Aplikasi tidak dapat dijalankan apabila izin ini ditolak",
                        "Ok",
                        "Batal"
                    )
                }
                .onForwardToSettings { scope, deniedList ->
                    scope.showForwardToSettingsDialog(
                        deniedList,
                        "Anda harus mengaktifkan izin ini secara manual dari Pengaturan",
                        "Ok",
                        "Batal"
                    )
                }
                .request { allGranted, grantedList, deniedList ->
                    if (allGranted) {
                        isAcceptPermission = true
                    } else {
                        this.finishAffinity()
                    }
                }
        } else {
            PermissionX.init(this)
                .permissions(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                .onExplainRequestReason { scope, deniedList ->
                    scope.showRequestReasonDialog(
                        deniedList,
                        "Aplikasi tidak dapat dijalankan apabila izin ini ditolak",
                        "Ok",
                        "Batal"
                    )
                }
                .onForwardToSettings { scope, deniedList ->
                    scope.showForwardToSettingsDialog(
                        deniedList,
                        "Anda harus mengaktifkan izin ini secara manual dari Pengaturan",
                        "Ok",
                        "Batal"
                    )
                }
                .request { allGranted, grantedList, deniedList ->
                    if (allGranted) {
                        isAcceptPermission = true
                    } else {
                        this.finishAffinity()
                    }
                }
        }
    }

    private fun onNewDataLocation(locationData: Location) {
        val geoCoder = Geocoder(binding.root.context)
        Log.d("CURRENTLOCATION->", locationData.toString())
        val currentLocation =
            geoCoder.getFromLocation(
                locationData.latitude,
                locationData.longitude,
                1
            )

        binding.txtLocation.text = currentLocation.first().toString()
        Log.d("LOCATIONDATA->", locationData.toString())
        removeLocationUpdate()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun setupLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = UPDATE_INTERVAL
            fastestInterval = FASTEST_INTERVAL
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        try {
            fusedLocationClient.lastLocation.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    mLocation = task.result
                } else {
                    Log.w("GetLastLocation->", "Failed to get Location.")
                }
            }
        } catch (e: SecurityException) {
            Log.e("GetLastLocation->", "Lost location permission.$e");
        }
    }

    private fun removeLocationUpdate() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private val resolutionForResult =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    startLocationUpdates()
                    getLastLocation()
                    Toast.makeText(binding.root.context, "GPS Diaktifkan..", Toast.LENGTH_SHORT)
                        .show()
                }
                Activity.RESULT_CANCELED -> {
                    Toast.makeText(binding.root.context, "GPS Ditolak..", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

    companion object {
        const val LOCATION_SETTING_REQUEST = 999
    }
}