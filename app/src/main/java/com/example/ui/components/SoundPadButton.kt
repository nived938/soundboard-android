package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.PlaybackStatus
import com.example.data.model.SoundClipEntity
import com.example.ui.theme.MinimalBorder
import com.example.ui.theme.MinimalBorderActive
import com.example.ui.theme.MinimalError
import com.example.ui.theme.MinimalSurface
import com.example.ui.theme.MinimalSurfaceElevated
import com.example.ui.theme.MinimalSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

fun parseColorSafe(hex: String, fallback: Color = Color(0xFF6750A4)): Color {
    return try {
        val cleanHex = if (hex.startsWith("#")) hex else "#$hex"
        Color(android.graphics.Color.parseColor(cleanHex))
    } catch (e: Exception) {
        fallback
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SoundPadButton(
    sound: SoundClipEntity,
    playbackStatus: PlaybackStatus?,
    highContrast: Boolean,
    onPadClick: () -> Unit,
    onLoopToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onEditClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPlaying = playbackStatus?.isPlaying == true
    val isLooping = playbackStatus?.isLooping ?: sound.isLooping
    val progress = playbackStatus?.progressFraction ?: 0f

    val accentColor = remember(sound.colorHex) { parseColorSafe(sound.colorHex) }
    var menuExpanded by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Tactile bounce scale animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else if (isPlaying) 1.01f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "padScale"
    )

    // Glowing border pulse when playing
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val currentBorderColor = when {
        isPlaying -> accentColor.copy(alpha = glowAlpha)
        highContrast -> TextPrimary
        else -> MinimalBorder
    }

    val padContentDescription = buildString {
        append("${sound.title}. ")
        if (isPlaying) append("Currently playing. ")
        if (isLooping) append("Seamless loop mode active. ")
        append("Duration ${sound.durationMs / 1000f} seconds. Double tap to play or stop, long press for menu.")
    }

    Card(
        modifier = modifier
            .testTag("sound_pad_${sound.id}")
            .semantics { contentDescription = padContentDescription }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (isPlaying) 6.dp else 1.5.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = if (isPlaying) accentColor.copy(alpha = 0.3f) else Color(0x10000000),
                spotColor = if (isPlaying) accentColor.copy(alpha = 0.3f) else Color(0x10000000)
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null, // Clean physical bounce feedback
                onClick = onPadClick,
                onLongClick = { menuExpanded = true }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MinimalSurface
        ),
        border = BorderStroke(
            width = if (isPlaying || highContrast) 2.dp else 1.dp,
            color = currentBorderColor
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (isPlaying) 0.16f else 0.05f),
                            MinimalSurface
                        )
                    )
                )
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Emoji / Icon & Status Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Emoji / Icon Avatar
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.12f))
                            .border(1.dp, accentColor.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = sound.emoji.ifEmpty { "🔊" },
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Badges (Looping, Favorite, Menu)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isLooping) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentColor.copy(alpha = 0.12f))
                                    .border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Repeat,
                                        contentDescription = "Looping",
                                        tint = accentColor,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "LOOP",
                                        color = accentColor,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (sound.isFavorite) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Favorite",
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("sound_pad_menu_${sound.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options for ${sound.title}",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier.background(MinimalSurface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit Pad & Sound", color = TextPrimary) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = accentColor)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onEditClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (sound.isLooping) "Disable Loop" else "Enable Seamless Loop",
                                            color = TextPrimary
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Repeat, contentDescription = null, tint = accentColor)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onLoopToggle()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (sound.isFavorite) "Remove from Favorites" else "Mark Favorite",
                                            color = TextPrimary
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (sound.isFavorite) Icons.Default.FavoriteBorder else Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = Color(0xFFE11D48)
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onFavoriteToggle()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Duplicate Pad", color = TextPrimary) },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = TextSecondary)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onDuplicateClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share Audio Clip", color = TextPrimary) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = accentColor)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onShareClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Sound", color = MinimalError) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = MinimalError)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onDeleteClick()
                                    }
                                )
                            }
                        }
                    }
                }

                // Middle: Visualizer Equalizer / Play indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaying) {
                        EqualizerWaveBars(accentColor = accentColor)
                    } else {
                        // Subtle Play Glyph
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(MinimalSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Bottom: Title & Progress / Duration
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = sound.title,
                        color = TextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    if (isPlaying) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = accentColor,
                            trackColor = MinimalBorder,
                        )
                    } else {
                        val durationSec = String.format("%.1fs", sound.durationMs / 1000f)
                        val fxTag = if (sound.activeFx != "NONE") " • ${sound.activeFx}" else ""
                        val playsTag = if (sound.playCount > 0) " • ${sound.playCount}x" else ""
                        Text(
                            text = "$durationSec$fxTag$playsTag",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EqualizerWaveBars(accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(280, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(220, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(310, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h4"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(24.dp)
    ) {
        listOf(h1, h2, h3, h4, h2, h1).forEach { fraction ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(22.dp * fraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
        }
    }
}
