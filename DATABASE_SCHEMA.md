# Calo Fit — Database Schema

Calo Fit uses an embedded local SQLite database via Room with the following tables:

### 1. `user_profile`
- `id` (INTEGER, PK, value=1)
- `name` (TEXT)
- `age` (INTEGER)
- `gender` (TEXT: MALE, FEMALE)
- `heightCm` (REAL)
- `currentWeightKg` (REAL)
- `targetWeightKg` (REAL)
- `activityLevel` (TEXT)
- `goal` (TEXT: LOSE_WEIGHT, MAINTAIN_WEIGHT, GAIN_WEIGHT)
- `targetCalories` (INTEGER)
- `proteinGrams` (INTEGER)
- `carbsGrams` (INTEGER)
- `fatGrams` (INTEGER)
- `waterMl` (INTEGER)
- `isSetupComplete` (INTEGER)

### 2. `food_items`
- `id` (INTEGER, PK, AutoGenerate)
- `nameEn` (TEXT, Indexed)
- `nameFa` (TEXT, Indexed)
- `category` (TEXT, Indexed)
- `servingSizeGrams` (REAL)
- `servingUnitEn` (TEXT)
- `servingUnitFa` (TEXT)
- `calories` (INTEGER)
- `protein` (REAL)
- `carbs` (REAL)
- `fat` (REAL)
- `fiber` (REAL)
- `isIranian` (INTEGER)
- `isVerified` (INTEGER)

### 3. `food_logs`
- `id` (INTEGER, PK, AutoGenerate)
- `dateIso` (TEXT, Indexed)
- `mealType` (TEXT: BREAKFAST, MORNING_SNACK, LUNCH, AFTERNOON_SNACK, DINNER, EVENING_SNACK, Indexed)
- `foodId` (INTEGER, Nullable)
- `foodName` (TEXT)
- `grams` (REAL)
- `calories` (INTEGER)
- `protein` (REAL)
- `carbs` (REAL)
- `fat` (REAL)
- `timestamp` (INTEGER)

### 4. `weight_records`
- `id` (INTEGER, PK, AutoGenerate)
- `dateIso` (TEXT, Unique Index)
- `weightKg` (REAL)
- `note` (TEXT)
- `timestamp` (INTEGER)

### 5. `water_records`
- `id` (INTEGER, PK, AutoGenerate)
- `dateIso` (TEXT, Indexed)
- `amountMl` (INTEGER)
- `timestamp` (INTEGER)

### 6. `activity_records`
- `id` (INTEGER, PK, AutoGenerate)
- `dateIso` (TEXT, Indexed)
- `activityType` (TEXT)
- `durationMinutes` (INTEGER)
- `burnedCalories` (INTEGER)
- `timestamp` (INTEGER)

### 7. `meal_plans`
- `id` (INTEGER, PK, AutoGenerate)
- `dayOfWeek` (INTEGER)
- `mealType` (TEXT)
- `titleEn` / `titleFa` (TEXT)
- `recipeSummaryEn` / `recipeSummaryFa` (TEXT)
- `calories` (INTEGER)
- `protein` / `carbs` / `fat` (REAL)

### 8. `achievements`
- `id` (INTEGER, PK, AutoGenerate)
- `code` (TEXT, Unique Index)
- `titleEn` / `titleFa` (TEXT)
- `descriptionEn` / `descriptionFa` (TEXT)
- `iconName` (TEXT)
- `isUnlocked` (INTEGER)
- `unlockedDate` (TEXT)
