package org.kimplify.kurrency.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import org.kimplify.kurrency.CurrencyFormatter
import org.kimplify.kurrency.KurrencyLocale

class CurrencyVisualTransformation(
    private val currencyCode: String,
    private val locale: KurrencyLocale = KurrencyLocale.systemLocale(),
    private val fractionDigits: Int = CurrencyFormatter.getFractionDigitsOrDefault(currencyCode),
) : VisualTransformation {

    private val formatter = CurrencyFormatter.forLocale(locale)

    override fun filter(text: AnnotatedString): TransformedText {
        val digitsOnly = text.text.filter { it.isDigit() }

        if (digitsOnly.isEmpty()) {
            val zeroAmount = buildZeroAmount()
            val formatted = formatter.formatCurrencyStyle(zeroAmount, currencyCode)
            return TransformedText(
                AnnotatedString(formatted),
                ZeroOffsetMapping(formatted.length),
            )
        }

        val amount = insertDecimalPoint(digitsOnly)
        val formatted = formatter.formatCurrencyStyle(amount, currencyCode)

        return TransformedText(
            AnnotatedString(formatted),
            CurrencyOffsetMapping(digitsOnly.length, formatted),
        )
    }

    private fun insertDecimalPoint(digits: String): String {
        if (fractionDigits == 0) return digits

        val padded = digits.padStart(fractionDigits + 1, '0')
        val integerPart = padded.substring(0, padded.length - fractionDigits)
        val decimalPart = padded.substring(padded.length - fractionDigits)
        return "$integerPart.$decimalPart"
    }

    private fun buildZeroAmount(): String {
        return if (fractionDigits == 0) "0"
        else "0." + "0".repeat(fractionDigits)
    }
}

private class ZeroOffsetMapping(private val formattedLength: Int) : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int = formattedLength
    override fun transformedToOriginal(offset: Int): Int = 0
}

private class CurrencyOffsetMapping(
    private val originalLength: Int,
    private val formatted: String,
) : OffsetMapping {

    override fun originalToTransformed(offset: Int): Int {
        return formatted.length.coerceAtMost(offset.coerceIn(0, originalLength))
            .let { formatted.length }
    }

    override fun transformedToOriginal(offset: Int): Int {
        return originalLength
    }
}

@Composable
fun rememberCurrencyVisualTransformation(
    currencyCode: String,
    locale: KurrencyLocale = KurrencyLocale.current(),
): CurrencyVisualTransformation {
    return remember(currencyCode, locale) {
        CurrencyVisualTransformation(currencyCode, locale)
    }
}
