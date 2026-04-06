package org.kimplify.kurrency

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MinorUnitsFormattingTest {

    private val formatter = CurrencyFormatter(KurrencyLocale.US)

    @Test
    fun plainStringUsdTwoDecimals() {
        assertEquals("150.50", formatter.minorUnitsToPlainString(15050, "USD"))
    }

    @Test
    fun plainStringJpyZeroDecimals() {
        assertEquals("500", formatter.minorUnitsToPlainString(500, "JPY"))
    }

    @Test
    fun plainStringKwdThreeDecimals() {
        assertEquals("15.050", formatter.minorUnitsToPlainString(15050, "KWD"))
    }

    @Test
    fun plainStringNegativeSmallAmount() {
        assertEquals("-0.50", formatter.minorUnitsToPlainString(-50, "USD"))
    }

    @Test
    fun plainStringNegativeLargeAmount() {
        assertEquals("-150.50", formatter.minorUnitsToPlainString(-15050, "USD"))
    }

    @Test
    fun plainStringZero() {
        assertEquals("0.00", formatter.minorUnitsToPlainString(0, "USD"))
    }

    @Test
    fun plainStringZeroJpy() {
        assertEquals("0", formatter.minorUnitsToPlainString(0, "JPY"))
    }

    @Test
    fun plainStringLargeValue() {
        assertEquals("99999999.99", formatter.minorUnitsToPlainString(9999999999, "USD"))
    }

    @Test
    fun plainStringSingleCent() {
        assertEquals("0.01", formatter.minorUnitsToPlainString(1, "USD"))
    }

    @Test
    fun plainStringNegativeSingleCent() {
        assertEquals("-0.01", formatter.minorUnitsToPlainString(-1, "USD"))
    }

    @Test
    fun formatMinorUnitsProducesLocaleAwareOutput() {
        val result = formatter.formatMinorUnits(15050, "USD")
        assertTrue(result.contains("150"))
        assertTrue(result.contains("50"))
    }

    @Test
    fun formatMinorUnitsIsoStyleContainsCurrencyCode() {
        val result = formatter.formatMinorUnitsIsoStyle(15050, "USD")
        assertTrue(result.contains("USD"))
        assertTrue(result.contains("150"))
    }

    @Test
    fun formatMinorUnitsIsoStyleJpy() {
        val result = formatter.formatMinorUnitsIsoStyle(500, "JPY")
        assertTrue(result.contains("JPY"))
        assertTrue(result.contains("500"))
    }

    @Test
    fun formatMinorUnitsCompactStyleLargeAmount() {
        val result = formatter.formatMinorUnitsCompactStyle(150000000, "USD")
        assertTrue(result.contains("1"))
    }

    @Test
    fun minorUnitsToPlainStringResultSuccess() {
        val result = formatter.minorUnitsToPlainStringResult(15050, "USD")
        assertTrue(result.isSuccess)
        assertEquals("150.50", result.getOrNull())
    }

    @Test
    fun minorUnitsToPlainStringResultInvalidCurrency() {
        val result = formatter.minorUnitsToPlainStringResult(15050, "ABC")
        assertTrue(result.isFailure)
    }

    @Test
    fun formatMinorUnitsIsoStyleResultSuccess() {
        val result = formatter.formatMinorUnitsIsoStyleResult(15050, "USD")
        assertTrue(result.isSuccess)
        val formatted = result.getOrNull()!!
        assertTrue(formatted.contains("USD"))
        assertTrue(formatted.contains("150"))
    }

    @Test
    fun formatMinorUnitsCompactStyleResultSuccess() {
        val result = formatter.formatMinorUnitsCompactStyleResult(15050, "USD")
        assertTrue(result.isSuccess)
    }

    @Test
    fun formatMinorUnitsWithOptionsNoSymbol() {
        val options = CurrencyFormatOptions(symbolDisplay = SymbolDisplay.NONE)
        val result = formatter.formatMinorUnitsWithOptions(15050, "USD", options)
        assertTrue(result.isSuccess)
        val formatted = result.getOrNull()!!
        assertTrue(formatted.contains("150"))
    }

    @Test
    fun formatMinorUnitsWithOptionsIsoCode() {
        val options = CurrencyFormatOptions(symbolDisplay = SymbolDisplay.ISO_CODE)
        val result = formatter.formatMinorUnitsWithOptions(15050, "USD", options)
        assertTrue(result.isSuccess)
        val formatted = result.getOrNull()!!
        assertTrue(formatted.contains("USD"))
    }

    @Test
    fun formatMinorUnitsResultInvalidCurrencyFails() {
        val result = formatter.formatMinorUnitsResult(15050, "INVALID")
        assertTrue(result.isFailure)
    }

    @Test
    fun kurrencyFormatMinorUnitsIsoStyle() {
        val result = Kurrency.USD.formatMinorUnitsIsoStyle(15050)
        assertTrue(result.isSuccess)
        val formatted = result.getOrNull()!!
        assertTrue(formatted.contains("USD"))
        assertTrue(formatted.contains("150"))
    }

    @Test
    fun kurrencyFormatMinorUnitsCompact() {
        val result = Kurrency.USD.formatMinorUnitsCompact(150000000)
        assertTrue(result.isSuccess)
    }

    @Test
    fun kurrencyFormatMinorUnitsWithOptions() {
        val options = CurrencyFormatOptions(symbolDisplay = SymbolDisplay.NONE)
        val result = Kurrency.USD.formatMinorUnitsWithOptions(15050, options)
        assertTrue(result.isSuccess)
        val formatted = result.getOrNull()!!
        assertTrue(formatted.contains("150"))
    }

    @Test
    fun currencyAmountFormatMatchesPlainString() {
        val amount = CurrencyAmount(15050, Kurrency.USD)
        val result = amount.format(CurrencyStyle.Standard, KurrencyLocale.US)
        assertTrue(result.isSuccess)
        val formatted = result.getOrNull()!!
        assertTrue(formatted.contains("150"))
        assertTrue(formatted.contains("50"))
    }

    @Test
    fun currencyAmountFormatJpy() {
        val amount = CurrencyAmount(500, Kurrency.JPY)
        val result = amount.format(CurrencyStyle.Standard, KurrencyLocale.US)
        assertTrue(result.isSuccess)
        val formatted = result.getOrNull()!!
        assertTrue(formatted.contains("500"))
    }

    @Test
    fun currencyAmountFormatWithOptions() {
        val amount = CurrencyAmount(15050, Kurrency.USD)
        val options = CurrencyFormatOptions(symbolDisplay = SymbolDisplay.ISO_CODE)
        val result = amount.format(options, KurrencyLocale.US)
        assertTrue(result.isSuccess)
        val formatted = result.getOrNull()!!
        assertTrue(formatted.contains("USD"))
        assertTrue(formatted.contains("150"))
    }

    @Test
    fun currencyAmountRoundTrip() {
        val original = CurrencyAmount(15050, Kurrency.USD)
        val formatted = original.format(CurrencyStyle.Standard, KurrencyLocale.US).getOrNull()!!
        val parsed = CurrencyAmount.parse(formatted, Kurrency.USD, KurrencyLocale.US)
        assertTrue(parsed.isSuccess)
        assertEquals(15050, parsed.getOrNull()!!.minorUnits)
    }
}
