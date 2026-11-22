package sh.taf.billi.ui.analytics

import sh.taf.billi.platform.BilliCalendar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import sh.taf.billi.core.AppNavigator
import sh.taf.billi.core.PrivacySession
import sh.taf.billi.data.Settings
import sh.taf.billi.data.TransactionWithDetails
import sh.taf.billi.data.repository.SettingsRepository
import sh.taf.billi.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * State for the Analytics tab. Scoped to MainActivity, so the period and the
 * open drill-down survive opening a transaction and coming back.
 */
class AnalyticsViewModel(
    transactionRepository: TransactionRepository,
    settingsRepository: SettingsRepository,
    privacySession: PrivacySession,
    private val navigator: AppNavigator
) : ViewModel() {

    val transactions: StateFlow<List<TransactionWithDetails>> = transactionRepository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<Settings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val balancesHidden: StateFlow<Boolean> = privacySession.balancesHidden

    private val _analyticsPeriod = MutableStateFlow(AnalyticsPeriod.MONTH)
    val analyticsPeriod: StateFlow<AnalyticsPeriod> = _analyticsPeriod.asStateFlow()
    private val _analyticsOffset = MutableStateFlow(0)
    val analyticsOffset: StateFlow<Int> = _analyticsOffset.asStateFlow()
    private val _analyticsGroup = MutableStateFlow<AnalyticsGroup?>(null)
    val analyticsGroup: StateFlow<AnalyticsGroup?> = _analyticsGroup.asStateFlow()

    // Custom range, in local epoch millis. End is the last minute included.
    private val _analyticsCustom = MutableStateFlow(defaultCustomRange())
    val analyticsCustom: StateFlow<Pair<Long, Long>> = _analyticsCustom.asStateFlow()

    fun setAnalyticsPeriod(period: AnalyticsPeriod) {
        if (_analyticsPeriod.value != period) {
            _analyticsPeriod.value = period
            _analyticsOffset.value = 0
        }
    }

    /** Step back (+1) or forward (-1). A custom range moves by its own length. */
    fun shiftAnalytics(by: Int) {
        if (_analyticsPeriod.value == AnalyticsPeriod.CUSTOM) {
            val (start, end) = _analyticsCustom.value
            val span = end + 60_000L - start
            _analyticsCustom.value = (start - by * span) to (end - by * span)
        } else {
            _analyticsOffset.value = (_analyticsOffset.value + by).coerceAtLeast(0)
        }
    }

    fun setAnalyticsCustomRange(start: Long, end: Long) {
        _analyticsCustom.value = if (end >= start) start to end else end to start
        _analyticsOffset.value = 0
    }

    // "All time" on a drill-down page. Kept here so it survives opening a transaction.
    private val _groupAllTime = MutableStateFlow(false)
    val groupAllTime: StateFlow<Boolean> = _groupAllTime.asStateFlow()

    fun setGroupAllTime(allTime: Boolean) {
        _groupAllTime.value = allTime
    }

    fun openAnalyticsGroup(group: AnalyticsGroup?) {
        if (group != _analyticsGroup.value) _groupAllTime.value = false
        _analyticsGroup.value = group
    }

    fun openTransaction(id: Int) = navigator.openTransaction(id)

    /** The last 7 whole days, ending today. */
    private fun defaultCustomRange(): Pair<Long, Long> {
        val end = BilliCalendar.getInstance().apply {
            set(BilliCalendar.HOUR_OF_DAY, 23)
            set(BilliCalendar.MINUTE, 59)
            set(BilliCalendar.SECOND, 0)
            set(BilliCalendar.MILLISECOND, 0)
        }
        val start = (end.clone()).apply {
            add(BilliCalendar.DAY_OF_MONTH, -6)
            set(BilliCalendar.HOUR_OF_DAY, 0)
            set(BilliCalendar.MINUTE, 0)
        }
        return start.timeInMillis to end.timeInMillis
    }
}
