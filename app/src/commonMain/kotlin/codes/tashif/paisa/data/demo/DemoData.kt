package codes.tashif.paisa.data.demo

import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.data.sql.Sql
import codes.tashif.paisa.data.sql.sqlTransaction
import codes.tashif.paisa.platform.PaisaCalendar
import codes.tashif.paisa.platform.formatDate
import codes.tashif.paisa.sync.SyncSchema
import kotlin.random.Random

/**
 * Three months of believable sample data: four accounts, about 120
 * transactions, budgets, a recurring rule and two people in Debts.
 *
 * Used for the web demo and for screenshots. The same seed gives the same
 * data on every platform, relative to today's date.
 */
object DemoData {

    /** True if the database has no transactions yet. */
    suspend fun isEmpty(db: AppDatabase): Boolean =
        db.sqlTransaction { it.long("SELECT COUNT(*) FROM transactions") == 0L }

    suspend fun seed(db: AppDatabase) {
        db.sqlTransaction { sql -> seed(sql) }
    }

    private class Tx(
        val daysAgo: Int,
        val hour: Int,
        val minute: Int,
        val amount: Double,
        val type: String,
        val category: String,
        val account: Int,
        val title: String,
        val source: String = "manual"
    )

    private suspend fun seed(sql: Sql) {
        val now = nowIso(0, 10, 0)

        // Show amounts and skip the tour.
        sql.exec(
            "UPDATE settings SET onboardingCompleted = 1, hideBalancesByDefault = 0, " +
                "colorPalette = 'green' WHERE id = 1"
        )

        // The two seeded accounts become a cash wallet and an HDFC savings account.
        sql.exec(
            "UPDATE accounts SET openingBalance = 4200, balanceAnchor = 4200, balanceAnchorAt = '' WHERE id = 1"
        )
        sql.exec(
            "UPDATE accounts SET name = 'HDFC Savings', bankName = 'HDFC Bank', accountLast4 = '4821', " +
                "openingBalance = 86500, balanceAnchor = 86500, balanceAnchorAt = '', isDefault = 1 WHERE id = 2"
        )
        val card = sql.insert(
            "accounts",
            mapOf(
                "name" to "ICICI Amazon Pay", "type" to "Credit Card", "openingBalance" to 0.0,
                "currentBalance" to 0.0, "icon" to "credit_card", "color" to "#FFB3BA",
                "bankName" to "ICICI Bank", "accountLast4" to "7703", "creditLimit" to 150000.0,
                "orderIndex" to 2, "balanceAnchor" to 0.0, "balanceAnchorAt" to ""
            )
        ).toInt()
        val wallet = sql.insert(
            "accounts",
            mapOf(
                "name" to "Paytm Wallet", "type" to "Wallet", "openingBalance" to 1500.0,
                "currentBalance" to 1500.0, "icon" to "account_balance_wallet", "color" to "#BDB2FF",
                "orderIndex" to 3, "balanceAnchor" to 1500.0, "balanceAnchorAt" to ""
            )
        ).toInt()

        val categories = sql.rows("SELECT id, name, type FROM categories")
            .associate { (it.getString(1) to it.getString(2)) to it.getInt(0) }

        val txs = buildList {
            for (month in 0..2) {
                val base = month * 30
                // Income
                add(Tx(base + 1, 9, 5, 118500.0, "income", "Salary", 2, "Salary · Acme Labs", "sms"))
                if (month != 1) add(Tx(base + 12, 18, 40, 15000.0, "income", "Freelance", 2, "Logo project"))
                add(Tx(base + 20, 11, 0, 412.0, "income", "Interest", 2, "Savings interest"))
                // Fixed costs
                add(Tx(base + 2, 10, 15, 28000.0, "expense", "Rent & Housing", 2, "Rent · Indiranagar"))
                add(Tx(base + 5, 21, 2, 649.0, "expense", "Subscriptions", card, "Netflix"))
                add(Tx(base + 6, 8, 30, 199.0, "expense", "Subscriptions", card, "Spotify"))
                add(Tx(base + 8, 12, 10, 1840.0, "expense", "Bills & Utilities", 2, "BESCOM electricity", "sms"))
                add(Tx(base + 9, 12, 45, 999.0, "expense", "Bills & Utilities", 2, "Airtel fibre", "sms"))
                add(Tx(base + 15, 7, 50, 2499.0, "expense", "Health & Fitness", card, "Cult.fit"))
                add(Tx(base + 25, 16, 0, 12000.0, "expense", "Investments", 2, "SIP · Nifty 50 index"))
            }
            // Day-to-day spending, the same every run.
            val rnd = Random(42)
            val daily = listOf(
                Triple("Food & Dining", listOf("Swiggy", "Zomato", "Truffles", "Third Wave Coffee", "Meghana Foods"), 180.0..1400.0),
                Triple("Groceries", listOf("BigBasket", "Zepto", "Blinkit", "Namdhari's"), 240.0..2800.0),
                Triple("Transport", listOf("Uber", "Namma Metro", "Rapido", "Ola"), 45.0..620.0),
                Triple("Shopping", listOf("Amazon", "Myntra", "Decathlon", "IKEA"), 399.0..5200.0),
                Triple("Entertainment", listOf("PVR Cinemas", "BookMyShow", "Steam"), 250.0..1600.0),
                Triple("Fuel", listOf("Indian Oil", "HP Petrol"), 500.0..2200.0),
                Triple("UPI Transfer", listOf("UPI · Ramesh", "UPI · Chai stall"), 20.0..450.0),
                Triple("Personal Care", listOf("Urban Company", "Nykaa"), 299.0..1800.0)
            )
            val weights = listOf(9, 5, 7, 3, 2, 2, 4, 1)
            for (daysAgo in 0 until 90) {
                val count = when (rnd.nextInt(10)) { in 0..2 -> 0; in 3..7 -> 1; else -> 2 }
                repeat(count) {
                    var pick = rnd.nextInt(weights.sum())
                    var index = 0
                    while (pick >= weights[index]) { pick -= weights[index]; index++ }
                    val (category, merchants, range) = daily[index]
                    val amount = (range.start + rnd.nextDouble() * (range.endInclusive - range.start))
                        .let { kotlin.math.round(it) }
                    val account = when {
                        category == "UPI Transfer" -> 2
                        category == "Transport" && rnd.nextBoolean() -> wallet
                        amount < 300 && rnd.nextInt(3) == 0 -> 1
                        rnd.nextInt(3) == 0 -> card
                        else -> 2
                    }
                    add(
                        Tx(
                            daysAgo, 8 + rnd.nextInt(14), rnd.nextInt(60), amount, "expense", category,
                            account, merchants[rnd.nextInt(merchants.size)],
                            if (account == 2 && rnd.nextBoolean()) "sms" else "manual"
                        )
                    )
                }
            }
            // A card bill paid from savings.
            add(Tx(18, 19, 20, 14250.0, "expense", "Credit Card Payment", 2, "ICICI card bill"))
        }

        for (tx in txs) {
            val categoryId = categories[tx.category to tx.type] ?: continue
            val date = nowIso(tx.daysAgo, tx.hour, tx.minute)
            sql.insert(
                "transactions",
                mapOf(
                    "amount" to tx.amount, "type" to tx.type, "categoryId" to categoryId,
                    "accountId" to tx.account, "note" to "", "transactionDate" to date,
                    "createdAt" to date, "updatedAt" to date, "tags" to "",
                    "merchantName" to tx.title, "source" to tx.source, "currency" to "INR",
                    "isDeleted" to 0
                )
            )
        }

        // Budgets for this month.
        val cal = PaisaCalendar.getInstance()
        val month = cal.get(PaisaCalendar.MONTH) + 1
        val year = cal.get(PaisaCalendar.YEAR)
        fun budget(category: String?, amount: Double, name: String?) = mapOf(
            "categoryId" to category?.let { categories[it to "expense"] }, "budgetAmount" to amount,
            "month" to month, "year" to year, "budgetType" to "MONTHLY", "budgetName" to name
        )
        sql.insert("budgets", budget(null, 60000.0, "Monthly spending"))
        sql.insert("budgets", budget("Food & Dining", 8000.0, null))
        sql.insert("budgets", budget("Groceries", 9000.0, null))
        sql.insert("budgets", budget("Shopping", 6000.0, null))
        sql.insert("budgets", budget("Transport", 3500.0, null))

        // A monthly SIP that hasn't run yet this month.
        categories["Investments" to "expense"]?.let { investments ->
            val next = PaisaCalendar.getInstance().apply { add(PaisaCalendar.DAY_OF_MONTH, 6) }
            sql.insert(
                "recurring_transactions",
                mapOf(
                    "amount" to 12000.0, "type" to "expense", "categoryId" to investments,
                    "accountId" to 2, "note" to "SIP · Nifty 50 index", "frequency" to "monthly",
                    "nextExecutionDate" to formatDate(next.timeInMillis, "yyyy-MM-dd", machine = true),
                    "enabled" to 1
                )
            )
        }

        // People: Radhika owes for a split dinner, we owe Omar for concert tickets.
        val radhika = sql.insert("debt_people", mapOf("name" to "Radhika", "createdAt" to now)).toInt()
        val omar = sql.insert("debt_people", mapOf("name" to "Omar", "createdAt" to now)).toInt()
        fun entry(person: Int, amount: Double, note: String, daysAgo: Int) = mapOf(
            "personId" to person, "amount" to amount, "note" to note,
            "entryDate" to nowIso(daysAgo, 20, 0), "createdAt" to nowIso(daysAgo, 20, 0)
        )
        sql.insert("debt_entries", entry(radhika, 1450.0, "Dinner at Truffles", 4))
        sql.insert("debt_entries", entry(radhika, 600.0, "Cab to airport", 11))
        sql.insert("debt_entries", entry(radhika, -600.0, "Paid back via UPI", 2))
        sql.insert("debt_entries", entry(omar, -3200.0, "Concert tickets", 9))

        // Merchant rules a user would have built up.
        for ((merchant, category) in listOf("SWIGGY" to "Food & Dining", "BIGBASKET" to "Groceries", "UBER" to "Transport")) {
            sql.exec(
                "INSERT OR IGNORE INTO merchant_mappings (merchantName, categoryName, categoryType, createdAt, updatedAt) " +
                    "VALUES (?, ?, 'expense', ?, ?)",
                merchant, category, now, now
            )
        }

        SyncSchema.recomputeBalances(sql)
    }

    /** Local ISO timestamp [daysAgo] days back at [hour]:[minute], as transactions store dates. */
    private fun nowIso(daysAgo: Int, hour: Int, minute: Int): String {
        val cal = PaisaCalendar.getInstance()
        cal.add(PaisaCalendar.DAY_OF_MONTH, -daysAgo)
        cal.set(PaisaCalendar.HOUR_OF_DAY, hour)
        cal.set(PaisaCalendar.MINUTE, minute)
        cal.set(PaisaCalendar.SECOND, 0)
        cal.set(PaisaCalendar.MILLISECOND, 0)
        return formatDate(cal.timeInMillis, "yyyy-MM-dd'T'HH:mm:ss", machine = true)
    }
}
