package sh.taf.billi.notifications

/**
 * The type of the holding account a notification app's payments wait on until
 * the user says which real account paid. One per app, named after it.
 */
const val UNASSIGNED_ACCOUNT_TYPE = "Unassigned"

/**
 * Payments that [packageName] reports go to [accountId]. With [last4] the link
 * covers only the account ending in those digits ("Google Pay ••1234"); without
 * it, it's the app's default for everything else.
 */
data class AccountLink(
    val packageName: String,
    val last4: String?,
    val accountId: Int
)
