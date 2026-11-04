package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.example.data.model.Mess
import com.example.ui.Language
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.EmeraldPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessTopBar(
    mess: Mess?,
    isAdmin: Boolean,
    language: Language,
    currentMonth: String,
    userEmail: String? = null,
    userPhotoUrl: String? = null,
    onToggleLanguage: () -> Unit,
    onSignOut: () -> Unit,
    onSwitchToAdmin: (() -> Unit)? = null
) {
    var showProfileDialog by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column(modifier = Modifier.padding(end = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = mess?.name ?: "Meal Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (isAdmin) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AmberSecondary.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (language == Language.ENGLISH) "ADMIN" else "অ্যাডমিন",
                                color = AmberSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (language == Language.ENGLISH) "MEMBER" else "সদস্য",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Text(
                    text = "${if (language == Language.ENGLISH) "Month:" else "মাস:"} $currentMonth",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        },
        actions = {
            // Language Toggle Button
            IconButton(
                onClick = onToggleLanguage,
                modifier = Modifier
                    .testTag("language_toggle_btn")
                    .size(36.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = language.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Switch to Admin Button (if currently in member mode)
            if (!isAdmin && onSwitchToAdmin != null) {
                IconButton(
                    onClick = onSwitchToAdmin,
                    modifier = Modifier
                        .testTag("switch_to_admin_button")
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin Login",
                        tint = AmberSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // User Profile Photo and Gmail Display (Top Right)
            if (!userEmail.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isAdmin) AmberSecondary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isAdmin) AmberSecondary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .clickable { showProfileDialog = true }
                        .testTag("user_profile_header_chip")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 3.dp, end = 8.dp, top = 3.dp, bottom = 3.dp)
                    ) {
                        if (!userPhotoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = userPhotoUrl,
                                contentDescription = "Profile Photo",
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(if (isAdmin) AmberSecondary else EmeraldPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userEmail.firstOrNull()?.uppercase() ?: "U",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 110.dp)
                            )
                            Text(
                                text = if (isAdmin)
                                    (if (language == Language.ENGLISH) "Admin" else "অ্যাডমিন")
                                else
                                    (if (language == Language.ENGLISH) "Member" else "সদস্য"),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isAdmin) AmberSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Exit / Sign Out
            IconButton(
                onClick = onSignOut,
                modifier = Modifier
                    .testTag("sign_out_button")
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Exit / Sign Out",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )

    // User Profile Details Dialog when tapped
    if (showProfileDialog && !userEmail.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = {
                Text(
                    text = if (language == Language.ENGLISH) "Signed-in Account" else "লগইনকৃত গুগল অ্যাকাউন্ট",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!userPhotoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = userPhotoUrl,
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(if (isAdmin) AmberSecondary else EmeraldPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userEmail.firstOrNull()?.uppercase() ?: "U",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isAdmin) AmberSecondary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = if (isAdmin)
                                (if (language == Language.ENGLISH) "Current Role: Verified Admin / Manager" else "বর্তমান ভূমিকা: অনুমোদিত অ্যাডমিন / ম্যানেজার")
                            else
                                (if (language == Language.ENGLISH) "Current Role: Mess Member" else "বর্তমান ভূমিকা: মেস সদস্য"),
                            color = if (isAdmin) AmberSecondary else MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text(text = if (language == Language.ENGLISH) "Close" else "বন্ধ করুন")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showProfileDialog = false
                        onSignOut()
                    }
                ) {
                    Text(
                        text = if (language == Language.ENGLISH) "Sign Out" else "লগআউট",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}

@Composable
fun NavigationTabRow(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    language: Language,
    isAdmin: Boolean = true
) {
    val tabs = if (language == Language.ENGLISH) {
        if (isAdmin) {
            listOf("Overview", "Daily Meals", "Bazaar", "Joma Tk", "Egg Stock", "Rice Stock", "Utilities", "Reports & Share", "Admin Log")
        } else {
            listOf("Overview", "Daily Meals", "Bazaar", "Joma Tk", "Egg Stock", "Rice Stock", "Utilities", "Reports & Share")
        }
    } else {
        if (isAdmin) {
            listOf("ওভারভিউ", "দৈনিক মিল", "বাজার খরচ", "জমা টাকা", "ডিমের হিসাব", "চাউলের স্টক", "ইউটিলিটি", "রিপোর্ট ও শেয়ার", "অ্যাডমিন লগ")
        } else {
            listOf("ওভারভিউ", "দৈনিক মিল", "বাজার খরচ", "জমা টাকা", "ডিমের হিসাব", "চাউলের স্টক", "ইউটিলিটি", "রিপোর্ট ও শেয়ার")
        }
    }

    ScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        edgePadding = 12.dp,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.testTag("tab_${index}")
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconTint: Color = EmeraldPrimary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
