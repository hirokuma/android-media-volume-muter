package work.hirokuma.mediavolumemuter

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// アイテムのデータクラス
data class LogItem(val id: Int, val timestamp: String, val message: String)

/**
 * ログデータを管理するシングルトンのリポジトリ
 * アプリケーション全体で唯一のデータソース (Single Source of Truth)
 */
object LogRepository {
    private val _logItems = MutableStateFlow<List<LogItem>>(emptyList())
    val logItems: StateFlow<List<LogItem>> = _logItems

    private var currentItemCount = 0

    @Synchronized // 複数のスレッドから同時にアクセスされる可能性に備える
    fun addLog(logMessage: String) {
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val newItem = LogItem(id = currentItemCount++, timestamp = currentTime, message = logMessage)
        _logItems.value = _logItems.value + newItem
        Log.d("LogRepository", "Log added: ${newItem.id}")
    }

    @Synchronized
    fun clearLogs() {
        _logItems.value = emptyList()
        currentItemCount = 0 // カウンターもリセット
        Log.d("LogRepository", "Logs cleared.")
    }
}