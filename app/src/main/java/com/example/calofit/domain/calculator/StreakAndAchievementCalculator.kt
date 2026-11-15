package com.example.calofit.domain.calculator

import com.example.calofit.data.database.InitialFoodData
import com.example.calofit.data.model.AchievementEntity
import com.example.calofit.data.model.FoodLogEntryEntity
import com.example.calofit.data.model.UserProfileEntity
import com.example.calofit.data.model.WaterRecordEntity
import com.example.calofit.data.model.WeightRecordEntity
import com.example.calofit.domain.calendar.JalaliCalendarHelper

/**
 * Pure domain logic for real consecutive-day tracking streaks and Bronze/Silver/Gold
 * consistency achievement evaluation. Never fabricates progress or resets unlocked history.
 */
object StreakAndAchievementCalculator {

    data class AchievementEvaluation(
        val code: String,
        val tier: String,
        val titleEn: String,
        val titleFa: String,
        val descriptionEn: String,
        val descriptionFa: String,
        val progressCurrent: Int,
        val progressTarget: Int,
        val shouldUnlock: Boolean,
        val unlockedDate: String?
    )

    /**
     * Computes consecutive days of logged meals ending today or yesterday.
     * If neither today nor yesterday has logs, the current active streak is 0.
     */
    fun calculateConsecutiveStreak(
        loggedDates: Collection<String>,
        todayIso: String = JalaliCalendarHelper.getTodayIsoDate()
    ): Int {
        if (loggedDates.isEmpty()) return 0
        val distinctSet = loggedDates.toHashSet()
        val yesterdayIso = JalaliCalendarHelper.addDays(todayIso, -1)

        var cursor = when {
            distinctSet.contains(todayIso) -> todayIso
            distinctSet.contains(yesterdayIso) -> yesterdayIso
            else -> return 0
        }

        var streak = 0
        while (distinctSet.contains(cursor)) {
            streak++
            cursor = JalaliCalendarHelper.addDays(cursor, -1)
        }
        return streak
    }

    /**
     * Computes the longest historical consecutive streak from all distinct logged dates.
     */
    fun calculateLongestStreak(loggedDates: Collection<String>): Int {
        val sortedAsc = loggedDates.distinct().sorted()
        if (sortedAsc.isEmpty()) return 0
        var longest = 1
        var current = 1
        for (i in 1 until sortedAsc.size) {
            val expectedNext = JalaliCalendarHelper.addDays(sortedAsc[i - 1], 1)
            if (sortedAsc[i] == expectedNext) {
                current++
                if (current > longest) longest = current
            } else {
                current = 1
            }
        }
        return longest
    }

    fun evaluateAll(
        profile: UserProfileEntity?,
        allFoodLogs: List<FoodLogEntryEntity>,
        allWaterRecords: List<WaterRecordEntity>,
        allWeightRecords: List<WeightRecordEntity>,
        existingAchievements: List<AchievementEntity>,
        todayIso: String = JalaliCalendarHelper.getTodayIsoDate()
    ): List<AchievementEvaluation> {
        val templates = InitialFoodData.getInitialAchievements().associateBy { it.code }
        val existingByCode = existingAchievements.associateBy { it.code }

        val logsByDate = allFoodLogs.groupBy { it.dateIso }
        val loggedDates = logsByDate.keys
        val currentStreak = calculateConsecutiveStreak(loggedDates, todayIso)
        val bestStreak = maxOf(currentStreak, calculateLongestStreak(loggedDates))

        val targetCalories = (profile?.targetCalories ?: 2000).coerceAtLeast(1000)
        val targetProtein = (profile?.proteinGrams ?: 120).coerceAtLeast(40)
        val targetWaterMl = (profile?.waterMl ?: 2500).coerceAtLeast(1000)

        val waterByDate = allWaterRecords.groupBy { it.dateIso }
            .mapValues { (_, list) -> list.sumOf { it.amountMl } }

        val hydrationGoalReachedDates = waterByDate.filterValues { it >= targetWaterMl }.keys.sorted()
        val proteinGoalDates = logsByDate.filterValues { entries ->
            entries.sumOf { it.protein } >= targetProtein * 0.95
        }.keys.sorted()

        val calorieDisciplineDates = logsByDate.filterValues { entries ->
            val cal = entries.sumOf { it.calories }
            cal in (targetCalories * 0.85).toInt()..(targetCalories * 1.15).toInt()
        }.keys.sorted()

        val activeHealthyDays = (loggedDates.filter { date ->
            val cal = logsByDate[date]?.sumOf { it.calories } ?: 0
            val inCalRange = cal in (targetCalories * 0.80).toInt()..(targetCalories * 1.20).toInt()
            val waterMet = (waterByDate[date] ?: 0) >= targetWaterMl
            inCalRange || waterMet
        }).distinct().sorted()

        fun buildEval(
            code: String,
            currentProgress: Int,
            targetProgress: Int,
            unlockDateCandidate: String?
        ): AchievementEvaluation {
            val tpl = templates.getValue(code)
            val prev = existingByCode[code]
            val wasUnlocked = prev?.isUnlocked == true
            val isNowUnlocked = wasUnlocked || currentProgress >= targetProgress
            val resolvedDate = prev?.unlockedDate ?: if (isNowUnlocked) (unlockDateCandidate ?: todayIso) else null

            return AchievementEvaluation(
                code = code,
                tier = tpl.tier,
                titleEn = tpl.titleEn,
                titleFa = tpl.titleFa,
                descriptionEn = tpl.descriptionEn,
                descriptionFa = tpl.descriptionFa,
                progressCurrent = if (wasUnlocked) targetProgress else currentProgress.coerceIn(0, targetProgress),
                progressTarget = targetProgress,
                shouldUnlock = isNowUnlocked,
                unlockedDate = resolvedDate
            )
        }

        val sortedLoggedDates = loggedDates.sorted()

        return listOf(
            // Bronze
            buildEval(
                code = "FIRST_LOG",
                currentProgress = if (allFoodLogs.isNotEmpty()) 1 else 0,
                targetProgress = 1,
                unlockDateCandidate = sortedLoggedDates.firstOrNull()
            ),
            buildEval(
                code = "FIRST_WEIGHT",
                currentProgress = if (allWeightRecords.isNotEmpty()) 1 else 0,
                targetProgress = 1,
                unlockDateCandidate = allWeightRecords.minByOrNull { it.dateIso }?.dateIso
            ),
            buildEval(
                code = "HYDRATION_HERO",
                currentProgress = if (hydrationGoalReachedDates.isNotEmpty()) 1 else 0,
                targetProgress = 1,
                unlockDateCandidate = hydrationGoalReachedDates.firstOrNull()
            ),
            buildEval(
                code = "STREAK_3",
                currentProgress = maxOf(currentStreak, if (bestStreak >= 3) 3 else 0),
                targetProgress = 3,
                unlockDateCandidate = todayIso
            ),
            // Silver
            buildEval(
                code = "STREAK_7",
                currentProgress = maxOf(currentStreak, if (bestStreak >= 7) 7 else 0),
                targetProgress = 7,
                unlockDateCandidate = todayIso
            ),
            buildEval(
                code = "MACRO_BALANCE",
                currentProgress = if (proteinGoalDates.isNotEmpty()) 1 else 0,
                targetProgress = 1,
                unlockDateCandidate = proteinGoalDates.firstOrNull()
            ),
            buildEval(
                code = "WEEKLY_GOAL_5",
                currentProgress = calorieDisciplineDates.size,
                targetProgress = 5,
                unlockDateCandidate = calorieDisciplineDates.getOrNull(4)
            ),
            // Gold
            buildEval(
                code = "STREAK_30",
                currentProgress = maxOf(currentStreak, if (bestStreak >= 30) 30 else 0),
                targetProgress = 30,
                unlockDateCandidate = todayIso
            ),
            buildEval(
                code = "MONTHLY_CHAMPION",
                currentProgress = activeHealthyDays.size,
                targetProgress = 20,
                unlockDateCandidate = activeHealthyDays.getOrNull(19)
            )
        )
    }
}
