# Calo Fit — Test Execution Report

## Test Suites
1. **NutritionCalculatorTest**
   - `testBmiCalculation`: Verifies BMI formula ($weight / height^2$) and boundary category mappings (Underweight, Normal, Overweight, Obese).
   - `testBmrMifflinStJeorMale`: Verifies male BMR formula ($10w + 6.25h - 5a + 5$).
   - `testBmrMifflinStJeorFemale`: Verifies female BMR formula ($10w + 6.25h - 5a - 161$).
   - `testTdeeCalculation`: Verifies physical activity multipliers (1.2 to 1.9).
   - `testFullPlanCalculation`: Verifies macro distribution (protein/carbs/fat) and hydration targets.
   - `testExerciseBurnedCalories`: Verifies MET-based workout expenditure calculation.

2. **JalaliCalendarTest**
   - `testGregorianToJalaliConversion`: Verifies Nowruz / Solar Hijri conversion.
   - `testPersianDigitsConversion`: Verifies mapping of 0-9 to ۰-۹.

3. **Robolectric Context Test**
   - `ExampleRobolectricTest`: PASSED (validated localized app context string `R.string.app_name` matches "Calo Fit").

4. **Automated Test Results**
   - **All 10 unit tests PASSED** (`BUILD SUCCESSFUL in 18s`).
   - Compilation: **PASSED** (`compile_applet` clean build).
