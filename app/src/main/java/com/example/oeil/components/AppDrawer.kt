package com.example.oeil.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.oeil.model.ChatThread
import com.example.oeil.model.User
import com.example.oeil.ui.theme.AccentGradient
import com.example.oeil.ui.theme.AccentGradientSoft
import com.example.oeil.ui.theme.AccentPrimary
import com.example.oeil.ui.theme.DividerGradient
import com.example.oeil.ui.theme.OutlineDark
import com.example.oeil.ui.theme.SurfaceDrawer
import com.example.oeil.ui.theme.SurfaceElevated
import com.example.oeil.ui.theme.SurfaceGlass
import com.example.oeil.ui.theme.TextOnAccent
import com.example.oeil.ui.theme.TextPrimary
import com.example.oeil.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Assistant-style sidebar: brand header, prominent new chat, live search,
 * conversations grouped by recency, and the account card pinned at the bottom.
 */
@Composable
fun AppDrawer(
    user: User,
    threads: List<ChatThread>,
    activeThreadId: String?,
    onNewChat: () -> Unit,
    onOpenThread: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val filtered = if (query.isBlank()) {
        threads
    } else {
        threads.filter { it.title.contains(query.trim(), ignoreCase = true) }
    }
    val groups = grouped(filtered)

    Column(
        modifier = modifier
            .fillMaxHeight()
            .widthIn(max = 340.dp)
            .fillMaxWidth()
            .background(SurfaceDrawer)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)),
    ) {
        Header(onClose)
        NewChatButton {
            onNewChat()
            onClose()
        }
        SearchField(query) { query = it }
        RecentHeader(
            canClear = threads.isNotEmpty(),
            onClearAll = {
                onClearAll()
                query = ""
            },
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            if (filtered.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = if (threads.isEmpty()) "No conversations yet" else "No matches for “${query.trim()}”",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                    )
                }
            } else {
                groups.forEach { group ->
                    item(key = "group-${group.label}") { GroupLabel(group.label) }
                    items(group.threads, key = { it.id }) { thread ->
                        ThreadRow(
                            thread = thread,
                            active = thread.id == activeThreadId,
                            onClick = {
                                onOpenThread(thread.id)
                                onClose()
                            },
                        )
                    }
                }
            }
        }

        AccountCard(user, threads.size, onOpenSettings)
    }
}

@Composable
private fun Header(onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(AccentGradient),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Visibility, null, tint = TextOnAccent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Oeil",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Text("AI assistant", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        IconButton(Icons.Outlined.Close, "Close menu", onClose)
    }
}

@Composable
private fun NewChatButton(onClick: () -> Unit) {
    OeilTouch(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(46.dp),
        shape = RoundedCornerShape(14.dp),
        brush = AccentGradient,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Add, null, tint = TextOnAccent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "New chat",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = TextOnAccent,
            )
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .border(1.dp, OutlineDark, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Search, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
            cursorBrush = SolidColor(TextPrimary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text("Search chats", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    inner()
                }
            },
        )
        if (value.isNotEmpty()) {
            IconButton(Icons.Outlined.Close, "Clear search", { onValueChange("") }, size = 24.dp, iconSize = 14.dp)
        }
    }
}

@Composable
private fun RecentHeader(canClear: Boolean, onClearAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 10.dp, top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Recent",
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
            modifier = Modifier.weight(1f),
        )
        if (canClear) {
            IconButton(
                Icons.Outlined.DeleteSweep,
                "Clear conversations",
                onClearAll,
                size = 30.dp,
                iconSize = 17.dp,
            )
        }
    }
}

@Composable
private fun GroupLabel(label: String) {
    Text(
        text = label.uppercase(Locale.getDefault()),
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary.copy(alpha = 0.65f),
        modifier = Modifier.padding(start = 22.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun ThreadRow(thread: ChatThread, active: Boolean, onClick: () -> Unit) {
    OeilTouch(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 1.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        brush = if (active) AccentGradientSoft else null,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.ChatBubbleOutline,
                null,
                tint = if (active) AccentPrimary else TextSecondary.copy(alpha = 0.8f),
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = thread.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                color = TextPrimary.copy(alpha = if (active) 1f else 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = timeLabel(thread.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(alpha = 0.65f),
            )
        }
    }
}

@Composable
private fun AccountCard(user: User, conversationCount: Int, onOpenSettings: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(DividerGradient))
    OeilTouch(
        onClick = onOpenSettings,
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceGlass.copy(alpha = 0.55f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AccentGradientSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    initials(user.name),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentPrimary,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    user.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = user.email ?: if (conversationCount == 1) "1 conversation" else "$conversationCount conversations",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Outlined.Settings, "Settings", tint = TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun IconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 34.dp,
    iconSize: androidx.compose.ui.unit.Dp = 20.dp,
) {
    OeilTouch(onClick = onClick, modifier = Modifier.size(size)) {
        Icon(icon, description, tint = TextSecondary, modifier = Modifier.size(iconSize))
    }
}

private data class ThreadGroup(val label: String, val threads: List<ChatThread>)

private fun grouped(threads: List<ChatThread>): List<ThreadGroup> {
    val today = startOfDay(System.currentTimeMillis())
    val yesterday = today - DAY
    val week = today - 7 * DAY
    val labels = listOf("Today", "Yesterday", "Previous 7 days", "Older")
    return labels.mapNotNull { label ->
        val matches = threads.filter {
            val bucket = when {
                it.updatedAt >= today -> "Today"
                it.updatedAt >= yesterday -> "Yesterday"
                it.updatedAt >= week -> "Previous 7 days"
                else -> "Older"
            }
            bucket == label
        }
        if (matches.isEmpty()) null else ThreadGroup(label, matches)
    }
}

private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun timeLabel(timestamp: Long): String {
    val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0)
    return when {
        diff < 60_000 -> "now"
        diff < 3_600_000 -> "${diff / 60_000}m"
        diff < 86_400_000 -> "${diff / 3_600_000}h"
        diff < 7 * DAY -> "${diff / DAY}d"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun initials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "O"
        parts.size == 1 -> parts.first().take(1).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

private const val DAY = 86_400_000L
