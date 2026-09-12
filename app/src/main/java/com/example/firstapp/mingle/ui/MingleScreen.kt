package com.example.firstapp.mingle.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.firstapp.chat.ui.viewmodel.AdvertsViewModel
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
import com.example.firstapp.common.ui.viewmodel.UserViewModel
import com.example.firstapp.mingle.ui.viewmodel.MingleViewModel
import com.example.firstapp.ui.navigation.Screen
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
    val uiState by mingleViewModel.uiState.collectAsState()

    val points by advertsViewModel.points.collectAsState()
    val hourlyAdCount by advertsViewModel.hourlyAdCount.collectAsState()
    var showPointsGateDialog by remember { mutableStateOf(false) }

    var showRadiusSheet by remember { mutableStateOf(false) }
    var showUploadPhotosSheet by remember { mutableStateOf(false) }
    val selectedRadiusKm = uiState.radiusKm
    val mingleProfiles = uiState.profiles
    val myPhotos = uiState.myPhotos.mapNotNull { it.url }

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

    val totalPages = remember(mingleProfiles) {
        if (mingleProfiles.isEmpty()) 0
        else mingleProfiles.size + (mingleProfiles.size / 4)
    }
    val verticalPagerState = rememberPagerState(pageCount = { totalPages.coerceAtLeast(1) })

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
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mingle",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    // Notification Bell Icon Button
                    IconButton(onClick = { navController.navigate(Screen.MingleNotification.route) }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Radius filter pill
                    Surface(
                        onClick = { showRadiusSheet = true },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
                                text = "${selectedRadiusKm.toInt()} km",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Upload my Mingle photos
                    IconButton(onClick = { showUploadPhotosSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = "My Mingle Profile",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(bottom = bottomPadding)
        ) {
            if (uiState.isLoading && mingleProfiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (mingleProfiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.hasLocationPermission) Icons.Default.LocationOff else Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (!uiState.hasLocationPermission) "Enable location to mingle nearby"
                            else "No one found within ${selectedRadiusKm.toInt()} km",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Try increasing your discovery radius to see more people nearby.",
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                if (!uiState.hasLocationPermission) {
                                    locationPermissionLauncher.launch(
                                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                    )
                                } else {
                                    showRadiusSheet = true
                                }
                            },
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(if (!uiState.hasLocationPermission) "Enable Location" else "Increase Radius")
                        }
                    }
                }
            } else {
                VerticalPager(
                    state = verticalPagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val isAdPage = (pageIndex + 1) % 5 == 0
                    if (isAdPage) {
                        MingleNativeAdCard(
                            onContinue = {
                                if (pageIndex < totalPages - 1) {
                                    scope.launch { verticalPagerState.animateScrollToPage(pageIndex + 1) }
                                }
                            }
                        )
                    } else {
                        val profileIndex = pageIndex - (pageIndex / 5)
                        val profile = mingleProfiles.getOrNull(profileIndex) ?: return@VerticalPager
                        MingleCard(
                            profile = profile,
                            onLike = {
                                val available = if (uiState.hasLocation) uiState.balance else points
                                if (available < uiState.likeCost) {
                                    showPointsGateDialog = true
                                    return@MingleCard
                                }
                                mingleViewModel.swipe(
                                    profile = profile,
                                    action = "like",
                                    onMatched = { conversationId ->
                                        Toast.makeText(context, "It's a match with ${profile.name}! 🎉", Toast.LENGTH_SHORT).show()
                                        navController.navigate(Screen.Chat.createRoute(conversationId, "mingle"))
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
                                    onMatched = {},
                                    onInsufficientPoints = {}
                                )
                            },
                            onChat = {
                                mingleViewModel.openChat(
                                    userId = profile.id,
                                    onOpened = { conversationId ->
                                        navController.navigate(Screen.Chat.createRoute(conversationId, "mingle"))
                                    },
                                    onError = { message ->
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            onProfileClick = {
                                Toast.makeText(context, "🔒 Secret Mingle Profile - Profile details are kept private.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    val walletPoints = if (uiState.hasLocation) uiState.balance else points
    if (showPointsGateDialog || uiState.needsPoints) {
        val activity = context as? android.app.Activity
        MinglePointsGateDialog(
            points = walletPoints,
            hourlyAdCount = hourlyAdCount,
            canWatchAd = advertsViewModel.canWatchHourlyAd(),
            remainingMinutes = advertsViewModel.remainingMinutesInHourlyWindow(),
            onWatchAdClick = {
                if (activity != null) {
                    advertsViewModel.showRewardedAd(
                        activity,
                        com.example.firstapp.common.data.ads.AdUnitConfig.REWARDED_POINTS_ID,
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

    // Radius Filter Sheet
    if (showRadiusSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRadiusSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Discovery Radius",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Show people within ${selectedRadiusKm.toInt()} km of your location",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))

                Slider(
                    value = selectedRadiusKm,
                    onValueChange = { mingleViewModel.setRadius(it) },
                    valueRange = 1f..100f,
                    steps = 99
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1 km", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${selectedRadiusKm.toInt()} km", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("100 km", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        showRadiusSheet = false
                        mingleViewModel.applyRadiusAndReload()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Apply Filter")
                }
            }
        }
    }

    // Upload My Mingle Photos Sheet
    if (showUploadPhotosSheet) {
        ModalBottomSheet(
            onDismissRequest = { showUploadPhotosSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "My Mingle Photos (Up to 5)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Add up to 5 photos to showcase yourself on Mingle.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(5) { index ->
                        val photoUrl = myPhotos.getOrNull(index)
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
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = "Photo ${index + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
                                    Text("${index + 1}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Photos from Gallery")
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
    onProfileClick: () -> Unit
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
            val formatted = com.example.firstapp.common.util.MediaUtils.formatUrl(photoUrl)

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

        // User Info Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, end = 20.dp, bottom = 90.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onProfileClick() }
            ) {
                Text(
                    text = "${profile.name}, ${profile.age}",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Verified",
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NearMe,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${profile.distanceKm} km away",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = profile.bio,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Action Buttons Row (Pass, Like, Meet Chat)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pass Button
            FloatingActionButton(
                onClick = onPass,
                containerColor = Color.White.copy(alpha = 0.2f),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Pass", modifier = Modifier.size(28.dp))
            }

            // Like Button (Heart)
            FloatingActionButton(
                onClick = onLike,
                containerColor = Color(0xFFE91E63),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(68.dp)
            ) {
                Icon(Icons.Default.Favorite, contentDescription = "Like", modifier = Modifier.size(36.dp))
            }

            // Meet / Direct Chat Button
            FloatingActionButton(
                onClick = onChat,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.ChatBubble, contentDescription = "Meet Chat", modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun MingleNativeAdCard(
    onContinue: () -> Unit
) {
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
                com.example.firstapp.common.ui.components.NativeAdCard(
                    adUnitId = com.example.firstapp.common.data.ads.AdUnitConfig.NATIVE_POSTS_ID
                )
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
