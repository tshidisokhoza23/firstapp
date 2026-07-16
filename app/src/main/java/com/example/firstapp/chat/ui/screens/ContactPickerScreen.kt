package com.example.firstapp.chat.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonAdd
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
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.chat.ui.viewmodel.ContactPickerViewModel
import com.example.firstapp.common.ui.theme.AppAccent
import com.example.firstapp.common.ui.theme.AppBackground
import com.example.firstapp.common.model.User
import com.example.firstapp.common.util.DisplayNameResolver
import com.example.firstapp.common.util.Resource
import kotlin.collections.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPickerScreen(
    viewModel: ContactPickerViewModel,
    initialIsGroupMode: Boolean = false,
    onBack: () -> Unit,
    onContactSelected: (Contact) -> Unit,
    onInviteClicked: () -> Unit,
    onGroupCreate: (List<Contact>) -> Unit,
) {
    var isGroupMode by remember { mutableStateOf(initialIsGroupMode) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    
    val selectedContacts = remember { mutableStateListOf<Contact>() }
    val contactsResource by viewModel.contacts.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    
    val contacts = if (isSearchActive && searchQuery.isNotBlank()) searchResults else (contactsResource.data ?: emptyList())

    Scaffold(
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { 
                                searchQuery = it
                                viewModel.searchContacts(it)
                            },
                            placeholder = { Text("Search contacts...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = AppAccent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { 
                            isSearchActive = false 
                            searchQuery = ""
                            viewModel.searchContacts("")
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(if (isGroupMode) "Add participants" else "Select contact", fontWeight = FontWeight.Bold)
                            Text("${contacts.size} contacts", fontSize = 12.sp, color = Color.Gray)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (isGroupMode) {
                            TextButton(onClick = {
                                val registeredContacts = contacts.filter { it.isRegistered }
                                if (selectedContacts.size == registeredContacts.size) {
                                    selectedContacts.clear()
                                } else {
                                    selectedContacts.clear()
                                    selectedContacts.addAll(registeredContacts)
                                }
                            }) {
                                val registeredContacts = contacts.filter { it.isRegistered }
                                Text(
                                    if (selectedContacts.size == registeredContacts.size && registeredContacts.isNotEmpty()) "None" else "All",
                                    color = AppAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppBackground,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        },
        floatingActionButton = {
            if (isGroupMode && selectedContacts.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { onGroupCreate(selectedContacts.toList()) },
                    containerColor = AppAccent,
                    contentColor = AppBackground,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Done")
                }
            }
        },
        containerColor = AppBackground
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!isGroupMode) {
                item {
                    ContactActionItem(icon = Icons.Default.GroupAdd, label = "New group") {
                        isGroupMode = true
                    }
                }
                item {
                    ContactActionItem(icon = Icons.Default.PersonAdd, label = "New contact") {
                    }
                }
            }

            items(contacts) { contact ->
                val isSelected = selectedContacts.contains(contact)
                val context = LocalContext.current
                ContactItem(
                    contact = contact,
                    isSelected = isSelected,
                    showCheckbox = isGroupMode,
                    onClick = {
                        if (isGroupMode) {
                            if (contact.isRegistered) {
                                if (isSelected) selectedContacts.remove(contact)
                                else selectedContacts.add(contact)
                            }
                        } else {
                            if (contact.isRegistered) {
                                onContactSelected(contact)
                            } else {
                                val intent = Intent(Intent.ACTION_SENDTO)
                                intent.data = Uri.parse("smsto:${contact.phoneNumber}")
                                intent.putExtra("sms_body", "Join me on FirstApp! http://firstapp.com/download")
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        }
                    }
                )
            }
            
            item {
                ContactActionItem(icon = Icons.Default.Search, label = "Invite friends", onClick = onInviteClicked)
            }
        }
    }
}

@Composable
fun ContactActionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = AppAccent
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = AppBackground, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ContactItem(contact: Contact, isSelected: Boolean, showCheckbox: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val displayNameResolver = remember(context) { DisplayNameResolver(context) }
    
    // Resolve name with safer initialization
    var resolvedName by remember { mutableStateOf(if (contact.displayName.isNotEmpty()) contact.displayName else contact.phoneNumber) }
    
    // Move heavy/background resolving to a side-effect
    LaunchedEffect(contact.phoneNumber, contact.displayName) {
        val user = User(
            id = contact.id,
            username = contact.username,
            displayName = contact.displayName,
            phoneNumber = contact.phoneNumber
        )
        // Only attempt resolve if we have a phone number
        if (contact.phoneNumber.isNotEmpty()) {
            val resolved = displayNameResolver.resolve(user, contact.phoneNumber)
            if (resolved.isNotEmpty()) {
                resolvedName = resolved
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (contact.isRegistered) AppAccent else Color.Gray),
            contentAlignment = Alignment.Center
        ) {
                Text(resolvedName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
                Text(resolvedName, color = Color.White, fontWeight = FontWeight.Bold)
            Text(if (contact.isRegistered) (contact.bio ?: "Available") else contact.phoneNumber, color = Color.Gray, fontSize = 14.sp)
        }
        
        if (showCheckbox && contact.isRegistered) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(checkedColor = AppAccent, checkmarkColor = AppBackground)
            )
        } else if (!contact.isRegistered) {
            TextButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO)
                    intent.data = Uri.parse("smsto:${contact.phoneNumber}")
                    intent.putExtra("sms_body", "Join me on FirstApp! http://firstapp.com/download")
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.textButtonColors(contentColor = AppAccent)
            ) {
                Text("INVITE", fontWeight = FontWeight.Bold)
            }
        }
    }
}
