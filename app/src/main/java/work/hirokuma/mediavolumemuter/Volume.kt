package work.hirokuma.mediavolumemuter

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import androidx.core.content.edit


class Volume {
    companion object {
        private const val PREFS_NAME = "MediaVolume"
        private const val KEY_NAME = "volume"
        private const val DEFAULT_VOLUME = 10

        fun saveVolume(context: Context): Int {
            val audioManager =
                context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val sharedPreferences: SharedPreferences =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedVolume = sharedPreferences.getInt(KEY_NAME, 5)
            sharedPreferences.edit {
                putInt(KEY_NAME, volume)
            }

            return savedVolume
        }

        fun setVolume(context: Context, silent: Boolean): Int {
            val sharedPreferences: SharedPreferences =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedVolume = sharedPreferences.getInt(KEY_NAME, DEFAULT_VOLUME)
            val volume = if (silent) 0 else savedVolume
            val audioManager =
                context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)

            return volume
        }
    }
}