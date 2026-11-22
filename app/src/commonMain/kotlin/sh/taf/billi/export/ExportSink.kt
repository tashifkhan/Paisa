package sh.taf.billi.export

/** Where an export goes: a document the user picked on Android, a download on the web. */
interface ExportSink {
    suspend fun write(bytes: ByteArray)

    /** Finishes the file. Safe to call more than once. */
    fun close()
}
