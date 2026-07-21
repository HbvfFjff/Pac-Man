package com.example.data

import kotlinx.coroutines.flow.Flow

class HighScoreRepository(private val highScoreDao: HighScoreDao) {
    val topScores: Flow<List<HighScore>> = highScoreDao.getTopScores()

    suspend fun insert(highScore: HighScore) {
        highScoreDao.insertScore(highScore)
    }

    suspend fun clear() {
        highScoreDao.clearScores()
    }
}
