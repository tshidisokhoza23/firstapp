package com.tpsk.tj.ui.screens.contest

import android.app.Activity
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.ads.rewarded.RewardedAd

enum class VoteState { FREE_AVAILABLE, WATCH_AD, COMPLETED_LOCKED }

@Composable
fun ContestVoteButton(
    initialVotes: Int,
    rewardedAd: RewardedAd? = null,
    onCastVote: (isAdVote: Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var voteCount by remember { mutableIntStateOf(initialVotes) }
    var voteState by remember { mutableStateOf(VoteState.FREE_AVAILABLE) }

    val (buttonText, containerColor) = when (voteState) {
        VoteState.FREE_AVAILABLE -> "Vote ($voteCount)" to Color(0xFF2563EB)
        VoteState.WATCH_AD -> "Vote Again (Watch Ad)" to Color(0xFF7C3AED)
        VoteState.COMPLETED_LOCKED -> "Voted Today ✓" to Color(0xFF374151)
    }

    Button(
        onClick = {
            when (voteState) {
                VoteState.FREE_AVAILABLE -> {
                    voteCount++
                    voteState = VoteState.WATCH_AD
                    onCastVote(false)
                }
                VoteState.WATCH_AD -> {
                    val activity = context as? Activity
                    if (activity != null && rewardedAd != null) {
                        rewardedAd.show(activity) { _ ->
                            voteCount++
                            voteState = VoteState.COMPLETED_LOCKED
                            onCastVote(true)
                        }
                    } else {
                        // Fallback if ad is unavailable
                        voteCount++
                        voteState = VoteState.COMPLETED_LOCKED
                        onCastVote(true)
                    }
                }
                VoteState.COMPLETED_LOCKED -> { /* Locked */ }
            }
        },
        enabled = voteState != VoteState.COMPLETED_LOCKED,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            disabledContainerColor = Color(0xFF1E2228),
            disabledContentColor = Color.Gray
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.height(38.dp)
    ) {
        Text(text = buttonText, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
