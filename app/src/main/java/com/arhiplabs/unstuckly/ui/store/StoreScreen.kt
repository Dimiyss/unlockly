package com.arhiplabs.unstuckly.ui.store

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arhiplabs.unstuckly.UnstucklyApplication
import com.arhiplabs.unstuckly.data.model.Wallet
import com.arhiplabs.unstuckly.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = remember { UnstucklyApplication.instance }
    val walletState by app.walletManager.walletFlow.collectAsState(initial = Wallet())
    val wallet = walletState ?: Wallet()

    var selectedPlan by remember { mutableIntStateOf(1) } // 0: Monthly, 1: Annual

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("Store & Power-Ups", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Active Power-ups Status Card (if any active)
            if (wallet.isProActive || wallet.isBoostActive || wallet.isUnfrozen) {
                ActiveStatusCard(wallet = wallet)
                Spacer(modifier = Modifier.height(18.dp))
            }

            // PRO Hero Banner Card
            ProHeroCard(
                isPro = wallet.isProActive,
                selectedPlan = selectedPlan,
                onSelectPlan = { selectedPlan = it },
                onUpgradePro = {
                    scope.launch {
                        app.walletManager.setProActive(true)
                        app.walletManager.activateBoost(1.5f, 1440 * 30) // 1.5x permanent Pro boost
                        Toast.makeText(context, "🎉 Welcome to Unlockly PRO!", Toast.LENGTH_LONG).show()
                    }
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Section: Instant Power-Ups
            Text(
                text = "⚡ Instant Power-Ups",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Boost reward earnings or temporarily bypass limits.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Mega Boost 2.0x
            PowerUpCard(
                title = "Mega Boost 2.0x",
                description = "Double all earned screen time rewards for the next 24 hours.",
                price = "$1.99",
                icon = Icons.Default.Bolt,
                accentColor = WarningAmber,
                isActive = wallet.isBoostActive && wallet.boostMultiplier >= 2.0f,
                onActivate = {
                    scope.launch {
                        app.walletManager.activateBoost(2.0f, 1440) // 24 hours
                        Toast.makeText(context, "🚀 2.0x Mega Boost Activated for 24 hours!", Toast.LENGTH_LONG).show()
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 1-Hour Unfreeze Pass
            PowerUpCard(
                title = "1-Hour Unfreeze Pass",
                description = "Pause all app blockers and limits completely for 1 hour.",
                price = "$0.99",
                icon = Icons.Default.AcUnit,
                accentColor = Color(0xFF38BDF8),
                isActive = wallet.isUnfrozen,
                onActivate = {
                    scope.launch {
                        app.walletManager.activateUnfreeze(60) // 1 hour
                        Toast.makeText(context, "❄️ Unfreeze Pass Activated: Apps unblocked for 1 hour!", Toast.LENGTH_LONG).show()
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 24-Hour Weekend Unfreeze Pass
            PowerUpCard(
                title = "24-Hour Vacation Pass",
                description = "Bypass all blocking rules for a full 24-hour break without penalties.",
                price = "$2.99",
                icon = Icons.Default.BeachAccess,
                accentColor = Color(0xFFA855F7),
                isActive = wallet.isUnfrozen,
                onActivate = {
                    scope.launch {
                        app.walletManager.activateUnfreeze(1440) // 24 hours
                        Toast.makeText(context, "🏖️ 24-Hour Pass Activated: Enjoy your break!", Toast.LENGTH_LONG).show()
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ActiveStatusCard(wallet: Wallet) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PrimaryIndigo.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Active Upgrades",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = PrimaryIndigo
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (wallet.isProActive) {
                ActiveUpgradeItem(
                    label = "Unlockly PRO",
                    detail = "Active Member",
                    color = WarningAmber
                )
            }
            if (wallet.isBoostActive) {
                ActiveUpgradeItem(
                    label = "${wallet.boostMultiplier}x Boost",
                    detail = "Active (${formatRemainingTime(wallet.boostExpirationTimestamp)})",
                    color = WarningAmber
                )
            }
            if (wallet.isUnfrozen) {
                ActiveUpgradeItem(
                    label = "Unfreeze State",
                    detail = "Bypassing rules (${formatRemainingTime(wallet.unfreezeExpirationTimestamp)})",
                    color = Color(0xFF38BDF8)
                )
            }
        }
    }
}

@Composable
private fun ActiveUpgradeItem(label: String, detail: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
        }
        Text(text = detail, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProHeroCard(
    isPro: Boolean,
    selectedPlan: Int,
    onSelectPlan: (Int) -> Unit,
    onUpgradePro: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            PrimaryIndigo.copy(alpha = 0.25f),
                            Color(0xFF312E81).copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Crown Badge
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFFBBF24))),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isPro) "You Are a PRO Member!" else "Unlockly PRO",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                )

                Text(
                    text = "Maximize your learning efficiency and customize every rule without restrictions.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Feature Bullets
                ProFeatureRow(text = "Unlimited customized app rules & schedules")
                ProFeatureRow(text = "1.5x Permanent learning boost multiplier")
                ProFeatureRow(text = "Monthly emergency unfreeze passes included")
                ProFeatureRow(text = "Advanced screen time analytics & habits")

                Spacer(modifier = Modifier.height(20.dp))

                if (!isPro) {
                    // Subscription Plan Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PlanOptionBox(
                            title = "Monthly",
                            price = "$4.99 / mo",
                            badge = null,
                            isSelected = selectedPlan == 0,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelectPlan(0) }
                        )

                        PlanOptionBox(
                            title = "Annual",
                            price = "$29.99 / yr",
                            badge = "SAVE 50%",
                            isSelected = selectedPlan == 1,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelectPlan(1) }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onUpgradePro,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedPlan == 1) "Start Annual Plan ($29.99)" else "Start Monthly ($4.99)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                } else {
                    Surface(
                        color = SuccessGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PRO Membership Active", color = SuccessGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProFeatureRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = SuccessGreen,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 13.sp)
        )
    }
}

@Composable
private fun PlanOptionBox(
    title: String,
    price: String,
    badge: String?,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrimaryIndigo.copy(alpha = 0.2f) else BackgroundDark
        ),
        modifier = modifier.border(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) PrimaryIndigo else SurfaceVariantDark,
            shape = RoundedCornerShape(16.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(WarningAmber)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = badge, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                }
                Spacer(modifier = Modifier.height(4.dp))
            } else {
                Spacer(modifier = Modifier.height(14.dp))
            }

            Text(text = title, fontWeight = FontWeight.SemiBold, color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = price, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun PowerUpCard(
    title: String,
    description: String,
    price: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    isActive: Boolean,
    onActivate: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = onActivate,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) SuccessGreen else accentColor
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isActive) "Active" else price,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }
    }
}

private fun formatRemainingTime(expirationMs: Long): String {
    val remainingSec = maxOf(0L, (expirationMs - System.currentTimeMillis()) / 1000L)
    val hours = remainingSec / 3600
    val minutes = (remainingSec % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m left" else "${minutes}m left"
}
