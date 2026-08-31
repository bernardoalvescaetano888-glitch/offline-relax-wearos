package com.example.relaxwatchplayer.presentation

import com.example.relaxwatchplayer.R
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer
    private lateinit var audioManager: AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        player = ExoPlayer.Builder(this).build()

        val listaNomesMusicas = mutableStateListOf<String>()
        carregarMusicas(listaNomesMusicas)

        setContent {
            WearPlayerApp(
                player = player,
                audioManager = audioManager,
                listaMusicas = listaNomesMusicas
            )
        }
    }

    private fun carregarMusicas(listaNomesOut: MutableList<String>) {
        val camposRaw = R.raw::class.java.fields

        for (campo in camposRaw) {
            try {
                val resourceId = campo.getInt(null)
                val nomeFormatado = campo.name.replace("_", " ").replaceFirstChar { it.uppercase() }
                listaNomesOut.add(nomeFormatado)

                val uri = Uri.parse("android.resource://$packageName/$resourceId")
                val mediaItem = MediaItem.fromUri(uri)
                player.addMediaItem(mediaItem)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        player.prepare()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}

@Composable
fun WearPlayerApp(
    player: ExoPlayer,
    audioManager: AudioManager,
    listaMusicas: List<String>
) {
    var isPlaying by remember { mutableStateOf(false) }
    var nomeMusicaAtual by remember { mutableStateOf("Relax Music") }
    var indiceAtual by remember { mutableIntStateOf(0) }
    var progresso by remember { mutableFloatStateOf(0f) }
    var isShuffle by remember { mutableStateOf(false) }
    var isRepeat by remember { mutableStateOf(false) }
    var mostrarLista by remember { mutableStateOf(false) }

    // Atualização contínua do tempo / progresso
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            if (player.duration > 0) {
                progresso = player.currentPosition.toFloat() / player.duration.toFloat()
            }
            delay(500L)
        }
    }

    // Listener do ExoPlayer para sincronizar estado
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = player.currentMediaItemIndex
                if (index in listaMusicas.indices) {
                    indiceAtual = index
                    nomeMusicaAtual = listaMusicas[index]
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    MaterialTheme {
        Scaffold {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF1E2A38), Color(0xFF000000)),
                            radius = 350f
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!mostrarLista) {
                    // TELA PRINCIPAL DO PLAYER
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                    ) {
                        // Topo: Botão da Lista de 50 Músicas
                        Chip(
                            onClick = { mostrarLista = true },
                            label = {
                                Text(
                                    text = "Músicas (${listaMusicas.size})",
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = "Lista",
                                    tint = Color.Cyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = ChipDefaults.secondaryChipColors(),
                            modifier = Modifier
                                .height(28.dp)
                                .padding(bottom = 2.dp)
                        )

                        // Nome da Música
                        Text(
                            text = nomeMusicaAtual,
                            style = MaterialTheme.typography.body2,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )

                        // Barra de Progresso Circular com Controles no Centro
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(110.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = progresso,
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 5.dp,
                                indicatorColor = Color.Cyan,
                                trackColor = Color.DarkGray.copy(alpha = 0.5f)
                            )

                            // Controles principais
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Button(
                                    onClick = {
                                        if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
                                    },
                                    colors = ButtonDefaults.secondaryButtonColors(),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FastRewind,
                                        contentDescription = "Anterior",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Button(
                                    onClick = {
                                        if (isPlaying) player.pause() else player.play()
                                    },
                                    colors = ButtonDefaults.primaryButtonColors(backgroundColor = Color.Cyan),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Button(
                                    onClick = {
                                        if (player.hasNextMediaItem()) player.seekToNextMediaItem()
                                    },
                                    colors = ButtonDefaults.secondaryButtonColors(),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FastForward,
                                        contentDescription = "Próxima",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Opções: Shuffle, Volume -/+, Loop
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Modo Aleatório (Shuffle)
                            CompactButton(
                                onClick = {
                                    isShuffle = !isShuffle
                                    player.shuffleModeEnabled = isShuffle
                                },
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = if (isShuffle) Color.Cyan else Color.Gray.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Aleatório",
                                    tint = if (isShuffle) Color.Black else Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Volume -
                            CompactButton(
                                onClick = {
                                    audioManager.adjustStreamVolume(
                                        AudioManager.STREAM_MUSIC,
                                        AudioManager.ADJUST_LOWER,
                                        AudioManager.FLAG_SHOW_UI
                                    )
                                },
                                colors = ButtonDefaults.secondaryButtonColors(),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeDown,
                                    contentDescription = "Diminuir Volume",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Volume +
                            CompactButton(
                                onClick = {
                                    audioManager.adjustStreamVolume(
                                        AudioManager.STREAM_MUSIC,
                                        AudioManager.ADJUST_RAISE,
                                        AudioManager.FLAG_SHOW_UI
                                    )
                                },
                                colors = ButtonDefaults.secondaryButtonColors(),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Aumentar Volume",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Modo Repetir (Loop)
                            CompactButton(
                                onClick = {
                                    isRepeat = !isRepeat
                                    player.repeatMode = if (isRepeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                                },
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = if (isRepeat) Color.Cyan else Color.Gray.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = "Loop",
                                    tint = if (isRepeat) Color.Black else Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                } else {
                    // ABA COM A LISTA DAS 50 MÚSICAS
                    val listState = rememberScalingLazyListState()

                    ScalingLazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Chip(
                                onClick = { mostrarLista = false },
                                label = { Text(" Voltar ao Player", fontSize = 12.sp) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Voltar",
                                        tint = Color.Cyan
                                    )
                                },
                                colors = ChipDefaults.primaryChipColors(backgroundColor = Color.DarkGray),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                        }

                        itemsIndexed(listaMusicas) { index, titulo ->
                            val estaTocando = (index == indiceAtual)

                            Chip(
                                onClick = {
                                    player.seekTo(index, 0)
                                    player.play()
                                    mostrarLista = false
                                },
                                label = {
                                    Text(
                                        text = "${index + 1}. $titulo",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (estaTocando) FontWeight.Bold else FontWeight.Normal,
                                        color = if (estaTocando) Color.Cyan else Color.White
                                    )
                                },
                                icon = {
                                    if (estaTocando) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Tocando",
                                            tint = Color.Cyan
                                        )
                                    }
                                },
                                colors = ChipDefaults.secondaryChipColors(
                                    backgroundColor = if (estaTocando) Color(0xFF132B38) else Color(0xFF1F1F1F)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}