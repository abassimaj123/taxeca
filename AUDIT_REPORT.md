# TaxeCA UI/UX Audit Report
**Generated:** 2026-05-04  
**Framework:** Kotlin/Jetpack Compose, Material Design 3  
**Target:** Android app (tax calculator with history + dark mode)

---

## Executive Summary

**Overall Score:** 6/10 (Improvement Needed)
- **Critical Issues:** 4 (Accessibility, Dark Mode Contrast)
- **High Issues:** 6 (Touch targets, Error messaging, Reduced Motion)
- **Medium Issues:** 5 (Dark Mode polish, Component consistency)

### Quick Stats
- ✅ Material Design 3 theme properly configured
- ✅ Dark mode implemented with system integration
- ❌ **No contentDescription labels on any Icons (CRITICAL A11Y)**
- ❌ **Dark mode contrast ratios NOT verified (High Risk)**
- ❌ Form validation lacks inline error feedback
- ❌ No reduced-motion support for animations
- ⚠️ GradientButton lacks semantic accessibility attributes

---

## 1. CRITICAL ISSUES

### 1.1 Missing Accessibility Labels on All Icons
**Severity:** CRITICAL (WCAG 2.1 Level AA violation)  
**Files Affected:** All screen files + components  
**Impact:** Screen reader users cannot identify icon buttons

**Found In:**
- CalculatorScreen.kt: Icons.Savings, Icons.CallSplit (no contentDescription)
- All Icon() calls lack `contentDescription` parameter
- Switch components missing semantic labels

**Required Fix:**
```kotlin
// BEFORE (WRONG)
Icon(Icons.Filled.Savings, modifier = Modifier.size(24.dp))

// AFTER (CORRECT)
Icon(
    Icons.Filled.Savings, 
    contentDescription = stringResource(R.string.desc_savings_icon),
    modifier = Modifier.size(24.dp)
)
```

**Fix Priority:** Fix IMMEDIATELY (blocking Play Store submission)

---

### 1.2 Dark Mode Contrast Ratios NOT Verified
**Severity:** CRITICAL (WCAG 2.1 Level AA violation)  
**Files Affected:** Theme.kt, all screen components  
**Impact:** Text may be illegible in dark mode

**Current Issues:**
- Blue90 (Light Mode): 0xFFD8E2FF - No contrast test in dark mode
- Text on dark surfaces may not meet 4.5:1 minimum
- Error red (Red80 = 0xFFB4AB) needs contrast verification

**Required Verification:**
```
Light Mode Text Pairs:
- Grey10 (0xFF1A1C1E) on Grey99 (0xFFFBFBFE): ✅ Likely 16:1
- Grey10 on Blue90 (0xFFD8E2FF): ⚠️ NEEDS CHECK (~13:1?)

Dark Mode Text Pairs:
- Grey90 (0xFFE2E2E5) on Grey10 (0xFF1A1C1E): ⚠️ NEEDS CHECK (~12:1?)
- Grey90 on Blue30 (0xFF0042A8): ⚠️ NEEDS CHECK
- Error Red80 (0xFFB4AB) for error text: ⚠️ NEEDS CHECK
```

**Fix Priority:** TEST BEFORE SUBMISSION

---

## 2. HIGH-PRIORITY ISSUES

### 2.1 No Reduced-Motion Support
**Severity:** HIGH (A11y guideline)  
**Files Affected:** CalculatorScreen.kt (AnimatedVisibility)  
**Impact:** Animations cannot be disabled for motion-sensitive users

**Current Code:**
```kotlin
AnimatedVisibility(
    visible = hasResult,
    enter = expandVertically() + fadeIn(),
    exit = shrinkVertically() + fadeOut()
) { ResultCard(...) }
```

**Missing:**
```kotlin
// Need to check System setting and conditionally apply animations
val disableAnimations = LocalDensity.current.let { ... } // Use Settings.Secure
```

**Fix Priority:** HIGH (post-submission hotfix)

---

### 2.2 Form Validation Errors Not Shown Inline
**Severity:** HIGH (UX issue)  
**Files Affected:** CalculatorScreen.kt, RestaurantScreen.kt, ShoppingScreen.kt  
**Impact:** Users don't know what's wrong with their input

**Current Implementation:**
```kotlin
val isInputValid by viewModel.isInputValid.collectAsStateWithLifecycle()
// Only disables button, no error message shown
```

**Missing:**
- No error text below OutlinedTextField when invalid
- No field-level validation feedback
- No error summary for required fields

**Required Pattern:**
```kotlin
OutlinedTextField(
    value = amount,
    onValueChange = { viewModel.onAmountChange(it) },
    isError = !isInputValid && amount.isNotEmpty(),
    supportingText = {
        if (isInputValid == false) {
            Text(stringResource(R.string.error_invalid_amount))
        }
    }
)
```

**Fix Priority:** HIGH (usability blocker)

---

### 2.3 GradientButton Missing Semantic Accessibility
**Severity:** HIGH (A11y)  
**File:** GradientButton.kt  
**Impact:** Button intent unclear to screen readers

**Current:**
```kotlin
Box(
    modifier = modifier
        .height(52.dp)
        .clickable(enabled = enabled, onClick = onClick)
)
```

**Issues:**
- No `role = Role.Button` semantic
- No disabled state semantic attribute
- No focus ring/outline for keyboard navigation

**Fix Priority:** HIGH

---

### 2.4 No Toast/Snackbar for Save Confirmation
**Severity:** HIGH (UX)  
**File:** CalculatorScreen.kt  
**Impact:** Users unsure if calculation was saved

**Current:**
```kotlin
val saveConfirmed by viewModel.saveConfirmed.collectAsStateWithLifecycle()
// State exists but no visible feedback shown
```

**Missing:** Snackbar showing "Calculation saved to history"

**Fix Priority:** HIGH (UX polish)

---

## 3. MEDIUM-PRIORITY ISSUES

### 3.1 Dark Mode Surface Contrast Polish
**Severity:** MEDIUM  
**Issue:** Cards/surfaces may not be distinguishable from background in dark mode

**Current Dark Theme:**
```kotlin
background = Grey10      // 0xFF1A1C1E
surface = Grey10         // SAME COLOR - WRONG!
```

**Problem:** Surface and background are identical - cards disappear!

**Fix Required:**
```kotlin
surface = Grey20    // 0xFF2F3033 (elevates from background)
```

**Fix Priority:** MEDIUM (cosmetic but important for dark mode usability)

---

### 3.2 ProvinceSelector Missing Semantic Label
**Severity:** MEDIUM (A11y)  
**File:** ProvinceSelector.kt  
**Impact:** Dropdown purpose unclear to screen readers

**Issue:**
```kotlin
OutlinedTextField(
    label = { Text(stringResource(R.string.label_province)) }
    // Has label text but may not be read by screen readers
)
```

**Fix Required:** Add `contentDescription` to ExposedDropdownMenuBox

**Fix Priority:** MEDIUM

---

### 3.3 No Keyboard Navigation in SegmentedButton
**Severity:** MEDIUM (A11y)  
**File:** ModeToggle.kt  
**Impact:** Keyboard-only users cannot navigate mode selection

**Fix Required:** Ensure SegmentedButton supports arrow key navigation (check Material3 version)

**Fix Priority:** MEDIUM

---

### 3.4 Missing Loading State Feedback
**Severity:** MEDIUM (UX)  
**File:** PdfExportService.kt, SaveCalculationUseCase  
**Impact:** No visual feedback during PDF generation

**Fix Required:**
- Show CircularProgressIndicator during export
- Disable button while exporting
- Show success/error Snackbar after completion

**Fix Priority:** MEDIUM

---

## 4. TOUCH TARGET VERIFICATION

### Touch Target Audit
| Component | Size | Status | Notes |
|-----------|------|--------|-------|
| GradientButton | 52dp height | ✅ OK | Exceeds 48dp minimum |
| ModeToggle (SegmentedButton) | ~48dp | ⚠️ CHECK | Verify actual touch area |
| ProvinceSelector (TextField) | ~56dp | ✅ OK | Standard TextField height |
| Icons in Cards | 24dp | ❌ FAIL | Expand hit area with Modifier.size(48.dp) |

**Fix Priority:** Expand icon tap areas to 48×48dp minimum

---

## 5. DARK MODE IMPLEMENTATION REVIEW

### ✅ What's Correct
- Color scheme properly implemented with Material Design 3
- Dynamic color support framework in place
- Status bar styling applied

### ❌ What's Missing
- Surface/background color hierarchy broken (both Grey10)
- No verified contrast ratios for text pairs
- No separate light/dark icon color pairs
- Accent colors (Green) may have contrast issues in dark mode

### Recommended Fixes
1. **Surface Elevation Fix:** Change `surface = Grey20` (darker than background)
2. **Contrast Testing:** Use WebAIM Contrast Checker on all text pairs
3. **Icon Colors:** Define semantic icon colors (currently using hardcoded White)

---

## 6. IMPLEMENTATION CHECKLIST

### Priority 1 (CRITICAL - Block Submission)
- [ ] Add contentDescription to ALL Icon() instances
- [ ] Verify/fix dark mode contrast ratios
- [ ] Fix surface/background color in dark theme
- [ ] Add error state display for form inputs

### Priority 2 (HIGH - Before Launch)
- [ ] Add reduced-motion support for AnimatedVisibility
- [ ] Add GradientButton semantic accessibility
- [ ] Implement Snackbar for save confirmation
- [ ] Fix icon tap area sizing to 48×48dp
- [ ] Add keyboard navigation support check

### Priority 3 (MEDIUM - Polish)
- [ ] Add loading states for async operations
- [ ] Add helper text to form fields
- [ ] Improve error message clarity
- [ ] Add success feedback animations
- [ ] Review typography hierarchy

### Priority 4 (NICE-TO-HAVE)
- [ ] Implement fancy entrance animations with reduced-motion respect
- [ ] Add haptic feedback to buttons
- [ ] Implement form autosave for long forms
- [ ] Add custom animations for transitions

---

## 7. RECOMMENDED RESOURCES

### Testing Tools
- **Contrast Checker:** https://webaim.org/resources/contrastchecker/
- **Compose Accessibility:** https://developer.android.com/jetpack/compose/accessibility
- **Material Design 3 Specs:** https://m3.material.io/

### Code Changes Required
- Add `contentDescription` parameters throughout
- Update color scheme surface colors
- Implement SemanticModifier where needed
- Add proper error state handling
- Respect system accessibility preferences

---

## 8. NEXT STEPS

1. **Immediate (Today):** Fix contentDescription + contrast issues
2. **Short-term (This Week):** Fix form validation, reduced-motion, semantic attrs
3. **Before Submission:** Full accessibility audit + testing on real device
4. **Post-Launch:** Implement polish features + gather user feedback

---

## Appendix: Files Requiring Changes

### Screens (All)
- CalculatorScreen.kt
- RestaurantScreen.kt
- ShoppingScreen.kt
- HistoryScreen.kt
- SettingsScreen.kt
- HistoryDetailScreen.kt

### Components (All)
- GradientButton.kt
- ModeToggle.kt
- ProvinceSelector.kt
- PillButton.kt
- ResultCard.kt
- All other UI components

### Theme
- Theme.kt (fix surface color)
- Color.kt (verify dark mode colors)

### Services
- PdfExportService.kt (add loading feedback)

---

**Report Status:** ACTIONABLE  
**Next Audit:** After implementing Priority 1 fixes
