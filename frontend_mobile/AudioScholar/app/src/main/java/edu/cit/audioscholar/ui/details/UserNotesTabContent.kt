package edu.cit.audioscholar.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.util.Log
import edu.cit.audioscholar.data.remote.dto.UserNoteDto
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UserNotesTabContent(
    userNotes: List<UserNoteDto>,
    currentUserId: String?,
    isLoading: Boolean,
    error: String?,
    onCreateNote: (String) -> Unit,
    onUpdateNote: (String, String) -> Unit,
    onDeleteNote: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<UserNoteDto?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading && userNotes.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (error != null && userNotes.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { /* Retry? Or just let user switch tabs */ }) {
                    Text("Retry")
                }
            }
        } else {
            if (userNotes.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No notes yet. Tap + to add one.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (error != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    if (isLoading) {
                        item {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }

                    items(userNotes, key = { it.id ?: UUID.randomUUID().toString() }) { note ->
                        val isOwner = currentUserId != null && note.userId == currentUserId
                        Log.d("UserNotesTabContent", "Note ID: ${note.id}, Note Owner: ${note.userId}, Current User: $currentUserId, IsOwner: $isOwner")

                        NoteItemCard(
                            note = note,
                            actionsEnabled = note.id != null && isOwner,
                            onEdit = {
                                noteToEdit = it
                            },
                            onDelete = {
                                it.id?.let { id -> onDeleteNote(id) }
                            }
                        )
                    }
                    
                    // Spacer for FAB
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
            
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Note")
            }
        }
    }

    if (showAddDialog) {
        AddEditNoteDialog(
            note = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { content ->
                onCreateNote(content)
                showAddDialog = false
            }
        )
    }

    if (noteToEdit != null) {
        AddEditNoteDialog(
            note = noteToEdit,
            onDismiss = { noteToEdit = null },
            onConfirm = { content ->
                noteToEdit?.let { note ->
                    note.id?.let { id -> onUpdateNote(id, content) }
                }
                noteToEdit = null
            }
        )
    }
}

@Composable
fun NoteItemCard(
    note: UserNoteDto,
    actionsEnabled: Boolean = true,
    onEdit: (UserNoteDto) -> Unit,
    onDelete: (UserNoteDto) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = formatIsoDate(note.updatedAt ?: note.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (actionsEnabled) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    showMenu = false
                                    onEdit(note)
                                },
                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    showMenu = false
                                    onDelete(note)
                                },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun AddEditNoteDialog(
    note: UserNoteDto?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var content by remember { mutableStateOf(note?.content ?: "") }
    val isEditing = note != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Note" else "Add Note") },
        text = {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Note Content") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                minLines = 3
            )
        },
        confirmButton = {
            Button(
                onClick = { if (content.isNotBlank()) onConfirm(content) },
                enabled = content.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatIsoDate(dateString: String?): String {
    if (dateString.isNullOrBlank()) return ""
    return try {
        // Attempt to parse ISO 8601 string
        // Assuming format like "2023-11-29T07:11:37.596Z" or "2023-11-29T07:11:37Z"
        val inputFormat = if (dateString.contains(".")) {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
        } else {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        }
        
        // Remove 'Z' if present as SimpleDateFormat (prior to Java 8/API 26) with 'Z' pattern expects RFC 822 timezone (e.g. -0800)
        // or just force UTC timezone on the formatter
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        
        val cleanDateString = dateString.replace("Z", "")
        val date = inputFormat.parse(cleanDateString)
        
        val outputFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
        if (date != null) outputFormat.format(date) else dateString
    } catch (e: Exception) {
        dateString
    }
}