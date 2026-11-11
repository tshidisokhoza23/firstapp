package com.tpsk.tj.boards.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateBoardPostScreen(
    board: String,
    onBack: () -> Unit,
    onPostCreated: () -> Unit,
    viewModel: BoardViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    
    // Rent
    val rentAmenities = listOf("Wi-Fi", "Single Room", "Sharing", "Parking", "En-suite", "Laundry")
    var selectedAmenities by remember { mutableStateOf(setOf<String>()) }
    var availabilityDate by remember { mutableStateOf("") }
    var contactDetails by remember { mutableStateOf("") }
    
    // Market
    val marketCategories = listOf("Appliances", "Clothes", "Electronics", "Furniture", "Others")
    var selectedCategory by remember { mutableStateOf(setOf<String>()) }
    var isNegotiable by remember { mutableStateOf(false) }
    val conditions = listOf("Brand New", "Like New", "Good", "Fair")
    var selectedCondition by remember { mutableStateOf("") }
    
    // Services
    val serviceCategories = listOf("Plumbers", "Electricians", "Painters", "Carpenters")
    var selectedServiceCategory by remember { mutableStateOf("") }
    var contactPerson by remember { mutableStateOf("") }
    var specificBadges by remember { mutableStateOf("") }
    val pricingModels = listOf("From R250", "Call-out from R300", "Quotes available")
    var selectedPricingModel by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    
    val headerColor = Color(0xFF0B0F3A)
    val accentColor = Color(0xFF583EF2)
    val bgColor = Color.White
    val textColor = Color(0xFF1F1F1F)

    val headerTitle = when (board) {
        "rent" -> "Rooms available"
        "market" -> "Second-hand market"
        "services" -> "Handyman services"
        else -> "Create Post"
    }

    val isPublishEnabled = when (board) {
        "rent" -> title.isNotBlank() && location.isNotBlank() && price.isNotBlank() && contactDetails.isNotBlank()
        "market" -> title.isNotBlank() && location.isNotBlank() && price.isNotBlank() && selectedCategory.isNotEmpty() && selectedCondition.isNotBlank()
        "services" -> title.isNotBlank() && contactPerson.isNotBlank() && selectedServiceCategory.isNotBlank() && phoneNumber.isNotBlank()
        else -> false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(headerTitle, color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val desc = when (board) {
                                "rent" -> "Amenities: ${selectedAmenities.joinToString()}, Date: $availabilityDate, Contact: $contactDetails"
                                "market" -> "Category: ${selectedCategory.joinToString()}, Negotiable: $isNegotiable, Condition: $selectedCondition"
                                "services" -> "Category: $selectedServiceCategory, Contact: $contactPerson, Badges: $specificBadges, Pricing: $selectedPricingModel, Phone: $phoneNumber"
                                else -> ""
                            }
                            viewModel.publish(
                                title = title,
                                description = desc,
                                price = price,
                                area = location,
                                photo = "placeholder.jpg"
                            )
                            onPostCreated()
                        },
                        enabled = isPublishEnabled,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White,
                            disabledContentColor = Color.White.copy(alpha = 0.5f)
                        )
                    ) {
                        Text("Publish", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = headerColor)
            )
        },
        containerColor = bgColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            when (board) {
                "rent" -> {
                    Text("Create a Listing", style = MaterialTheme.typography.titleLarge, color = textColor, fontWeight = FontWeight.Bold)
                    
                    ImageUploadSquare(label = "Images *", hint = "+ Add photos")
                    
                    CustomTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = "Room to Rent Title",
                        required = true,
                        placeholder = "e.g., Single room in Tembisa"
                    )
                    
                    CustomTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = "Location / Area",
                        required = true,
                        placeholder = "e.g., Tembisa, Gauteng"
                    )
                    
                    RequiredLabelText("Amenities Badges", true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        rentAmenities.forEach { amenity ->
                            CustomChip(
                                text = amenity,
                                selected = selectedAmenities.contains(amenity),
                                onClick = { 
                                    if (selectedAmenities.contains(amenity)) selectedAmenities -= amenity
                                    else selectedAmenities += amenity
                                },
                                accentColor = accentColor
                            )
                        }
                    }
                    
                    CustomTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = "Monthly Rental Rate",
                        required = true,
                        prefix = "R ",
                        placeholder = "1 200 / month",
                        keyboardType = KeyboardType.Number
                    )
                    
                    CustomTextField(
                        value = availabilityDate,
                        onValueChange = { availabilityDate = it },
                        label = "Availability Date",
                        required = false,
                        placeholder = "Immediate"
                    )
                    
                    CustomTextField(
                        value = contactDetails,
                        onValueChange = { contactDetails = it },
                        label = "Contact Details",
                        required = true,
                        placeholder = "Phone number or email",
                        keyboardType = KeyboardType.Phone
                    )
                }
                "market" -> {
                    Text("Create a Listing", style = MaterialTheme.typography.titleLarge, color = textColor, fontWeight = FontWeight.Bold)
                    
                    ImageUploadGallery(label = "Image uploading gallery *")
                    
                    CustomTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = "Item Name",
                        required = true,
                        placeholder = "e.g., Samsung Galaxy S24"
                    )
                    
                    RequiredLabelText("Item Category Tag", true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        marketCategories.forEach { cat ->
                            CustomChip(
                                text = cat,
                                selected = selectedCategory.contains(cat),
                                onClick = { 
                                    if (selectedCategory.contains(cat)) selectedCategory -= cat
                                    else selectedCategory += cat
                                },
                                accentColor = accentColor
                            )
                        }
                    }
                    
                    CustomTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = "Item Price",
                        required = true,
                        prefix = "R ",
                        placeholder = "7 500",
                        keyboardType = KeyboardType.Number
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isNegotiable,
                            onCheckedChange = { isNegotiable = it },
                            colors = CheckboxDefaults.colors(checkedColor = accentColor, uncheckedColor = Color.Gray)
                        )
                        Text("Negotiable", color = textColor)
                    }
                    
                    RequiredLabelText("Item Condition", true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        conditions.forEach { cond ->
                            CustomChip(
                                text = cond,
                                selected = selectedCondition == cond,
                                onClick = { selectedCondition = cond },
                                accentColor = accentColor
                            )
                        }
                    }
                    
                    CustomTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = "Pick-up / Location",
                        required = true,
                        placeholder = "e.g., Tembisa, Gauteng"
                    )
                }
                "services" -> {
                    Text("Create a Listing", style = MaterialTheme.typography.titleLarge, color = textColor, fontWeight = FontWeight.Bold)
                    
                    ImageUploadSquare(label = "Individual/Company Logo or Profile Image", hint = "+ Upload Logo", required = false)
                    
                    RequiredLabelText("Primary Service Category", true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        serviceCategories.forEach { cat ->
                            CustomChip(
                                text = cat,
                                selected = selectedServiceCategory == cat,
                                onClick = { selectedServiceCategory = cat },
                                accentColor = accentColor
                            )
                        }
                    }
                    
                    CustomTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = "Service Title",
                        required = true,
                        placeholder = "e.g., Emergency plumbing"
                    )
                    
                    CustomTextField(
                        value = contactPerson,
                        onValueChange = { contactPerson = it },
                        label = "Main Contact Person / Business Name",
                        required = true,
                        placeholder = "e.g., Thabo Plumbing"
                    )
                    
                    CustomTextField(
                        value = specificBadges,
                        onValueChange = { specificBadges = it },
                        label = "Specific service badges",
                        required = false,
                        placeholder = "e.g., Burst Pipes, Leak Repairs"
                    )
                    
                    RequiredLabelText("Pricing Model dropdown", true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        pricingModels.forEach { model ->
                            CustomChip(
                                text = model,
                                selected = selectedPricingModel == model,
                                onClick = { selectedPricingModel = model },
                                accentColor = accentColor
                            )
                        }
                    }
                    
                    CustomTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = "Call details / Phone Number",
                        required = true,
                        placeholder = "e.g., 082 123 4567",
                        keyboardType = KeyboardType.Phone
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun RequiredLabelText(text: String, required: Boolean) {
    Text(
        text = buildAnnotatedString {
            append(text)
            if (required) {
                withStyle(style = SpanStyle(color = Color.Red)) {
                    append(" *")
                }
            }
        },
        color = Color(0xFF1F1F1F),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    required: Boolean,
    placeholder: String = "",
    prefix: String = "",
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RequiredLabelText(label, required)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.Gray) },
            prefix = if (prefix.isNotEmpty()) { { Text(prefix, color = Color.Black) } } else null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF583EF2),
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                cursorColor = Color(0xFF583EF2)
            ),
            singleLine = true
        )
    }
}

@Composable
fun CustomChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (selected) accentColor else Color(0xFFF0F0F0),
        modifier = Modifier
            .clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color(0xFF4A4A4A),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun ImageUploadSquare(label: String, hint: String, required: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RequiredLabelText(label, required)
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF5F5F5))
                .border(1.dp, Color(0xFFD0D0D0), RoundedCornerShape(12.dp))
                .clickable { /* action */ },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Filled.Image, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(28.dp))
                Text(hint, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun ImageUploadGallery(label: String, required: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RequiredLabelText(label, required)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF5F5F5))
                        .border(1.dp, Color(0xFFD0D0D0), RoundedCornerShape(12.dp))
                        .clickable { /* action */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}
