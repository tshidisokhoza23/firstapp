package com.tpsk.tj.ui.screens.contest

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.ads.rewarded.RewardedAd

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContestVoteButton(
    initialVotes: Int,
    candidateName: String = "Candidate",
    postId: String? = null,
    rewardedAd: RewardedAd? = null,
    onCastVote: (voteCount: Int, pointsSpent: Int) -> Unit = { _, _ -> },
    contestViewModel: ContestViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var currentVotes by remember(postId) { mutableIntStateOf(initialVotes) }
    val serverPoints by contestViewModel.points.collectAsState()
    var userPointsBalance by remember { mutableIntStateOf(serverPoints) }
    var showVoteSheet by remember { mutableStateOf(false) }

    LaunchedEffect(initialVotes) {
        if (initialVotes > currentVotes) currentVotes = initialVotes
    }
    LaunchedEffect(Unit) { contestViewModel.refreshPoints() }
    LaunchedEffect(serverPoints) { userPointsBalance = serverPoints }

    Button(
        onClick = { showVoteSheet = true },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF2563EB),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Text(
            text = "Vote ($currentVotes)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }

    if (showVoteSheet) {
        ModalBottomSheet(
            onDismissRequest = { showVoteSheet = false },
            containerColor = Color(0xFF1E2228)
        ) {
            InteractiveVotingSheetContent(
                candidateName = candidateName,
                currentVotes = currentVotes,
                userPointsBalance = userPointsBalance,
                rewardedAd = rewardedAd,
                onConfirmVote = { votesToCast, ptsCost ->
                    if (postId.isNullOrBlank()) {
                        Toast.makeText(context, "This entry is not ready to receive votes yet.", Toast.LENGTH_SHORT).show()
                    } else if (userPointsBalance < ptsCost) {
                        Toast.makeText(context, "Not enough points. Watch an ad to add 10.", Toast.LENGTH_SHORT).show()
                    } else contestViewModel.castVote(postId, votesToCast) { result ->
                        result.onSuccess { (votes, points) ->
                            currentVotes = votes
                            userPointsBalance = points
                            onCastVote(votesToCast, ptsCost)
                            Toast.makeText(context, "Vote recorded for $candidateName.", Toast.LENGTH_SHORT).show()
                            showVoteSheet = false
                        }.onFailure { err ->
                            Toast.makeText(context, err.message ?: "Vote failed", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onTopUpPoints = { _ ->
                    contestViewModel.topUp { result ->
                        result.onSuccess { points ->
                            userPointsBalance = points
                            Toast.makeText(context, "Points added. You now have $points.", Toast.LENGTH_SHORT).show()
                        }.onFailure { err ->
                            Toast.makeText(context, err.message ?: "Could not add points", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun InteractiveVotingSheetContent(
    candidateName: String,
    currentVotes: Int,
    userPointsBalance: Int,
    rewardedAd: RewardedAd?,
    onConfirmVote: (votesToCast: Int, ptsCost: Int) -> Unit,
    onTopUpPoints: (Int) -> Unit
) {
    val context = LocalContext.current
    var voteQty by remember { mutableIntStateOf(1) }
    val pointsCost = voteQty * 5
    val hasEnoughPoints = userPointsBalance >= pointsCost

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Vote for $candidateName",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Text(
            text = "Current Votes: $currentVotes",
            color = Color.Gray,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Quantity Selector: [-] [ N Votes ] [+]
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            IconButton(
                onClick = { if (voteQty > 1) voteQty-- },
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF2A303A), CircleShape)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color.White)
            }

            Text(
                text = "$voteQty Vote${if (voteQty == 1) "" else "s"}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(100.dp)
            )

            IconButton(
                onClick = { if (voteQty < 20) voteQty++ },
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF2563EB), CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White)
            }
        }

        Text(
            text = "Cost: $pointsCost Points (5 pts / vote)",
            color = Color(0xFFF59E0B),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
        )

        // User Points Balance Badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF2A303A),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(
                text = "Your Balance: $userPointsBalance Points",
                color = if (hasEnoughPoints) Color(0xFF10B981) else Color(0xFFEF4444),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Confirm Vote Button
        Button(
            onClick = { onConfirmVote(voteQty, pointsCost) },
            enabled = hasEnoughPoints,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2563EB),
                disabledContainerColor = Color(0xFF374151),
                disabledContentColor = Color.Gray
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = if (hasEnoughPoints) "Confirm Vote ($pointsCost pts)" else "Need ${pointsCost - userPointsBalance} More Points",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ad Top-Up Option
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Running low on points?", color = Color.Gray, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedButton(
                onClick = {
                    val activity = context as? Activity
                    if (activity != null && rewardedAd != null) {
                        rewardedAd.show(activity) { _ ->
                            onTopUpPoints(10)
                        }
                    } else {
                        // Fallback top up
                        onTopUpPoints(10)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C3AED)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text("🎬 Watch Video Ad (+10 pts)", color = Color(0xFFA78BFA), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
