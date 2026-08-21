package com.stockmaster.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stockmaster.app.data.BackupBundle
import com.stockmaster.app.data.BackupManager
import com.stockmaster.app.data.InventoryItem
import com.stockmaster.app.data.Repository
import com.stockmaster.app.data.SizeBreakdown
import com.stockmaster.app.data.TransactionRecord
import com.stockmaster.app.data.TxType
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime
import java.util.UUID

data class TxDraft(
    val itemId: String,
    val itemName: String,
    val sku: String,
    val type: TxType,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double,
    val reason: String,
    val location: String,
    val imageUrl: String = "",
    val size: String? = null,
    val sizeBreakdown: List<SizeBreakdown> = emptyList()
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(app)

    private val _items = MutableStateFlow<List<InventoryItem>>(emptyList())
    val items: StateFlow<List<InventoryItem>> = _items.asStateFlow()

    private val _transactions = MutableStateFlow<List<TransactionRecord>>(emptyList())
    val transactions: StateFlow<List<TransactionRecord>> = _transactions.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _locations = MutableStateFlow<List<String>>(emptyList())
    val locations: StateFlow<List<String>> = _locations.asStateFlow()

    init {
        // 后台加载，避免启动时主线程做 JSON 解析
        viewModelScope.launch(Dispatchers.IO) {
            val loadedItems = repo.loadItems()
            val loadedTx = repo.loadTransactions()
            val loadedCats = repo.loadCategories()
            val loadedLocs = repo.loadLocations()
            _items.value = loadedItems
            _transactions.value = loadedTx
            _categories.value = loadedCats
            _locations.value = loadedLocs
        }
    }


    private val itemPersistMutex = Mutex()
    private val txPersistMutex = Mutex()
    private val catPersistMutex = Mutex()
    private val locPersistMutex = Mutex()

    private fun persistItems(items: List<InventoryItem>) {
        viewModelScope.launch(Dispatchers.IO) {
            itemPersistMutex.withLock {
                repo.saveItems(items)
            }
        }
    }

    private fun persistTransactions(transactions: List<TransactionRecord>) {
        viewModelScope.launch(Dispatchers.IO) {
            txPersistMutex.withLock {
                repo.saveTransactions(transactions)
            }
        }
    }

    private fun persistCategories(categories: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            catPersistMutex.withLock {
                repo.saveCategories(categories)
            }
        }
    }

    private fun persistLocations(locations: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            locPersistMutex.withLock {
                repo.saveLocations(locations)
            }
        }
    }

    // ---------- 分类 / 库位 ----------

    fun updateCategories(newCats: List<String>) {
        _categories.value = newCats
        persistCategories(newCats)
    }

    fun updateLocations(newLocs: List<String>) {
        _locations.value = newLocs
        persistLocations(newLocs)
    }

    fun addCategory(name: String) {
        val clean = name.trim()
        if (clean.isEmpty() || _categories.value.contains(clean)) return
        val next = _categories.value + clean
        _categories.value = next
        persistCategories(next)
    }

    fun addLocation(name: String) {
        val clean = name.trim()
        if (clean.isEmpty() || _locations.value.contains(clean)) return
        val next = _locations.value + clean
        _locations.value = next
        persistLocations(next)
    }

    fun renameCategory(old: String, new: String) {
        val clean = new.trim()
        if (clean.isEmpty() || clean == old) return
        if (_categories.value.contains(clean)) return
        updateCategories(_categories.value.map { if (it == old) clean else it })
        // 同步更新商品档案
        val next = _items.value.map {
            if (it.category == old) it.copy(category = clean, updatedAt = nowIso()) else it
        }
        if (next != _items.value) {
            _items.value = next
            persistItems(next)
        }
    }

    fun renameLocation(old: String, new: String) {
        val clean = new.trim()
        if (clean.isEmpty() || clean == old) return
        if (_locations.value.contains(clean)) return
        updateLocations(_locations.value.map { if (it == old) clean else it })
        val next = _items.value.map {
            if (it.location == old) it.copy(location = clean, updatedAt = nowIso()) else it
        }
        if (next != _items.value) {
            _items.value = next
            persistItems(next)
        }
    }

    // ---------- 商品 ----------

    /** 新增商品档案；SKU/条码与其他商品重复时返回 false。 */
    fun addItem(newItem: InventoryItem): Boolean {
        val items = _items.value
        val dup = items.any {
            it.sku.equals(newItem.sku, ignoreCase = true) ||
                (newItem.barcode.isNotBlank() && it.barcode.isNotBlank() &&
                    it.barcode.equals(newItem.barcode, ignoreCase = true))
        }
        if (dup) return false

        val next = listOf(newItem) + _items.value
        _items.value = next
        persistItems(next)

        // 初始库存 > 0 时记录一笔初始入库流水（不再次调整库存，避免翻倍）
        if (newItem.stock > 0) {
            val initialBreakdown = if (newItem.hasSizes && newItem.sizeVariants.isNotEmpty()) {
                newItem.sizeVariants.map { SizeBreakdown(it.size, it.stock) }
            } else emptyList()

            recordTransaction(
                TxDraft(
                    itemId = newItem.id,
                    itemName = newItem.name,
                    sku = newItem.sku,
                    type = TxType.IN,
                    quantity = newItem.stock,
                    unitPrice = newItem.unitCost,
                    totalPrice = newItem.stock * newItem.unitCost,
                    reason = "初始建档录入",
                    location = newItem.location,
                    imageUrl = newItem.imageUrl,
                    sizeBreakdown = initialBreakdown
                ),
                adjustStock = false
            )
        }
        return true
    }

    /** 更新商品档案；与其他商品 SKU/条码冲突时返回 false。 */
    fun updateItem(updated: InventoryItem): Boolean {
        val others = _items.value.filter { it.id != updated.id }
        val dup = others.any {
            it.sku.equals(updated.sku, ignoreCase = true) ||
                (updated.barcode.isNotBlank() && it.barcode.isNotBlank() &&
                    it.barcode.equals(updated.barcode, ignoreCase = true))
        }
        if (dup) return false

        val next = _items.value.map { if (it.id == updated.id) updated else it }
        _items.value = next
        persistItems(next)
        return true
    }

    fun deleteItem(itemId: String) {
        val next = _items.value.filter { it.id != itemId }
        _items.value = next
        persistItems(next)
    }

    fun importItems(imported: List<InventoryItem>) {
        if (imported.isEmpty()) return
        val current = _items.value.toMutableList()
        val currentSkus = current.associateBy { it.sku.lowercase() }.toMutableMap()
        val currentBarcodes = current.filter { it.barcode.isNotBlank() }.associateBy { it.barcode.lowercase() }.toMutableMap()

        imported.forEach { item ->
            val existing = currentSkus[item.sku.lowercase()] ?: if (item.barcode.isNotBlank()) currentBarcodes[item.barcode.lowercase()] else null
            if (existing != null) {
                val index = current.indexOfFirst { it.id == existing.id }
                if (index != -1) {
                    current[index] = existing.copy(
                        name = item.name.ifBlank { existing.name },
                        category = item.category.ifBlank { existing.category },
                        stock = item.stock,
                        minStock = item.minStock,
                        unitCost = if (item.unitCost > 0) item.unitCost else existing.unitCost,
                        unitPrice = if (item.unitPrice > 0) item.unitPrice else existing.unitPrice,
                        location = item.location.ifBlank { existing.location },
                        unit = item.unit.ifBlank { existing.unit },
                        description = item.description.ifBlank { existing.description },
                        updatedAt = nowIso()
                    )
                }
            } else {
                current.add(0, item)
                currentSkus[item.sku.lowercase()] = item
                if (item.barcode.isNotBlank()) currentBarcodes[item.barcode.lowercase()] = item
            }
        }
        _items.value = current
        persistItems(current)

        // 自动补充新发现的分类与库位
        val newCats = (imported.map { it.category } + _categories.value).distinct().filter { it.isNotBlank() }
        val newLocs = (imported.map { it.location } + _locations.value).distinct().filter { it.isNotBlank() }
        updateCategories(newCats)
        updateLocations(newLocs)
    }

    fun importTransactions(imported: List<TransactionRecord>) {
        if (imported.isEmpty()) return
        val existingIds = _transactions.value.map { it.id }.toSet()
        val newOnes = imported.filter { it.id !in existingIds }
        val next = newOnes + _transactions.value
        _transactions.value = next
        persistTransactions(next)
    }

    fun restoreFromBackupJson(jsonString: String, onResult: (Result<BackupBundle>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val bundle = BackupManager.parseBackupBundle(jsonString)
            if (bundle == null) {
                withContext(Dispatchers.Main) {
                    onResult(Result.failure(IllegalArgumentException("无法解析该备份文件，格式可能不正确")))
                }
                return@launch
            }

            _items.value = bundle.items
            _transactions.value = bundle.transactions
            _categories.value = bundle.categories
            _locations.value = bundle.locations

            persistItems(bundle.items)
            persistTransactions(bundle.transactions)
            persistCategories(bundle.categories)
            persistLocations(bundle.locations)

            withContext(Dispatchers.Main) {
                onResult(Result.success(bundle))
            }
        }
    }

    fun clearAll() {
        _items.value = emptyList()
        _transactions.value = emptyList()
        persistItems(emptyList())
        persistTransactions(emptyList())
    }

    // ---------- 出入库流水 ----------

    fun recordTransaction(draft: TxDraft, adjustStock: Boolean = true): Boolean {
        // 出库前校验：库存不足直接拒绝，避免静默截断
        if (adjustStock && draft.type == TxType.OUT) {
            val item = _items.value.firstOrNull { it.id == draft.itemId } ?: return false
            val insufficient = when {
                item.hasSizes && item.sizeVariants.isNotEmpty() -> {
                    if (draft.sizeBreakdown.isNotEmpty()) {
                        draft.sizeBreakdown.any { b ->
                            val cur = item.sizeVariants.firstOrNull { it.size == b.size }?.stock ?: 0
                            cur < b.quantity
                        }
                    } else if (draft.size != null) {
                        val cur = item.sizeVariants.firstOrNull { it.size == draft.size }?.stock ?: 0
                        cur < draft.quantity
                    } else false
                }
                else -> item.stock < draft.quantity
            }
            if (insufficient) return false
        }

        val now = LocalDateTime.now()
        val randomSuffix = UUID.randomUUID().toString().take(6)
        val newTx = TransactionRecord(
            id = "tx-${System.currentTimeMillis()}-$randomSuffix",
            itemId = draft.itemId,
            itemName = draft.itemName,
            sku = draft.sku,
            type = draft.type,
            quantity = draft.quantity,
            unitPrice = draft.unitPrice,
            totalPrice = draft.totalPrice,
            reason = draft.reason,
            location = draft.location,
            size = draft.size,
            sizeBreakdown = draft.sizeBreakdown,
            timestamp = now.toString(),
            formattedTime = null,
            imageUrl = draft.imageUrl
        )

        // 1. 更新库存（初始建档流水不需要，库存已按录入值入账）
        if (adjustStock) {
            val updatedItems = _items.value.map { item ->
                if (item.id == draft.itemId) {
                    val stockDelta = if (draft.type == TxType.IN) draft.quantity else -draft.quantity

                    var updatedVariants = item.sizeVariants
                    if (item.hasSizes && item.sizeVariants.isNotEmpty()) {
                        updatedVariants = if (draft.sizeBreakdown.isNotEmpty()) {
                            val map = draft.sizeBreakdown.associate { it.size to it.quantity }
                            item.sizeVariants.map { v ->
                                val qty = map[v.size] ?: 0
                                val delta = if (draft.type == TxType.IN) qty else -qty
                                v.copy(stock = maxOf(0, v.stock + delta))
                            }
                        } else if (draft.size != null) {
                            item.sizeVariants.map { v ->
                                if (v.size == draft.size) {
                                    val delta = if (draft.type == TxType.IN) draft.quantity else -draft.quantity
                                    v.copy(stock = maxOf(0, v.stock + delta))
                                } else v
                            }
                        } else item.sizeVariants
                    }

                    val finalStock = if (item.hasSizes && updatedVariants.isNotEmpty()) {
                        updatedVariants.sumOf { it.stock }
                    } else {
                        maxOf(0, item.stock + stockDelta)
                    }

                    item.copy(
                        stock = finalStock,
                        sizeVariants = updatedVariants,
                        location = draft.location.ifEmpty { item.location },
                        updatedAt = nowIso()
                    )
                } else item
            }
            _items.value = updatedItems
            persistItems(updatedItems)
        }

        // 2. 前置流水
        val nextTx = listOf(newTx) + _transactions.value
        _transactions.value = nextTx
        persistTransactions(nextTx)
        return true
    }

    private fun nowIso() = LocalDateTime.now().toString()
}