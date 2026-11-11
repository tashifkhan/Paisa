package codes.tashif.paisa.data.repository

import codes.tashif.paisa.data.AppDatabase
import codes.tashif.paisa.data.Category
import codes.tashif.paisa.data.MerchantMapping
import codes.tashif.paisa.sms.CategoryMapping
import kotlinx.coroutines.flow.Flow

/** Categories and the merchant to category rules. */
class CategoryRepository(db: AppDatabase) {

    private val categoryDao = db.categoryDao()
    private val transactionDao = db.transactionDao()
    private val merchantMappingDao = db.merchantMappingDao()

    // --- CATEGORIES ---
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    fun getCategoriesByType(type: String): Flow<List<Category>> =
        categoryDao.getCategoriesByType(type)

    suspend fun getCategoryById(id: Int): Category? = categoryDao.getCategoryById(id)

    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)

    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)

    suspend fun deleteCategory(category: Category): Boolean {
        val count = transactionDao.getTransactionCountForCategory(category.id)
        if (count > 0) return false
        categoryDao.deleteCategory(category)
        return true
    }

    /**
     * Resolve a category by name+type, creating a lightweight default if missing.
     * Used by SMS import when merchant mapping produces a category not yet seeded.
     */
    suspend fun findOrCreateCategory(name: String, type: String): Int {
        val canonical = canonicalizeCategoryName(name, type)
        categoryDao.getCategoryByName(canonical, type)?.let { return it.id }
        // Fallbacks for legacy / alias names
        val aliases = buildList {
            add(canonical)
            add(name)
            CATEGORY_ALIASES[name.lowercase()]?.let { add(it) }
            if (type == "expense") {
                add("Others")
                add("Other")
            } else {
                add("Income")
                add("Other")
            }
        }.distinct()
        for (alias in aliases) {
            categoryDao.getCategoryByName(alias, type)?.let { return it.id }
        }
        val id = categoryDao.insertCategory(
            Category(
                name = canonical,
                type = type,
                icon = "more_horiz",
                color = if (type == "income") "#BFFCC6" else "#E8AEB2",
                isDefault = false,
                orderIndex = 99
            )
        )
        return id.toInt()
    }

    /** Map historical / PennyWise names onto seeded Paisa categories. */
    fun canonicalizeCategoryName(name: String, type: String): String {
        if (type == "income") return name
        return CATEGORY_ALIASES[name.lowercase()] ?: name
    }

    // --- MERCHANT MAPPINGS ---
    val allMerchantMappings: Flow<List<MerchantMapping>> = merchantMappingDao.getAllMappings()

    suspend fun getCategoryForMerchant(merchantName: String): MerchantMapping? =
        merchantMappingDao.getMappingForMerchant(merchantName.trim())

    suspend fun saveMerchantMapping(
        merchantName: String,
        categoryName: String,
        categoryType: String = "expense",
        applyToPast: Boolean = true
    ) {
        val merchant = merchantName.trim()
        if (merchant.isEmpty()) return
        val now = nowIso()
        val existing = merchantMappingDao.getMappingForMerchant(merchant)
        merchantMappingDao.upsert(
            MerchantMapping(
                merchantName = existing?.merchantName ?: merchant,
                categoryName = categoryName,
                categoryType = categoryType,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )
        if (applyToPast) {
            val categoryId = findOrCreateCategory(categoryName, categoryType)
            merchantMappingDao.updateTransactionsForMerchant(merchant, categoryId, now)
        }
    }

    suspend fun deleteMerchantMapping(merchantName: String) {
        merchantMappingDao.deleteByMerchant(merchantName)
    }

    /**
     * Resolve category name for a merchant: user mapping first, then keywords.
     */
    suspend fun resolveCategoryName(merchantName: String, transactionType: String): String {
        val type = if (transactionType.equals("income", ignoreCase = true)) "income" else "expense"
        val mapping = getCategoryForMerchant(merchantName)
        if (mapping != null && mapping.categoryType == type) {
            return canonicalizeCategoryName(mapping.categoryName, type)
        }
        return canonicalizeCategoryName(
            CategoryMapping.determineCategory(merchantName, type.uppercase()),
            type
        )
    }

    private companion object {
        val CATEGORY_ALIASES = mapOf(
            "transportation" to "Transport",
            "transport" to "Transport",
            "healthcare" to "Health & Fitness",
            "health" to "Health & Fitness",
            "fitness" to "Health & Fitness",
            "health & fitness" to "Health & Fitness",
            "mobile" to "Bills & Utilities",
            "food" to "Food & Dining",
            "dining" to "Food & Dining",
            "food & dining" to "Food & Dining",
            "grocery" to "Groceries",
            "utilities" to "Bills & Utilities",
            "bills" to "Bills & Utilities",
            "other" to "Others",
            "uncategorized" to "Others"
        )
    }
}
