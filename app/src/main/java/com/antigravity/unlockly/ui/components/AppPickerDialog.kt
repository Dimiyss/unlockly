package com.antigravity.unlockly.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.antigravity.unlockly.data.model.InstalledApp
import com.antigravity.unlockly.ui.theme.*

enum class AppSelectionFilter {
    ALL,
    SELECTED,
    UNSELECTED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerDialog(
    title: String,
    subtitle: String,
    accentColor: Color = PrimaryIndigo,
    installedApps: List<InstalledApp>,
    selectedPackages: Set<String>,
    excludedPackages: Set<String> = emptySet(),
    excludedLabel: String = "Already chosen in another rule",
    maxSelection: Int? = null,
    onSaveSelection: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(AppSelectionFilter.ALL) }
    
    // Filter out packages that are not actually in installedApps to avoid ghost counts
    val installedPackageNames = remember(installedApps) { installedApps.map { it.packageName }.toSet() }
    var currentSelection by remember(selectedPackages, installedPackageNames) {
        mutableStateOf(selectedPackages.intersect(installedPackageNames))
    }

    val selectedCount = currentSelection.size

    val filteredApps = remember(searchQuery, installedApps, activeFilter, currentSelection) {
        installedApps.filter { app ->
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                app.name.contains(searchQuery, ignoreCase = true) ||
                        app.packageName.contains(searchQuery, ignoreCase = true)
            }

            val matchesFilter = when (activeFilter) {
                AppSelectionFilter.ALL -> true
                AppSelectionFilter.SELECTED -> currentSelection.contains(app.packageName)
                AppSelectionFilter.UNSELECTED -> !currentSelection.contains(app.packageName)
            }

            matchesSearch && matchesFilter
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
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

                Spacer(modifier = Modifier.height(12.dp))

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

                // Filter Tabs / Chips (All, Selected, Unselected)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeFilter == AppSelectionFilter.ALL,
                        onClick = { activeFilter = AppSelectionFilter.ALL },
                        label = { Text("All (${installedApps.size})", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor.copy(alpha = 0.2f),
                            selectedLabelColor = accentColor,
                            containerColor = SurfaceDark,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (activeFilter == AppSelectionFilter.ALL) accentColor else SurfaceVariantDark,
                            selectedBorderColor = accentColor,
                            enabled = true,
                            selected = activeFilter == AppSelectionFilter.ALL
                        )
                    )

                    FilterChip(
                        selected = activeFilter == AppSelectionFilter.SELECTED,
                        onClick = { activeFilter = AppSelectionFilter.SELECTED },
                        label = { Text("Already Chosen ($selectedCount)", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor.copy(alpha = 0.2f),
                            selectedLabelColor = accentColor,
                            containerColor = SurfaceDark,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (activeFilter == AppSelectionFilter.SELECTED) accentColor else SurfaceVariantDark,
                            selectedBorderColor = accentColor,
                            enabled = true,
                            selected = activeFilter == AppSelectionFilter.SELECTED
                        )
                    )

                    FilterChip(
                        selected = activeFilter == AppSelectionFilter.UNSELECTED,
                        onClick = { activeFilter = AppSelectionFilter.UNSELECTED },
                        label = { Text("Unselected (${maxOf(0, installedApps.size - selectedCount)})", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor.copy(alpha = 0.2f),
                            selectedLabelColor = accentColor,
                            containerColor = SurfaceDark,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (activeFilter == AppSelectionFilter.UNSELECTED) accentColor else SurfaceVariantDark,
                            selectedBorderColor = accentColor,
                            enabled = true,
                            selected = activeFilter == AppSelectionFilter.UNSELECTED
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Selected Count & Quick Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (maxSelection != null) {
                                "Selected $selectedCount / $maxSelection apps (Free Plan)"
                            } else {
                                "Selected $selectedCount of ${installedApps.size} apps (PRO: Unlimited)"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (maxSelection != null && selectedCount >= maxSelection) WarningAmber else accentColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        if (maxSelection != null && selectedCount >= maxSelection) {
                            Text(
                                text = "Limit reached • Upgrade to PRO for unlimited",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = WarningAmber,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    Row {
                        TextButton(
                            onClick = {
                                val selectableFiltered = filteredApps
                                    .filter { !excludedPackages.contains(it.packageName) }
                                    .map { it.packageName }
                                
                                if (maxSelection != null) {
                                    val availableSlots = maxOf(0, maxSelection - currentSelection.size)
                                    val toAdd = selectableFiltered.filter { !currentSelection.contains(it) }.take(availableSlots)
                                    currentSelection = currentSelection + toAdd
                                    if (selectableFiltered.size > availableSlots) {
                                        Toast.makeText(
                                            context,
                                            "Free tier allows up to $maxSelection apps. Upgrade to PRO for unlimited apps.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    currentSelection = currentSelection + selectableFiltered
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Select All", fontSize = 12.sp, color = TextSecondary)
                        }

                        TextButton(
                            onClick = {
                                val filteredPkgs = filteredApps.map { it.packageName }.toSet()
                                currentSelection = currentSelection - filteredPkgs
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Clear", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }

                HorizontalDivider(color = SurfaceVariantDark, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                // Apps List
                if (filteredApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (activeFilter == AppSelectionFilter.SELECTED && selectedCount == 0) {
                                "No apps have been chosen yet."
                            } else {
                                "No apps found matching criteria."
                            },
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
                            val isExcluded = excludedPackages.contains(app.packageName)
                            AppRowItem(
                                app = app,
                                isSelected = isSelected,
                                isExcluded = isExcluded,
                                excludedLabel = excludedLabel,
                                accentColor = accentColor,
                                onToggle = {
                                    if (!isExcluded) {
                                        if (isSelected) {
                                            currentSelection = currentSelection - app.packageName
                                        } else {
                                            if (maxSelection != null && currentSelection.size >= maxSelection) {
                                                Toast.makeText(
                                                    context,
                                                    "Free tier allows selecting up to $maxSelection apps. Upgrade to PRO for unlimited apps.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                currentSelection = currentSelection + app.packageName
                                            }
                                        }
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
                            if (maxSelection != null && currentSelection.size > maxSelection) {
                                Toast.makeText(
                                    context,
                                    "Free tier allows up to $maxSelection apps. Upgrade to PRO for unlimited apps.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }
                            onSaveSelection(currentSelection)
                            onDismiss()
                        },
                        enabled = !(maxSelection != null && selectedCount > maxSelection),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text(
                            text = "Save Selection ($selectedCount)",
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
    isExcluded: Boolean = false,
    excludedLabel: String = "",
    accentColor: Color,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        enabled = !isExcluded,
        shape = RoundedCornerShape(12.dp),
        color = when {
            isExcluded -> SurfaceDark.copy(alpha = 0.35f)
            isSelected -> SurfaceDark
            else -> Color.Transparent
        },
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
                        color = if (isExcluded) TextMuted else TextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isExcluded && excludedLabel.isNotEmpty()) excludedLabel else app.packageName,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isExcluded) WarningAmber else TextMuted,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Checkbox or Excluded indicator
            if (isExcluded) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Excluded",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            } else {
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
