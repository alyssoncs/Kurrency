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

    @Test
    fun formatMinorUnitsUsdAcrossLocales() {
        val locales = listOf(KurrencyLocale.US, KurrencyLocale.GERMANY, KurrencyLocale.FRANCE, KurrencyLocale.JAPAN)
        locales.forEach { locale ->
            val f = CurrencyFormatter(locale)
            val result = f.formatMinorUnits(123456, "USD")
            assertTrue(result.contains("1") && result.contains("23"), "Locale ${locale.languageTag}: $result")
        }
    }

    @Test
    fun formatMinorUnitsJpyAcrossLocales() {
        val locales = listOf(KurrencyLocale.US, KurrencyLocale.GERMANY, KurrencyLocale.FRANCE, KurrencyLocale.JAPAN)
        locales.forEach { locale ->
            val f = CurrencyFormatter(locale)
            val result = f.formatMinorUnits(1234, "JPY")
            assertTrue(result.contains("1") && result.contains("234"), "Locale ${locale.languageTag}: $result")
        }
    }

    @Test
    fun formatMinorUnitsKwdAcrossLocales() {
        val locales = listOf(KurrencyLocale.US, KurrencyLocale.UK)
        locales.forEach { locale ->
            val f = CurrencyFormatter(locale)
            val result = f.formatMinorUnits(1234567, "KWD")
            assertTrue(result.contains("1") && result.contains("234"), "Locale ${locale.languageTag}: $result")
        }
    }

    @Test
    fun formatMinorUnitsIsoStyleAcrossLocales() {
        val locales = listOf(KurrencyLocale.US, KurrencyLocale.GERMANY, KurrencyLocale.JAPAN)
        locales.forEach { locale ->
            val f = CurrencyFormatter(locale)
            val result = f.formatMinorUnitsIsoStyle(15050, "EUR")
            assertTrue(result.contains("EUR"), "Locale ${locale.languageTag}: $result")
            assertTrue(result.contains("150"), "Locale ${locale.languageTag}: $result")
        }
    }

    @Test
    fun roundTripUsdMinorUnits() {
        val f = CurrencyFormatter(KurrencyLocale.US)
        val formatted = f.formatMinorUnits(123456, "USD")
        val parsed = f.parseToMinorUnitsResult(formatted, "USD")
        assertTrue(parsed.isSuccess, "Round-trip failed for USD: '$formatted'")
        assertEquals(123456L, parsed.getOrNull())
    }

    @Test
    fun roundTripJpyMinorUnits() {
        val f = CurrencyFormatter(KurrencyLocale.JAPAN)
        val formatted = f.formatMinorUnits(1234, "JPY")
        val parsed = f.parseToMinorUnitsResult(formatted, "JPY")
        assertTrue(parsed.isSuccess, "Round-trip failed for JPY: '$formatted'")
        assertEquals(1234L, parsed.getOrNull())
    }

    @Test
    fun roundTripKwdMinorUnits() {
        val f = CurrencyFormatter(KurrencyLocale.US)
        val plain = f.minorUnitsToPlainString(1234567, "KWD")
        val parsed = f.parseToMinorUnitsResult(plain, "KWD")
        assertTrue(parsed.isSuccess, "Round-trip failed for KWD plain: '$plain'")
        assertEquals(1234567L, parsed.getOrNull())
    }

    @Test
    fun roundTripNegativeMinorUnits() {
        val f = CurrencyFormatter(KurrencyLocale.US)
        val formatted = f.formatMinorUnits(-5075, "USD")
        val parsed = f.parseToMinorUnitsResult(formatted, "USD")
        assertTrue(parsed.isSuccess, "Round-trip failed for negative: '$formatted'")
        assertEquals(-5075L, parsed.getOrNull())
    }

    @Test
    fun roundTripZeroMinorUnits() {
        val f = CurrencyFormatter(KurrencyLocale.US)
        val formatted = f.formatMinorUnits(0, "USD")
        val parsed = f.parseToMinorUnitsResult(formatted, "USD")
        assertTrue(parsed.isSuccess, "Round-trip failed for zero: '$formatted'")
        assertEquals(0L, parsed.getOrNull())
    }

    @Test
    fun formatMinorUnitsNegativeContainsIndicator() {
        val result = formatter.formatMinorUnits(-15050, "USD")
        assertTrue(
            result.contains("-") || result.contains("\u2212") || result.contains("("),
            "Negative should have indicator: $result"
        )
        assertTrue(result.contains("150"), "Should contain amount: $result")
    }

    @Test
    fun formatMinorUnitsIsoStyleNegative() {
        val result = formatter.formatMinorUnitsIsoStyle(-15050, "USD")
        assertTrue(result.contains("USD"), "Should contain code: $result")
        assertTrue(
            result.contains("-") || result.contains("\u2212") || result.contains("("),
            "Negative should have indicator: $result"
        )
    }

    @Test
    fun formatMinorUnitsZeroUsd() {
        val result = formatter.formatMinorUnits(0, "USD")
        assertTrue(result.contains("0"), "Zero should format: $result")
    }

    @Test
    fun formatMinorUnitsIsoStyleZero() {
        val result = formatter.formatMinorUnitsIsoStyle(0, "EUR")
        assertTrue(result.contains("EUR"), "Zero ISO should contain code: $result")
    }

    @Test
    fun formatMinorUnitsWithOptionsZeroDash() {
        val options = CurrencyFormatOptions(zeroDisplay = ZeroDisplay.DASH)
        val result = formatter.formatMinorUnitsWithOptions(0, "USD", options)
        assertTrue(result.isSuccess)
        val formatted = result.getOrNull()!!
        assertTrue(formatted.contains("\u2014"), "Zero dash should be em-dash: $formatted")
    }

    @Test
    fun formatMinorUnitsWithOptionsZeroEmpty() {
        val options = CurrencyFormatOptions(zeroDisplay = ZeroDisplay.EMPTY)
        val result = formatter.formatMinorUnitsWithOptions(0, "USD", options)
        assertTrue(result.isSuccess)
        assertEquals("", result.getOrNull())
    }

    @Test
    fun plainStringBhdThreeDecimals() {
        assertEquals("1.234", formatter.minorUnitsToPlainString(1234, "BHD"))
    }

    @Test
    fun plainStringOmrThreeDecimals() {
        assertEquals("10.500", formatter.minorUnitsToPlainString(10500, "OMR"))
    }

    @Test
    fun plainStringMaxLong() {
        val result = formatter.minorUnitsToPlainString(Long.MAX_VALUE, "USD")
        assertTrue(result.endsWith(".07"), "MAX_VALUE last two digits: $result")
        assertTrue(result.length > 10, "Should be a long string: $result")
    }

    @Test
    fun plainStringMinLong() {
        val result = formatter.minorUnitsToPlainString(Long.MIN_VALUE, "USD")
        // -9223372036854775808 with 2 fraction digits → "-92233720368547758.08".
        // Guards against kotlin.math.abs(Long.MIN_VALUE) overflow producing "--..." output.
        assertEquals("-92233720368547758.08", result)
    }

    @Test
    fun plainStringMinLongJpy() {
        val result = formatter.minorUnitsToPlainString(Long.MIN_VALUE, "JPY")
        assertEquals(Long.MIN_VALUE.toString(), result)
    }

    @Test
    fun plainStringMinLongKwd() {
        val result = formatter.minorUnitsToPlainString(Long.MIN_VALUE, "KWD")
        assertEquals("-9223372036854775.808", result)
    }

    @Test
    fun plainStringSingleMillForKwd() {
        assertEquals("0.001", formatter.minorUnitsToPlainString(1, "KWD"))
    }

    @Test
    fun plainStringNegativeSingleMillForKwd() {
        assertEquals("-0.001", formatter.minorUnitsToPlainString(-1, "KWD"))
    }

    @Test
    fun formatMinorUnitsResultInvalidCode() {
        assertTrue(formatter.formatMinorUnitsResult(100, "ABC").isFailure)
    }

    @Test
    fun formatMinorUnitsIsoStyleResultInvalidCode() {
        assertTrue(formatter.formatMinorUnitsIsoStyleResult(100, "ABC").isFailure)
    }

    @Test
    fun formatMinorUnitsCompactStyleResultInvalidCode() {
        assertTrue(formatter.formatMinorUnitsCompactStyleResult(100, "ABC").isFailure)
    }

    @Test
    fun formatMinorUnitsWithOptionsInvalidCode() {
        val options = CurrencyFormatOptions()
        assertTrue(formatter.formatMinorUnitsWithOptions(100, "ABC", options).isFailure)
    }

    @Test
    fun formatMinorUnitsResultEmptyCode() {
        assertTrue(formatter.formatMinorUnitsResult(100, "").isFailure)
    }

    @Test
    fun formatMinorUnitsResultTooShortCode() {
        assertTrue(formatter.formatMinorUnitsResult(100, "US").isFailure)
    }

    @Test
    fun currencyAmountFromMajorUnitsRoundTrip() {
        val created = CurrencyAmount.fromMajorUnits("150.50", Kurrency.USD)
        assertTrue(created.isSuccess)
        val amount = created.getOrNull()!!
        assertEquals(15050L, amount.minorUnits)
        val formatted = amount.format(CurrencyStyle.Standard, KurrencyLocale.US).getOrNull()!!
        assertTrue(formatted.contains("150"))
    }

    @Test
    fun currencyAmountFromMajorUnitsJpy() {
        val created = CurrencyAmount.fromMajorUnits("500", Kurrency.JPY)
        assertTrue(created.isSuccess)
        assertEquals(500L, created.getOrNull()!!.minorUnits)
    }

    @Test
    fun currencyAmountFormatOrEmptySuccess() {
        val amount = CurrencyAmount(15050, Kurrency.USD)
        val result = amount.formatOrEmpty(CurrencyStyle.Standard, KurrencyLocale.US)
        assertTrue(result.isNotEmpty())
        assertTrue(result.contains("150"))
    }

    @Test
    fun currencyAmountFormatIsoStyle() {
        val amount = CurrencyAmount(15050, Kurrency.USD)
        val result = amount.format(CurrencyStyle.Iso, KurrencyLocale.US)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.contains("USD"))
    }

    @Test
    fun currencyAmountFormatAccountingNegative() {
        val amount = CurrencyAmount(-15050, Kurrency.USD)
        val result = amount.format(CurrencyStyle.Accounting, KurrencyLocale.US)
        assertTrue(result.isSuccess)
        val formatted = result.getOrNull()!!
        assertTrue(formatted.contains("(") || formatted.contains("-"), "Accounting negative: $formatted")
    }

    @Test
    fun formatMinorUnitsWithOptionsParenthesesNegative() {
        val options = CurrencyFormatOptions(negativeStyle = NegativeStyle.PARENTHESES)
        val result = formatter.formatMinorUnitsWithOptions(-15050, "USD", options)
        assertTrue(result.isSuccess)
        val formatted = result.getOrNull()!!
        assertTrue(formatted.contains("("), "Should have parentheses: $formatted")
        assertTrue(formatted.contains("150"), "Should contain amount: $formatted")
    }

    @Test
    fun formatMinorUnitsWithOptionsNoGrouping() {
        val options = CurrencyFormatOptions(grouping = false, symbolDisplay = SymbolDisplay.NONE)
        val result = formatter.formatMinorUnitsWithOptions(123456789, "USD", options)
        assertTrue(result.isSuccess)
        val formatted = result.getOrNull()!!
        assertTrue(formatted.contains("1234567"), "Should not have grouping: $formatted")
    }

    @Test
    fun formatMinorUnitsWithOptionsTrailingSymbol() {
        val options = CurrencyFormatOptions(symbolPosition = SymbolPosition.TRAILING)
        val result = formatter.formatMinorUnitsWithOptions(15050, "USD", options)
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun formatMinorUnitsWithOptionsCurrencyName() {
        val options = CurrencyFormatOptions(symbolDisplay = SymbolDisplay.NAME)
        val result = formatter.formatMinorUnitsWithOptions(15050, "USD", options)
        assertTrue(result.isSuccess)
        val formatted = result.getOrNull()!!
        assertTrue(
            formatted.contains("Dollar", ignoreCase = true) || formatted.contains("USD"),
            "Should contain currency name: $formatted"
        )
    }
}
