package work.hirokuma.mediavolumemuter

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// アイテムのデータクラス (ViewModel と UI で共有)
//data class LogItem(val timestamp: String, val log: String)

class LogViewModel : ViewModel() {
    val logItems: StateFlow<List<LogItem>> = LogRepository.logItems

    fun clearLogs() {
        LogRepository.clearLogs()
    }

//    fun addLog(message: String) {
//        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
//        val newItem = LogItem(timestamp = currentTime, log = message)
//        _logItems.value = _logItems.value + newItem
//    }
}