package com.example.snappah.qr

import com.google.mlkit.vision.barcode.common.Barcode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.mockito.Mockito

class QrCodeParserTest {

    @Test
    fun testUrlParsing() {
        val barcode = Mockito.mock(Barcode::class.java)
        val urlBookmark = Mockito.mock(Barcode.UrlBookmark::class.java)
        Mockito.`when`(barcode.valueType).thenReturn(Barcode.TYPE_URL)
        Mockito.`when`(barcode.rawValue).thenReturn("https://www.github.com/cfeg/snappah")
        Mockito.`when`(barcode.url).thenReturn(urlBookmark)
        Mockito.`when`(urlBookmark.url).thenReturn("https://www.github.com/cfeg/snappah")

        val result = QrCodeParser.parse(barcode)
        assertEquals(QrType.URL, result.type)
        assertEquals("github.com", result.title)
        assertEquals("https://www.github.com/cfeg/snappah", result.subtitle)
        assertEquals("Open Link", result.actionLabel)
    }

    @Test
    fun testWifiParsing() {
        val barcode = Mockito.mock(Barcode::class.java)
        val wifi = Mockito.mock(Barcode.WiFi::class.java)
        Mockito.`when`(barcode.valueType).thenReturn(Barcode.TYPE_WIFI)
        Mockito.`when`(barcode.rawValue).thenReturn("WIFI:S:MyCafe;T:WPA;P:supersecret;;")
        Mockito.`when`(barcode.wifi).thenReturn(wifi)
        Mockito.`when`(wifi.ssid).thenReturn("MyCafe")
        Mockito.`when`(wifi.password).thenReturn("supersecret")
        Mockito.`when`(wifi.encryptionType).thenReturn(Barcode.WiFi.TYPE_WPA)

        val result = QrCodeParser.parse(barcode)
        assertEquals(QrType.WIFI, result.type)
        assertEquals("MyCafe", result.title)
        assertEquals("Connect", result.actionLabel)
        assertNotNull(result.wifiData)
        assertEquals("supersecret", result.wifiData?.password)
    }

    @Test
    fun testPhoneParsing() {
        val barcode = Mockito.mock(Barcode::class.java)
        val phone = Mockito.mock(Barcode.Phone::class.java)
        Mockito.`when`(barcode.valueType).thenReturn(Barcode.TYPE_PHONE)
        Mockito.`when`(barcode.rawValue).thenReturn("tel:+15551234567")
        Mockito.`when`(barcode.phone).thenReturn(phone)
        Mockito.`when`(phone.number).thenReturn("+15551234567")

        val result = QrCodeParser.parse(barcode)
        assertEquals(QrType.PHONE, result.type)
        assertEquals("+15551234567", result.title)
        assertEquals("Call", result.actionLabel)
    }

    @Test
    fun testRawTextUrlInference() {
        val barcode = Mockito.mock(Barcode::class.java)
        Mockito.`when`(barcode.valueType).thenReturn(Barcode.TYPE_TEXT)
        Mockito.`when`(barcode.rawValue).thenReturn("example.org/test")

        val result = QrCodeParser.parse(barcode)
        assertEquals(QrType.URL, result.type)
        assertEquals("example.org", result.title)
    }

    @Test
    fun testPlainTextFallback() {
        val barcode = Mockito.mock(Barcode::class.java)
        Mockito.`when`(barcode.valueType).thenReturn(Barcode.TYPE_TEXT)
        Mockito.`when`(barcode.rawValue).thenReturn("Order #12345 Hello World")

        val result = QrCodeParser.parse(barcode)
        assertEquals(QrType.TEXT, result.type)
        assertEquals("Order #12345 Hello World", result.title)
        assertEquals("Copy", result.actionLabel)
    }
}
