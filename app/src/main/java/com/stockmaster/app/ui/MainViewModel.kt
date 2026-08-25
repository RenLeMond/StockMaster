package com.stockmaster.app.ui

import android.app.Application
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stockmaster.app.data.BackupBundle
import com.stockmaster.app.data.BackupImageEntry
import com.stockmaster.app.data.BackupManager
import com.stockmaster.app.data.InventoryItem
import com.stockmaster.app.data.PRESET_CATEGORIES
import com.stockmaster.app.data.PRESET_LOCATIONS
import com.stockmaster.app.data.Repository
import com.stockmaster.app.data.SizeBreakdown
import com.stockmaster.app.data.StockMath
import com.stockmaster.app.data.TransactionRecord
import com.stockmaster.app.data.TxType
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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

/**
 * 全局唯一 ViewModel。
 *
 * 线程模型约定：
 * - 所有公开状态写入收敛到主线程（Compose 调用点天然主线程；restore 在 IO 解析后切回主线程赋值）；
 * - 持久化采用「脏标记 + 锁内消费」合并策略：标记置位先于入队，写者在互斥锁内消费最新内存快照，
 *   连续变更自动归并为一次落盘；排队写者读到已消费的标记即空转退出，杜绝 lost update；
 * - 启动异步加载完成后与内存态做并集合并，加载窗口内的用户/扫码枪写入不会丢失。
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private companion object {
        const val TAG_VM = "MainViewModel"
    }

    private val repo = Repository(app)

    private val _items = MutableStateFlow<List<InventoryItem>>(emptyList())
    val items: StateFlow<List<InventoryItem>> = _items.asStateFlow()

    private val _transactions = MutableStateFlow<List<TransactionRecord>>(emptyList())
    val transactions: StateFlow<List<TransactionRecord>> = _transactions.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _locations = MutableStateFlow<List<String>>(emptyList())
    val locations: StateFlow<List<String>> = _locations.asStateFlow()

    /** 最近一次 recordTransaction 被拒的原因，供 UI 展示具体提示。 */
    var txRejectMessage: String? = null
        private set

    // ---------- 合并式持久化（脏标记 + 单写者） ----------

    private val itemsDirty = AtomicBoolean(false)
    private val txDirty = AtomicBoolean(false)
    private val catsDirty = AtomicBoolean(false)
    private val locsDirty = AtomicBoolean(false)

    private val itemPersistMutex = Mutex()
    private val txPersistMutex = Mutex()
    private val catPersistMutex = Mutex()
    private val locPersistMutex = Mutex()

    private fun requestPersistItems() {
        itemsDirty.set(true)
        viewModelScope.launch(Dispatchers.IO) {
            itemPersistMutex.withLock {
                // 脏标记消费必须在锁内：set(true) 先于入队发生，
                // 后续写者必然看到标记，杜绝「tryLock 失败即放弃」的丢更新窗口；
                // 排队的重复写者读到 false 直接空转退出，连续变更仍归并为一次落盘
                if (itemsDirty.getAndSet(false)) {
                    repo.saveItems(_items.value)
                }
            }
        }
    }

    private fun requestPersistTransactions() {
        txDirty.set(true)
        viewModelScope.launch(Dispatchers.IO) {
            txPersistMutex.withLock {
                if (txDirty.getAndSet(false)) {
                    repo.saveTransactions(_transactions.value)
                }
            }
        }
    }

    private fun requestPersistCategories() {
        catsDirty.set(true)
        viewModelScope.launch(Dispatchers.IO) {
            catPersistMutex.withLock {
                if (catsDirty.getAndSet(false)) {
                    repo.saveCategories(_categories.value)
                }
            }
        }
    }

    private fun requestPersistLocations() {
        locsDirty.set(true)
        viewModelScope.launch(Dispatchers.IO) {
            locPersistMutex.withLock {
                if (locsDirty.getAndSet(false)) {
                    repo.saveLocations(_locations.value)
                }
            }
        }
    }

    init {
        // 后台加载，避免启动时主线程做 JSON 解析
        viewModelScope.launch(Dispatchers.IO) {
            val loadedItems = repo.loadItems()
            val loadedTx = repo.loadTransactions()
            val loadedCats = repo.loadCategories()
            val loadedLocs = repo.loadLocations()
            withContext(Dispatchers.Main.immediate) {
                // 并集合并而非整体覆盖：加载期间用户可能已通过扫码枪/界面写入内存态
                _items.value = mergeBy(loadedItems, _items.value) { it.id }
                _transactions.value = mergeBy(loadedTx, _transactions.value) { it.id }
                _categories.value = (loadedCats + _categories.value).distinct()
                _locations.value = (loadedLocs + _locations.value).distinct()
            }
        }
    }

    /** 磁盘数据 + 内存新增记录的并集（按 key 去重），新增项排前与列表风格一致。 */
    private fun <T> mergeBy(loaded: List<T>, memory: List<T>, keyOf: (T) -> String): List<T> {
        if (memory.isEmpty()) return loaded
        val keys = loaded.mapTo(HashSet()) { keyOf(it) }
        val extras = memory.filter { keyOf(it) !in keys }
        return extras + loaded
    }

    // ---------- 分类 / 库位 ----------

    fun updateCategories(newCats: List<String>) {
        _categories.value = newCats
        requestPersistCategories()
    }

    fun updateLocations(newLocs: List<String>) {
        _locations.value = newLocs
        requestPersistLocations()
    }

    fun addCategory(name: String) {
        val clean = name.trim()
        if (clean.isEmpty() || _categories.value.contains(clean)) return
        val next = _categories.value + clean
        _categories.value = next
        requestPersistCategories()
    }

    fun addLocation(name: String) {
        val clean = name.trim()
        if (clean.isEmpty() || _locations.value.contains(clean)) return
        val next = _locations.value + clean
        _locations.value = next
        requestPersistLocations()
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
            requestPersistItems()
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
            requestPersistItems()
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

        val next = listOf(newItem) + items
        _items.value = next
        requestPersistItems()

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
        requestPersistItems()
        return true
    }

    fun deleteItem(itemId: String) {
        val target = _items.value.firstOrNull { it.id == itemId }
        val next = _items.value.filter { it.id != itemId }
        _items.value = next
        requestPersistItems()
        // 联动清理私有图片目录中的孤儿图片
        deleteImageFile(target?.imageUrl)
    }

    fun importItems(imported: List<InventoryItem>) {
        if (imported.isEmpty()) return
        val current = _items.value.toMutableList()
        // 空 SKU 不参与去重（避免 "" 碰撞覆盖），仅以有效 SKU 建索引
        val currentSkus = current.filter { it.sku.isNotBlank() }.associateBy { it.sku.lowercase() }.toMutableMap()
        val currentBarcodes = current.filter { it.barcode.isNotBlank() }.associateBy { it.barcode.lowercase() }.toMutableMap()

        imported.forEach { item ->
            val existing = if (item.sku.isNotBlank()) currentSkus[item.sku.lowercase()] else null
                ?: if (item.barcode.isNotBlank()) currentBarcodes[item.barcode.lowercase()] else null
            if (existing != null) {
                val index = current.indexOfFirst { it.id == existing.id }
                if (index != -1) {
                    // 导入语义：CSV 为准覆盖库存；条码修正也纳入合并，避免老商品永远扫不上新码
                    current[index] = existing.copy(
                        name = item.name.ifBlank { existing.name },
                        barcode = item.barcode.ifBlank { existing.barcode },
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
                if (item.sku.isNotBlank()) currentSkus[item.sku.lowercase()] = item
                if (item.barcode.isNotBlank()) currentBarcodes[item.barcode.lowercase()] = item
            }
        }
        _items.value = current
        requestPersistItems()

        // 自动补充新发现的分类与库位
        val newCats = (imported.map { it.category } + _categories.value).distinct().filter { it.isNotBlank() }
        val newLocs = (imported.map { it.location } + _locations.value).distinct().filter { it.isNotBlank() }
        updateCategories(newCats)
        updateLocations(newLocs)
    }

    fun importTransactions(imported: List<TransactionRecord>) {
        if (imported.isEmpty()) return
        val existingIds = _transactions.value.map { it.id }.toSet()
        // 按 SKU 回填 itemId，修复导入流水与商品档案关联断裂的问题
        val skuToId = _items.value.associate { it.sku.lowercase() to it.id }
        val newOnes = imported
            .filter { it.id !in existingIds }
            .map { if (it.itemId.isBlank()) it.copy(itemId = skuToId[it.sku.lowercase()] ?: "") else it }
        val next = newOnes + _transactions.value
        _transactions.value = next
        requestPersistTransactions()
    }

    fun restoreFromBackupJson(jsonString: String, onResult: (Result<BackupBundle>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var errorText: String? = null
            val bundle = BackupManager.parseBackupBundle(jsonString) { errorText = it }
            if (bundle == null) {
                withContext(Dispatchers.Main) {
                    onResult(Result.failure(IllegalArgumentException(errorText ?: "无法解析该备份文件")))
                }
                return@launch
            }

            // 物化备份内嵌图片到当前设备私有目录并重映射 file:// 路径（磁盘 IO，留在 IO 线程）
            val restoredItems = materializeEmbeddedImages(
                list = bundle.items,
                embedded = bundle.images,
                urlOf = { it.imageUrl },
                copy = { it, u -> it.copy(imageUrl = u) }
            )
            val restoredTxs = materializeEmbeddedImages(
                list = bundle.transactions,
                embedded = bundle.images,
                urlOf = { it.imageUrl },
                copy = { it, u -> it.copy(imageUrl = u) }
            )

            withContext(Dispatchers.Main.immediate) {
                // 状态写回收敛主线程：消除与主线程读改写的跨线程竞态
                _items.value = restoredItems
                _transactions.value = restoredTxs
                _categories.value = bundle.categories
                _locations.value = bundle.locations
            }
            requestPersistItems()
            requestPersistTransactions()
            requestPersistCategories()
            requestPersistLocations()

            withContext(Dispatchers.Main) {
                // 返回物化后的副本而非原始 bundle：items/transactions 的 file:// 路径
                // 已重映射到本机，调用方拿到即与当前内存状态一致
                onResult(Result.success(bundle.copy(items = restoredItems, transactions = restoredTxs)))
            }
        }
    }

    fun clearAll() {
        _items.value = emptyList()
        _transactions.value = emptyList()
        // 分类/库位是字典数据：重置为预设而非清空。
        // 若持久化 []，loadCategories 读到空数组（非缺失）不会回种预设，下拉框将永久为空
        updateCategories(PRESET_CATEGORIES.toList())
        updateLocations(PRESET_LOCATIONS.toList())
        requestPersistItems()
        requestPersistTransactions()
        // 清空业务数据后清理孤儿图片，避免私有目录无限膨胀
        viewModelScope.launch(Dispatchers.IO) {
            repo.imagesDir().deleteRecursively()
        }
    }

    // ---------- 出入库流水 ----------

    fun recordTransaction(draft: TxDraft, adjustStock: Boolean = true): Boolean {
        txRejectMessage = null
        val snapshot = _items.value
        val item = snapshot.firstOrNull { it.id == draft.itemId } ?: run {
            txRejectMessage = "商品不存在或已被删除"
            return false
        }

        if (adjustStock) {
            val error = when (draft.type) {
                TxType.OUT -> StockMath.checkOut(item, draft.quantity, draft.size, draft.sizeBreakdown)
                TxType.IN -> StockMath.checkIn(item, draft.quantity, draft.size, draft.sizeBreakdown)
            }
            if (error != null) {
                txRejectMessage = error
                return false
            }
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

        if (adjustStock) {
            val updatedItems = StockMath.applyToItems(
                items = snapshot,
                itemId = draft.itemId,
                isIn = draft.type == TxType.IN,
                quantity = draft.quantity,
                size = draft.size,
                sizeBreakdown = draft.sizeBreakdown,
                newLocation = draft.location,
                nowIso = nowIso()
            )
            _items.value = updatedItems
            requestPersistItems()
        }

        // 前置流水
        val nextTx = listOf(newTx) + _transactions.value
        _transactions.value = nextTx
        requestPersistTransactions()
        return true
    }

    private fun deleteImageFile(url: String?) {
        if (url.isNullOrBlank() || !url.startsWith("file://")) return
        val file = File(url.removePrefix("file://"))
        val dir = repo.imagesDir()
        // 只清理私有 images 目录内的文件，防止误删
        if (file.absolutePath.startsWith(dir.absolutePath)) {
            viewModelScope.launch(Dispatchers.IO) { file.delete() }
        }
    }

    /**
     * 恢复备份时物化内嵌图片：把备份包中的 Base64 图片写回当前设备私有目录，
     * 并将条目上的 file:// 绝对路径重映射为当前设备的真实路径。
     *
     * - 同机恢复：同名文件已存在则直接复用，不重复写入
     * - 跨设备/重装恢复：文件缺失时从内嵌载荷还原，URL 指向新路径
     * - 旧格式备份（无 images 键）：整体保持原样返回（同机恢复时绝对路径仍有效，
     *   跨设备本就无图可救，交由图片加载失败的占位兜底）
     * - 有载荷但对应条目缺失/解码写盘失败：清空该条目图片引用，避免留下永远加载失败的脏路径
     *
     * 涉及磁盘 IO，须在 IO 线程调用。
     */
    private fun <T> materializeEmbeddedImages(
        list: List<T>,
        embedded: List<BackupImageEntry>,
        urlOf: (T) -> String,
        copy: (T, String) -> T
    ): List<T> {
        // 旧格式备份：保持原样（同机恢复时绝对路径仍有效，跨设备本就无图可救）
        if (embedded.isEmpty()) return list
        val dir = repo.imagesDir()
        if (!dir.exists()) dir.mkdirs()
        val byName = embedded.associateBy { it.name }
        var changed = false
        val out = list.map { entry ->
            val url = urlOf(entry)
            if (!url.startsWith("file://")) return@map entry
            val name = File(url.removePrefix("file://")).name
            // 空文件名会解析出目录本身（File(dir, "") == dir），必须按无效引用处理
            if (name.isBlank()) {
                changed = true
                return@map copy(entry, "")
            }
            val target = File(dir, name)
            if (!target.exists()) {
                byName[name]?.let { e ->
                    runCatching {
                        val bytes = Base64.decode(e.b64, Base64.NO_WRAP)
                        if (bytes.isNotEmpty()) target.writeBytes(bytes)
                    }.onFailure {
                        Log.w(TAG_VM, "恢复备份物化图片失败: $name", it)
                    }
                }
            }
            if (target.exists()) {
                val newUrl = "file://${target.absolutePath}"
                if (newUrl != url) changed = true
                copy(entry, newUrl)
            } else {
                changed = true
                copy(entry, "")
            }
        }
        return if (changed) out else list
    }

    private fun nowIso() = LocalDateTime.now().toString()
}
