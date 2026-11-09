package codes.tashif.paisa.ui.sms

data class SmsScanProgress(
    val isRunning: Boolean = false,
    val total: Int = 0,
    val processed: Int = 0,
    val saved: Int = 0,
    val duplicates: Int = 0,
    val unrecognized: Int = 0
)
