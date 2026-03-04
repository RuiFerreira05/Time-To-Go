package com.timetogo.app.data.remote.dto

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for Moshi deserialization of [RoutesResponse] and nested DTOs.
 * Uses a real-world response captured from the Google Routes API.
 */
class RoutesResponseDeserializationTest {

    private lateinit var moshi: Moshi

    @Before
    fun setUp() {
        moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    @Test
    fun `parse real API response correctly`() {
        val json = """
        {
          "routes": [
            {
              "legs": [
                {
                  "duration": "1270s",
                  "steps": [
                    { "staticDuration": "13s", "travelMode": "WALK" },
                    { "staticDuration": "91s", "travelMode": "WALK" },
                    { "staticDuration": "283s", "travelMode": "WALK" },
                    {
                      "staticDuration": "625s",
                      "travelMode": "TRANSIT",
                      "transitDetails": {
                        "stopDetails": {
                          "departureStop": { "name": "Lg. Frei Heitor Pinto" },
                          "arrivalStop": { "name": "R. Cons. Emídio Navarro (ISEL)" },
                          "departureTime": "2026-03-03T22:52:40Z",
                          "arrivalTime": "2026-03-03T23:04:18Z"
                        },
                        "headsign": "Poço Bispo",
                        "transitLine": {
                          "name": "Poço Bispo - Sete Rios",
                          "nameShort": "755",
                          "vehicle": { "name": { "text": "Ônibus" }, "type": "BUS" }
                        },
                        "stopCount": 15
                      }
                    },
                    { "staticDuration": "79s", "travelMode": "WALK" }
                  ]
                }
              ],
              "duration": "1378s"
            }
          ]
        }
        """.trimIndent()

        val adapter = moshi.adapter(RoutesResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertNotNull(response!!.routes)
        assertEquals(1, response.routes!!.size)

        val route = response.routes!![0]
        assertEquals("1378s", route.duration)

        val leg = route.legs!![0]
        assertEquals("1270s", leg.duration)
        assertEquals(5, leg.steps!!.size)

        // Verify WALK steps
        assertEquals("WALK", leg.steps!![0].travelMode)
        assertEquals("13s", leg.steps!![0].staticDuration)

        // Verify TRANSIT step
        val transitStep = leg.steps!![3]
        assertEquals("TRANSIT", transitStep.travelMode)
        assertEquals("625s", transitStep.staticDuration)

        val details = transitStep.transitDetails!!
        assertEquals("Poço Bispo", details.headsign)
        assertEquals(15, details.stopCount)
        assertEquals("755", details.transitLine!!.nameShort)
        assertEquals("Lg. Frei Heitor Pinto", details.stopDetails!!.departureStop!!.name)
        assertEquals("R. Cons. Emídio Navarro (ISEL)", details.stopDetails!!.arrivalStop!!.name)
    }

    @Test
    fun `empty routes array parses correctly`() {
        val json = """{ "routes": [] }"""
        val adapter = moshi.adapter(RoutesResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertTrue(response!!.routes!!.isEmpty())
    }

    @Test
    fun `missing optional fields default to null`() {
        val json = """{ }"""
        val adapter = moshi.adapter(RoutesResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertNull(response!!.routes)
    }

    @Test
    fun `step with no transit details parses correctly`() {
        val json = """
        {
          "routes": [{
            "legs": [{
              "steps": [{ "staticDuration": "60s", "travelMode": "WALK" }]
            }],
            "duration": "60s"
          }]
        }
        """.trimIndent()

        val adapter = moshi.adapter(RoutesResponse::class.java)
        val response = adapter.fromJson(json)

        val step = response!!.routes!![0].legs!![0].steps!![0]
        assertEquals("WALK", step.travelMode)
        assertEquals("60s", step.staticDuration)
        assertNull(step.transitDetails)
    }

    @Test
    fun `transit line with only long name parses correctly`() {
        val json = """
        {
          "routes": [{
            "legs": [{
              "steps": [{
                "travelMode": "TRANSIT",
                "staticDuration": "300s",
                "transitDetails": {
                  "transitLine": { "name": "Some Long Bus Name" },
                  "stopCount": 5
                }
              }]
            }],
            "duration": "300s"
          }]
        }
        """.trimIndent()

        val adapter = moshi.adapter(RoutesResponse::class.java)
        val response = adapter.fromJson(json)

        val transitLine = response!!.routes!![0].legs!![0].steps!![0].transitDetails!!.transitLine!!
        assertEquals("Some Long Bus Name", transitLine.name)
        assertNull(transitLine.nameShort)
    }
}
