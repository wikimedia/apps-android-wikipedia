package org.wikipedia.yearinreview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import io.github.dellisd.spatialk.geojson.Feature
import io.github.dellisd.spatialk.geojson.FeatureCollection
import io.github.dellisd.spatialk.geojson.Point
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.not
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.wikipedia.WikipediaApp
import kotlin.random.Random

@Composable
fun YearInReviewGeoLocation(
    modifier: Modifier = Modifier
) {
    val cameraState = rememberCameraState()
    val testLocations = remember { generateTestLocations() }
    var shouldFindCluster by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        cameraState.position = CameraPosition(
            target = Position(0.0, 20.0),
            zoom = 0.0, // Lower zoom to see entire world
            bearing = 0.0,
            tilt = 0.0
        )

    }

    LaunchedEffect(shouldFindCluster) {
        if (shouldFindCluster) {
            println("orange finding largest cluster")
            delay(1500)
            findLargestVisibleCluster(cameraState)
            shouldFindCluster = false
        }
    }

    val assetForTheme = if (WikipediaApp.instance.currentTheme.isDark) "asset://mapstyle-dark.json" else "asset://mapstyle.json"

    MaplibreMap(
        modifier = modifier,
        cameraState = cameraState,
        baseStyle = BaseStyle.Demo,
        onMapLoadFinished = {
            shouldFindCluster = true
        }
    ) {
        val testDataSource = rememberGeoJsonSource(
            data = GeoJsonData.Features(createFeatureCollectionFromTestData(testLocations)),
            options = GeoJsonOptions(
                cluster = true,
                clusterRadius = 80,
                clusterMaxZoom = 10,
                clusterMinPoints = 3
            )
        )

        CircleLayer(
            id = "clustered-points",
            source = testDataSource,
            filter = feature.has("point_count"),
            color = const(Color.Red),        // All clusters = bright red
            radius = const(30.dp),           // All clusters = big size
            strokeWidth = const(3.dp),
            strokeColor = const(Color.White)
        )


        SymbolLayer(
            id = "clustered-text",
            source = testDataSource,
            filter = feature.has("point_count"),
            textField = feature["point_count_abbreviated"].asString(),
            textFont = const(listOf("Noto Sans Regular")),
            textColor = const(Color.White)
        )

        CircleLayer(
            id = "unclustered-points",
            source = testDataSource,
            filter = !feature.has("point_count"), // Points WITHOUT point_count
            color = const(Color.Blue),
            radius = const(8.dp),
            strokeWidth = const(2.dp),
            strokeColor = const(Color.White)
        )
    }
}


// Simple test data structure
data class TestLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

private suspend fun findLargestVisibleCluster(cameraState: CameraState) {
    try {
        val projection = cameraState.projection
        if (projection == null) {
            println("orange Projection is null")
            return
        }

        val screenRect = DpRect(
            left = 0.dp,
            top = 0.dp,
            right = 2000.dp,
            bottom = 2000.dp
        )
        val allFeatures = projection.queryRenderedFeatures(
            rect = screenRect,
            layerIds = null,
            predicate = const(true)
        )

        val clusterFeatures = allFeatures.filter { feature ->
            feature.properties.containsKey("point_count")
        }

        println("orange Found ${clusterFeatures.size} cluster features")

        clusterFeatures.forEach { feature ->
            val pointCount = feature.properties["point_count"]?.jsonPrimitive?.int ?: 0
            val geometry = feature.geometry
            if (geometry is Point) {
                val pos = geometry.coordinates
                println("orange Cluster: $pointCount points at lat=${pos.latitude}, lng=${pos.longitude}")
            }
        }

        if (clusterFeatures.isEmpty()) {
            println("orange No cluster features found")
            return
        }

        // Find the cluster with the highest point count
        var largestCluster: Feature? = null
        var maxPointCount = 0

        clusterFeatures.forEach { feature ->
            val pointCount = feature.properties["point_count"]?.jsonPrimitive?.int ?: 0
            println("orange Cluster with $pointCount points")

            if (pointCount > maxPointCount) {
                maxPointCount = pointCount
                largestCluster = feature
            }
        }

        if (largestCluster != null) {
            println("orange Found largest cluster with $maxPointCount points")

            val geometry = largestCluster.geometry
            if (geometry is Point) {
                val position = geometry.coordinates
                println("orange Largest cluster position: lat=${position.latitude}, lng=${position.longitude}")
                cameraState.animateTo(CameraPosition(
                    target = position,
                    zoom = 3.0,
                    bearing = 0.0,
                    tilt = 0.0
                ))
                delay(2000)
            }
        }

    }
    catch (e: Exception) {
        println("orange Error finding largest cluster: ${e.message}")
        e.printStackTrace()
    }
}

// Generate test data with clusters in different regions
private fun generateTestLocations(): List<TestLocation> {
    val locations = mutableListOf<TestLocation>()

    // Europe cluster
    repeat(50) {
        locations.add(
            TestLocation(
                name = "European Article $it",
                latitude = 48.0 + Random.nextDouble(-10.0, 15.0), // 38-63 latitude
                longitude = 10.0 + Random.nextDouble(-15.0, 25.0) // -5 to 35 longitude
            )
        )
    }

    // North America cluster
    repeat(250) {
        locations.add(
            TestLocation(
                name = "North American Article $it",
                latitude = 45.0 + Random.nextDouble(-15.0, 15.0), // 30-60 latitude
                longitude = -100.0 + Random.nextDouble(-25.0, 25.0) // -125 to -75 longitude
            )
        )
    }

    // Asia cluster
    repeat(20) {
        locations.add(
            TestLocation(
                name = "Asian Article $it",
                latitude = 30.0 + Random.nextDouble(-10.0, 20.0), // 20-50 latitude
                longitude = 110.0 + Random.nextDouble(-20.0, 30.0) // 90-140 longitude
            )
        )
    }

    // Scattered locations worldwide
    repeat(10) {
        locations.add(
            TestLocation(
                name = "Random Article $it",
                latitude = Random.nextDouble(-60.0, 60.0),
                longitude = Random.nextDouble(-180.0, 180.0)
            )
        )
    }

    return locations
}

// Convert to GeoJSON
private fun createFeatureCollectionFromTestData(locations: List<TestLocation>): FeatureCollection {
    val features = locations.map { location ->
        Feature(
            geometry = Point(Position(location.longitude, location.latitude)),
            properties = JsonObject(
                mapOf(
                    "name" to JsonPrimitive(location.name),
                    "type" to JsonPrimitive("article")
                )
            )
        )
    }
    return FeatureCollection(features)
}