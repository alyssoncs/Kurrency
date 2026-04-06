# Minor Units Formatting — Precision Fix and Full API Parity

Fix the Double-based minor-to-major conversion used in `formatMinorUnits` and `CurrencyAmount.format()`, replace it with string-based integer arithmetic, and add full API parity so every format style that accepts a String amount also accepts minor units as Long.

## Problem

The library's minor units formatting pipeline converts `Long` to `Double` before formatting:

```kotlin
val divisor = 10.0.pow(fractionDigits)
val majorAmount = minorUnits / divisor  // Double — precision loss possible
return formatCurrencyStyle(majorAmount.toString(), currencyCode)
```

This exists in three places:
- `CurrencyFormat.formatMinorUnits` (line 89-94)
- `CurrencyAmount.format()` (line 16-18)
- `CurrencyAmount.format(options)` (line 38-40)

Meanwhile, `parseToMinorUnitsResult` (the reverse direction) already uses precise string-based arithmetic. The asymmetry means a round-trip can lose precision.

Additionally, `formatMinorUnits` only produces currency-symbol style output. There are no minor-units overloads for ISO style, compact style, options-based formatting, or plain string conversion.

## Core Fix: String-Based Conversion

A single function replaces all `minorUnits / 10.0.pow(fractionDigits)` usage:

```kotlin
fun minorUnitsToPlainString(minorUnits: Long, currencyCode: String): String {
    val fractionDigits = getFractionDigitsOrDefault(currencyCode)
    if (fractionDigits == 0) return minorUnits.toString()
    val abs = kotlin.math.abs(minorUnits)
    val sign = if (minorUnits < 0) "-" else ""
    val str = abs.toString().padStart(fractionDigits + 1, '0')
    val whole = str.dropLast(fractionDigits)
    val fraction = str.takeLast(fractionDigits)
    return "$sign$whole.$fraction"
}
```

Behavior by currency:
- `15050L, "USD"` (2 digits) → `"150.50"`
- `500L, "JPY"` (0 digits) → `"500"`
- `15050L, "KWD"` (3 digits) → `"15.050"`
- `-50L, "USD"` (2 digits) → `"-0.50"`
- `0L, "EUR"` (2 digits) → `"0.00"`

Pure integer/string arithmetic. No Double involved. No precision loss for any Long value.

## API Changes by Layer

### CurrencyFormat Interface

**Fix existing:**

`formatMinorUnits(Long, String): String` — replace Double-based body with:
```kotlin
fun formatMinorUnits(minorUnits: Long, currencyCode: String): String {
    val plainAmount = minorUnitsToPlainString(minorUnits, currencyCode)
    return formatCurrencyStyle(plainAmount, currencyCode)
}
```

**New default methods:**

```kotlin
fun minorUnitsToPlainString(minorUnits: Long, currencyCode: String): String
fun formatMinorUnitsIsoStyle(minorUnits: Long, currencyCode: String): String
fun formatMinorUnitsCompactStyle(minorUnits: Long, currencyCode: String): String
```

Each follows the same pattern: call `minorUnitsToPlainString`, delegate to the corresponding String-based formatter.

### CurrencyFormatter

**New Result methods:**

```kotlin
fun minorUnitsToPlainStringResult(minorUnits: Long, currencyCode: String): Result<String>
fun formatMinorUnitsIsoStyleResult(minorUnits: Long, currencyCode: String): Result<String>
fun formatMinorUnitsCompactStyleResult(minorUnits: Long, currencyCode: String): Result<String>
fun formatMinorUnitsWithOptions(minorUnits: Long, currencyCode: String, options: CurrencyFormatOptions): Result<String>
```

Each validates the currency code, calls `minorUnitsToPlainString`, then delegates to the corresponding String-based Result method.

### Kurrency

**New convenience methods matching existing pattern:**

```kotlin
fun formatMinorUnitsIsoStyle(minorUnits: Long, locale: KurrencyLocale = systemLocale()): Result<String>
fun formatMinorUnitsCompact(minorUnits: Long, locale: KurrencyLocale = systemLocale()): Result<String>
fun formatMinorUnitsWithOptions(minorUnits: Long, options: CurrencyFormatOptions, locale: KurrencyLocale = systemLocale()): Result<String>
```

Each creates a `CurrencyFormatter(locale)` and delegates, matching the pattern of `formatAmount`, `formatAmountCompact`, `formatAmountWithOptions`.

### CurrencyAmount

**Fix internals only — no new public API.** Replace Double division in:

- `format(style, locale)` (line 16-18)
- `format(options, locale)` (line 38-40)

Both change from:
```kotlin
val fractionDigits = currency.fractionDigitsOrDefault
val divisor = 10.0.pow(fractionDigits)
val majorAmount = minorUnits / divisor
return currency.formatAmount(majorAmount.toString(), ...)
```

To:
```kotlin
val formatter = CurrencyFormatter(locale)
val plainAmount = formatter.minorUnitsToPlainString(minorUnits, currency.code)
return currency.formatAmount(plainAmount, ...)
```

This works because `CurrencyFormatter` implements `CurrencyFormat` which has the `minorUnitsToPlainString` default method.

`CurrencyAmount` already has full style and options support — fixing the internals is all that's needed.

## Files Changed

| File | Change |
|------|--------|
| `CurrencyFormat.kt` | Fix `formatMinorUnits`, add `minorUnitsToPlainString`, `formatMinorUnitsIsoStyle`, `formatMinorUnitsCompactStyle` |
| `CurrencyFormatter.kt` | Add Result methods: `minorUnitsToPlainStringResult`, `formatMinorUnitsIsoStyleResult`, `formatMinorUnitsCompactStyleResult`, `formatMinorUnitsWithOptions` |
| `Kurrency.kt` | Add `formatMinorUnitsIsoStyle`, `formatMinorUnitsCompact`, `formatMinorUnitsWithOptions` |
| `CurrencyAmount.kt` | Fix `format()` and `format(options)` to use string-based conversion |

## Files Unchanged

- All platform `CurrencyFormatterImpl` files — they receive String amounts, no changes needed
- `CurrencyMetadata.kt` — fractionDigits definitions untouched
- `CurrencyFormatOptions.kt` — untouched
- `KurrencyError.kt` — untouched
- All parsing methods — already string-precise

## Testing

New tests for:
- `minorUnitsToPlainString` — USD (2 digits), JPY (0 digits), KWD (3 digits), negative values, zero, large values
- `formatMinorUnits` — verify locale-aware output matches `formatCurrencyStyle` for equivalent amounts
- `formatMinorUnitsIsoStyle` — verify ISO code output from minor units
- `formatMinorUnitsCompactStyle` — verify compact output from minor units
- `formatMinorUnitsWithOptions` — verify options-based output from minor units
- `CurrencyAmount.format()` — verify precision fix (round-trip: parse → format matches original)

## Out of Scope

- New currencies in `CurrencyMetadata`
- Changes to parsing methods (already precise)
- Changes to platform implementations
- Compose module changes
- Deci module changes
