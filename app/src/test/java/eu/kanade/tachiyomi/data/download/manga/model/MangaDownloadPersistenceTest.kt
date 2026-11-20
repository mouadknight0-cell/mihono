package eu.kanade.tachiyomi.data.download.manga.model

import android.content.SharedPreferences
import eu.kanade.tachiyomi.source.online.HttpSource
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.items.chapter.model.Chapter
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.fullType

/**
 * Verifies that [MangaDownload] persists resumable states (QUEUE/DOWNLOADING/ERROR) per chapter
 * into the `pending_downloads` SharedPreferences, that terminal states clear the persisted entry
 * (so finished chapters are never re-queued after a restart), and that [MangaDownload.restoreStatus]
 * only sets the in-memory state without writing back.
 */
class MangaDownloadPersistenceTest {

    private fun download(chapterId: Long): MangaDownload {
        val manga = Manga.create().copy(id = 1L)
        val chapter = Chapter.create().copy(id = chapterId, mangaId = manga.id)
        return MangaDownload(mockk<HttpSource>(relaxed = true), manga, chapter)
    }

    @Test
    fun `queued status is persisted and readable after restart`() {
        val download = download(42L)
        download.status = MangaDownload.State.QUEUE

        assertEquals(MangaDownload.State.QUEUE, MangaDownload.persistedStatus(42L))
    }

    @Test
    fun `downloading and error statuses are persisted for retry after restart`() {
        val download = download(7L)
        download.status = MangaDownload.State.DOWNLOADING
        assertEquals(MangaDownload.State.DOWNLOADING, MangaDownload.persistedStatus(7L))

        download.status = MangaDownload.State.ERROR
        assertEquals(MangaDownload.State.ERROR, MangaDownload.persistedStatus(7L))
    }

    @Test
    fun `downloaded status clears persisted entry so chapter is not requeued`() {
        val download = download(9L)
        download.status = MangaDownload.State.QUEUE
        download.status = MangaDownload.State.DOWNLOADED

        assertNull(MangaDownload.persistedStatus(9L))
    }

    @Test
    fun `removing from queue clears persisted entry`() {
        val download = download(11L)
        download.status = MangaDownload.State.QUEUE
        download.status = MangaDownload.State.NOT_DOWNLOADED

        assertNull(MangaDownload.persistedStatus(11L))
    }

    @Test
    fun `restoreStatus sets in-memory state without persisting`() {
        val download = download(13L)
        download.restoreStatus(MangaDownload.State.ERROR)

        assertEquals(MangaDownload.State.ERROR, download.status)
        assertNull(MangaDownload.persistedStatus(13L))
    }

    @Test
    fun `no persisted status returns null`() {
        assertNull(MangaDownload.persistedStatus(999L))
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun registerSharedPreferences() {
            Injekt.addSingleton(fullType<SharedPreferences>(), InMemorySharedPreferences())
        }
    }
}

/** Minimal in-memory [SharedPreferences] for JVM unit tests. */
private class InMemorySharedPreferences : SharedPreferences {

    private val values = mutableMapOf<String, Any?>()

    override fun getAll(): Map<String, *> = values.toMap()

    @Suppress("UNCHECKED_CAST")
    override fun getString(key: String?, defValue: String?): String? =
        values[key] as? String ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? =
        values[key] as? Set<String> ?: defValues

    override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue

    override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        values[key] as? Boolean ?: defValue

    override fun contains(key: String?): Boolean = values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = Editor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    private inner class Editor : SharedPreferences.Editor {
        private val edits = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        private var clearAll = false

        override fun putString(key: String?, value: String?) = apply { edits[key!!] = value }
        override fun putStringSet(key: String?, vals: Set<String>?) = apply { edits[key!!] = vals }
        override fun putInt(key: String?, value: Int) = apply { edits[key!!] = value }
        override fun putLong(key: String?, value: Long) = apply { edits[key!!] = value }
        override fun putFloat(key: String?, value: Float) = apply { edits[key!!] = value }
        override fun putBoolean(key: String?, value: Boolean) = apply { edits[key!!] = value }
        override fun remove(key: String?) = apply { removals += key!! }
        override fun clear() = apply { clearAll = true }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clearAll) values.clear()
            removals.forEach { values.remove(it) }
            values.putAll(edits)
        }
    }
}
