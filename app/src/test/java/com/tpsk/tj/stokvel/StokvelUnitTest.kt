package com.tpsk.tj.stokvel

import org.junit.Assert.assertEquals
import org.junit.Test

class StokvelUnitTest {

    private fun maskVoucherSerial(serial: String): String {
        if (serial.length < 7) return "xxxx"
        return serial.substring(0, 4) + "x".repeat(serial.length - 7) + serial.substring(serial.length - 3)
    }

    @Test
    fun `test 1Voucher serial PIN masking logic`() {
        val rawSerial = "1234567890123456"
        val expectedMasked = "1234xxxxxxxxx456"
        val masked = maskVoucherSerial(rawSerial)

        assertEquals(expectedMasked, masked)
    }

    @Test
    fun `test short voucher serial handling`() {
        val shortSerial = "12345"
        val masked = maskVoucherSerial(shortSerial)

        assertEquals("xxxx", masked)
    }
}
