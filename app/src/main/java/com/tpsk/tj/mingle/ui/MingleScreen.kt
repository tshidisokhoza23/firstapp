package com.tpsk.tj.mingle.ui

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.hilt.navigation.compose.hiltViewModel
import com.tpsk.tj.chat.ui.viewmodel.AdvertsViewModel
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.tpsk.tj.common.ui.viewmodel.UserViewModel
import com.tpsk.tj.mingle.ui.viewmodel.MingleViewModel
import com.tpsk.tj.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MingleScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    bottomPadding: Dp = 0.dp,
    advertsViewModel: AdvertsViewModel = hiltViewModel(),
    mingleViewModel: MingleViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        com.tpsk.tj.common.data.ads.NativeAdPool.warm(
            advertsViewModel.adManager,
            context,
            com.tpsk.tj.common.data.ads.AdUnitConfig.NATIVE_POSTS_ID,
            portrait = true,
            target = 2
        )
    }
    val uiState by mingleViewModel.uiState.collectAsState()
    val currentUser by userViewModel.currentUser.collectAsState()
    val contacts by userViewModel.contacts.collectAsState()
    val contactUserIds = remember(contacts) { contacts.map { it.id }.toSet() }

    LaunchedEffect(contactUserIds) {
        mingleViewModel.setExcludedContactIds(contactUserIds)
    }

    val points by advertsViewModel.points.collectAsState()
    val hourlyAdCount by advertsViewModel.hourlyAdCount.collectAsState()
    val rewardCooldownSeconds by advertsViewModel.cooldownRemainingSeconds.collectAsState()
    var showPointsGateDialog by remember { mutableStateOf(false) }
    var showProfileRequiredDialog by remember { mutableStateOf(false) }

    var showRadiusSheet by remember { mutableStateOf(false) }
    var showUploadPhotosSheet by remember { mutableStateOf(false) }
    var introTarget by remember { mutableStateOf<MingleProfile?>(null) }
    var introText by remember { mutableStateOf("") }
    val selectedRadiusKm = uiState.radiusKm
    val mingleProfiles = uiState.profiles
    val myPhotos = uiState.myPhotos.mapNotNull { it.url }

    val hasUploadedProfile = remember(myPhotos) {
        myPhotos.any { it.isNotBlank() }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        mingleViewModel.refreshPermission()
        if (granted.values.any { it }) {
            mingleViewModel.refreshLocationAndFeed()
        } else {
            Toast.makeText(context, "Location is required to find people nearby", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        mingleViewModel.refreshPermission()
        mingleViewModel.flushPendingSwipes()
        if (!uiState.hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        } else {
            mingleViewModel.refreshLocationAndFeed()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            mingleViewModel.clearError()
        }
    }

    val profilesBetweenAds = 8
    val totalPages = remember(mingleProfiles) {
        if (mingleProfiles.isEmpty()) 0
        else mingleProfiles.size + (mingleProfiles.size / profilesBetweenAds)
    }
    val verticalPagerState = rememberPagerState(pageCount = { totalPages.coerceAtLeast(1) })

    LaunchedEffect(verticalPagerState, mingleProfiles.size) {
        snapshotFlow { verticalPagerState.currentPage }.collect { page ->
            val profileIndex = page - (page / (profilesBetweenAds + 1))
            if (mingleProfiles.isNotEmpty() && profileIndex >= mingleProfiles.size - 3) {
                mingleViewModel.loadMore()
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            mingleViewModel.uploadPhotos(uris)
            Toast.makeText(context, "Uploading ${uris.size.coerceAtMost(5)} photo(s) to Mingle", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    // Left and Right controls row
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left section: Envelope + Bell + Rewind
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { navController.navigate(Screen.MingleChats.route) }) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Mingle Secret Chats",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            IconButton(onClick = { navController.navigate(Screen.MingleNotification.route) }) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            IconButton(onClick = { 
                                mingleViewModel.rewind { 
                                    Toast.makeText(context, "Restored previous candidate", Toast.LENGTH_SHORT).show() 
                                } 
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.RotateLeft,
                                    contentDescription = "Rewind Swipe",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Right section: Radius filter pill + Profile Camera
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                onClick = { showRadiusSheet = true },
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (selectedRadiusKm >= 100f) "100+ km" else "${selectedRadiusKm.toInt()} km",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(onClick = { showUploadPhotosSheet = true }) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = "My Mingle Profile",
                                    tint = if (hasUploadedProfile) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    // Absolute center: TJ Logo (centered relative to full screen width)
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        com.tpsk.tj.home.ui.components.AppLogo(size = 32.dp)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(bottom = bottomPadding)
        ) {
            if (!hasUploadedProfile) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable {
                            showUploadPhotosSheet = true
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Profile Requirement",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Upload photos to complete your Mingle profile to like candidates.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Text(
                                text = "Upload Now",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (uiState.isLoading && mingleProfiles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (mingleProfiles.isEmpty()) {
                    val reason = if (!uiState.hasLocationPermission) "NO_LOCATION" else uiState.emptyReason
                    val copy = emptyDeckCopy(reason, selectedRadiusKm.toInt(), uiState.hasLocationPermission)
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = if (reason == "NO_LOCATION") Icons.Default.LocationOn else Icons.Default.LocationOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = copy.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = copy.body,
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (uiState.emptyReason == "DAILY_LIMIT") {
                                Spacer(modifier = Modifier.height(20.dp))
                                val extendPrice = uiState.extendCost.toInt()
                                Button(
                                    onClick = { mingleViewModel.extendFeed("coins") },
                                    enabled = extendPrice > 0,
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Text(if (extendPrice > 0) "See 30 more for $extendPrice coins" else "Daily limit")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        val activity = context as? android.app.Activity
                                        if (activity != null) {
                                            advertsViewModel.showRewardedAd(
                                                activity,
                                                com.tpsk.tj.common.data.ads.AdUnitConfig.REWARDED_POINTS_ID,
                                                "mingle_feed_extend"
                                            ) { mingleViewModel.extendFeed("ad") }
                                        }
                                    },
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Text("Watch 1 ad for 20 more profiles")
                                }
                            } else if (copy.action != EmptyDeckAction.NONE) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = {
                                        when (copy.action) {
                                            EmptyDeckAction.ENABLE_LOCATION -> locationPermissionLauncher.launch(
                                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                            )
                                            EmptyDeckAction.RETRY_LOCATION -> mingleViewModel.refreshLocationAndFeed()
                                            EmptyDeckAction.INCREASE_RADIUS -> showRadiusSheet = true
                                            EmptyDeckAction.EDIT_FILTERS -> showUploadPhotosSheet = true
                                            EmptyDeckAction.REFRESH -> mingleViewModel.refreshLocationAndFeed()
                                            EmptyDeckAction.NONE -> Unit
                                        }
                                    },
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Text(copy.actionLabel)
                                }
                            }
                        }
                    }
                } else {
                    VerticalPager(
                        state = verticalPagerState,
                        beyondViewportPageCount = 2,
                        modifier = Modifier.fillMaxSize(),
                        key = { pageIndex ->
                            val isAd = (pageIndex + 1) % (profilesBetweenAds + 1) == 0
                            if (isAd) "ad_$pageIndex" else "profile_$pageIndex"
                        }
                    ) { pageIndex ->
                        val isAdPage = (pageIndex + 1) % (profilesBetweenAds + 1) == 0
                        if (isAdPage) {
                            val playAd = verticalPagerState.currentPage == pageIndex &&
                                verticalPagerState.currentPageOffsetFraction == 0f
                            MingleNativeAdCard(
                                adManager = advertsViewModel.adManager,
                                playMedia = playAd,
                                slotKey = "mingle_ad_$pageIndex",
                                onContinue = {
                                    if (pageIndex < totalPages - 1) {
                                        scope.launch { verticalPagerState.animateScrollToPage(pageIndex + 1) }
                                    }
                                }
                            )
                        } else {
                            val profileIndex = pageIndex - (pageIndex / (profilesBetweenAds + 1))
                            val profile = mingleProfiles.getOrNull(profileIndex) ?: return@VerticalPager
                            MingleCard(
                                profile = profile,
                                onLike = {
                                    if (!hasUploadedProfile) {
                                        showUploadPhotosSheet = true
                                        return@MingleCard
                                    }
                                    val walletBalance = currentUser?.walletBalance ?: 0.0
                                    val available = maxOf(uiState.balance, points.toDouble(), walletBalance)
                                    val paidLike = uiState.freeLikesRemaining <= 0
                                    val likePrice = if (paidLike) maxOf(uiState.nextLikeCost, uiState.likeCost) else 0.0
                                    if (paidLike && available + 0.001 < likePrice) {
                                        showPointsGateDialog = true
                                        return@MingleCard
                                    }
                                    mingleViewModel.swipe(
                                        profile = profile,
                                        action = "like",
                                        candidateName = profile.name,
                                        onMatched = { conversationId ->
                                            Toast.makeText(context, "It's a match with ${profile.name}! 🎉", Toast.LENGTH_SHORT).show()
                                            navController.navigate(Screen.Chat.createRoute(conversationId, type = "mingle", displayName = profile.name))
                                        },
                                        onInsufficientPoints = {
                                            showPointsGateDialog = true
                                        }
                                    )
                                },
                                onPass = {
                                    mingleViewModel.swipe(
                                        profile = profile,
                                        action = "pass",
                                        candidateName = profile.name,
                                        onMatched = {},
                                        onInsufficientPoints = {}
                                    )
                                },
                                onChat = {
                                    introText = ""
                                    introTarget = profile
                                },
                                onProfileClick = {
                                    Toast.makeText(context, "🔒 Secret Mingle Profile - Profile details are kept private.", Toast.LENGTH_SHORT).show()
                                },
                                onSuperLike = {
                                    if (!hasUploadedProfile) {
                                        showUploadPhotosSheet = true
                                        return@MingleCard
                                    }
                                    val price = uiState.superLikeCost
                                    val available = maxOf(uiState.balance, points.toDouble(), currentUser?.walletBalance ?: 0.0)
                                    if (price > 0 && available + 0.001 < price) {
                                        showPointsGateDialog = true
                                        return@MingleCard
                                    }
                                    mingleViewModel.swipe(
                                        profile = profile,
                                        action = "super_like",
                                        candidateName = profile.name,
                                        onMatched = { conversationId ->
                                            Toast.makeText(context, "It's a match with ${profile.name}!", Toast.LENGTH_SHORT).show()
                                            navController.navigate(Screen.Chat.createRoute(conversationId, type = "mingle", displayName = profile.name))
                                        },
                                        onInsufficientPoints = { showPointsGateDialog = true }
                                    )
                                },
                                onBoost = { mingleViewModel.boostProfile() },
                                superLikeCost = uiState.superLikeCost.toInt(),
                                boostCost = uiState.boostCost.toInt()
                            )
                        }
                    }
                }
            }
        }
    }

    if (showProfileRequiredDialog) {
        AlertDialog(
            onDismissRequest = { showProfileRequiredDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.AddAPhoto,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Profile Required to Like",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "You must upload at least one photo or complete your Mingle profile before you can like people.",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showProfileRequiredDialog = false
                        showUploadPhotosSheet = true
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.AccountBox, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Complete Profile Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileRequiredDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val walletPoints = maxOf(uiState.balance, points)
    if (showPointsGateDialog || uiState.needsPoints) {
        val activity = context as? android.app.Activity
        MinglePointsGateDialog(
            points = walletPoints,
            hourlyAdCount = hourlyAdCount,
            canWatchAd = advertsViewModel.canWatchHourlyAd(),
            remainingMinutes = advertsViewModel.remainingMinutesInHourlyWindow(),
            cooldownSeconds = rewardCooldownSeconds,
            onWatchAdClick = {
                if (activity != null) {
                    advertsViewModel.showRewardedAd(
                        activity,
                        com.tpsk.tj.common.data.ads.AdUnitConfig.REWARDED_POINTS_ID,
                        "mingle_gate_ad"
                    )
                }
                showPointsGateDialog = false
                mingleViewModel.clearPointsGate()
            },
            onOpenWalletClick = {
                showPointsGateDialog = false
                mingleViewModel.clearPointsGate()
                navController.navigate(Screen.Wallet.route)
            },
            onDismiss = {
                showPointsGateDialog = false
                mingleViewModel.clearPointsGate()
            }
        )
    }

    introTarget?.let { target ->
        ModalBottomSheet(onDismissRequest = { introTarget = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text("Say hi to ${target.name}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    if (uiState.introCost > 0) {
                        "One message, up to 150 characters. This costs ${uiState.introCost.toInt()} points and cannot be refunded. They will not see that it was paid."
                    } else {
                        "One message, up to 150 characters. The server sets the price. It cannot be refunded, and they will not see that it was paid."
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = introText,
                    onValueChange = { if (it.length <= 150) introText = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("Plain text only. No links.") },
                    supportingText = { Text("${introText.length}/150") }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val text = introText.trim()
                        if (text.isEmpty()) return@Button
                        mingleViewModel.sendIntro(
                            recipientId = target.id,
                            message = text,
                            onSent = {
                                introTarget = null
                                Toast.makeText(context, "Request sent", Toast.LENGTH_SHORT).show()
                            },
                            onNeedPoints = {
                                introTarget = null
                                showPointsGateDialog = true
                            }
                        )
                    },
                    enabled = introText.trim().isNotEmpty() && uiState.introCost > 0,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(if (uiState.introCost > 0) "Send for ${uiState.introCost.toInt()} points" else "Waiting for the server price")
                }
            }
        }
    }

    // Radius Filter Sheet (Section 5.1 Discovery Radius Sheet)
    if (showRadiusSheet) {
        val displayValue = selectedRadiusKm.toInt()
        ModalBottomSheet(
            onDismissRequest = { showRadiusSheet = false },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Discovery Radius",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (displayValue >= 100) {
                        "Showing people everywhere (expanding beyond 100 km)"
                    } else {
                        "Show people within $displayValue km of your location"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(28.dp))
                Slider(
                    value = selectedRadiusKm,
                    onValueChange = { mingleViewModel.setRadius(it) },
                    valueRange = 1f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF0F3995),
                        activeTrackColor = Color(0xFF0F3995),
                        inactiveTrackColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1 km", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (displayValue >= 100) "100+ km" else "$displayValue km",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F3995)
                    )
                    Text("100 km", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        showRadiusSheet = false
                        mingleViewModel.applyRadiusAndReload()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F3995))
                ) {
                    Text("Apply Filter", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }



    // Upload My Mingle Photos & Profile Sheet
    if (showUploadPhotosSheet) {
        var editName by remember(uiState.myName, currentUser) {
            mutableStateOf(uiState.myName.ifBlank { currentUser?.displayName.orEmpty() })
        }
        var nameError by remember { mutableStateOf(false) }
        var editAge by remember(uiState.myAge) {
            mutableStateOf(if (uiState.myAge in 18..120) uiState.myAge.toString() else "")
        }
        var editBio by remember(uiState.myBio, currentUser) {
            mutableStateOf(uiState.myBio.ifBlank { currentUser?.bio.orEmpty() })
        }
        var editGender by remember(uiState.myGender) {
            mutableStateOf(uiState.myGender.ifBlank { "Other" })
        }
        var editLookingFor by remember(uiState.myLookingFor) {
            mutableStateOf(uiState.myLookingFor.ifBlank { "Everyone" })
        }
        var editTargetAgeRange by remember(uiState.myTargetAgeRange) {
            mutableStateOf(uiState.myTargetAgeRange.ifBlank { "18 - 35" })
        }

        ModalBottomSheet(
            onDismissRequest = { showUploadPhotosSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "My Mingle Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Update your photos and profile details visible to people on Mingle.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Photos section header
                Text(
                    text = "Photos (Up to 5)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(5) { index ->
                        val photoDto = uiState.myPhotos.getOrNull(index)
                        val photoUrl = photoDto?.url
                        val photoId = photoDto?.id

                        Box(
                            modifier = Modifier
                                .size(90.dp, 120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .clickable {
                                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!photoUrl.isNullOrBlank()) {
                                val formattedPhotoUrl = com.tpsk.tj.common.util.MediaUtils.formatUrl(photoUrl)
                                AsyncImage(
                                    model = formattedPhotoUrl ?: photoUrl,
                                    contentDescription = "Photo ${index + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                if (!photoId.isNullOrBlank()) {
                                    IconButton(
                                        onClick = { mingleViewModel.deletePhoto(photoId) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(26.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                            .padding(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete photo",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
                                    Text("${index + 1}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Photos from Gallery")
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Profile Details section header
                Text(
                    text = "Profile Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Preferred Mingle Display Name field
                OutlinedTextField(
                    value = editName,
                    onValueChange = {
                        editName = it
                        if (it.isNotBlank()) nameError = false
                    },
                    label = { Text("Preferred Mingle Display Name *") },
                    placeholder = { Text("e.g. Alex, Sam, Joy") },
                    supportingText = {
                        if (nameError) {
                            Text("Mingle display name is required", color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("The display name shown on your Mingle profile")
                        }
                    },
                    isError = nameError,
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Age field
                OutlinedTextField(
                    value = editAge,
                    onValueChange = { editAge = it.filter { char -> char.isDigit() }.take(3) },
                    label = { Text("Age") },
                    leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Bio field
                OutlinedTextField(
                    value = editBio,
                    onValueChange = { editBio = it },
                    label = { Text("About Me / Bio") },
                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Gender Selection (LazyRow ensures chips fit cleanly without text clipping)
                Text(
                    text = "Gender",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(listOf("Male", "Female", "Non-binary", "Other")) { g ->
                        val selected = genderChipSelected(editGender, g)
                        FilterChip(
                            selected = selected,
                            onClick = { editGender = g },
                            label = { Text(g, fontSize = 12.sp, softWrap = false) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Looking For Selection
                Text(
                    text = "Looking For",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(listOf("Men", "Women", "Non-binary", "Everyone")) { option ->
                        val selected = lookingForChipSelected(editLookingFor, option)
                        FilterChip(
                            selected = selected,
                            onClick = { editLookingFor = option },
                            label = { Text(option, fontSize = 12.sp, softWrap = false) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Target Age Range Selection (18+)
                Text(
                    text = "Target Age Range (18+)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                var ageRangeExpanded by remember { mutableStateOf(false) }
                val ageRangeOptions = listOf("18 - 25", "18 - 30", "18 - 35", "25 - 35", "30 - 45", "35 - 50", "18+ (All ages 18+)")

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editTargetAgeRange,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Age Range") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { ageRangeExpanded = !ageRangeExpanded }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Range")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { ageRangeExpanded = true },
                        shape = RoundedCornerShape(12.dp)
                    )
                    DropdownMenu(
                        expanded = ageRangeExpanded,
                        onDismissRequest = { ageRangeExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        ageRangeOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    editTargetAgeRange = option
                                    ageRangeExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save & Update Button
                Button(
                    onClick = {
                        if (editName.isBlank()) {
                            nameError = true
                            Toast.makeText(context, "Please enter your preferred Mingle display name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val parsedAge = editAge.toIntOrNull()
                        if (parsedAge == null || parsedAge !in 18..120) {
                            Toast.makeText(context, "Enter an age from 18 to 120", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val normalizedGender = when (editGender.trim().lowercase()) {
                            "male", "man" -> "male"
                            "female", "woman" -> "female"
                            "nonbinary", "non-binary" -> "nonbinary"
                            else -> "other"
                        }
                        val normalizedLookingFor = when (editLookingFor.trim().lowercase()) {
                            "male", "men", "man" -> "male"
                            "female", "women", "woman" -> "female"
                            "nonbinary", "non-binary" -> "nonbinary"
                            else -> "everyone"
                        }
                        val normalizedAgeRange = when {
                            editTargetAgeRange.isBlank() -> "18 - 35"
                            editTargetAgeRange.trim().startsWith("18+") -> "18 - 120"
                            else -> editTargetAgeRange.trim()
                        }

                        mingleViewModel.updateProfile(
                            name = editName.trim(),
                            bio = editBio,
                            age = parsedAge,
                            gender = normalizedGender,
                            lookingFor = normalizedLookingFor,
                            targetAgeRange = normalizedAgeRange,
                            onComplete = {
                                // REQUIREMENT: Mingle bio remains private to Mingle and does not overwrite main profile bio
                                userViewModel.updateProfile(
                                    displayName = editName.trim(),
                                    bio = null,
                                    location = null,
                                    avatarUrl = null
                                )
                                Toast.makeText(context, "Mingle Profile Updated! 🎉", Toast.LENGTH_SHORT).show()
                                showUploadPhotosSheet = false
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save & Update Profile", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MingleCard(
    profile: MingleProfile,
    onLike: () -> Unit,
    onPass: () -> Unit,
    onChat: () -> Unit,
    onProfileClick: () -> Unit,
    onSuperLike: () -> Unit = {},
    onBoost: () -> Unit = {},
    superLikeCost: Int = 0,
    boostCost: Int = 0
) {
    val horizontalPagerState = rememberPagerState(pageCount = { profile.photos.size.coerceAtLeast(1) })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black)
    ) {
        // Horizontal Photo Carousel (Up to 5 pictures)
        HorizontalPager(
            state = horizontalPagerState,
            modifier = Modifier.fillMaxSize()
        ) { photoIndex ->
            val photoUrl = profile.photos.getOrNull(photoIndex)
            val formatted = com.tpsk.tj.common.util.MediaUtils.formatUrl(photoUrl)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(photoIndex) {
                        detectTapGestures(
                            onTap = { offset ->
                                if (offset.x < size.width / 2) {
                                    if (photoIndex > 0) {
                                        scope.launch { horizontalPagerState.animateScrollToPage(photoIndex - 1) }
                                    }
                                } else {
                                    if (photoIndex < profile.photos.size - 1) {
                                        scope.launch { horizontalPagerState.animateScrollToPage(photoIndex + 1) }
                                    }
                                }
                            }
                        )
                    }
            ) {
                if (formatted != null) {
                    AsyncImage(
                        model = formatted,
                        contentDescription = profile.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Color(0xFF3F51B5), Color(0xFF1A237E)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 64.sp
                        )
                    }
                }
            }
        }

        // Top Photo Dot Indicators (Circles)
        if (profile.photos.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                profile.photos.forEachIndexed { idx, _ ->
                    val isSelected = idx == horizontalPagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (isSelected) 10.dp else 7.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.White
                                else Color.White.copy(alpha = 0.4f)
                            )
                    )
                }
            }
        }

        // Bottom Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )

        // Unified Bottom Content Overlay (User Info + Action Bar)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // User Info Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onProfileClick() }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val ageText = if (profile.age > 0) "${profile.name}, ${profile.age}" else profile.name
                    Text(
                        text = ageText,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val distanceText = if (profile.distanceKm > 0) "${profile.distanceKm} km away" else "Nearby"
                    Text(
                        text = distanceText,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (profile.bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = profile.bio,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons Bar (Pass, SuperLike, Like, Meet Chat)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(32.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(32.dp))
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pass Button
                FloatingActionButton(
                    onClick = onPass,
                    containerColor = Color.White.copy(alpha = 0.22f),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Pass", modifier = Modifier.size(26.dp))
                }

                // SuperLike Button
                FloatingActionButton(
                    onClick = onSuperLike,
                    containerColor = Color(0xFFFFC107),
                    contentColor = Color.Black,
                    shape = CircleShape,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = if (superLikeCost > 0) "Super like, $superLikeCost coins" else "Super like", modifier = Modifier.size(26.dp))
                }

                // Like Button (Heart)
                FloatingActionButton(
                    onClick = onLike,
                    containerColor = Color(0xFFE91E63),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = "Like", modifier = Modifier.size(34.dp))
                }

                // Meet / Direct Chat Button
                FloatingActionButton(
                    onClick = onChat,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.Default.ChatBubble, contentDescription = "Meet Chat", modifier = Modifier.size(26.dp))
                }
            }
        }
    }
}

@Composable
private fun MingleNativeAdCard(
    adManager: com.tpsk.tj.common.data.ads.AdManager,
    playMedia: Boolean,
    slotKey: String,
    onContinue: () -> Unit
) {
    val ad = com.tpsk.tj.common.ui.components.awaitPooledNativeAd(
        adManager = adManager,
        unitId = com.tpsk.tj.common.data.ads.AdUnitConfig.NATIVE_POSTS_ID,
        portrait = true,
        slotKey = slotKey
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFFF9800).copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sponsored Ad 📢",
                        color = Color(0xFFFF9800),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (ad == null) {
                    com.tpsk.tj.common.ui.components.NativeAdShimmer(Modifier.fillMaxSize())
                } else {
                    com.tpsk.tj.common.ui.components.ReadyNativeAd(
                        adObject = ad,
                        modifier = Modifier.fillMaxSize(),
                        layoutResId = com.tpsk.tj.R.layout.item_short_native_ad,
                        playMedia = playMedia,
                        cornerRadius = 16
                    )
                }
            }

            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(52.dp)
            ) {
                Text("Continue Mingling ➡️", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }
        }
    }
}

private fun genderChipSelected(stored: String, chip: String): Boolean {
    val value = stored.trim().lowercase()
    return when (chip) {
        "Male" -> value == "male" || value == "man"
        "Female" -> value == "female" || value == "woman"
        "Non-binary" -> value == "nonbinary" || value == "non-binary"
        "Other" -> value.isBlank() || value == "other"
        else -> stored.equals(chip, ignoreCase = true)
    }
}

private fun lookingForChipSelected(stored: String, chip: String): Boolean {
    val value = stored.trim().lowercase()
    return when (chip) {
        "Men" -> value == "men" || value == "male" || value == "man"
        "Women" -> value == "women" || value == "female" || value == "woman"
        "Non-binary" -> value == "nonbinary" || value == "non-binary"
        "Everyone" -> value.isBlank() || value == "everyone" || value == "all"
        else -> stored.equals(chip, ignoreCase = true)
    }
}

private enum class EmptyDeckAction { NONE, ENABLE_LOCATION, RETRY_LOCATION, INCREASE_RADIUS, EDIT_FILTERS, REFRESH }

private data class EmptyDeckCopy(
    val title: String,
    val body: String,
    val action: EmptyDeckAction,
    val actionLabel: String
)

private fun emptyDeckCopy(reason: String?, radiusKm: Int, hasLocationPermission: Boolean): EmptyDeckCopy {
    val resolved = if (reason == "NO_LOCATION" && hasLocationPermission) "RETRY_LOCATION" else reason
    return when (resolved) {
        "NO_LOCATION" -> EmptyDeckCopy(
            title = "Location is required",
            body = "Mingle can only show people near you after location is on.",
            action = EmptyDeckAction.ENABLE_LOCATION,
            actionLabel = "Enable Location"
        )
        "RETRY_LOCATION" -> EmptyDeckCopy(
            title = "Location is not available",
            body = "Your location could not be read. Try again outside, or check that location is on.",
            action = EmptyDeckAction.RETRY_LOCATION,
            actionLabel = "Try again"
        )
        "NO_CANDIDATE_LOCATION" -> EmptyDeckCopy(
            title = "No shared locations yet",
            body = "Other people are on Mingle, but none of them have shared a location.",
            action = EmptyDeckAction.REFRESH,
            actionLabel = "Check again"
        )
        "OUT_OF_RADIUS" -> EmptyDeckCopy(
            title = "No one within $radiusKm km",
            body = "People with photos are farther away. Increasing your radius will look farther.",
            action = EmptyDeckAction.INCREASE_RADIUS,
            actionLabel = "Increase Radius"
        )
        "NO_PHOTO" -> EmptyDeckCopy(
            title = "No photos nearby",
            body = "People in range have not added a Mingle photo yet, so their cards stay hidden.",
            action = EmptyDeckAction.REFRESH,
            actionLabel = "Check again"
        )
        "DAILY_LIMIT" -> EmptyDeckCopy(
            title = "Daily profile limit reached",
            body = "The free daily browse is used. Spend coins or watch a completed rewarded ad for more profiles.",
            action = EmptyDeckAction.NONE,
            actionLabel = ""
        )
        "DIFFERENT_COUNTRY" -> EmptyDeckCopy(
            title = "No one from your country",
            body = "People in range are in a different country. Mingle only shows the same country.",
            action = EmptyDeckAction.INCREASE_RADIUS,
            actionLabel = "Look farther"
        )
        "CONTACTS_HIDDEN" -> EmptyDeckCopy(
            title = "Nearby people are in your contacts",
            body = "Mingle hides people saved in your contacts, including matching phone numbers.",
            action = EmptyDeckAction.NONE,
            actionLabel = ""
        )
        "ALREADY_SWIPED" -> EmptyDeckCopy(
            title = "You've already seen everyone nearby",
            body = "Passed and liked profiles stay hidden. Increase your radius to look farther.",
            action = EmptyDeckAction.INCREASE_RADIUS,
            actionLabel = "Increase Radius"
        )
        "PREFERENCES" -> EmptyDeckCopy(
            title = "No one matches your filters",
            body = "People nearby are outside the age range or gender you are looking for.",
            action = EmptyDeckAction.EDIT_FILTERS,
            actionLabel = "Edit filters"
        )
        else -> EmptyDeckCopy(
            title = "No one else is on Mingle nearby",
            body = "When someone nearby opens Mingle and adds a photo, they will show up here.",
            action = EmptyDeckAction.REFRESH,
            actionLabel = "Check again"
        )
    }
}
