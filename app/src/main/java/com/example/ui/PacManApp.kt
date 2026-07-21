package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.HighScore
import com.example.game.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacManApp(viewModel: PacManViewModel) {
    val phase = viewModel.phase
    val topScores by viewModel.topScores.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF030114) // Deep Space black-blue
    ) {
        when (phase) {
            GamePhase.START_SCREEN -> StartScreen(
                viewModel = viewModel,
                topScores = topScores
            )
            GamePhase.COUNTDOWN,
            GamePhase.PLAYING,
            GamePhase.GHOST_EATEN_PAUSE,
            GamePhase.PACMAN_DEATH_ANIM,
            GamePhase.LEVEL_COMPLETE_ANIM -> PlayingScreen(viewModel = viewModel)
            GamePhase.GAME_OVER -> GameOverScreen(
                viewModel = viewModel,
                score = viewModel.score,
                level = viewModel.level
            )
            GamePhase.LEADERBOARD -> LeaderboardScreen(
                viewModel = viewModel,
                topScores = topScores
            )
        }
    }
}

@Composable
fun StartScreen(viewModel: PacManViewModel, topScores: List<HighScore>) {
    var isMuted by remember { mutableStateOf(viewModel.soundManager.isMuted) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF030114), // Deep Space Midnight Blue
                        Color(0xFF11072B), // Cosmic Deep Purple
                        Color(0xFF220A43)  // Nebula Violet
                    )
                )
            )
            .padding(24.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_galactic_wallpaper),
            contentDescription = "Galactic background wallpaper",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.35f
        )

        // Mute button in top right
        IconButton(
            onClick = {
                isMuted = !isMuted
                viewModel.soundManager.isMuted = isMuted
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .testTag("mute_toggle")
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = "Mute Toggle",
                tint = if (isMuted) Color.Red else Color.Yellow
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Neon Glow Pac-Man Logo
            Text(
                text = "PAC-MAN",
                fontSize = 54.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFFFEB3B), // Arcade Yellow
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "GALACTIC EDITION",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF00E5FF), // Galactic Neon Cyan
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Animated character lineup
            Row(
                modifier = Modifier.padding(bottom = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini Pac-man drawing
                Canvas(modifier = Modifier.size(24.dp)) {
                    drawArc(
                        color = Color(0xFFFFEB3B),
                        startAngle = 30f,
                        sweepAngle = 300f,
                        useCenter = true,
                        size = this.size
                    )
                }

                // Mini Red ghost
                Canvas(modifier = Modifier.size(24.dp)) {
                    drawGhostSymbol(Color.Red)
                }

                // Mini Pink ghost
                Canvas(modifier = Modifier.size(24.dp)) {
                    drawGhostSymbol(Color(0xFFFFB8FF))
                }

                // Mini Cyan ghost
                Canvas(modifier = Modifier.size(24.dp)) {
                    drawGhostSymbol(Color(0xFF00FFFF))
                }
            }

            // High Score Banner
            if (topScores.isNotEmpty()) {
                val highest = topScores.first()
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                    border = BorderStroke(1.dp, Color(0xFF49454F)),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(bottom = 32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TOP HIGH SCORE",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFD0BCFF)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${highest.playerName} : ${highest.score}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }
                }
            }

            // Buttons
            Button(
                onClick = { viewModel.startGame() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp)
                    .testTag("start_game_button")
            ) {
                Text(
                    text = "PLAY ARCADE",
                    color = Color(0xFF381E72),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { viewModel.showLeaderboard() },
                border = BorderStroke(2.dp, Color(0xFF49454F)),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD0BCFF)),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp)
                    .testTag("leaderboard_button")
            ) {
                Text(
                    text = "LEADERBOARD",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Level / Maze Selector
            Text(
                text = "CHOOSE LEVEL PLANET / LAYOUT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF00E5FF),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF16132D))
                    .border(2.dp, Color(0xFF7E57C2), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Retro option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!viewModel.isCosmicMaze) Color(0xFF7E57C2) else Color.Transparent)
                        .clickable { if (viewModel.isCosmicMaze) viewModel.toggleMazeType() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "RETRO CLASSIC",
                        color = if (!viewModel.isCosmicMaze) Color.White else Color(0xFFB39DDB),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Cosmic Galaxy option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (viewModel.isCosmicMaze) Color(0xFF7E57C2) else Color.Transparent)
                        .clickable { if (!viewModel.isCosmicMaze) viewModel.toggleMazeType() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "COSMIC GALAXY",
                        color = if (viewModel.isCosmicMaze) Color.White else Color(0xFFB39DDB),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawGhostSymbol(color: Color) {
    val path = Path().apply {
        moveTo(0f, size.height)
        quadraticTo(0f, 0f, size.width / 2, 0f)
        quadraticTo(size.width, 0f, size.width, size.height)
        // bottom tentacles wave
        lineTo(size.width * 0.8f, size.height * 0.85f)
        lineTo(size.width * 0.6f, size.height)
        lineTo(size.width * 0.4f, size.height * 0.85f)
        lineTo(size.width * 0.2f, size.height)
        close()
    }
    drawPath(path, color)

    // Eyes
    drawCircle(Color.White, radius = size.width * 0.15f, center = Offset(size.width * 0.35f, size.height * 0.35f))
    drawCircle(Color.White, radius = size.width * 0.15f, center = Offset(size.width * 0.65f, size.height * 0.35f))
}

@Composable
fun PlayingScreen(viewModel: PacManViewModel) {
    var isMuted by remember { mutableStateOf(viewModel.soundManager.isMuted) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Header (Scores & Lives)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // SCORE Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                border = BorderStroke(1.dp, Color(0xFF49454F)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "SCORE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFCAC4D0)
                    )
                    Text(
                        text = String.format("%06d", viewModel.score),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFFEB3B) // Yellow score
                    )
                }
            }

            // HIGH SCORE Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                border = BorderStroke(1.dp, Color(0xFF49454F)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.2f)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "HIGH SCORE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFCAC4D0)
                    )
                    Text(
                        text = String.format("%06d", viewModel.highScore),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }

            // LIVES Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                border = BorderStroke(1.dp, Color(0xFF49454F)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(0.9f)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "LIVES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFCAC4D0)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(viewModel.lives) {
                            Canvas(modifier = Modifier.size(14.dp)) {
                                drawArc(
                                    color = Color(0xFFFFEB3B),
                                    startAngle = 35f,
                                    sweepAngle = 290f,
                                    useCenter = true,
                                    size = this.size
                                )
                            }
                        }
                        if (viewModel.lives == 0) {
                            Spacer(modifier = Modifier.width(14.dp))
                        }
                    }
                }
            }
        }

        // Action Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LEVEL ${viewModel.level}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFD0BCFF)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = {
                        isMuted = !isMuted
                        viewModel.soundManager.isMuted = isMuted
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF2B2930), CircleShape)
                        .border(1.dp, Color(0xFF49454F), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Mute",
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.goHome() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF2B2930), CircleShape)
                        .border(1.dp, Color(0xFF49454F), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 2. Play Field (Maze Canvas)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(4.dp, Color(0xFF7E57C2), RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF030114)),
            contentAlignment = Alignment.Center
        ) {
            GameCanvas(viewModel = viewModel)

            // Countdown / State Overlays
            if (viewModel.phase == GamePhase.COUNTDOWN) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (viewModel.countdownSeconds > 0) "${viewModel.countdownSeconds}" else "GO!",
                            fontSize = 60.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFFFFF00)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "READY PLAYER ONE",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF00FFFF)
                        )
                    }
                }
            }

            if (viewModel.phase == GamePhase.PACMAN_DEATH_ANIM) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Red.copy(alpha = 0.15f))
                )
            }
        }

        // 3. Controller Area (D-Pad and swipe guide)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "SWIPE SCREEN OR USE BUTTONS",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))

        ArcadeDPad(
            onDirection = { viewModel.handleDirectionInput(it) },
            currentDir = viewModel.pacman.dir,
            modifier = Modifier
                .padding(bottom = 24.dp)
                .testTag("arcade_controls")
        )
    }
}

@Composable
fun GameCanvas(viewModel: PacManViewModel) {
    val grid = viewModel.grid
    val pacman = viewModel.pacman
    val ghosts = viewModel.ghosts
    val phase = viewModel.phase

    // Power pellets flash animation
    val infiniteTransition = rememberInfiniteTransition(label = "flash")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(250, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val flashState = pulse > 0.5f

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val cw = constraints.maxWidth.toFloat()
        val ch = constraints.maxHeight.toFloat()

        val tileWidth = cw / GameConfig.MAZE_COLS
        val tileHeight = ch / GameConfig.MAZE_ROWS

        // Maintain exact aspect ratio to prevent distortions
        val cellSize = tileWidth.coerceAtMost(tileHeight)

        // Center maze offset
        val offsetX = (cw - (cellSize * GameConfig.MAZE_COLS)) / 2
        val offsetY = (ch - (cellSize * GameConfig.MAZE_ROWS)) / 2

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val (dx, dy) = dragAmount
                            if (abs(dx) > abs(dy)) {
                                if (dx > 10) viewModel.handleDirectionInput(Direction.RIGHT)
                                else if (dx < -10) viewModel.handleDirectionInput(Direction.LEFT)
                            } else {
                                if (dy > 10) viewModel.handleDirectionInput(Direction.DOWN)
                                else if (dy < -10) viewModel.handleDirectionInput(Direction.UP)
                            }
                        }
                    )
                }
        ) {
            // A. Draw Maze Walls and Pellets
            for (r in 0 until GameConfig.MAZE_ROWS) {
                for (c in 0 until GameConfig.MAZE_COLS) {
                    val tile = grid[r][c]
                    val x = offsetX + c * cellSize
                    val y = offsetY + r * cellSize

                    when (tile) {
                        TileType.WALL -> {
                            val margin = cellSize * 0.1f
                            val wallSize = cellSize - (margin * 2)
                            
                            if (viewModel.isCosmicMaze) {
                                // Outer cosmic nebula pink glow
                                drawRoundRect(
                                    color = Color(0xFFE040FB).copy(alpha = 0.45f),
                                    topLeft = Offset(x + margin - 1.dp.toPx(), y + margin - 1.dp.toPx()),
                                    size = Size(wallSize + 2.dp.toPx(), wallSize + 2.dp.toPx()),
                                    cornerRadius = CornerRadius(cellSize * 0.25f, cellSize * 0.25f),
                                    style = Stroke(width = 3.5.dp.toPx())
                                )
                                
                                // Inner high-intensity stellar magenta line
                                drawRoundRect(
                                    color = if (phase == GamePhase.LEVEL_COMPLETE_ANIM) {
                                        if (System.currentTimeMillis() % 200 < 100) Color.White else Color(0xFFFF007F)
                                    } else {
                                        Color(0xFFFF007F) // Stellar Magenta
                                    },
                                    topLeft = Offset(x + margin, y + margin),
                                    size = Size(wallSize, wallSize),
                                    cornerRadius = CornerRadius(cellSize * 0.25f, cellSize * 0.25f),
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                            } else {
                                // Classic Retro Theme
                                // Outer deep blue glow
                                drawRoundRect(
                                    color = Color(0xFF0D47A1).copy(alpha = 0.6f),
                                    topLeft = Offset(x + margin - 1.dp.toPx(), y + margin - 1.dp.toPx()),
                                    size = Size(wallSize + 2.dp.toPx(), wallSize + 2.dp.toPx()),
                                    cornerRadius = CornerRadius(cellSize * 0.25f, cellSize * 0.25f),
                                    style = Stroke(width = 3.5.dp.toPx())
                                )
                                
                                // Inner high-intensity classic neon blue line
                                drawRoundRect(
                                    color = if (phase == GamePhase.LEVEL_COMPLETE_ANIM) {
                                        if (System.currentTimeMillis() % 200 < 100) Color.White else Color(0xFF2196F3)
                                    } else {
                                        Color(0xFF2196F3) // Arcade Blue
                                    },
                                    topLeft = Offset(x + margin, y + margin),
                                    size = Size(wallSize, wallSize),
                                    cornerRadius = CornerRadius(cellSize * 0.25f, cellSize * 0.25f),
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                            }
                        }
                        TileType.GHOST_GATE -> {
                            // Cosmic barrier gate
                            drawLine(
                                color = Color(0xFF00E5FF),
                                start = Offset(x, y + cellSize / 2f),
                                end = Offset(x + cellSize, y + cellSize / 2f),
                                strokeWidth = 3.dp.toPx()
                            )
                        }
                        TileType.PELLET -> {
                            // Standard pellet: space dust
                            drawCircle(
                                color = Color(0xFF00E5FF).copy(alpha = 0.25f),
                                radius = 4.dp.toPx(),
                                center = Offset(x + cellSize / 2f, y + cellSize / 2f)
                            )
                            drawCircle(
                                color = Color(0xFFE0F7FA),
                                radius = 1.8.dp.toPx(),
                                center = Offset(x + cellSize / 2f, y + cellSize / 2f)
                            )
                        }
                        TileType.POWER_PELLET -> {
                            if (flashState) {
                                // Pulsing supernova outer aura
                                drawCircle(
                                    color = Color(0xFFFF4081).copy(alpha = 0.4f),
                                    radius = (6.dp.toPx() + pulse * 4.dp.toPx()),
                                    center = Offset(x + cellSize / 2f, y + cellSize / 2f)
                                )
                                // Golden hyper-core
                                drawCircle(
                                    color = Color(0xFFFFD700),
                                    radius = 5.5.dp.toPx(),
                                    center = Offset(x + cellSize / 2f, y + cellSize / 2f)
                                )
                            } else {
                                drawCircle(
                                    color = Color(0xFFFFEB3B),
                                    radius = 3.5.dp.toPx(),
                                    center = Offset(x + cellSize / 2f, y + cellSize / 2f)
                                )
                            }
                        }
                        else -> {
                            // Empty space: draw deterministic twinkling background stars!
                            if ((r * 7 + c * 13) % 4 == 0) {
                                val starX = x + cellSize / 2f + ((r * 11 + c * 17) % 7 - 3) * (cellSize * 0.1f)
                                val starY = y + cellSize / 2f + ((r * 13 + c * 19) % 7 - 3) * (cellSize * 0.1f)
                                val starSize = ((r * 3 + c * 5) % 3 + 1) * 0.7f
                                val opacity = 0.12f + (pulse * 0.18f) // Twinkle!
                                drawCircle(
                                    color = Color.White.copy(alpha = opacity),
                                    radius = starSize.dp.toPx(),
                                    center = Offset(starX, starY)
                                )
                            }
                        }
                    }
                }
            }

            // B. Draw Pac-Man
            val pX = offsetX + pacman.x * cellSize + cellSize / 2f
            val pY = offsetY + pacman.y * cellSize + cellSize / 2f
            val rad = (cellSize * 0.45f)

            if (phase == GamePhase.PACMAN_DEATH_ANIM) {
                // Death spin animation: close mouth and shrink
                val deathShrink = (1f - viewModel.pacmanDeathProgress).coerceIn(0f, 1f)
                val deathSpin = viewModel.pacmanDeathProgress * 360f
                drawArc(
                    color = Color.Yellow,
                    startAngle = deathSpin,
                    sweepAngle = 360f * deathShrink,
                    useCenter = true,
                    topLeft = Offset(pX - rad * deathShrink, pY - rad * deathShrink),
                    size = Size(rad * 2 * deathShrink, rad * 2 * deathShrink)
                )
            } else {
                // Draw galactic engine spark trail behind him
                if (pacman.dir != Direction.NONE) {
                    val trailOffset = rad * 1.1f
                    val trailX = when (pacman.dir) {
                        Direction.LEFT -> pX + trailOffset
                        Direction.RIGHT -> pX - trailOffset
                        else -> pX
                    }
                    val trailY = when (pacman.dir) {
                        Direction.UP -> pY + trailOffset
                        Direction.DOWN -> pY - trailOffset
                        else -> pY
                    }
                    drawCircle(
                        color = Color(0xFFFF5252).copy(alpha = 0.7f * pulse),
                        radius = rad * 0.35f,
                        center = Offset(trailX, trailY)
                    )
                    drawCircle(
                        color = Color(0xFFFFD740).copy(alpha = 0.5f * (1f - pulse)),
                        radius = rad * 0.2f,
                        center = Offset(
                            trailX + if (pacman.dir == Direction.LEFT) rad * 0.3f else if (pacman.dir == Direction.RIGHT) -rad * 0.3f else 0f,
                            trailY + if (pacman.dir == Direction.UP) rad * 0.3f else if (pacman.dir == Direction.DOWN) -rad * 0.3f else 0f
                        )
                    )
                }

                // Classic wedge mouth opening rotation
                val startAngle = when (pacman.dir) {
                    Direction.RIGHT, Direction.NONE -> pacman.mouthAngle
                    Direction.DOWN -> 90f + pacman.mouthAngle
                    Direction.LEFT -> 180f + pacman.mouthAngle
                    Direction.UP -> 270f + pacman.mouthAngle
                }
                val sweepAngle = 360f - (pacman.mouthAngle * 2f)

                drawArc(
                    color = Color(0xFFFFEB3B), // Brilliant yellow
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(pX - rad, pY - rad),
                    size = Size(rad * 2, rad * 2)
                )

                // Draw a Saturn-like cosmic orbit ring around Pac-Man
                withTransform({
                    rotate(degrees = -25f, pivot = Offset(pX, pY))
                }) {
                    drawOval(
                        color = Color(0xFF00E5FF).copy(alpha = 0.8f),
                        topLeft = Offset(pX - rad * 1.35f, pY - rad * 0.25f),
                        size = Size(rad * 2.7f, rad * 0.5f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }

            // C. Draw Ghosts
            ghosts.forEach { ghost ->
                val gX = offsetX + ghost.x * cellSize + cellSize / 2f
                val gY = offsetY + ghost.y * cellSize + cellSize / 2f
                val gRad = cellSize * 0.45f

                if (ghost.mode == GhostMode.EATEN) {
                    // Only draw retro eyes
                    drawGhostEyes(this, gX, gY, gRad, ghost.dir)
                } else {
                    // Decide body color
                    val bodyColor = if (ghost.mode == GhostMode.FRIGHTENED) {
                        // Flashing blue/white warning at < 2 seconds remaining
                        val flashPeriod = 250L
                        if (viewModel.frightenedTimeLeftMs < 2000L && (viewModel.frightenedTimeLeftMs / flashPeriod) % 2 == 0L) {
                            Color.White
                        } else {
                            Color(0xFF0022FF) // Rich retro blue
                        }
                    } else {
                        ghost.type.color
                    }

                    // Draw alien antennae on top of head
                    drawLine(
                        color = bodyColor,
                        start = Offset(gX - gRad * 0.3f, gY - gRad * 0.8f),
                        end = Offset(gX - gRad * 0.55f, gY - gRad * 1.25f),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawCircle(
                        color = if (ghost.mode == GhostMode.FRIGHTENED) Color(0xFFFFB852) else bodyColor,
                        radius = gRad * 0.18f,
                        center = Offset(gX - gRad * 0.55f, gY - gRad * 1.25f)
                    )
                    drawLine(
                        color = bodyColor,
                        start = Offset(gX + gRad * 0.3f, gY - gRad * 0.8f),
                        end = Offset(gX + gRad * 0.55f, gY - gRad * 1.25f),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawCircle(
                        color = if (ghost.mode == GhostMode.FRIGHTENED) Color(0xFFFFB852) else bodyColor,
                        radius = gRad * 0.18f,
                        center = Offset(gX + gRad * 0.55f, gY - gRad * 1.25f)
                    )

                    // Draw ghost body dome with tentacles wave path
                    val path = Path().apply {
                        moveTo(gX - gRad, gY + gRad)
                        quadraticTo(gX - gRad, gY - gRad, gX, gY - gRad)
                        quadraticTo(gX + gRad, gY - gRad, gX + gRad, gY + gRad)
                        
                        // Tentacles wavy bottom
                        lineTo(gX + gRad * 0.7f, gY + gRad * 0.75f)
                        lineTo(gX + gRad * 0.4f, gY + gRad)
                        lineTo(gX + gRad * 0.1f, gY + gRad * 0.75f)
                        lineTo(gX - gRad * 0.2f, gY + gRad)
                        lineTo(gX - gRad * 0.5f, gY + gRad * 0.75f)
                        lineTo(gX - gRad * 0.8f, gY + gRad)
                        close()
                    }
                    drawPath(path, bodyColor)

                    if (ghost.mode == GhostMode.FRIGHTENED) {
                        // Frightened face (white mini eyes, squiggly red/white mouth)
                        val faceColor = if (bodyColor == Color.White) Color.Red else Color(0xFFFFB852)
                        
                        // Small eyes
                        drawCircle(faceColor, radius = gRad * 0.15f, center = Offset(gX - gRad * 0.35f, gY - gRad * 0.2f))
                        drawCircle(faceColor, radius = gRad * 0.15f, center = Offset(gX + gRad * 0.35f, gY - gRad * 0.2f))

                        // Squiggly mouth line
                        drawLine(
                            color = faceColor,
                            start = Offset(gX - gRad * 0.5f, gY + gRad * 0.35f),
                            end = Offset(gX + gRad * 0.5f, gY + gRad * 0.35f),
                            strokeWidth = 2.dp.toPx()
                        )
                    } else {
                        // Standard eyes gazing in direction
                        drawGhostEyes(this, gX, gY, gRad, ghost.dir)
                    }
                }
            }

            // D. Draw Score Text popup (+200 / +400, etc)
            if (viewModel.scoreTextToShow.isNotEmpty()) {
                val scX = offsetX + viewModel.scoreTextPos.first * cellSize + cellSize / 2f
                val scY = offsetY + viewModel.scoreTextPos.second * cellSize + cellSize / 2f
                
                // Draw brief popup score points
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GREEN
                        textSize = cellSize * 0.7f
                        typeface = android.graphics.Typeface.MONOSPACE
                        isFakeBoldText = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    drawText(viewModel.scoreTextToShow, scX, scY, paint)
                }
            }
        }
    }
}

private fun drawGhostEyes(scope: DrawScope, gX: Float, gY: Float, gRad: Float, dir: Direction) {
    // Eye offset relative to center depending on gaze direction
    val offsetMult = 0.18f
    val ox = when (dir) {
        Direction.LEFT -> -gRad * offsetMult
        Direction.RIGHT -> gRad * offsetMult
        else -> 0f
    }
    val oy = when (dir) {
        Direction.UP -> -gRad * offsetMult
        Direction.DOWN -> gRad * offsetMult
        else -> 0f
    }

    // White globes
    scope.drawCircle(
        color = Color.White,
        radius = gRad * 0.32f,
        center = Offset(gX - gRad * 0.35f + ox, gY - gRad * 0.2f + oy)
    )
    scope.drawCircle(
        color = Color.White,
        radius = gRad * 0.32f,
        center = Offset(gX + gRad * 0.35f + ox, gY - gRad * 0.2f + oy)
    )

    // Blue pupils
    val px = ox * 1.5f
    val py = oy * 1.5f
    scope.drawCircle(
        color = Color(0xFF0033CC),
        radius = gRad * 0.14f,
        center = Offset(gX - gRad * 0.35f + ox + px, gY - gRad * 0.2f + oy + py)
    )
    scope.drawCircle(
        color = Color(0xFF0033CC),
        radius = gRad * 0.14f,
        center = Offset(gX + gRad * 0.35f + ox + px, gY - gRad * 0.2f + oy + py)
    )
}

@Composable
fun ArcadeDPad(
    onDirection: (Direction) -> Unit,
    currentDir: Direction,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(170.dp),
        contentAlignment = Alignment.Center
    ) {
        // D-Pad Background circular plate
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(Color(0xFF2B2930), CircleShape)
                .border(1.dp, Color(0xFF49454F), CircleShape)
        )

        // UP Button
        ArcadeArrowButton(
            icon = Icons.Default.ArrowUpward,
            isActive = currentDir == Direction.UP,
            onClick = { onDirection(Direction.UP) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
                .testTag("dpad_up")
        )

        // DOWN Button
        ArcadeArrowButton(
            icon = Icons.Default.ArrowDownward,
            isActive = currentDir == Direction.DOWN,
            onClick = { onDirection(Direction.DOWN) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
                .testTag("dpad_down")
        )

        // LEFT Button
        ArcadeArrowButton(
            icon = Icons.Default.ArrowBack,
            isActive = currentDir == Direction.LEFT,
            onClick = { onDirection(Direction.LEFT) },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
                .testTag("dpad_left")
        )

        // RIGHT Button
        ArcadeArrowButton(
            icon = Icons.Default.ArrowForward,
            isActive = currentDir == Direction.RIGHT,
            onClick = { onDirection(Direction.RIGHT) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
                .testTag("dpad_right")
        )

        // Central Red Joystick nub
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFF5555), Color(0xFF880000))
                    ),
                    CircleShape
                )
                .border(2.dp, Color.Black, CircleShape)
        )
    }
}

@Composable
fun ArcadeArrowButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(46.dp)
            .background(
                color = if (isActive) Color(0xFFD0BCFF) else Color(0xFF49454F),
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = if (isActive) Color(0xFFD0BCFF) else Color(0xFF49454F),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Direction Button",
            tint = if (isActive) Color(0xFF381E72) else Color(0xFFD0BCFF),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun GameOverScreen(viewModel: PacManViewModel, score: Int, level: Int) {
    var nameInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF040216), // Midnight Black-Blue
                        Color(0xFF0F0B30), // Deep Space Nebula Blue
                        Color(0xFF210C3C)  // Nebula Violet
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_galactic_wallpaper),
            contentDescription = "Galactic background wallpaper",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.35f
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "GAME OVER",
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFF44336), // Vibrant M3 Red
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "YOUR FINAL SCORE",
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFCAC4D0),
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = String.format("%06d", score),
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFFFEB3B), // Yellow highlight
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "REACHED LEVEL $level",
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFD0BCFF), // M3 Light Purple
                modifier = Modifier.padding(bottom = 36.dp)
            )

            // High Score Submission Name Input
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                border = BorderStroke(1.dp, Color(0xFF49454F)),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ENTER INITIALS FOR LEADERBOARD",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFCAC4D0),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { if (it.length <= 3) nameInput = it.uppercase() },
                        singleLine = true,
                        placeholder = { Text("AAA", color = Color(0xFF49454F)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color(0xFF49454F),
                            cursorColor = Color(0xFFD0BCFF)
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier
                            .width(120.dp)
                            .testTag("initials_input")
                    )
                }
            }

            // Buttons
            Button(
                onClick = { viewModel.saveHighScore(nameInput) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(52.dp)
                    .testTag("save_score_button")
            ) {
                Text(
                    text = "SAVE & CONTINUE",
                    color = Color(0xFF381E72),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { viewModel.startGame() },
                border = BorderStroke(2.dp, Color(0xFF49454F)),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD0BCFF)),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(52.dp)
                    .testTag("try_again_button")
            ) {
                Text(
                    text = "PLAY AGAIN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun LeaderboardScreen(viewModel: PacManViewModel, topScores: List<HighScore>) {
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF040216), // Midnight Black-Blue
                        Color(0xFF0F0B30), // Deep Space Nebula Blue
                        Color(0xFF210C3C)  // Nebula Violet
                    )
                )
            )
            .padding(24.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_galactic_wallpaper),
            contentDescription = "Galactic background wallpaper",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.35f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "HALL OF FAME",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFFFEB3B), // Yellow
                modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
            )

            Text(
                text = "TOP ARCADE PLAYERS",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFD0BCFF), // M3 Light Purple
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF49454F), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(Color(0xFF2B2930))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("RANK", color = Color(0xFFCAC4D0), fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                Text("NAME", color = Color(0xFFCAC4D0), fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(2f))
                Text("LEVEL", color = Color(0xFFCAC4D0), fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
                Text("SCORE", color = Color(0xFFCAC4D0), fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(2.5f), textAlign = TextAlign.End)
            }

            // Table Content
            Box(modifier = Modifier.weight(1f)) {
                if (topScores.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NO HIGH SCORES YET\nBE THE FIRST!",
                            color = Color(0xFFCAC4D0),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, Color(0xFF49454F), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    ) {
                        itemsIndexed(topScores) { index, entry ->
                            val rowBg = if (index % 2 == 0) Color(0xFF1C1B1F) else Color(0xFF252429)
                            val rankColor = when (index) {
                                0 -> Color(0xFFFFEB3B) // Gold
                                1 -> Color(0xFFCAC4D0) // Silver/Grey
                                2 -> Color(0xFFCD7F32) // Bronze
                                else -> Color.White
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(rowBg)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = rankColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = entry.playerName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(2f)
                                )
                                Text(
                                    text = "L${entry.level}",
                                    color = Color(0xFFD0BCFF),
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1.5f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = String.format("%,d", entry.score),
                                    color = Color(0xFFFFEB3B),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(2.5f),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.goHome() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(52.dp)
                    .testTag("leaderboard_back_button")
            ) {
                Text(
                    text = "BACK TO MENU",
                    color = Color(0xFF381E72),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
