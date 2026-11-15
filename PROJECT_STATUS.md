# Calo Fit — Project Status

## Overview
- **Project Name:** Calo Fit (کالو فیت)
- **Package:** com.calofit.app (ApplicationId: `com.aistudio.calofit.qvlr`)
- **Platform:** Native Android (Kotlin, Jetpack Compose, Material 3)
- **Status:** Phase 2 Complete, Production-Ready Offline-First Architecture

## Key Milestones Achieved
1. **Design System & Aesthetics:** Clean, modern Material 3 interface with Light (#F1F5F5) and Dark (#101716) themes, neon lime accents, specular highlights, and strictly compliant appearance options (`روشن`, `تاریک`, `مطابق سیستم` / `Light`, `Dark`, `Follow system`). Zero unapproved naming.
2. **Custom 3D App Icon & Branding:** High-resolution 3D translucent glass nutrition icon and adaptive launcher icons across all mipmap densities (mdpi to xxxhdpi) with custom dark background.
3. **Bilingual Localization & Strict Persian Formatting:** Complete Persian (fa-IR) and English (en-US) support with RTL layout direction, Solar Hijri (Jalali) calendar conversion, optional Persian digits (۱۲۳۴), explicit visible field labels across all inputs, and Persian units (`کالری`, `گرم`, `کیلوگرم`, `میلی‌لیتر`, `سانتی‌متر`).
4. **Offline Room Database:** Embedded pre-populated database with authentic Iranian traditional cuisine (قورمه سبزی، قیمه، فسنجان، زرشک پلو، کباب کوبیده، جوجه کباب، سنگک، بربری، لواش، و غیره) and international essentials, supporting Iranian household serving sizes (ملاقه، کفگیر، کف دست نان، قاشق غذاخوری، لیوان).
5. **Mifflin-St Jeor Engine:** Full scientific calculation engine for BMR, TDEE, BMI, safe caloric deficit/surplus, macro ratios, and water intake.
6. **Feature Suite:**
   - Language Selection (strictly minimal: `فارسی | Persian` & `English | انگلیسی` with continue button)
   - Interactive 4-step onboarding with visible labels and real-time plan calculation
   - Home Dashboard with circular calorie meter, macro progress bars, and quick actions
   - Food log grouped by meal with Persian macro displays
   - Instant search across dishes with custom food creation dialog
   - Food detail screen with Iranian household unit conversions
   - Smart Kitchen (`آشپز هوشمند`): Discover traditional recipes based on available pantry ingredients
   - Food Comparison (`مقایسه هوشمند غذاها`): Side-by-side macro and calorie comparison
   - Daily water tracker with quick add presets
   - Weight tracking with target progress and BMI category indicators
   - Activity tracking with MET-based calorie burn calculations
   - Food image analysis with mandatory user confirmation
   - Conversational Nutrition Coach with Persian dietary advice
   - Weekly meal planner & grocery list
   - Nutrition reports & analytics
   - Streaks & achievement badges
   - Local JSON data backup & export
7. **Verification & Testing:** All local unit and Robolectric tests pass (`BUILD SUCCESSFUL in 22s`). `compile_applet` clean compilation.
