package com.osmdroidtest

import android.graphics.Color
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import java.io.IOException

// fetch a route from the Graphhopper API
fun getRoute(
    startLat: Double,
    startLon: Double,
    endLat: Double,
    endLon: Double,
    profile: String,
    apiKey: String,
    onResult: (JSONObject?) -> Unit
) {
    val client = OkHttpClient()
    val url = "https://graphhopper.com/api/1/route?point=${startLat},${startLon}&point=${endLat},${endLon}&profile=$profile&key=$apiKey&points_encoded=false"
    val request = Request.Builder().url(url).build()
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("RouteAPI", "Error: ${e.message}")
            onResult(null)
        }
        override fun onResponse(call: Call, response: Response) {
            val responseBody = response.body?.string()
            if (response.isSuccessful && responseBody != null) {
                try {
                    val jsonResponse = JSONObject(responseBody)
                    onResult(jsonResponse)
                } catch (e: Exception) {
                    Log.e("RouteAPI", "Parsing error: ${e.message}")
                    onResult(null)
                }
            } else {
                Log.e("RouteAPI", "Request failed: ${response.code}")
                onResult(null)
            }
        }
    })
}

// parse the response from the Graphhopper API into Geopoints
fun responseToGeopoints(response: JSONObject): List<GeoPoint> {
    val paths = response.getJSONArray("paths").getJSONObject(0)
    val points = paths.getJSONObject("points")
    val coordinates =  points.getJSONArray("coordinates")
    val geopoints = mutableListOf<GeoPoint>()
    for (i in 0 until coordinates.length()) {
        geopoints.add(GeoPoint(coordinates.getJSONArray(i).getDouble(1), coordinates.getJSONArray(i).getDouble(0)))
    }
    return geopoints
}

// render the route on an OSM map view
fun renderPolylineRoute(map: MapView, geopoints: List<GeoPoint>) {
    val routeOverlay = Polyline()
    routeOverlay.setPoints(geopoints)
    routeOverlay.outlinePaint.color = Color.RED
    routeOverlay.outlinePaint.strokeWidth = 5f
    map.overlays.add(routeOverlay)
    map.invalidate()
}