package com.example.toastytraveltools

import android.R.attr.label
import android.R.attr.onClick
import android.content.Context
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.material3.OutlinedTextField

@Composable
fun DisplayCampsite(query: String) {
    Text("Results for: $query")
}

@Composable
fun SearchScreen() {
    var searchClicked by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var showResults by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!searchClicked) {
            Button(onClick = { searchClicked = true }) {
                Text("Search")
            }
        } else {
            Row {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Search for campsite") }
                )
                Button(
                    onClick = {
                        if (search.isNotBlank()) {
                            showResults = true
                        }
                    }
                ) {
                    Text("Search")
                }
            }

            if (showResults) {
                DisplayCampsite(search)
            }
        }
    }
}