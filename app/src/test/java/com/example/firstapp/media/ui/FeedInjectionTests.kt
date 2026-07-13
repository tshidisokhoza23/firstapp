package com.example.firstapp.media.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.ArrayList

class FeedInjectionTests {

    @Test
    fun `inject ads logic correctly adds ad objects after every 3 items`() {
        val items = ArrayList<Int>()
        for (i in 1..10) items.add(i)
        val result = ArrayList<Any>()
        
        var index = 0
        while (index < items.size) {
            result.add(items.get(index))
            if ((index + 1) % 3 == 0) {
                result.add("AD")
            }
            index++
        }
        
        // Items: 1, 2, 3, AD, 4, 5, 6, AD, 7, 8, 9, AD, 10
        assertEquals(13, result.size)
        assertEquals("AD", result.get(3))
        assertEquals("AD", result.get(7))
        assertEquals("AD", result.get(11))
    }
}
