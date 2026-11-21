package codes.tashif.paisa.ingest

import codes.tashif.paisa.ai.local.LocalAiEngine
import codes.tashif.paisa.ai.local.LocalAiState
import codes.tashif.paisa.ai.local.LocalAiStatus
import codes.tashif.paisa.ai.provider.LlmRequest
import codes.tashif.paisa.ai.store.AiConfigStore
import codes.tashif.paisa.ai.store.AiKeyValueStore
import codes.tashif.paisa.ai.store.MessageReaderMode
import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.data.repository.AccountRepository
import codes.tashif.paisa.data.repository.CategoryRepository
import codes.tashif.paisa.data.repository.DebtRepository
import codes.tashif.paisa.data.repository.SmsInboxRepository
import codes.tashif.paisa.data.repository.TransactionRepository
import codes.tashif.paisa.notifications.AccountApproval
import codes.tashif.paisa.notifications.AccountLink
import codes.tashif.paisa.notifications.InstalledNotificationApp
import codes.tashif.paisa.notifications.NotificationImportService
import codes.tashif.paisa.notifications.NotificationImportState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking

internal class MapStore : AiKeyValueStore {
    private val values = mutableMapOf<String, String>()
    override fun getString(key: String): String? = values[key]
    override fun contains(key: String): Boolean = key in values
    override fun strings(): Map<String, String> = values.toMap()
    override fun commit(changes: Map<String, String?>): Boolean {
        changes.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
        return true
    }
}

/** A model that always gives [reply], or throws when [fails] is set. */
internal class FakeEngine(
    var reply: String = """{"isPayment":false}""",
    state: LocalAiState = LocalAiState.AVAILABLE,
    var fails: Boolean = false,
) : LocalAiEngine {
    private val _status = MutableStateFlow(LocalAiStatus(state = state, modelName = "test"))
    override val status: StateFlow<LocalAiStatus> = _status
    var calls = 0

    fun setState(state: LocalAiState) {
        _status.value = _status.value.copy(state = state)
    }

    override suspend fun refresh(): LocalAiStatus = _status.value
    override suspend fun download(): LocalAiStatus = _status.value
    override suspend fun complete(request: LlmRequest): String {
        calls++
        if (fails) error("Gemini Nano only runs while Paisa is open")
        return reply
    }
}

internal fun aiConfig(localParser: Boolean, mode: MessageReaderMode = MessageReaderMode.ON_DEVICE): AiConfigStore =
    AiConfigStore(MapStore()).also { store ->
        if (localParser) runBlocking { store.update { it.copy(messageReader = mode) } }
    }

/** A cloud model that answers [reply], fails when [fails] is set, or isn't set up when [ready] is false. */
internal class FakeCloud(
    var reply: String = """{"isPayment":false}""",
    var ready: Boolean = true,
    var fails: Boolean = false,
) : MessageCloud {
    var calls = 0
    override suspend fun canRun(): Boolean = ready
    override suspend fun complete(request: LlmRequest): String {
        calls++
        if (fails) error("No network")
        return reply
    }
}

/** Account links in memory; the rest of the notification settings do nothing. */
internal class FakeLinks : NotificationImportService {
    val links = mutableListOf<AccountLink>()
    override val state: StateFlow<NotificationImportState> = MutableStateFlow(NotificationImportState())
    override fun refreshAccess() = Unit
    override fun setEnabled(enabled: Boolean) = Unit
    override fun setAppEnabled(packageName: String, enabled: Boolean) = Unit
    override fun setAllAppsEnabled(enabled: Boolean) = Unit
    override fun openAccessSettings() = Unit
    override suspend fun installedApps(): List<InstalledNotificationApp> = emptyList()
    override fun addApp(packageName: String, name: String, isBank: Boolean) = Unit
    override fun removeApp(packageName: String) = Unit
    override fun linkedAccount(packageName: String, last4: String?): Int? =
        links.firstOrNull { it.packageName == packageName && it.last4 == last4 }?.accountId
    override fun setLink(packageName: String, last4: String?, accountId: Int?) {
        links.removeAll { it.packageName == packageName && it.last4 == last4 }
        if (accountId != null) links += AccountLink(packageName, last4, accountId)
    }
}

/** The ingest graph on an in-memory database. */
internal class IngestFixture(
    val db: AppDatabase,
    engine: FakeEngine? = null,
    localParser: Boolean = true,
    mode: MessageReaderMode = MessageReaderMode.ON_DEVICE,
    val cloud: FakeCloud? = null,
) {
    val accounts = AccountRepository(db)
    val transactions = TransactionRepository(db, accounts, DebtRepository(db))
    val categories = CategoryRepository(db)
    val inbox = SmsInboxRepository(db)
    val links = FakeLinks()
    val writer = TransactionWriter(accounts, categories, transactions, links)
    val approval = AccountApproval(transactions, accounts, links)
    val reader = engine?.let { AiMessageReader(it, aiConfig(localParser, mode), cloud) }
    val ingestor = TransactionIngestor(writer, inbox, reader)
}
