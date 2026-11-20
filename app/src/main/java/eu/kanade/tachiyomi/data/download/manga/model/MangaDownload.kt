package eu.kanade.tachiyomi.data.download.manga.model

import android.content.SharedPreferences

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import tachiyomi.domain.entries.manga.interactor.GetManga
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.items.chapter.interactor.GetChapter
import tachiyomi.domain.items.chapter.model.Chapter
import tachiyomi.domain.source.manga.service.MangaSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data class MangaDownload(
    val source: HttpSource,
    val manga: Manga,
    val chapter: Chapter,
) {

    /**
     * File backing per-chapter status (QUEUE/DOWNLOADING/ERROR) so the queue can report it and be
     * restored after an app restart.
     */
    private val _statusFlow = MutableStateFlow(State.NOT_DOWNLOADED)
    val statusFlow = _statusFlow.asStateFlow()

    /**
     * Sets the initial status from [MangaDownloadStore] when restoring after a restart. Restored
     * state must not be persisted again.
     */
    fun restoreStatus(status: State) {
        _statusFlow.value = status
    }

    var status: State
        get() = _statusFlow.value
        set(status) {
            _statusFlow.value = status
            try {
                val editor = Injekt.get<SharedPreferences>().edit()
                // Only persist states that must survive a restart. DOWNLOADED chapters are removed
                // from the queue by the downloader and must not be re-queued, and NOT_DOWNLOADED
                // means the chapter left the queue, so clear any stale persisted entry instead.
                when (status) {
                    State.QUEUE, State.DOWNLOADING, State.ERROR ->
                        editor.putInt(KEY_PREFIX_PENDING + chapter.id, status.value)
                    State.DOWNLOADED, State.NOT_DOWNLOADED ->
                        editor.remove(KEY_PREFIX_PENDING + chapter.id)
                }
                editor.apply()
            } catch (e: Exception) {
                // Never crash the downloader for persistence failures.
            }
        }

    var pages: List<Page>? = null

    val totalProgress: Int
        get() = pages?.sumOf(Page::progress) ?: 0

    val downloadedImages: Int
        get() = pages?.count { it.status == Page.State.READY } ?: 0

    @Transient
    val progressFlow = flow {
        if (pages == null) {
            emit(0)
            while (pages == null) {
                delay(50)
            }
        }

        val progressFlows = pages!!.map(Page::progressFlow)
        emitAll(combine(progressFlows) { it.average().toInt() })
    }
        .distinctUntilChanged()
        .debounce(50)

    val progress: Int
        get() {
            val pages = pages ?: return 0
            return pages.map(Page::progress).average().toInt()
        }

    enum class State(val value: Int) {
        NOT_DOWNLOADED(0),
        QUEUE(1),
        DOWNLOADING(2),
        DOWNLOADED(3),
        ERROR(4),
    }

    companion object {
        const val KEY_PREFIX_PENDING = "dl_pending_"
        private const val NO_PERSISTED_STATUS = -1

        /**
         * Reads the persisted status for [chapterId], or null if none.
         */
        fun persistedStatus(chapterId: Long): State? {
            val raw = try {
                Injekt.get<SharedPreferences>()
                    .getInt(KEY_PREFIX_PENDING + chapterId, NO_PERSISTED_STATUS)
            } catch (e: Exception) {
                NO_PERSISTED_STATUS
            }
            if (raw == NO_PERSISTED_STATUS) return null
            return State.entries.firstOrNull { it.value == raw }
        }

        suspend fun fromChapterId(
            chapterId: Long,
            getChapter: GetChapter = Injekt.get(),
            getManga: GetManga = Injekt.get(),
            sourceManager: MangaSourceManager = Injekt.get(),
        ): MangaDownload? {
            val chapter = getChapter.await(chapterId) ?: return null
            val manga = getManga.await(chapter.mangaId) ?: return null
            val source = sourceManager.get(manga.source) as? HttpSource ?: return null

            return MangaDownload(source, manga, chapter)
        }
    }
}
