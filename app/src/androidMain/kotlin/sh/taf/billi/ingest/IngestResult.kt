package sh.taf.billi.ingest

/** What happened to one SMS or notification. [detail] is for logs only. */
data class IngestResult(
    val outcome: Outcome,
    val transactionId: Long? = null,
    val detail: String? = null,
    /** Saved on the app's holding account: the user still has to say which account paid. */
    val needsAccount: Boolean = false
) {
    enum class Outcome {
        /** A new transaction, [transactionId]. */
        SAVED,

        /** Another channel already saved this charge, and this message filled in its account and balance. */
        MERGED,

        /** Already saved, by this channel or another. */
        DUPLICATE,

        /** The user deleted this charge before. It stays deleted. */
        DELETED,

        /** A balance alert: the account was updated, no transaction. */
        BALANCE_UPDATED,

        /** The rules couldn't read it and the on-device model couldn't run now. Kept for later. */
        QUEUED_FOR_AI,

        /** Looks like a payment nobody could read. Kept in the review list. */
        NEEDS_REVIEW,

        /** Not a payment, or from a sender Billi doesn't read. */
        IGNORED,

        FAILED
    }

    val saved: Boolean get() = outcome == Outcome.SAVED

    /** The database changed, so widgets and balances need a refresh. */
    val changedData: Boolean
        get() = outcome == Outcome.SAVED || outcome == Outcome.MERGED || outcome == Outcome.BALANCE_UPDATED

    companion object {
        fun saved(id: Long) = IngestResult(Outcome.SAVED, transactionId = id)
        fun of(outcome: Outcome, detail: String? = null) = IngestResult(outcome, detail = detail)
    }
}
