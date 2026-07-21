package com.example.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.HighScore
import com.example.data.HighScoreRepository
import com.example.sound.PacSoundManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

class PacManViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = HighScoreRepository(database.highScoreDao())

    // Leaderboard flow
    val topScores: StateFlow<List<HighScore>> = repository.topScores
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val soundManager = PacSoundManager()

    // Game states
    var phase by mutableStateOf(GamePhase.START_SCREEN)
        private set

    var score by mutableStateOf(0)
        private set

    var highScore by mutableStateOf(10000)
        private set

    var lives by mutableStateOf(3)
        private set

    var level by mutableStateOf(1)
        private set

    var isCosmicMaze by mutableStateOf(true) // Default to True for Celestial Galaxy theme
        private set

    fun toggleMazeType() {
        isCosmicMaze = !isCosmicMaze
        soundManager.isCosmicMode = isCosmicMaze
        if (phase == GamePhase.PLAYING || phase == GamePhase.COUNTDOWN) {
            initializeLevel()
            if (isCosmicMaze) {
                soundManager.startAmbientMusic()
            } else {
                soundManager.stopAmbientMusic()
            }
        }
    }

    var countdownSeconds by mutableStateOf(3)
        private set

    var grid = Array(GameConfig.MAZE_ROWS) { Array(GameConfig.MAZE_COLS) { TileType.EMPTY } }
        private set

    var pacman by mutableStateOf(PacManState(9f, 16f))
        private set

    var ghosts by mutableStateOf<List<Ghost>>(emptyList())
        private set

    var scoreTextToShow by mutableStateOf("")
    var scoreTextPos by mutableStateOf(Pair(0f, 0f))
    var scoreTextTimer by mutableStateOf(0)

    var pacmanDeathProgress by mutableStateOf(0f)
    var levelCompleteProgress by mutableStateOf(0f)

    var finalScoreSaved by mutableStateOf(false)
        private set

    // Speed configurations
    private val basePacmanSpeed = 0.08f
    private val baseGhostSpeed = 0.07f
    private val frightenedGhostSpeed = 0.04f
    private val eatenGhostSpeed = 0.16f

    private var gameLoopJob: Job? = null
    var frightenedTimeLeftMs by mutableStateOf(0L)
        private set
    private var scatterTimerMs = 0L
    private var ghostsEatenInSession = 0
    private var altPelletSound = false

    init {
        // Load initial high score
        viewModelScope.launch {
            topScores.collect { scores ->
                if (scores.isNotEmpty()) {
                    highScore = scores.first().score
                }
            }
        }
    }

    fun startGame() {
        score = 0
        lives = 3
        level = 1
        finalScoreSaved = false
        soundManager.isCosmicMode = isCosmicMaze
        initializeLevel()
        phase = GamePhase.COUNTDOWN
        soundManager.playStartDitty()
        startCountdown()
    }

    private fun startCountdown() {
        viewModelScope.launch {
            countdownSeconds = 3
            while (countdownSeconds > 0) {
                delay(1000)
                countdownSeconds--
            }
            phase = GamePhase.PLAYING
            if (isCosmicMaze) {
                soundManager.startAmbientMusic()
            }
            startGameLoop()
        }
    }

    private fun initializeLevel() {
        // Rebuild grid from blueprint
        val blueprint = if (isCosmicMaze) GameConfig.COSMIC_MAZE_BLUEPRINT else GameConfig.MAZE_BLUEPRINT
        for (r in 0 until GameConfig.MAZE_ROWS) {
            val line = blueprint[r]
            for (c in 0 until GameConfig.MAZE_COLS) {
                grid[r][c] = when (line[c]) {
                    '1' -> TileType.WALL
                    '.' -> TileType.PELLET
                    'o' -> TileType.POWER_PELLET
                    'G' -> TileType.GHOST_GATE
                    'H' -> TileType.GHOST_HOUSE
                    else -> TileType.EMPTY
                }
            }
        }

        // Spawn Pacman
        pacman = PacManState(
            x = GameConfig.PACMAN_SPAWN_COL.toFloat(),
            y = GameConfig.PACMAN_SPAWN_ROW.toFloat(),
            dir = Direction.NONE,
            nextDir = Direction.NONE
        )

        // Spawn Ghosts
        ghosts = listOf(
            Ghost(GhostType.BLINKY, 9f, 8f, Direction.UP, GhostMode.SCATTER),
            Ghost(GhostType.PINKY, 9f, 10f, Direction.UP, GhostMode.SCATTER),
            Ghost(GhostType.INKY, 8f, 10f, Direction.UP, GhostMode.SCATTER),
            Ghost(GhostType.CLYDE, 10f, 10f, Direction.UP, GhostMode.SCATTER)
        )

        frightenedTimeLeftMs = 0
        scatterTimerMs = 0
        ghostsEatenInSession = 0
    }

    fun handleDirectionInput(dir: Direction) {
        if (phase == GamePhase.PLAYING) {
            pacman = pacman.copy(nextDir = dir)
        }
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            while (phase == GamePhase.PLAYING) {
                val currentTime = System.currentTimeMillis()
                val delta = currentTime - lastTime
                lastTime = currentTime

                updateGamePhysics()
                delay(16) // Target ~60 FPS
            }
        }
    }

    private fun updateGamePhysics() {
        // 1. Update frightened timer
        if (frightenedTimeLeftMs > 0) {
            frightenedTimeLeftMs -= 16
            if (frightenedTimeLeftMs <= 0) {
                frightenedTimeLeftMs = 0
                ghosts.forEach { ghost ->
                    if (ghost.mode == GhostMode.FRIGHTENED) {
                        ghost.mode = GhostMode.CHASE
                    }
                }
            }
        }

        // Increment scatter/chase alternating timer
        scatterTimerMs += 16

        // 2. Move Pacman
        movePacman()

        // 3. Move Ghosts
        moveGhosts()

        // 4. Handle collisions (Pellets and Ghosts)
        checkCollisions()

        // 5. Update score text timer if visible
        if (scoreTextTimer > 0) {
            scoreTextTimer -= 16
            if (scoreTextTimer <= 0) {
                scoreTextToShow = ""
            }
        }
    }

    private fun movePacman() {
        val speed = basePacmanSpeed + (level * 0.005f).coerceAtMost(0.03f)
        var px = pacman.x
        var py = pacman.y
        var pDir = pacman.dir
        val pNextDir = pacman.nextDir

        // Snap tolerance: if we are close to integer grid coordinate
        val colRound = px.roundToInt()
        val rowRound = py.roundToInt()
        val isNearX = abs(px - colRound) < speed
        val isNearY = abs(py - rowRound) < speed

        if (isNearX && isNearY) {
            // We are close to the center of grid cell (colRound, rowRound)
            // Can we execute queued turn?
            if (pNextDir != Direction.NONE && isValidMove(rowRound + pNextDir.dRow, colRound + pNextDir.dCol, false)) {
                px = colRound.toFloat()
                py = rowRound.toFloat()
                pDir = pNextDir
                pacman = pacman.copy(nextDir = Direction.NONE)
            } else if (pDir != Direction.NONE && !isValidMove(rowRound + pDir.dRow, colRound + pDir.dCol, false)) {
                // Next cell is wall, so stop
                px = colRound.toFloat()
                py = rowRound.toFloat()
                pDir = Direction.NONE
            }
        }

        // Apply movement
        if (pDir != Direction.NONE) {
            px += pDir.dCol * speed
            py += pDir.dRow * speed

            // Wrap around portals
            if (px < -0.5f) {
                px = GameConfig.MAZE_COLS - 0.5f
            } else if (px > GameConfig.MAZE_COLS - 0.5f) {
                px = -0.5f
            }

            // Animate mouth
            var angle = pacman.mouthAngle
            var opening = pacman.mouthOpening
            if (opening) {
                angle += 8f
                if (angle >= 45f) {
                    angle = 45f
                    opening = false
                }
            } else {
                angle -= 8f
                if (angle <= 0f) {
                    angle = 0f
                    opening = true
                }
            }
            pacman = pacman.copy(x = px, y = py, dir = pDir, mouthAngle = angle, mouthOpening = opening)
        } else {
            pacman = pacman.copy(x = px, y = py, dir = pDir)
        }
    }

    private fun moveGhosts() {
        val pacRow = pacman.y.roundToInt()
        val pacCol = pacman.x.roundToInt()

        // 7 seconds SCATTER, then 20 seconds CHASE, and repeat
        val scatterCycleMs = 27000L
        val isScatterPhase = (scatterTimerMs % scatterCycleMs) < 7000L

        ghosts.forEach { ghost ->
            // Determine speed
            val speed = when (ghost.mode) {
                GhostMode.FRIGHTENED -> frightenedGhostSpeed
                GhostMode.EATEN -> eatenGhostSpeed
                else -> baseGhostSpeed + (level * 0.005f).coerceAtMost(0.02f)
            }

            var gx = ghost.x
            var gy = ghost.y
            var gDir = ghost.dir

            val colRound = gx.roundToInt()
            val rowRound = gy.roundToInt()

            val isNearX = abs(gx - colRound) < speed
            val isNearY = abs(gy - rowRound) < speed

            if (isNearX && isNearY) {
                // Ghost is centered in cell (colRound, rowRound)
                // If they transitioned to a new intersection tile, make a choice
                if (colRound != ghost.lastInterCol || rowRound != ghost.lastInterRow) {
                    ghost.lastInterCol = colRound
                    ghost.lastInterRow = rowRound

                    // Snap to cell centers to prevent drift
                    gx = colRound.toFloat()
                    gy = rowRound.toFloat()

                    // Choose Target row/col based on personality & mode
                    if (ghost.mode == GhostMode.EATEN) {
                        ghost.targetRow = GameConfig.GHOST_HOUSE_ROW
                        ghost.targetCol = GameConfig.GHOST_HOUSE_COL
                        // If they arrived inside the ghost house, restore them
                        if (rowRound == GameConfig.GHOST_HOUSE_ROW && colRound == GameConfig.GHOST_HOUSE_COL) {
                            ghost.mode = if (frightenedTimeLeftMs > 0) GhostMode.FRIGHTENED else GhostMode.CHASE
                        }
                    } else if (frightenedTimeLeftMs > 0 && ghost.mode != GhostMode.EATEN) {
                        ghost.mode = GhostMode.FRIGHTENED
                    } else {
                        ghost.mode = if (isScatterPhase) GhostMode.SCATTER else GhostMode.CHASE
                    }

                    if (ghost.mode == GhostMode.CHASE) {
                        when (ghost.type) {
                            GhostType.BLINKY -> {
                                ghost.targetRow = pacRow
                                ghost.targetCol = pacCol
                            }
                            GhostType.PINKY -> {
                                ghost.targetRow = pacRow + pacman.dir.dRow * 4
                                ghost.targetCol = pacCol + pacman.dir.dCol * 4
                            }
                            GhostType.INKY -> {
                                ghost.targetRow = pacRow + pacman.dir.dRow * 2
                                ghost.targetCol = pacCol - pacman.dir.dCol * 2
                            }
                            GhostType.CLYDE -> {
                                val dist = distSq(rowRound.toFloat(), colRound.toFloat(), pacRow.toFloat(), pacCol.toFloat())
                                if (dist > 64f) { // 8 tiles away
                                    ghost.targetRow = pacRow
                                    ghost.targetCol = pacCol
                                } else {
                                    ghost.targetRow = GameConfig.MAZE_ROWS - 1
                                    ghost.targetCol = 0
                                }
                            }
                        }
                    } else if (ghost.mode == GhostMode.SCATTER) {
                        when (ghost.type) {
                            GhostType.BLINKY -> {
                                ghost.targetRow = 0
                                ghost.targetCol = GameConfig.MAZE_COLS - 1
                            }
                            GhostType.PINKY -> {
                                ghost.targetRow = 0
                                ghost.targetCol = 0
                            }
                            GhostType.INKY -> {
                                ghost.targetRow = GameConfig.MAZE_ROWS - 1
                                ghost.targetCol = GameConfig.MAZE_COLS - 1
                            }
                            GhostType.CLYDE -> {
                                ghost.targetRow = GameConfig.MAZE_ROWS - 1
                                ghost.targetCol = 0
                            }
                        }
                    }

                    // Choose direction (No 180s unless forced, minimizing distance to target)
                    gDir = selectGhostDirection(rowRound, colRound, gDir, ghost)
                }
            }

            // Move ghost
            if (gDir != Direction.NONE) {
                gx += gDir.dCol * speed
                gy += gDir.dRow * speed

                // Portal wrap around
                if (gx < -0.5f) {
                    gx = GameConfig.MAZE_COLS - 0.5f
                } else if (gx > GameConfig.MAZE_COLS - 0.5f) {
                    gx = -0.5f
                }
            }

            ghost.x = gx
            ghost.y = gy
            ghost.dir = gDir
        }
    }

    private fun selectGhostDirection(row: Int, col: Int, curDir: Direction, ghost: Ghost): Direction {
        val validDirs = mutableListOf<Direction>()

        for (d in Direction.values()) {
            if (d == Direction.NONE) continue
            if (d == curDir.opposite()) continue // No 180s

            val nextR = row + d.dRow
            val nextC = col + d.dCol

            if (isValidMove(nextR, nextC, true)) {
                validDirs.add(d)
            }
        }

        if (validDirs.isEmpty()) {
            // Backtrack if stuck
            val opp = curDir.opposite()
            if (isValidMove(row + opp.dRow, col + opp.dCol, true)) {
                return opp
            }
            return Direction.NONE
        }

        if (ghost.mode == GhostMode.FRIGHTENED) {
            // Random direction in frightened mode
            return validDirs[Random.nextInt(validDirs.size)]
        }

        // Find the direction that brings the ghost closest to the target
        var bestDir = validDirs.first()
        var minDist = Double.MAX_VALUE

        for (d in validDirs) {
            val nextR = row + d.dRow
            val nextC = col + d.dCol
            val dist = distSq(nextR.toFloat(), nextC.toFloat(), ghost.targetRow.toFloat(), ghost.targetCol.toFloat())
            if (dist < minDist) {
                minDist = dist.toDouble()
                bestDir = d
            }
        }

        return bestDir
    }

    private fun isValidMove(row: Int, col: Int, isGhost: Boolean): Boolean {
        // Tunnels / portal wrap boundaries are valid
        if (row == 10 || row == 8 || row == 12) {
            if (col < 0 || col >= GameConfig.MAZE_COLS) return true
        }

        if (row < 0 || row >= GameConfig.MAZE_ROWS || col < 0 || col >= GameConfig.MAZE_COLS) {
            return false
        }

        val tile = grid[row][col]
        if (tile == TileType.WALL) return false

        if (!isGhost) {
            // Pacman cannot enter ghost house or gate
            if (tile == TileType.GHOST_GATE || tile == TileType.GHOST_HOUSE) {
                return false
            }
        }

        return true
    }

    private fun checkCollisions() {
        val pacRow = pacman.y.roundToInt()
        val pacCol = pacman.x.roundToInt()

        if (pacRow in 0 until GameConfig.MAZE_ROWS && pacCol in 0 until GameConfig.MAZE_COLS) {
            val tile = grid[pacRow][pacCol]

            // 1. Eat Pellet
            if (tile == TileType.PELLET) {
                grid[pacRow][pacCol] = TileType.EMPTY
                score += 10
                soundManager.playPellet(altPelletSound)
                altPelletSound = !altPelletSound
                checkVictory()
            }

            // 2. Eat Power Pellet
            else if (tile == TileType.POWER_PELLET) {
                grid[pacRow][pacCol] = TileType.EMPTY
                score += 50
                soundManager.playPowerPellet()
                frightenedTimeLeftMs = 8000L
                ghostsEatenInSession = 0
                ghosts.forEach { ghost ->
                    if (ghost.mode != GhostMode.EATEN) {
                        ghost.mode = GhostMode.FRIGHTENED
                    }
                }
                checkVictory()
            }
        }

        // Update highScore immediately
        if (score > highScore) {
            highScore = score
        }

        // 3. Collision with Ghosts
        val collisionThreshold = 0.6f
        ghosts.forEach { ghost ->
            val dist = sqrt((pacman.x - ghost.x).pow(2) + (pacman.y - ghost.y).pow(2))
            if (dist < collisionThreshold) {
                if (ghost.mode == GhostMode.FRIGHTENED) {
                    // Eat ghost!
                    ghost.mode = GhostMode.EATEN
                    ghostsEatenInSession++
                    val pointsAwarded = 200 * 2.0.pow(ghostsEatenInSession - 1).toInt().coerceAtMost(1600)
                    score += pointsAwarded
                    soundManager.playGhostEat()

                    // Brief pause for point pop
                    scoreTextToShow = "+$pointsAwarded"
                    scoreTextPos = Pair(ghost.x, ghost.y)
                    scoreTextTimer = 600

                    triggerGhostEatenPause()
                } else if (ghost.mode != GhostMode.EATEN) {
                    // Pacman dies!
                    triggerPacmanDeath()
                }
            }
        }
    }

    private fun checkVictory() {
        var pelletsLeft = 0
        for (r in 0 until GameConfig.MAZE_ROWS) {
            for (c in 0 until GameConfig.MAZE_COLS) {
                if (grid[r][c] == TileType.PELLET || grid[r][c] == TileType.POWER_PELLET) {
                    pelletsLeft++
                }
            }
        }

        if (pelletsLeft == 0) {
            triggerLevelComplete()
        }
    }

    private fun triggerGhostEatenPause() {
        val currentPhaseBeforePause = phase
        phase = GamePhase.GHOST_EATEN_PAUSE
        gameLoopJob?.cancel()

        viewModelScope.launch {
            delay(600)
            phase = GamePhase.PLAYING
            startGameLoop()
        }
    }

    private fun triggerPacmanDeath() {
        phase = GamePhase.PACMAN_DEATH_ANIM
        gameLoopJob?.cancel()
        soundManager.playDeath()

        viewModelScope.launch {
            // Animate spin / shrink
            pacmanDeathProgress = 0f
            while (pacmanDeathProgress < 1f) {
                delay(40)
                pacmanDeathProgress += 0.05f
            }

            lives--
            if (lives > 0) {
                // Respawn pacman and ghosts
                pacman = PacManState(
                    x = GameConfig.PACMAN_SPAWN_COL.toFloat(),
                    y = GameConfig.PACMAN_SPAWN_ROW.toFloat(),
                    dir = Direction.NONE,
                    nextDir = Direction.NONE
                )
                ghosts = listOf(
                    Ghost(GhostType.BLINKY, 9f, 8f, Direction.UP, GhostMode.SCATTER),
                    Ghost(GhostType.PINKY, 9f, 10f, Direction.UP, GhostMode.SCATTER),
                    Ghost(GhostType.INKY, 8f, 10f, Direction.UP, GhostMode.SCATTER),
                    Ghost(GhostType.CLYDE, 10f, 10f, Direction.UP, GhostMode.SCATTER)
                )
                frightenedTimeLeftMs = 0
                phase = GamePhase.COUNTDOWN
                startCountdown()
            } else {
                phase = GamePhase.GAME_OVER
                soundManager.playGameOver()
            }
        }
    }

    private fun triggerLevelComplete() {
        phase = GamePhase.LEVEL_COMPLETE_ANIM
        gameLoopJob?.cancel()

        viewModelScope.launch {
            // Flash walls
            levelCompleteProgress = 0f
            while (levelCompleteProgress < 1f) {
                delay(150)
                levelCompleteProgress += 0.15f
            }

            level++
            initializeLevel()
            phase = GamePhase.COUNTDOWN
            startCountdown()
        }
    }

    fun saveHighScore(name: String) {
        if (finalScoreSaved) return
        val trimmedName = name.trim().uppercase()
        val finalName = if (trimmedName.isEmpty()) "PAC" else trimmedName.take(3)
        
        viewModelScope.launch {
            repository.insert(
                HighScore(
                    playerName = finalName,
                    score = score,
                    level = level
                )
            )
            finalScoreSaved = true
            phase = GamePhase.LEADERBOARD
        }
    }

    fun showLeaderboard() {
        soundManager.stopAmbientMusic()
        phase = GamePhase.LEADERBOARD
    }

    fun goHome() {
        soundManager.stopAmbientMusic()
        phase = GamePhase.START_SCREEN
    }

    private fun distSq(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return (x1 - x2).pow(2) + (y1 - y2).pow(2)
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.stopAmbientMusic()
    }
}
