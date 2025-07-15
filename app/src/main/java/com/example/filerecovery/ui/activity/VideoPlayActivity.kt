package com.example.filerecovery.ui.activity

import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.filerecovery.R
import com.example.filerecovery.data.model.FileItem
import com.example.filerecovery.databinding.ActivityVideoPlayBinding
import com.example.filerecovery.utils.DialogUtil
import com.example.filerecovery.utils.Utils.formatDuration
import java.io.File

class VideoPlayActivity : BaseActivity() {

    private lateinit var binding: ActivityVideoPlayBinding
    private var exoPlayer: ExoPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isMediaPlaying = false
    private var isUserSeeking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityVideoPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val fileItem: FileItem? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("fileItem", FileItem::class.java)
        } else {
            intent.getParcelableExtra("fileItem")
        }

        binding.ivArrowBack.setOnClickListener { finish() }

        fileItem?.let { item ->
            setupExoPlayer(item.path)
        }
    }

    private fun setupExoPlayer(filePath: String) {
        exoPlayer = ExoPlayer.Builder(this).build().also { player ->
            val mediaItem = MediaItem.fromUri(Uri.fromFile(File(filePath)))
            player.setMediaItem(mediaItem)
            player.prepare()
            binding.vvPreviewMedia.player = player

            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        val duration = player.duration.toInt()
                        binding.seekBarMedia.max = duration
                        binding.tvTotalDuration.text = formatDuration(duration)
                        binding.tvCurrentDuration.text = "00:00"
                    }
                }
            })

//            binding.btnPlayPause.setOnClickListener {
//                if (player.isPlaying) {
//                    player.pause()
//                    isMediaPlaying = false
//                    binding.btnPlayPause.visibility = View.VISIBLE
//                    binding.btnPlayPause.setImageResource(R.drawable.ic_play)
//                    handler.removeCallbacksAndMessages(null)
//                } else {
//                    player.play()
//                    isMediaPlaying = true
//                    binding.btnPlayPause.visibility = View.INVISIBLE
//                    updateSeekBar()
//                }
//            }

            binding.seekBarMedia.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        binding.tvCurrentDuration.text = formatDuration(progress)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    isUserSeeking = true
                    handler.removeCallbacksAndMessages(null)
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    isUserSeeking = false
                    val seekTo = seekBar?.progress?.toLong() ?: 0L
                    player.seekTo(seekTo)
                    handler.postDelayed({
                        val current = player.currentPosition.toInt()
                        binding.seekBarMedia.progress = current
                        binding.tvCurrentDuration.text = formatDuration(current)
                        if (isMediaPlaying) updateSeekBar()
                    }, 300)
                }
            })
        }
    }

    private fun updateSeekBar() {
        exoPlayer?.let { player ->
            if (isMediaPlaying && !isUserSeeking) {
                val currentPos = player.currentPosition.toInt()
                binding.seekBarMedia.progress = currentPos
                binding.tvCurrentDuration.text = formatDuration(currentPos)
            }
            if (isMediaPlaying) {
                handler.postDelayed({ updateSeekBar() }, 1000)
            }
        }
    }

    private fun formatDuration(milliseconds: Int): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
        isMediaPlaying = false
        binding.btnPlayPause.setImageResource(R.drawable.ic_play)
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
        handler.removeCallbacksAndMessages(null)
    }
}