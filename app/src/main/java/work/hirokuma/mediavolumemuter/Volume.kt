package work.hirokuma.mediavolumemuter

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import androidx.core.content.edit


class Volume {
    companion object {
        private const val PREFS_NAME = "MediaVolume"
        private const val KEY_NAME = "volume"

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
            LogRepository.addLog("save volume=$savedVolume")

            return savedVolume
        }

        // https://developer.android.com/media/platform/output?hl=ja
        fun setSilent(context: Context, silent: Boolean): Int {
            val audioManager =
                context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

            val sharedPreferences: SharedPreferences =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedVolume = sharedPreferences.getInt(KEY_NAME, currentVolume)
            val volume = if (silent) 0 else savedVolume

            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
            LogRepository.addLog("set media volume=$volume")

            return volume
        }
    }
}