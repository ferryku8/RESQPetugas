package com.uxonauts.resqadmin.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL

data class RouteResult(
    val points: List<GeoPoint>,
    val distanceMeters: Double,
    val durationSeconds: Double
)

object RoutingHelper {
    suspend fun getRoute(
        startLat: Double, startLng: Double,
        endLat: Double, endLng: Double
    ): RouteResult? = withContext(Dispatchers.IO) {
        try {
            val urlStr = "https://router.project-osrm.org/route/v1/driving/" +
                    "$startLng,$startLat;$endLng,$endLat?overview=full&geometries=geojson"
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val routes = json.getJSONArray("routes")
            if (routes.length() == 0) return@withContext null

            val route = routes.getJSONObject(0)
            val distance = route.getDouble("distance")
            val duration = route.getDouble("duration")
            val geometry = route.getJSONObject("geometry")
            val coordinates = geometry.getJSONArray("coordinates")

            val points = mutableListOf<GeoPoint>()
            for (i in 0 until coordinates.length()) {
                val coord = coordinates.getJSONArray(i)
                val lng = coord.getDouble(0)
                val lat = coord.getDouble(1)
                points.add(GeoPoint(lat, lng))
            }

            RouteResult(points, distance, duration)
        } catch (e: Exception) {
            null
        }
    }

    fun formatDuration(seconds: Double): String {
        val mins = (seconds / 60).toInt()
        return when {
            mins < 1 -> "< 1 menit"
            mins < 60 -> "$mins menit"
            else -> {
                val hours = mins / 60
                val remMins = mins % 60
                "${hours}j ${remMins}m"
            }
        }
    }

    fun formatDistance(meters: Double): String {
        return if (meters < 1000) "${meters.toInt()} m"
        else String.format("%.1f km", meters / 1000)
    }
}