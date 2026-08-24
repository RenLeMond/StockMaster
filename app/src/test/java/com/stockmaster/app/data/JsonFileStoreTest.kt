package com.stockmaster.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class JsonFileStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    private fun store(dir: File = tmp.root): JsonFileStore =
        JsonFileStore(dir, json)

    @Serializable
    private data class Rec(val id: String, val qty: Int = 0)

    private fun writeRaw(dir: File, name: String, content: String) {
        File(dir, name).writeText(content)
    }

    @Test
    fun `写入后可读回`() {
        store().writeList("a.json", listOf(Rec("1", 5)), Rec.serializer())
        assertEquals(listOf(Rec("1", 5)), store().readList("a.json", Rec.serializer()))
    }

    @Test
    fun `文件不存在返回 null`() {
        assertNull(store().readList("missing.json", Rec.serializer()))
    }

    @Test
    fun `主文件缺失但 tmp 存在时从 tmp 恢复`() {
        writeRaw(tmp.root, "a.json.tmp", """[{"id":"t","qty":9}]""")
        val loaded = store().readList("a.json", Rec.serializer())
        assertEquals(listOf(Rec("t", 9)), loaded)
        // 恢复后 tmp 提升为主文件
        assertTrue(File(tmp.root, "a.json").exists())
    }

    @Test
    fun `tmp 更新且完好时择优恢复`() {
        writeRaw(tmp.root, "a.json", """[{"id":"old","qty":1}]""")
        val tmpFile = File(tmp.root, "a.json.tmp")
        writeRaw(tmp.root, "a.json.tmp", """[{"id":"new","qty":2}]""")
        if (!tmpFile.setLastModified(System.currentTimeMillis() + 5000)) {
            // 文件系统不支持改 mtime 时跳过该用例的时序前提
            return
        }
        assertEquals(listOf(Rec("new", 2)), store().readList("a.json", Rec.serializer()))
    }

    @Test
    fun `tmp 损坏时只隔离 tmp 不触碰主文件`() {
        val main = File(tmp.root, "a.json")
        writeRaw(tmp.root, "a.json", """[{"id":"good","qty":1}]""")
        writeRaw(tmp.root, "a.json.tmp", "{broken json")
        if (!main.setLastModified(System.currentTimeMillis() - 10000) ||
            !File(tmp.root, "a.json.tmp").setLastModified(System.currentTimeMillis())
        ) {
            return // 无法控制 mtime 时跳过时序前提
        }
        val loaded = store().readList("a.json", Rec.serializer())
        assertEquals(listOf(Rec("good", 1)), loaded)
        assertTrue("主文件必须保留", main.exists())
        // 坏 tmp 被隔离改名（隔离名形如 a.json.tmp.corrupt-<ts>-<uuid>）
        val corrupt = tmp.root.listFiles()!!.filter { it.name.startsWith("a.json") && it.name.contains(".corrupt-") }
        assertEquals(1, corrupt.size)
        assertTrue("原 tmp 路径不再存在", !File(tmp.root, "a.json.tmp").exists())
    }

    @Test
    fun `主文件损坏时隔离留底并返回 null`() {
        writeRaw(tmp.root, "a.json", "not-json-at-all")
        assertNull(store().readList("a.json", Rec.serializer()))
        val quarantined = tmp.root.listFiles()!!.filter { it.name.startsWith("a.json.corrupt-") }
        assertEquals(1, quarantined.size)
        assertEquals("not-json-at-all", quarantined[0].readText())
    }

    @Test
    fun `隔离文件名含 UUID 不撞名`() {
        repeat(3) { idx ->
            writeRaw(tmp.root, "b.json", "bad-$idx")
            assertNull(store().readList("b.json", Rec.serializer()))
        }
        assertEquals(3, tmp.root.listFiles()!!.count { it.name.startsWith("b.json.corrupt-") })
    }

    @Test
    fun `空白主文件视为缺失返回 null`() {
        writeRaw(tmp.root, "a.json", "   ")
        assertNull(store().readList("a.json", Rec.serializer()))
    }

    @Test
    fun `单条记录损坏时跳过该条而非整文件报废`() {
        writeRaw(
            tmp.root, "a.json",
            """[{"id":"ok1","qty":1},{"id":"bad","qty":"不是数字"},{"id":"ok2","qty":2}]"""
        )
        val loaded = store().readList("a.json", Rec.serializer())
        assertEquals(listOf(Rec("ok1", 1), Rec("ok2", 2)), loaded)
    }

    @Test
    fun `字符串列表读写`() {
        store().writeList("s.json", listOf("A", "B"), String.serializer())
        assertEquals(listOf("A", "B"), store().readList("s.json", String.serializer()))
    }
}
