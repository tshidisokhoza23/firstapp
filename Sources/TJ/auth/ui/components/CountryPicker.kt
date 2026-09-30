package com.tpsk.tj.auth.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tpsk.tj.common.ui.theme.AppAccent
import com.tpsk.tj.common.ui.theme.AppSecondary
import com.tpsk.tj.common.util.Country
import com.tpsk.tj.common.util.CountryUtils

@Composable
fun CountryPickerField(
    selectedCountry: Country,
    onCountrySelected: (Country) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedCard(
        onClick = { showDialog = true },
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = Color.Transparent,
            contentColor = Color.White
        ),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AppSecondary))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = selectedCountry.flag, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = selectedCountry.dialCode, fontSize = 16.sp, color = Color.White)
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }

    if (showDialog) {
        CountryPickerDialog(
            onDismissRequest = { showDialog = false },
            onCountrySelected = {
                onCountrySelected(it)
                showDialog = false
            }
        )
    }
}

@Composable
fun CountryPickerDialog(
    onDismissRequest: () -> Unit,
    onCountrySelected: (Country) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCountries = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            CountryUtils.allCountries
        } else {
            CountryUtils.allCountries.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.dialCode.contains(searchQuery)
            }
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(16.dp),
            color = AppSecondary
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Select Country",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search country or code", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppAccent,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredCountries) { country ->
                        CountryItem(
                            country = country,
                            onClick = { onCountrySelected(country) }
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel", color = AppAccent)
                    }
                }
            }
        }
    }
}

@Composable
fun CountryItem(
    country: Country,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = country.flag, fontSize = 24.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = country.name,
            modifier = Modifier.weight(1f),
            color = Color.White,
            fontSize = 16.sp
        )
        Text(
            text = country.dialCode,
            color = Color.Gray,
            fontSize = 16.sp
        )
    }
}
