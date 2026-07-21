package com.example.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

class PacSoundManager {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var ambientJob: Job? = null
    
    var isCosmicMode: Boolean = true
        set(value) {
            field = value
            if (!value) {
                stopAmbientMusic()
            } else if (!isMuted) {
                startAmbientMusic()
            }
        }

    var isMuted: Boolean = false
        set(value) {
            field = value
            if (value) {
                stopAmbientMusic()
            } else if (isCosmicMode) {
                startAmbientMusic()
            }
        }

    private fun generateTone(frequency: Double, durationMs: Int, type: String = "square"): ByteArray {
        val sampleRate = 8000
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val generatedSnd = ByteArray(2 * numSamples)
        
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val sampleVal = when (type) {
                "square" -> {
                    val cycleVal = sin(2.0 * Math.PI * frequency * t)
                    if (cycleVal >= 0) 32767 else -32768
                }
                "celestial" -> {
                    // Multi-harmonic synthesis: fundamental + octave + octave-fifth + perfect fifth
                    val f = frequency
                    val wave = sin(2.0 * Math.PI * f * t) * 0.55 +
                               sin(2.0 * Math.PI * (f * 2.0) * t) * 0.25 +
                               sin(2.0 * Math.PI * (f * 3.0) * t) * 0.12 +
                               sin(2.0 * Math.PI * (f * 1.5) * t) * 0.08
                    
                    // Crystalline exponential decay envelope
                    val totalDurationSec = durationMs / 1000.0
                    val decay = Math.exp(-4.5 * (t / totalDurationSec))
                    
                    (wave * decay * 32767).toInt()
                }
                else -> { // "sine"
                    (sin(2.0 * Math.PI * frequency * t) * 32767).toInt()
                }
            }
            
            // 16-bit PCM (little endian)
            generatedSnd[2 * i] = (sampleVal and 0x00FF).toByte()
            generatedSnd[2 * i + 1] = ((sampleVal and 0xFF00) shr 8).toByte()
        }
        return generatedSnd
    }

    private fun playRawTones(tones: List<Pair<Double, Int>>, type: String = "square") {
        if (isMuted) return
        scope.launch {
            try {
                // Calculate total bytes
                val sampleRate = 8000
                var totalBytes = 0
                val buffers = tones.map { (freq, duration) ->
                    val buf = generateTone(freq, duration, type)
                    totalBytes += buf.size
                    buf
                }

                val finalBuffer = ByteArray(totalBytes)
                var currentPos = 0
                for (buf in buffers) {
                    System.arraycopy(buf, 0, finalBuffer, currentPos, buf.size)
                    currentPos += buf.size
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(finalBuffer.size)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(finalBuffer, 0, finalBuffer.size)
                audioTrack.play()
                
                // Allow static playback to finish, then release
                val totalDuration = tones.sumOf { it.second }
                delay(totalDuration.toLong() + 50)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                Log.e("PacSoundManager", "Error playing tone", e)
            }
        }
    }

    fun startAmbientMusic() {
        if (isMuted || !isCosmicMode) return
        if (ambientJob?.isActive == true) return
        
        ambientJob = scope.launch {
            // Evolving slow space progression: Cmaj9 -> Am9 -> Fmaj9 -> G13
            val ambientMelody = listOf(
                listOf(261.63, 329.63, 392.00, 493.88, 587.33), // Cmaj9 chord (C4, E4, G4, B4, D5)
                listOf(220.00, 261.63, 329.63, 392.00, 440.00), // Am9 chord (A3, C4, E4, G4, A4)
                listOf(174.61, 220.00, 261.63, 329.63, 392.00), // Fmaj9 chord (F3, A3, C4, E4, G4)
                listOf(196.00, 246.94, 293.66, 349.23, 440.00)  // G13 chord (G3, B3, D4, F4, A4)
            )
            
            while (true) {
                if (isMuted || !isCosmicMode) {
                    delay(1000)
                    continue
                }
                
                for (chord in ambientMelody) {
                    if (isMuted || !isCosmicMode) break
                    
                    val durationMs = 4000
                    val sampleRate = 8000
                    val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                    val finalBuffer = ByteArray(2 * numSamples)
                    
                    for (i in 0 until numSamples) {
                        val t = i.toDouble() / sampleRate
                        var waveSum = 0.0
                        
                        // Mix all frequencies in the chord
                        for (freq in chord) {
                            // Subtly modulate frequencies over time for a shimmering space chorus effect!
                            val modulation = 1.0 + 0.004 * sin(2.0 * Math.PI * 0.4 * t)
                            val f = freq * modulation
                            
                            waveSum += sin(2.0 * Math.PI * f * t) * 0.45 +
                                       sin(2.0 * Math.PI * (f * 2.0) * t) * 0.18 +
                                       sin(2.0 * Math.PI * (f * 3.0) * t) * 0.08
                        }
                        
                        // Slowly swell in and out (Swell attack and deep space release)
                        val progress = t / (durationMs / 1000.0)
                        val envelope = if (progress < 0.25) {
                            progress / 0.25 // Smooth Attack Swell
                        } else {
                            // Exponential Release
                            Math.exp(-2.2 * (progress - 0.25))
                        }
                        
                        // Scale and prevent clipping
                        val sampleVal = (waveSum / chord.size * envelope * 20000).toInt()
                        
                        finalBuffer[2 * i] = (sampleVal and 0x00FF).toByte()
                        finalBuffer[2 * i + 1] = ((sampleVal and 0xFF00) shr 8).toByte()
                    }
                    
                    try {
                        val audioTrack = AudioTrack.Builder()
                            .setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_GAME)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build()
                            )
                            .setAudioFormat(
                                AudioFormat.Builder()
                                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                    .setSampleRate(sampleRate)
                                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                    .build()
                            )
                            .setBufferSizeInBytes(finalBuffer.size)
                            .setTransferMode(AudioTrack.MODE_STATIC)
                            .build()
                        audioTrack.write(finalBuffer, 0, finalBuffer.size)
                        audioTrack.play()
                        
                        // Wait for chord to evolve, plus a little cushion before next chord
                        delay(durationMs.toLong() + 100)
                        audioTrack.stop()
                        audioTrack.release()
                    } catch (e: Exception) {
                        Log.e("PacSoundManager", "Error playing ambient chord", e)
                        delay(1000)
                    }
                }
            }
        }
    }
    
    fun stopAmbientMusic() {
        ambientJob?.cancel()
        ambientJob = null
    }

    fun playPellet(isAlt: Boolean) {
        val freq = if (isAlt) 330.0 else 260.0
        val waveType = if (isCosmicMode) "celestial" else "square"
        val duration = if (isCosmicMode) 60 else 40
        playRawTones(listOf(Pair(freq, duration)), waveType)
    }

    fun playPowerPellet() {
        val waveType = if (isCosmicMode) "celestial" else "sine"
        val tones = if (isCosmicMode) {
            listOf(
                Pair(523.25, 70), // C5
                Pair(659.25, 70), // E5
                Pair(783.99, 70), // G5
                Pair(1046.50, 100) // C6
            )
        } else {
            listOf(
                Pair(440.0, 60),
                Pair(660.0, 60),
                Pair(440.0, 60),
                Pair(660.0, 60)
            )
        }
        playRawTones(tones, waveType)
    }

    fun playGhostEat() {
        val waveType = if (isCosmicMode) "celestial" else "sine"
        val tones = if (isCosmicMode) {
            listOf(
                Pair(523.25, 100),
                Pair(659.25, 100),
                Pair(783.99, 100),
                Pair(987.77, 100),
                Pair(1318.51, 200)
            )
        } else {
            listOf(
                Pair(261.63, 80),
                Pair(329.63, 80),
                Pair(392.00, 80),
                Pair(523.25, 120)
            )
        }
        playRawTones(tones, waveType)
    }

    fun playDeath() {
        stopAmbientMusic()
        val waveType = if (isCosmicMode) "celestial" else "square"
        val tones = mutableListOf<Pair<Double, Int>>()
        if (isCosmicMode) {
            var freq = 880.0
            while (freq > 110.0) {
                tones.add(Pair(freq, 45))
                freq -= 45.0
            }
        } else {
            var freq = 600.0
            while (freq > 80.0) {
                tones.add(Pair(freq, 35))
                freq -= 30.0
            }
        }
        playRawTones(tones, waveType)
    }

    fun playStartDitty() {
        stopAmbientMusic()
        val waveType = if (isCosmicMode) "celestial" else "sine"
        val tones = if (isCosmicMode) {
            listOf(
                Pair(523.25, 150), // C5
                Pair(587.33, 150), // D5
                Pair(659.25, 150), // E5
                Pair(783.99, 250), // G5
                Pair(659.25, 150), // E5
                Pair(783.99, 400), // G5
                
                Pair(880.00, 150), // A5
                Pair(783.99, 150), // G5
                Pair(659.25, 150), // E5
                Pair(587.33, 250), // D5
                Pair(523.25, 400)  // C5
            )
        } else {
            listOf(
                Pair(261.63, 100), // C4
                Pair(523.25, 100), // C5
                Pair(392.00, 100), // G4
                Pair(329.63, 100), // E4
                Pair(523.25, 100), // C5
                Pair(392.00, 100), // G4
                Pair(329.63, 100), // E4
                
                Pair(293.66, 100), // D4
                Pair(587.33, 100), // D5
                Pair(440.00, 100), // A4
                Pair(349.23, 100), // F4
                Pair(587.33, 100), // D5
                Pair(440.00, 100), // A4
                Pair(349.23, 100), // F4
                
                Pair(261.63, 100), // C4
                Pair(523.25, 100), // C5
                Pair(392.00, 100), // G4
                Pair(329.63, 100), // E4
                Pair(523.25, 100), // C5
                Pair(392.00, 100), // G4
                Pair(329.63, 100), // E4
                
                Pair(349.23, 100), // F4
                Pair(392.00, 100), // G4
                Pair(440.00, 100), // A4
                Pair(523.25, 200)  // C5
            )
        }
        playRawTones(tones, waveType)
        
        // After start ditty finishes (about 2.5 seconds), start background ambient loop!
        if (isCosmicMode) {
            scope.launch {
                val delayTime = if (isCosmicMode) 2500L else 3000L
                delay(delayTime)
                startAmbientMusic()
            }
        }
    }

    fun playGameOver() {
        stopAmbientMusic()
        val waveType = if (isCosmicMode) "celestial" else "square"
        val tones = if (isCosmicMode) {
            listOf(
                Pair(329.63, 250),
                Pair(261.63, 250),
                Pair(220.00, 350),
                Pair(164.81, 500),
                Pair(110.00, 800)
            )
        } else {
            listOf(
                Pair(392.00, 150),
                Pair(349.23, 150),
                Pair(293.66, 150),
                Pair(220.00, 250),
                Pair(146.83, 400)
            )
        }
        playRawTones(tones, waveType)
    }
}

