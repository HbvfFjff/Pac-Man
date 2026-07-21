package com.example.game

import androidx.compose.ui.graphics.Color

enum class Direction {
    UP, DOWN, LEFT, RIGHT, NONE;

    fun opposite(): Direction = when (this) {
        UP -> DOWN
        DOWN -> UP
        LEFT -> RIGHT
        RIGHT -> LEFT
        NONE -> NONE
    }

    val dRow: Int
        get() = when (this) {
            UP -> -1
            DOWN -> 1
            LEFT -> 0
            RIGHT -> 0
            NONE -> 0
        }

    val dCol: Int
        get() = when (this) {
            UP -> 0
            DOWN -> 0
            LEFT -> -1
            RIGHT -> 1
            NONE -> 0
        }
}

enum class TileType {
    WALL,
    PELLET,
    POWER_PELLET,
    EMPTY,
    GHOST_GATE,
    GHOST_HOUSE
}

enum class GhostType(val ghostName: String, val color: Color) {
    BLINKY("Blinky", Color(0xFFFF0000)), // Red
    PINKY("Pinky", Color(0xFFFFB8FF)),   // Pink
    INKY("Inky", Color(0xFF00FFFF)),     // Cyan
    CLYDE("Clyde", Color(0xFFFFB852))    // Orange
}

enum class GhostMode {
    CHASE,
    SCATTER,
    FRIGHTENED,
    EATEN
}

enum class GamePhase {
    START_SCREEN,
    COUNTDOWN,
    PLAYING,
    GHOST_EATEN_PAUSE,
    PACMAN_DEATH_ANIM,
    LEVEL_COMPLETE_ANIM,
    GAME_OVER,
    LEADERBOARD
}

data class Position(val row: Int, val col: Int)

data class Ghost(
    val type: GhostType,
    var x: Float,
    var y: Float,
    var dir: Direction = Direction.NONE,
    var mode: GhostMode = GhostMode.SCATTER,
    var targetRow: Int = 0,
    var targetCol: Int = 0,
    var lastInterRow: Int = -1,
    var lastInterCol: Int = -1
)

data class PacManState(
    var x: Float,
    var y: Float,
    var dir: Direction = Direction.NONE,
    var nextDir: Direction = Direction.NONE,
    var mouthAngle: Float = 0f,
    var mouthOpening: Boolean = true
)

object GameConfig {
    const val MAZE_COLS = 19
    const val MAZE_ROWS = 22

    val MAZE_BLUEPRINT = listOf(
        "1111111111111111111",
        "1o.......1.......o1",
        "1.11.111.1.111.11.1",
        "1.11.111.1.111.11.1",
        "1.................1",
        "1.11.1.11111.1.11.1",
        "1....1...1...1....1",
        "1111.111.1.111.1111",
        "0001.1.......1.1000",
        "1111.1.11G11.1.1111",
        "0000...1HHH1...0000",
        "1111.1.11111.1.1111",
        "0001.1.......1.1000",
        "1111.1.11111.1.1111",
        "1........1........1",
        "1.11.111.1.111.11.1",
        "1o..1.........1..o1",
        "111.1.1.11111.1.111",
        "1....1...1...1....1",
        "1.111111.1.111111.1",
        "1.................1",
        "1111111111111111111"
    )

    val COSMIC_MAZE_BLUEPRINT = listOf(
        "1111111111111111111",
        "1o.......1.......o1",
        "1.111111.1.111111.1",
        "1.1....1.1.1....1.1",
        "1.1.11.1.1.1.11.1.1",
        "1...11.......11...1",
        "111.1..11111..1.111",
        "111.1.1111111.1.111",
        "0001.1.......1.1000",
        "1111.1.11G11.1.1111",
        "0000...1HHH1...0000",
        "1111.1.11111.1.1111",
        "0001.1.......1.1000",
        "1111.1.11111.1.1111",
        "1........1........1",
        "1.111111.1.111111.1",
        "1o..11.......11..o1",
        "111.11.11111.11.111",
        "1......1...1......1",
        "1.1111.1.1.1.1111.1",
        "1.................1",
        "1111111111111111111"
    )

    const val PACMAN_SPAWN_ROW = 16
    const val PACMAN_SPAWN_COL = 9

    const val GHOST_HOUSE_ROW = 10
    const val GHOST_HOUSE_COL = 9
}
