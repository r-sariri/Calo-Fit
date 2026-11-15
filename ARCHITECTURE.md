# Calo Fit — Architecture & Design

## 1. Clean Architecture & MVVM Pattern
Calo Fit is constructed strictly following Android's Modern Application Architecture:

```
[ UI Layer (Jetpack Compose) ]
         │ (Events & User Actions)
         ▼
[ ViewModel Layer (CaloFitViewModel) ]
         │ (StateFlow / Kotlin Coroutines)
         ▼
[ Domain Layer (NutritionCalculator, JalaliCalendarHelper) ]
         │
         ▼
[ Data Layer (CaloFitRepository) ]
    ├── Room SQLite Database (Local on-device persistence)
    └── DataStore (Preferences: Theme, Language, Digits)
```

## 2. Liquid Glass Design System
The design system replicates translucent glassmorphism with high performance on mid-range Android devices:
- **Translucent Surfaces:** Gradients with controlled alpha (e.g., `0xEBFFFFFF` / `0xD9172220`) rather than expensive full-screen real-time blurs, ensuring a solid 60 FPS.
- **Specular Highlights:** Linear gradients on borders simulating light refraction.
- **Accent Glow:** Lime-green neon lighting (`#A6ED37` / `#B5F34B`) on calorie progress arcs and key actions.

## 3. Strict Offline-First Principle
- Operates 100% locally on device.
- Pre-bundled SQLite Room database populated with Iranian and international foods on first boot.
- DataStore preserves theme, language, and configuration without any cloud dependency.
