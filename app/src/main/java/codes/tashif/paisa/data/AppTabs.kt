package codes.tashif.paisa.data

/** Top-level screens. Any of them can sit in the bottom bar; the rest open from More. */
enum class AppTab(val id: String, val label: String) {
    HOME("home", "Home"),
    ANALYTICS("analytics", "Analytics"),
    BUDGETS("budgets", "Budgets"),
    ACCOUNTS("accounts", "Accounts"),
    DEBTS("debts", "Debts"),
    MORE("more", "More");

    companion object {
        fun fromId(id: String): AppTab? = entries.firstOrNull { it.id == id }
    }
}

/** Which tabs the bottom bar shows, stored as [Settings.navTabs]. */
object NavTabs {
    /** Past five, labels get cramped on a phone. */
    const val MAX = 5

    /** Home is where the app opens and More holds settings, so both always stay. */
    val REQUIRED = setOf(AppTab.HOME, AppTab.MORE)

    /** Tabs to show, in the fixed order of [AppTab], with Home and More always present. */
    fun parse(raw: String?): List<AppTab> {
        val picked = raw.orEmpty().split(',').mapNotNull { AppTab.fromId(it.trim()) }.toSet() + REQUIRED
        val ordered = AppTab.entries.filter { it in picked }
        if (ordered.size <= MAX) return ordered
        // Keep Home and More, and the first optional tabs that fit.
        val optional = ordered.filter { it !in REQUIRED }.take(MAX - REQUIRED.size)
        return AppTab.entries.filter { it in REQUIRED || it in optional }
    }

    fun serialize(tabs: List<AppTab>): String = parse(tabs.joinToString(",") { it.id }).joinToString(",") { it.id }

    /** [current] with [tab] turned on or off. Required tabs can't be removed; a full bar can't take more. */
    fun toggle(current: List<AppTab>, tab: AppTab, on: Boolean): List<AppTab> = when {
        tab in REQUIRED -> current
        on && tab in current -> current
        on && current.size >= MAX -> current
        on -> AppTab.entries.filter { it in current || it == tab }
        else -> current - tab
    }
}
