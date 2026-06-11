package com.eloi.retrolightgb.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.eloi.retrolightgb.core.library.RomLibrary
import com.eloi.retrolightgb.core.library.extractRomTitle
import com.eloi.retrolightgb.di.rememberInstance
import com.eloi.retrolightgb.ui.compose.rememberFilePickerLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data object LibraryScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val romFlow = rememberInstance<MutableSharedFlow<ByteArray>>()
        val entries by RomLibrary.entries.collectAsState()

        val filePicker = rememberFilePickerLauncher { bytes ->
            scope.launch(Dispatchers.Default) {
                val title = bytes.extractRomTitle()
                RomLibrary.add(title, bytes)
                romFlow.emit(bytes)
                withContext(Dispatchers.Main) { navigator.pop() }
            }
        }

        MaterialTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Library") },
                        navigationIcon = {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = { filePicker.launch() }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add ROM")
                    }
                },
            ) { paddingValues ->
                if (entries.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("No ROMs in library. Tap + to add one.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.padding(paddingValues)) {
                        items(entries, key = { it.title }) { entry ->
                            ListItem(
                                headlineContent = { Text(entry.title) },
                                trailingContent = {
                                    IconButton(onClick = { RomLibrary.remove(entry.title) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Remove")
                                    }
                                },
                                modifier = Modifier.clickable {
                                    scope.launch(Dispatchers.Default) {
                                        val bytes = RomLibrary.loadBytes(entry.title) ?: return@launch
                                        romFlow.emit(bytes)
                                        withContext(Dispatchers.Main) { navigator.pop() }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}