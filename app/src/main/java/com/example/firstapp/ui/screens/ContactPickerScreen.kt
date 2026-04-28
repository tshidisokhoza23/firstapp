package com.example.firstapp.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.firstapp.model.Contact
import com.example.firstapp.ui.theme.*
import com.example.firstapp.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPickerScreen(
    viewModel: MainViewModel,
    initialIsGroupMode: Boolean = false,
    onBack: () -> Unit,
    onContactSelected: (Contact) -> Unit,
    onInviteClicked: (String) -> Unit,
    onGroupCreate: (List<Contact>) -> Unit
) {
    val context = LocalContext.current
    val contacts by viewModel.contacts.collectAsState()
    val phoneContacts = remember { mutableStateListOf<Contact>() }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var isGroupMode by remember { mutableStateOf(initialIsGroupMode) }
    val selectedForGroup = remember { mutableStateListOf<Contact>() }
    
    val isContactsPermissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            phoneContacts.clear()
            val loaded = loadPhoneContacts(context)
            phoneContacts.addAll(loaded)
            val phoneNumbers = loaded.map { it.phoneNumber }.filter { it.isNotBlank() }
            if (phoneNumbers.isNotEmpty()) {
                viewModel.syncContacts(phoneNumbers)
            }
        } else {
            Toast.makeText(context, "Allow contacts access to see which numbers are on FirstApp", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchContacts()
        if (isContactsPermissionGranted) {
            phoneContacts.clear()
            val loaded = loadPhoneContacts(context)
            phoneContacts.addAll(loaded)
            val phoneNumbers = loaded.map { it.phoneNumber }.filter { it.isNotBlank() }
            if (phoneNumbers.isNotEmpty()) {
                viewModel.syncContacts(phoneNumbers)
            }
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    val mergedContacts = remember(contacts, phoneContacts) {
        val backendByPhone = contacts.filter { it.phoneNumber.isNotBlank() }
            .associateBy { normalizePhoneNumber(it.phoneNumber) }

        val mergedFromPhone = phoneContacts.map { localContact ->
            val normalized = normalizePhoneNumber(localContact.phoneNumber)
            val backendMatch = backendByPhone[normalized]
            if (backendMatch != null) {
                localContact.copy(
                    id = backendMatch.id,
                    username = backendMatch.username.ifBlank { localContact.phoneNumber },
                    phoneNumber = localContact.phoneNumber,
                    isRegistered = true,
                    isAuthenticated = true,
                    isVerified = backendMatch.isVerified,
                    avatarUrl = backendMatch.avatarUrl
                )
            } else {
                localContact.copy(
                    username = localContact.phoneNumber,
                    isRegistered = false,
                    isAuthenticated = false
                )
            }
        }

        val phoneNumbersSet = mergedFromPhone.map { normalizePhoneNumber(it.phoneNumber) }.toSet()
        val remainingBackend = contacts.filter { it.phoneNumber.isNotBlank() && normalizePhoneNumber(it.phoneNumber) !in phoneNumbersSet }

        mergedFromPhone + remainingBackend
    }

    val visibleContacts = remember(mergedContacts, searchQuery) {
        if (searchQuery.isBlank()) {
            mergedContacts
        } else {
            val q = searchQuery.trim().lowercase()
            mergedContacts.filter {
                it.displayName.lowercase().contains(q) ||
                    it.phoneNumber.lowercase().contains(q) ||
                    it.username.lowercase().contains(q)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (isGroupMode) "Add participants" else "Select contact", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isGroupMode) "${selectedForGroup.size} of ${mergedContacts.size} selected" else "${mergedContacts.size} contacts",
                            fontSize = 12.sp, 
                            color = AppTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isGroupMode) {
                            isGroupMode = false
                            selectedForGroup.clear()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) { Icon(Icons.Default.Search, "Search") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppSecondary,
                    titleContentColor = AppTextPrimary,
                    navigationIconContentColor = AppTextPrimary,
                    actionIconContentColor = AppTextPrimary
                )
            )
        },
        floatingActionButton = {
            if (isGroupMode && selectedForGroup.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { onGroupCreate(selectedForGroup.toList()) },
                    containerColor = AppAccent,
                    contentColor = AppBackground,
                    shape = CircleShape
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                }
            }
        },
        containerColor = AppBackground
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isGroupMode && selectedForGroup.isNotEmpty()) {
                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppBackground)
                            .padding(vertical = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(selectedForGroup) { contact ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(60.dp)
                            ) {
                                Box(modifier = Modifier.size(50.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(AppAccent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(contact.displayName.take(1).uppercase(), color = AppBackground, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(
                                        onClick = { selectedForGroup.remove(contact) },
                                        modifier = Modifier
                                            .size(20.dp)
                                            .align(Alignment.BottomEnd)
                                            .background(Color.Gray, CircleShape)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                                Text(
                                    contact.displayName.split(" ").first(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                }
            }

            val (registered, unregistered) = visibleContacts.partition { it.isRegistered }
            val sortedRegistered = registered.sortedBy { it.displayName.lowercase() }
            val sortedUnregistered = unregistered.sortedBy { it.displayName.lowercase() }
            
            if (!isGroupMode) {
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { /* Handle New Contact */ }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(AppAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = AppBackground)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("New contact", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = AppTextSecondary)
                        }
                    }
                }
            }

            if (showSearch) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        label = { Text("Search contacts") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AppAccent,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = AppAccent,
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                }
            }

            if (!isContactsPermissionGranted && !isGroupMode) {
                item {
                    Text(
                        "Grant contacts permission so FirstApp can detect which numbers already use the app.",
                        modifier = Modifier.padding(16.dp),
                        color = AppTextSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            if (sortedRegistered.isNotEmpty()) {
                item { Text("Contacts on FirstApp", modifier = Modifier.padding(16.dp), color = AppAccent, fontWeight = FontWeight.Bold) }
                items(sortedRegistered) { contact ->
                    ContactItem(
                        contact = contact,
                        isSelected = selectedForGroup.contains(contact),
                        onClick = {
                            if (isGroupMode) {
                                if (selectedForGroup.contains(contact)) {
                                    selectedForGroup.remove(contact)
                                } else {
                                    selectedForGroup.add(contact)
                                }
                            } else {
                                onContactSelected(contact)
                            }
                        }
                    )
                }
            }
            
            if (sortedUnregistered.isNotEmpty() && !isGroupMode) {
                item { Text("Invite to FirstApp", modifier = Modifier.padding(16.dp), color = AppTextSecondary, fontWeight = FontWeight.Bold) }
                items(sortedUnregistered) { contact ->
                    ContactItem(contact) { onInviteClicked(contact.phoneNumber) }
                }
            }
        }
    }
}

@Composable
fun ContactItem(contact: Contact, isSelected: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (contact.isRegistered) AppAccent else Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = AppBackground)
            } else {
                Icon(Icons.Default.Person, contentDescription = null, tint = AppBackground)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(contact.displayName, color = AppTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(contact.username, color = AppTextSecondary, fontSize = 14.sp)
        }
        if (!contact.isAuthenticated && !isSelected) {
            Text("INVITE", color = AppAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

private fun normalizePhoneNumber(number: String): String {
    return number.filter { it.isDigit() }.let { digits ->
        if (digits.length > 10) digits.takeLast(10) else digits
    }
}

@SuppressLint("Range")
private fun loadPhoneContacts(context: Context): List<Contact> {
    val resolver = context.contentResolver
    val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER
    )
    val cursor = resolver.query(uri, projection, null, null, "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC")
    val contactsMap = mutableMapOf<String, Contact>()

    cursor?.use {
        while (it.moveToNext()) {
            val displayName = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)) ?: "Unknown"
            val rawNumber = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)) ?: continue
            val normalized = normalizePhoneNumber(rawNumber)
            if (normalized.isEmpty()) continue

            if (contactsMap[normalized] == null) {
                contactsMap[normalized] = Contact(
                    id = normalized.hashCode(),
                    username = rawNumber,
                    displayName = displayName,
                    phoneNumber = rawNumber,
                    isRegistered = false,
                    isAuthenticated = false
                )
            }
        }
    }

    return contactsMap.values.toList()
}
