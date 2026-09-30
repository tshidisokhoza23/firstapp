package com.tpsk.tj.vocab.utils

import kotlin.math.min

object LevenshteinDistance {
    fun calculate(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) {
            for (j in 0..s2.length) {
                when {
                    i == 0 -> dp[i][j] = j
                    j == 0 -> dp[i][j] = i
                    else -> {
                        dp[i][j] = min(
                            min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                            dp[i - 1][j - 1] + (if (s1[i - 1] == s2[j - 1]) 0 else 1)
                        )
                    }
                }
            }
        }
        return dp[s1.length][s2.length]
    }

    fun getSimilarity(s1: String, s2: String): Double {
        val maxLength = maxOf(s1.length, s2.length)
        if (maxLength == 0) return 1.0
        return (maxLength - calculate(s1, s2)).toDouble() / maxLength
    }
}
