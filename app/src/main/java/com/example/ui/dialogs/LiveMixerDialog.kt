package com.example.ui.dialogs

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.PlaybackStatus
import com.example.data.model.SoundClipEntity

@Composable
fun LiveMixerDialog(
    activeSounds: List<SoundClipEntity>,
    playbackStates: Map<Long, PlaybackStatus>,
    channelVolumes: Map<Long, Float>,
    mutedChannels: Set<Long>,
    masterVolume: Float,
    onMasterVolumeChange: (Float) -> Unit,
    onChannelVolumeChange: (Long, Float) -> Unit,
    onToggleMute: (Long) -> Unit,
    onSoloChannel: (Long) -> Unit,
    onStopChannel: (Long) -> Unit,
    onFadeAllOut: () -> Unit,
    onStopAll: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFFDF8F6),
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6750A4).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color(0xFF6750A4),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Live Studio Mixer",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1B1F)
                            )
                            Text(
                                text = "${activeSounds.size} Active Audio Channels",
                                fontSize = 12.sp,
                                color = Color(0xFF79747E)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF49454F))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Master Volume & Quick Controls Strip
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "MASTER OUTPUT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6750A4),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${(masterVolume * 100).toInt()}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1C1B1F)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.VolumeDown,
                                contentDescription = null,
                                tint = Color(0xFF79747E),
                                modifier = Modifier.size(20.dp)
                            )
                            Slider(
                                value = masterVolume,
                                onValueChange = onMasterVolumeChange,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF6750A4),
                                    activeTrackColor = Color(0xFF6750A4)
                                )
                            )
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = Color(0xFF79747E),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = onFadeAllOut,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Fade All Out", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }

                            Button(
                                onClick = onStopAll,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Kill All", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Channel Strip Header
                Text(
                    text = "ACTIVE CHANNELS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF79747E),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )

                if (activeSounds.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔇", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No sounds currently playing",
                                fontSize = 14.sp,
                                color = Color(0xFF79747E)
                            )
                            Text(
                                "Tap any sound pad or loop to start mixing",
                                fontSize = 12.sp,
                                color = Color(0xFF9E9E9E)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(activeSounds, key = { it.id }) { sound ->
                            val status = playbackStates[sound.id]
                            val vol = channelVolumes[sound.id] ?: sound.volume
                            val isMuted = mutedChannels.contains(sound.id)

                            ChannelStripItem(
                                sound = sound,
                                status = status,
                                volume = vol,
                                isMuted = isMuted,
                                onVolumeChange = { onChannelVolumeChange(sound.id, it) },
                                onToggleMute = { onToggleMute(sound.id) },
                                onSolo = { onSoloChannel(sound.id) },
                                onStop = { onStopChannel(sound.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelStripItem(
    sound: SoundClipEntity,
    status: PlaybackStatus?,
    volume: Float,
    isMuted: Boolean,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onSolo: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(sound.emoji, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = sound.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1C1B1F),
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (status?.isLooping == true) {
                                Text(
                                    text = "🔄 LOOPING",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6750A4)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = "${status?.currentMs ?: 0}ms / ${sound.durationMs}ms",
                                fontSize = 11.sp,
                                color = Color(0xFF79747E)
                            )
                        }
                    }
                }

                // Control Action Badges
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Mute Button
                    FilledTonalButton(
                        onClick = onToggleMute,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isMuted) Color(0xFFBA1A1A).copy(alpha = 0.15f) else Color(0xFFE8DEF8)
                        ),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (isMuted) "MUTED" else "MUTE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMuted) Color(0xFFBA1A1A) else Color(0xFF6750A4)
                        )
                    }

                    // Solo Button
                    OutlinedButton(
                        onClick = onSolo,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("SOLO", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Stop Button
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Stop",
                            tint = Color(0xFF79747E),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Volume Slider & VU Meter Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AnimatedVUMeter(isPlaying = status?.isPlaying == true && !isMuted)
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    value = if (isMuted) 0f else volume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF6750A4),
                        activeTrackColor = Color(0xFF6750A4)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${(volume * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF49454F),
                    modifier = Modifier.width(36.dp)
                )
            }
        }
    }
}

@Composable
private fun AnimatedVUMeter(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "vu")
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(240, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(310, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b2"
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(190, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b3"
    )

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .height(20.dp)
            .width(20.dp)
    ) {
        val h1 = if (isPlaying) (bar1 * 18).dp else 3.dp
        val h2 = if (isPlaying) (bar2 * 18).dp else 3.dp
        val h3 = if (isPlaying) (bar3 * 18).dp else 3.dp

        Box(
            modifier = Modifier
                .width(4.dp)
                .height(h1)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isPlaying) Color(0xFF10B981) else Color(0xFFCAC4D0))
        )
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(h2)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isPlaying) Color(0xFF6750A4) else Color(0xFFCAC4D0))
        )
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(h3)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isPlaying) Color(0xFFF59E0B) else Color(0xFFCAC4D0))
        )
    }
}
