package com.example.firstapp.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
    onBack: () -> Unit,
    onContactSelected: (Contact) -> Unit,
    onInviteClicked: (String) -> Unit
) {
    val context = LocalContext.current
    val contacts by viewModel.contacts.collectAsState()
    val phoneContacts = remember { mutableStateListOf<Contact>() }
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
            // WhatsApp-style: sync local numbers to backend to discover registered users
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

        val phoneNumbers = mergedFromPhone.map { normalizePhoneNumber(it.phoneNumber) }.toSet()
        val remainingBackend = contacts.filter { it.phoneNumber.isNotBlank() && normalizePhoneNumber(it.phoneNumber) !in phoneNumbers }

        mergedFromPhone + remainingBackend
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Select contact", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("${mergedContacts.size} contacts", fontSize = 12.sp, color = AppTextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.Search, "Search") }
                    IconButton(onClick = {}) { Icon(Icons.Default.Person, "More") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppSecondary,
                    titleContentColor = AppTextPrimary,
                    navigationIconContentColor = AppTextPrimary,
                    actionIconContentColor = AppTextPrimary
                )
            )
        },
        containerColor = AppBackground
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            val (registered, unregistered) = mergedContacts.partition { it.isRegistered }
            
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select contact", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppTextPrimary)
                    Text("${mergedContacts.size} contacts", fontSize = 12.sp, color = AppTextSecondary)
                }
            }

            if (!isContactsPermissionGranted) {
                item {
                    Text(
                        "Grant contacts permission so FirstApp can detect which numbers already use the app.",
                        modifier = Modifier.padding(16.dp),
                        color = AppTextSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            if (registered.isNotEmpty()) {
                item { Text("Contacts on FirstApp", modifier = Modifier.padding(16.dp), color = AppAccent, fontWeight = FontWeight.Bold) }
                items(registered) { contact ->
                    ContactItem(contact) { onContactSelected(contact) }
                }
            }
            
            if (unregistered.isNotEmpty()) {
                item { Text("Invite to FirstApp", modifier = Modifier.padding(16.dp), color = AppTextSecondary, fontWeight = FontWeight.Bold) }
                items(unregistered) { contact ->
                    ContactItem(contact) { onInviteClicked(contact.phoneNumber) }
                }
            }
        }
    }
}

@Composable
fun ContactItem(contact: Contact, onClick: () -> Unit) {
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
            Icon(Icons.Default.Person, contentDescription = null, tint = AppBackground)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(contact.displayName, color = AppTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(contact.username, color = AppTextSecondary, fontSize = 14.sp)
        }
        if (!contact.isAuthenticated) {
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
    val contacts = mutableMapOf<String, Contact>()

    cursor?.use {
        while (it.moveToNext()) {
            val displayName = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)) ?: "Unknown"
            val rawNumber = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)) ?: continue
            val normalized = normalizePhoneNumber(rawNumber)
            if (normalized.isEmpty()) continue

            if (contacts[normalized] == null) {
                contacts[normalized] = Contact(
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

    return contacts.values.toList()
}
