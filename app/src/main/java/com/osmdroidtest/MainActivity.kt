package com.osmdroidtest

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

const val GRAPHHOPPER_APIKEY = "51e710c5-c9ea-493c-a466-8e85a4c60d69"

class MainActivity : AppCompatActivity() {
    private lateinit var map: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkPermission()) { requestPermission() }
        getInstance().load(this, getPreferences(MODE_PRIVATE))
        setContentView(R.layout.activity_main)
        map = findViewById(R.id.map)
        map.setMultiTouchControls(true)
        configureTileSource()
        setupLocationOverlay()
    }

    // setup and use the tile server from CyclOSM
    private fun configureTileSource() {
        val cyclOSMTileSource = XYTileSource(
            "CyclOSM", 0, 20, 256, ".png",
            arrayOf(
                "https://a.tile-cyclosm.openstreetmap.fr/cyclosm/",
                "https://b.tile-cyclosm.openstreetmap.fr/cyclosm/",
                "https://c.tile-cyclosm.openstreetmap.fr/cyclosm/"
            )
        )
        map.setTileSource(cyclOSMTileSource)
    }

    // setup and activate the location overlay + fetch a test route
    private fun setupLocationOverlay() {
        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        locationOverlay.enableMyLocation()
        locationOverlay.runOnFirstFix {
            runOnUiThread {
                val myLocation: Location? = locationOverlay.lastFix
                if (myLocation != null) {
                    val myGeoPoint = GeoPoint(myLocation.latitude, myLocation.longitude)
                    val mapController = map.controller
                    mapController.setZoom(15.0)
                    mapController.animateTo(myGeoPoint)
                }
                getRoute(myLocation!!.latitude, myLocation.longitude, 34.0549, -118.2426, "racingbike", GRAPHHOPPER_APIKEY) { response ->
                    response?.let {
                        try {
                            runOnUiThread {
                                renderPolylineRoute(map, responseToGeopoints(it))
                            }
                        } catch (e: Exception) {
                            Log.e("RouteRendering", "Error rendering route", e)
                        }
                    }
                }
            }
        }
        map.overlays.add(locationOverlay)
    }

    // check location permissions
    private fun checkPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED)
    }

    // request location permissions
    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            1
        )
    }

    // handle permissions result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // All permissions granted
            } else {
                // Permissions denied
                val deniedPermissions = permissions.filterIndexed { index, _ ->
                    grantResults[index] == PackageManager.PERMISSION_DENIED
                }

                if (deniedPermissions.any { ActivityCompat.shouldShowRequestPermissionRationale(this, it) }) {
                    // Show an explanation to the user
                    requestPermission()
                } else {
                    // User has denied permissions and chosen "Don't ask again"
                    // Inform the user that they need to go to settings to enable permissions
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}
