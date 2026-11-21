package codes.tashif.paisa.ingest

/** No on-device model on the web, so nothing is ever queued there. */
object NoPendingAiQueue : PendingAiQueue {
    override suspend fun process(): Int = 0
}
