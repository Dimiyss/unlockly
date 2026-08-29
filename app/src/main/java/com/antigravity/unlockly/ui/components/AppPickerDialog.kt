package com.antigravity.unlockly.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.antigravity.unlockly.data.model.InstalledApp
import com.antigravity.unlockly.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerDialog(
    title: String,
    subtitle: String,
    accentColor: Color = PrimaryIndigo,
    installedApps: List<InstalledApp>,
    selectedPackages: Set<String>,
    onSaveSelection: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var currentSelection by remember(selectedPackages) { mutableStateOf(selectedPackages) }

    val filteredApps = remember(searchQuery, installedApps) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundDark),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps or packages...", color = TextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextSecondary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = SurfaceVariantDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Selected Count & Quick Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Selected ${currentSelection.size} of ${installedApps.size} apps",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = accentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Row {
                        TextButton(
                            onClick = {
                                currentSelection = if (searchQuery.isBlank()) {
                                    installedApps.map { it.packageName }.toSet()
                                } else {
                                    currentSelection + filteredApps.map { it.packageName }
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Select All", fontSize = 12.sp, color = TextSecondary)
                        }

                        TextButton(
                            onClick = {
                                currentSelection = if (searchQuery.isBlank()) {
                                    emptySet()
                                } else {
                                    currentSelection - filteredApps.map { it.packageName }.toSet()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Clear", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }

                Divider(color = SurfaceVariantDark, thickness = 1.dp, modifier = Modifier.padding(vertical = 6.dp))

                // Apps List
                if (filteredApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No apps found matching \"$searchQuery\"",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            val isSelected = currentSelection.contains(app.packageName)
                            AppRowItem(
                                app = app,
                                isSelected = isSelected,
                                accentColor = accentColor,
                                onToggle = {
                                    currentSelection = if (isSelected) {
                                        currentSelection - app.packageName
                                    } else {
                                        currentSelection + app.packageName
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onSaveSelection(currentSelection)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text(
                            text = "Save Selection (${currentSelection.size})",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRowItem(
    app: InstalledApp,
    isSelected: Boolean,
    accentColor: Color,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) SurfaceDark else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            AppIconImage(app = app, modifier = Modifier.size(40.dp))

            Spacer(modifier = Modifier.width(14.dp))

            // App Name & Package
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextMuted,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Checkbox
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = accentColor,
                    uncheckedColor = TextMuted,
                    checkmarkColor = Color.White
                )
            )
        }
    }
}

@Composable
fun AppIconImage(app: InstalledApp, modifier: Modifier = Modifier) {
    val bitmap = remember(app.icon) {
        app.icon?.let { drawableToBitmap(it) }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = app.name,
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceVariantDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
