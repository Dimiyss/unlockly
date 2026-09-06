package com.arhiplabs.unstuckly

import com.arhiplabs.unstuckly.data.db.Converters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConvertersTest {

    private lateinit var converters: Converters

    @Before
    fun setUp() {
        converters = Converters()
    }

    @Test
    fun testFromListToString_withValidList() {
        val list = listOf("com.example.app1", "com.example.app2", "com.example.app3")
        val result = converters.fromListToString(list)
        assertEquals("com.example.app1,com.example.app2,com.example.app3", result)
    }

    @Test
    fun testFromListToString_withEmptyList() {
        val list = emptyList<String>()
        val result = converters.fromListToString(list)
        assertEquals("", result)
    }

    @Test
    fun testFromListToString_withNullList() {
        val result = converters.fromListToString(null)
        assertEquals("", result)
    }

    @Test
    fun testFromStringToList_withValidCommaSeparatedString() {
        val data = "com.example.app1,com.example.app2,com.example.app3"
        val result = converters.fromStringToList(data)
        assertEquals(3, result.size)
        assertEquals("com.example.app1", result[0])
        assertEquals("com.example.app2", result[1])
        assertEquals("com.example.app3", result[2])
    }

    @Test
    fun testFromStringToList_withWhitespaceAndEmptyEntries() {
        val data = "  com.example.app1 , , com.example.app2  ,  "
        val result = converters.fromStringToList(data)
        assertEquals(2, result.size)
        assertEquals("com.example.app1", result[0])
        assertEquals("com.example.app2", result[1])
    }

    @Test
    fun testFromStringToList_withEmptyString() {
        val result = converters.fromStringToList("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun testFromStringToList_withNullString() {
        val result = converters.fromStringToList(null)
        assertTrue(result.isEmpty())
    }

    @Test
    fun testBidirectionalConversion() {
        val original = listOf("com.duolingo", "com.coursera", "com.khanacademy")
        val serialized = converters.fromListToString(original)
        val deserialized = converters.fromStringToList(serialized)
        assertEquals(original, deserialized)
    }
}
