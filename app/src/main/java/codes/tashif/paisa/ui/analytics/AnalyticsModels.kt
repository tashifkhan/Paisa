package codes.tashif.paisa.ui.analytics

/** Analytics covers one month, one whole year, or a range you pick. */
enum class AnalyticsPeriod { MONTH, YEAR, CUSTOM }

/** A drill-down page in Analytics: every transaction in one category, merchant or account. */
data class AnalyticsGroup(val kind: Kind, val key: String) {
    enum class Kind { CATEGORY, MERCHANT, ACCOUNT }
}
