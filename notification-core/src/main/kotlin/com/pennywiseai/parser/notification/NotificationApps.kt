package com.pennywiseai.parser.notification

/** What kind of app posted a notification, which decides how it is read and deduplicated. */
enum class NotificationAppKind {
    /** A bank or card app. Its text goes through the bank's SMS parser first. */
    BANK,

    /** A UPI or payment app (Google Pay, PhonePe...). Read by [PaymentNotificationParser]. */
    PAYMENT
}

/**
 * An app whose notifications Paisa can read.
 *
 * [parserSender] is the sender id handed to `BankParserFactory`, so the bank's own
 * SMS parser reads the notification. Null means no parser claims it, and only
 * [PaymentNotificationParser] is tried.
 *
 * [custom] marks an app the user added from the phone's app list. Its package
 * and display name come from the system, not from the built-in lists.
 */
data class NotificationApp(
    val packageName: String,
    val name: String,
    val kind: NotificationAppKind,
    val parserSender: String? = null,
    val custom: Boolean = false
)

object NotificationApps {

    /**
     * Bank apps. The first block is PennyWise's list (BankNotificationConfig): those
     * banks send notifications worded like their SMS, and each alias matches its
     * parser's canHandle(). The Indian banks below reuse their SMS sender id; when
     * the parser can't read a notification, the payment-app reader tries it.
     */
    val banks: List<NotificationApp> = listOf(
        NotificationApp("com.avanza.ambitwizfbl", "Faysal Bank", NotificationAppKind.BANK, "FaysalBank"),
        NotificationApp("finansbank.enpara", "Enpara", NotificationAppKind.BANK, "Enpara"),
        NotificationApp("com.enparabank.retail", "Enpara Bank", NotificationAppKind.BANK, "Enpara"),
        NotificationApp("indwin.c3.shareapp", "slice", NotificationAppKind.BANK, "slice"),
        NotificationApp("com.chase.intl", "Chase UK", NotificationAppKind.BANK, "ChaseUK"),
        NotificationApp("com.avuscapital.trading212", "Trading 212", NotificationAppKind.BANK, "Trading212"),
        NotificationApp("com.huntington.m", "Huntington", NotificationAppKind.BANK, "Huntington"),
        NotificationApp("com.bankfab.pbg.ae.dubaifirst", "Dubai First", NotificationAppKind.BANK, "DubaiFirst"),

        NotificationApp("com.snapwork.hdfc", "HDFC Bank", NotificationAppKind.BANK, "AD-HDFCBK-S"),
        NotificationApp("com.sbi.lotusintouch", "SBI YONO", NotificationAppKind.BANK, "AD-SBIINB-S"),
        NotificationApp("com.csam.icici.bank.imobile", "ICICI iMobile", NotificationAppKind.BANK, "AD-ICICIB-S"),
        NotificationApp("com.axis.mobile", "Axis Mobile", NotificationAppKind.BANK, "AD-AXISBK-S"),
        NotificationApp("com.msf.kbank.mobile", "Kotak Bank", NotificationAppKind.BANK, "AD-KOTAKB-S"),
        NotificationApp("com.canarabank.mobility", "Canara ai1", NotificationAppKind.BANK, "AD-CANBNK-S")
    )

    /** UPI and payment apps, on by default once notification reading is switched on. */
    val payments: List<NotificationApp> = listOf(
        NotificationApp("com.google.android.apps.nbu.paisa.user", "Google Pay", NotificationAppKind.PAYMENT),
        NotificationApp("com.phonepe.app", "PhonePe", NotificationAppKind.PAYMENT),
        NotificationApp("net.one97.paytm", "Paytm", NotificationAppKind.PAYMENT),
        NotificationApp("in.org.npci.upiapp", "BHIM", NotificationAppKind.PAYMENT),
        NotificationApp("com.dreamplug.androidapp", "CRED", NotificationAppKind.PAYMENT),
        NotificationApp("com.fampay.in", "FamApp", NotificationAppKind.PAYMENT),
        NotificationApp("com.mobikwik_new", "MobiKwik", NotificationAppKind.PAYMENT)
    )

    private val custom = LinkedHashMap<String, NotificationApp>()

    private val builtInByPackage: Map<String, NotificationApp> =
        (banks + payments).associateBy { it.packageName.lowercase() }

    val all: List<NotificationApp>
        get() = synchronized(this) { banks + payments + custom.values.toList() }

    /** Built-ins plus the apps the user added, in a safe snapshot. */
    val customApps: List<NotificationApp>
        get() = synchronized(this) { custom.values.toList() }

    fun find(packageName: String?): NotificationApp? {
        val key = packageName?.lowercase() ?: return null
        return builtInByPackage[key] ?: synchronized(this) { custom[key] }
    }

    /**
     * Adds a user-picked app. A built-in package can't be replaced, and adding
     * the same package twice keeps the newest name and kind in place.
     */
    fun addCustom(app: NotificationApp): Boolean {
        val packageName = app.packageName.trim()
        val key = packageName.lowercase()
        if (key.isEmpty() || builtInByPackage.containsKey(key)) return false
        synchronized(this) {
            custom[key] = app.copy(packageName = packageName, custom = true)
        }
        return true
    }

    fun removeCustom(packageName: String): Boolean =
        synchronized(this) { custom.remove(packageName.lowercase()) != null }
}
