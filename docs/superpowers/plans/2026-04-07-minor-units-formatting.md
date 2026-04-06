# Minor Units Formatting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix Double-based minor-to-major conversion with string-based integer arithmetic, and add full API parity so every format style accepts minor units as Long.

**Architecture:** A single `minorUnitsToPlainString` function in `CurrencyFormat` interface converts Long minor units to a plain decimal string using integer/string arithmetic (no Double). All minor units formatting methods call this first, then delegate to the existing String-based formatters. New methods are added at three layers: interface defaults, CurrencyFormatter Result methods, and Kurrency convenience methods.

**Tech Stack:** Kotlin Multiplatform, kotlin.test

**Spec:** `docs/superpowers/specs/2026-04-07-minor-units-formatting-design.md`

---

### Task 1: Add `minorUnitsToPlainString` to CurrencyFormat interface

**Files:**
- Modify: `kurrency-core/src/commonMain/kotlin/org/kimplify/kurrency/CurrencyFormat.kt:89-94`
- Test: `kurrency-core/src/commonTest/kotlin/org/kimplify/kurrency/MinorUnitsFormattingTest.kt`

- [ ] **Step 1: Create test file with tests for minorUnitsToPlainString**

Create `kurrency-core/src/commonTest/kotlin/org/kimplify/kurrency/MinorUnitsFormattingTest.kt`:

```kotlin
package org.kimplify.kurrency

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd /Users/kostakttipay/StudioProjects/Kurrency && ./gradlew :kurrency-core:jvmTest --tests "org.kimplify.kurrency.MinorUnitsFormattingTest" --no-daemon`
Expected: FAIL — `minorUnitsToPlainString` does not exist yet as a public method on the interface.

- [ ] **Step 3: Add minorUnitsToPlainString to CurrencyFormat interface and fix formatMinorUnits**

In `kurrency-core/src/commonMain/kotlin/org/kimplify/kurrency/CurrencyFormat.kt`, replace lines 89-94 (the existing `formatMinorUnits` default) and add `minorUnitsToPlainString` before it:

Replace:
```kotlin
    fun formatMinorUnits(minorUnits: Long, currencyCode: String): String {
        val fractionDigits = getFractionDigitsOrDefault(currencyCode)
        val divisor = 10.0.pow(fractionDigits)
        val majorAmount = minorUnits / divisor
        return formatCurrencyStyle(majorAmount.toString(), currencyCode)
    }
```

With:
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

    fun formatMinorUnits(minorUnits: Long, currencyCode: String): String {
        val plainAmount = minorUnitsToPlainString(minorUnits, currencyCode)
        return formatCurrencyStyle(plainAmount, currencyCode)
    }
```

Remove the `import kotlin.math.pow` at the top of the file if it's only used by the old `formatMinorUnits` (check — it's line 3). Yes, the only usage of `pow` was in `formatMinorUnits`. Remove it.

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd /Users/kostakttipay/StudioProjects/Kurrency && ./gradlew :kurrency-core:jvmTest --tests "org.kimplify.kurrency.MinorUnitsFormattingTest" --no-daemon`
Expected: PASS — all 10 tests green.

- [ ] **Step 5: Commit**

```bash
cd /Users/kostakttipay/StudioProjects/Kurrency
git add kurrency-core/src/commonMain/kotlin/org/kimplify/kurrency/CurrencyFormat.kt kurrency-core/src/commonTest/kotlin/org/kimplify/kurrency/MinorUnitsFormattingTest.kt
git commit -m "feat: add minorUnitsToPlainString with string-based arithmetic, fix formatMinorUnits"
```

---

### Task 2: Add ISO, compact, and options-based minor units formatting to CurrencyFormat interface

**Files:**
- Modify: `kurrency-core/src/commonMain/kotlin/org/kimplify/kurrency/CurrencyFormat.kt`
- Test: `kurrency-core/src/commonTest/kotlin/org/kimplify/kurrency/MinorUnitsFormattingTest.kt`

- [ ] **Step 1: Add tests for new interface methods**

Append to `MinorUnitsFormattingTest.kt`:

```kotlin
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
```

Add imports at top:
```kotlin
import kotlin.test.assertTrue
```

- [ ] **Step 2: Run tests to verify new ones fail**

Run: `cd /Users/kostakttipay/StudioProjects/Kurrency && ./gradlew :kurrency-core:jvmTest --tests "org.kimplify.kurrency.MinorUnitsFormattingTest" --no-daemon`
Expected: FAIL — `formatMinorUnitsIsoStyle` and `formatMinorUnitsCompactStyle` don't exist.

- [ ] **Step 3: Add new default methods to CurrencyFormat interface**

In `CurrencyFormat.kt`, add after the `formatMinorUnits` method:

```kotlin
    fun formatMinorUnitsIsoStyle(minorUnits: Long, currencyCode: String): String {
        val plainAmount = minorUnitsToPlainString(minorUnits, currencyCode)
        return formatIsoCurrencyStyle(plainAmount, currencyCode)
    }

    fun formatMinorUnitsCompactStyle(minorUnits: Long, currencyCode: String): String {
        val plainAmount = minorUnitsToPlainString(minorUnits, currencyCode)
        return formatCompactStyle(plainAmount, currencyCode)
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd /Users/kostakttipay/StudioProjects/Kurrency && ./gradlew :kurrency-core:jvmTest --tests "org.kimplify.kurrency.MinorUnitsFormattingTest" --no-daemon`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
cd /Users/kostakttipay/StudioProjects/Kurrency
git add kurrency-core/src/commonMain/kotlin/org/kimplify/kurrency/CurrencyFormat.kt kurrency-core/src/commonTest/kotlin/org/kimplify/kurrency/MinorUnitsFormattingTest.kt
git commit -m "feat: add formatMinorUnitsIsoStyle and formatMinorUnitsCompactStyle to CurrencyFormat"
```

---

### Task 3: Add Result methods to CurrencyFormatter

**Files:**
- Modify: `kurrency-core/src/commonMain/kotlin/org/kimplify/kurrency/CurrencyFormatter.kt`
- Test: `kurrency-core/src/commonTest/kotlin/org/kimplify/kurrency/MinorUnitsFormattingTest.kt`

- [ ] **Step 1: Add tests for Result methods**

Append to `MinorUnitsFormattingTest.kt`:

```kotlin
    @Test
    fun minorUnitsToPlainStringResultSuccess() {
        val result = formatter.minorUnitsToPlainStringResult(15050, "USD")
        assertTrue(result.isSuccess)
        assertEquals("150.50", result.getOrNull())
    }

    @Test
    fun minorUnitsToPlainStringResultInvalidCurrency() {
        val result = formatter.minorUnitsToPlainStringResult(15050, "XXX")
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
```

- [ ] **Step 2: Run tests to verify new ones fail**

Run: `cd /Users/kostakttipay/StudioProjects/Kurrency && ./gradlew :kurrency-core:jvmTest --tests "org.kimplify.kurrency.MinorUnitsFormattingTest" --no-daemon`
Expected: FAIL — new Result methods don't exist.

- [ ] **Step 3: Add Result methods to CurrencyFormatter**

In `CurrencyFormatter.kt`, after the existing `formatMinorUnitsResult` method (line 243), add:

```kotlin
    fun minorUnitsToPlainStringResult(minorUnits: Long, currencyCode: String): Result<String> {
        if (!isValidCurrencyCode(currencyCode)) {
            return Result.failure(KurrencyError.InvalidCurrencyCode(currencyCode))
        }
        return Result.success(minorUnitsToPlainString(minorUnits, currencyCode))
    }

    fun formatMinorUnitsIsoStyleResult(minorUnits: Long, currencyCode: String): Result<String> {
        if (!isValidCurrencyCode(currencyCode)) {
            return Result.failure(KurrencyError.InvalidCurrencyCode(currencyCode))
        }
        val plainAmount = minorUnitsToPlainString(minorUnits, currencyCode)
        return formatIsoCurrencyStyleResult(plainAmount, currencyCode)
    }

    fun formatMinorUnitsCompactStyleResult(minorUnits: Long, currencyCode: String): Result<String> {
        if (!isValidCurrencyCode(currencyCode)) {
            return Result.failure(KurrencyError.InvalidCurrencyCode(currencyCode))
        }
        val plainAmount = minorUnitsToPlainString(minorUnits, currencyCode)
        return formatCompactStyleResult(plainAmount, currencyCode)
    }

    fun formatMinorUnitsWithOptions(
        minorUnits: Long,
        currencyCode: String,
        options: CurrencyFormatOptions,
    ): Result<String> {
        if (!isValidCurrencyCode(currencyCode)) {
            return Result.failure(KurrencyError.InvalidCurrencyCode(currencyCode))
        }
        val plainAmount = minorUnitsToPlainString(minorUnits, currencyCode)
        return formatWithOptions(plainAmount, currencyCode, options)
    }
```

Also update the existing `formatMinorUnitsResult` (line 238-243) to use string-based conversion instead of delegating to `impl.formatMinorUnits`:

Replace:
```kotlin
    fun formatMinorUnitsResult(minorUnits: Long, currencyCode: String): Result<String> {
        if (!isValidCurrencyCode(currencyCode)) {
            return Result.failure(KurrencyError.InvalidCurrencyCode(currencyCode))
        }
        return Result.success(impl.formatMinorUnits(minorUnits, currencyCode))
    }
```

With:
```kotlin
    fun formatMinorUnitsResult(minorUnits: Long, currencyCode: String): Result<String> {
        if (!isValidCurrencyCode(currencyCode)) {
            return Result.failure(KurrencyError.InvalidCurrencyCode(currencyCode))
        }
        val plainAmount = minorUnitsToPlainString(minorUnits, currencyCode)
        return formatCurrencyStyleResult(plainAmount, currencyCode)
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd /Users/kostakttipay/StudioProjects/Kurrency && ./gradlew :kurrency-core:jvmTest --tests "org.kimplify.kurrency.MinorUnitsFormattingTest" --no-daemon`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
cd /Users/kostakttipay/StudioProjects/Kurrency
git add kurrency-core/src/commonMain/kotlin/org/kimplify/kurrency/CurrencyFormatter.kt kurrency-core/src/commonTest/kotlin/org/kimplify/kurrency/MinorUnitsFormattingTest.kt
git commit -m "feat: add minor units Result methods to CurrencyFormatter"
```

---

### Task 4: Add convenience methods to Kurrency class

**Files:**
- Modify: `kurrency-core/src/commonMain/kotlin/org/kimplify/kurrency/Kurrency.kt`
- Test: `kurrency-core/src/commonTest/kotlin/org/kimplify/kurrency/MinorUnitsFormattingTest.kt`

- [ ] **Step 1: Add tests for Kurrency convenience methods**

Append to `MinorUnitsFormattingTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Run tests to verify new ones fail**

Run: `cd /Users/kostakttipay/StudioProjects/Kurrency && ./gradlew :kurrency-core:jvmTest --tests "org.kimplify.kurrency.MinorUnitsFormattingTest" --no-daemon`
Expected: FAIL — new methods don't exist.

- [ ] **Step 3: Add convenience methods to Kurrency.kt**

In `kurrency-core/src/commonMain/kotlin/org/kimplify/kurrency/Kurrency.kt`, add after the existing `formatMinorUnits` method (line 160):

```kotlin
    fun formatMinorUnitsIsoStyle(
        minorUnits: Long,
        locale: KurrencyLocale = KurrencyLocale.systemLocale(),
    ): Result<String> {
        val formatter = CurrencyFormatter(locale)
        return formatter.formatMinorUnitsIsoStyleResult(minorUnits, code)
    }

    fun formatMinorUnitsCompact(
        minorUnits: Long,
        locale: KurrencyLocale = KurrencyLocale.systemLocale(),
    ): Result<String> {
        val formatter = CurrencyFormatter(locale)
        return formatter.formatMinorUnitsCompactStyleResult(minorUnits, code)
    }

    fun formatMinorUnitsWithOptions(
        minorUnits: Long,
        options: CurrencyFormatOptions,
        locale: KurrencyLocale = KurrencyLocale.systemLocale(),
    ): Result<String> {
        val formatter = CurrencyFormatter(locale)
        return formatter.formatMinorUnitsWithOptions(minorUnits, code, options)
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd /Users/kostakttipay/StudioProjects/Kurrency && ./gradlew :kurrency-core:jvmTest --tests "org.kimplify.kurrency.MinorUnitsFormattingTest" --no-daemon`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
cd /Users/kostakttipay/StudioProjects/Kurrency
git add kurrency-core/src/commonMain/kotlin/org/kimplify/kurrency/Kurrency.kt kurrency-core/src/commonTest/kotlin/org/kimplify/kurrency/MinorUnitsFormattingTest.kt
git commit -m "feat: add minor units convenience methods to Kurrency class"
```

---

### Task 5: Fix CurrencyAmount internals

**Files:**
- Modify: `kurrency-core/src/commonMain/kotlin/org/kimplify/kurrency/CurrencyAmount.kt`
- Test: `kurrency-core/src/commonTest/kotlin/org/kimplify/kurrency/MinorUnitsFormattingTest.kt`

- [ ] **Step 1: Add round-trip precision test**

Append to `MinorUnitsFormattingTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Run tests (they may pass already since the old code works for typical values, but verify)**

Run: `cd /Users/kostakttipay/StudioProjects/Kurrency && ./gradlew :kurrency-core:jvmTest --tests "org.kimplify.kurrency.MinorUnitsFormattingTest" --no-daemon`

- [ ] **Step 3: Fix CurrencyAmount.format() internals**

In `kurrency-core/src/commonMain/kotlin/org/kimplify/kurrency/CurrencyAmount.kt`, replace the `format(style, locale)` method (lines 11-18):

Replace:
```kotlin
    fun format(
        style: CurrencyStyle = CurrencyStyle.Standard,
        locale: KurrencyLocale = KurrencyLocale.systemLocale(),
    ): Result<String> {
        val fractionDigits = currency.fractionDigitsOrDefault
        val divisor = 10.0.pow(fractionDigits)
        val majorAmount = minorUnits / divisor
        return currency.formatAmount(majorAmount.toString(), style, locale)
    }
```

With:
```kotlin
    fun format(
        style: CurrencyStyle = CurrencyStyle.Standard,
        locale: KurrencyLocale = KurrencyLocale.systemLocale(),
    ): Result<String> {
        val formatter = CurrencyFormatter(locale)
        val plainAmount = formatter.minorUnitsToPlainString(minorUnits, currency.code)
        return currency.formatAmount(plainAmount, style, locale)
    }
```

Replace the `format(options, locale)` method (lines 33-41):

Replace:
```kotlin
    fun format(
        options: CurrencyFormatOptions,
        locale: KurrencyLocale = KurrencyLocale.systemLocale(),
    ): Result<String> {
        val fractionDigits = currency.fractionDigitsOrDefault
        val divisor = 10.0.pow(fractionDigits)
        val majorAmount = minorUnits / divisor
        return currency.formatAmountWithOptions(majorAmount.toString(), options, locale)
    }
```

With:
```kotlin
    fun format(
        options: CurrencyFormatOptions,
        locale: KurrencyLocale = KurrencyLocale.systemLocale(),
    ): Result<String> {
        val formatter = CurrencyFormatter(locale)
        val plainAmount = formatter.minorUnitsToPlainString(minorUnits, currency.code)
        return currency.formatAmountWithOptions(plainAmount, options, locale)
    }
```

Remove unused imports: `import kotlin.math.pow` and `import kotlin.math.roundToLong` if `roundToLong` is only used in `fromMajorUnits` — check. `roundToLong` IS still used in `fromMajorUnits` (line 67). So only remove `import kotlin.math.pow`. Actually check — `pow` is used in `fromMajorUnits` too (line 61: `val multiplier = 10.0.pow(fractionDigits)`). So keep `pow` import. Only remove it if it's no longer used — in this case it IS still used by `fromMajorUnits`. Keep both imports.

- [ ] **Step 4: Run full test suite**

Run: `cd /Users/kostakttipay/StudioProjects/Kurrency && ./gradlew :kurrency-core:jvmTest --no-daemon`
Expected: ALL PASS

- [ ] **Step 5: Commit**

```bash
cd /Users/kostakttipay/StudioProjects/Kurrency
git add kurrency-core/src/commonMain/kotlin/org/kimplify/kurrency/CurrencyAmount.kt kurrency-core/src/commonTest/kotlin/org/kimplify/kurrency/MinorUnitsFormattingTest.kt
git commit -m "fix: replace Double-based conversion in CurrencyAmount with string-based arithmetic"
```

---

### Task 6: Run full test suite and verify

- [ ] **Step 1: Run all tests across all source sets**

Run: `cd /Users/kostakttipay/StudioProjects/Kurrency && ./gradlew check --no-daemon`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Fix any failures**

If any existing tests fail due to the interface changes, fix them. The most likely issue: platform implementations that override `formatMinorUnits` may need updates — but since `formatMinorUnits` is a default method on the interface and platform impls inherit it, this should be fine.

- [ ] **Step 3: Commit any fixes**

```bash
cd /Users/kostakttipay/StudioProjects/Kurrency
git add -A
git commit -m "fix: resolve any test failures from minor units changes"
```

Only commit if there were actual fixes. Skip if everything passed.
