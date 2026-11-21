package codes.tashif.paisa.ingest

/**
 * Retries messages that were kept because the on-device model couldn't run at
 * the time. Android runs this when Paisa comes to the foreground; the web has
 * no local model and does nothing.
 */
interface PendingAiQueue {
    /** Returns how many pending messages were saved as transactions. */
    suspend fun process(): Int
}
