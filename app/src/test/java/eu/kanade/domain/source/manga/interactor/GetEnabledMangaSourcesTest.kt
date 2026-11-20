package eu.kanade.domain.source.manga.interactor

import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.source.manga.model.Source
import tachiyomi.domain.source.manga.repository.MangaSourceRepository
import tachiyomi.source.local.entries.manga.LocalMangaSource

/**
 * Regression test: the Manhwa-only cleanup filtered enabled sources by
 * `lang == "manhwa"`, a language tag no real extension source uses, which
 * emptied Browse -> Manga Sources (only the local source survived). Enabled
 * sources must be selected by the user's enabled languages instead.
 */
class GetEnabledMangaSourcesTest {

    private fun source(id: Long, lang: String, name: String): Source {
        return Source(
            id = id,
            lang = lang,
            name = name,
            supportsLatest = true,
            isStub = false,
        )
    }

    private fun interactor(
        store: FakePreferenceStore,
        sources: List<Source>,
    ): GetEnabledMangaSources {
        val repository = object : MangaSourceRepository {
            override fun getMangaSources(): Flow<List<Source>> = flowOf(sources)
            override fun getOnlineMangaSources(): Flow<List<Source>> = flowOf(sources)
            override fun getMangaSourcesWithFavoriteCount() = TODO("Unused in test")
            override fun getMangaSourcesWithNonLibraryManga() = TODO("Unused in test")
            override fun searchManga(sourceId: Long, query: String, filterList: eu.kanade.tachiyomi.source.model.FilterList) = TODO("Unused in test")
            override fun getPopularManga(sourceId: Long) = TODO("Unused in test")
            override fun getLatestManga(sourceId: Long) = TODO("Unused in test")
        }
        return GetEnabledMangaSources(repository, SourcePreferences(store))
    }

    @Test
    fun `sources in enabled languages plus local source are kept`() = runTest {
        val store = FakePreferenceStore()
        store.setStringSet("source_languages", setOf("en", "ar"))
        val sources = listOf(
            source(LocalMangaSource.ID, "", "Local source"),
            source(1L, "en", "Asura Scans"),
            source(2L, "ar", "MangaSwat"),
            source(3L, "ko", "Korean Source"),
        )

        val result = interactor(store, sources).subscribe().first()

        assertEquals(listOf(0L, 1L, 2L), result.map { it.id })
    }

    @Test
    fun `arabic sources are not removed by a manhwa language filter`() = runTest {
        val store = FakePreferenceStore()
        store.setStringSet("source_languages", setOf("ar"))
        val sources = listOf(
            source(LocalMangaSource.ID, "", "Local source"),
            source(2L, "ar", "MangaSwat"),
        )

        val result = interactor(store, sources).subscribe().first()

        assertEquals(listOf(0L, 2L), result.map { it.id })
    }

    @Test
    fun `disabled sources stay hidden even when their language is enabled`() = runTest {
        val store = FakePreferenceStore()
        store.setStringSet("source_languages", setOf("en"))
        store.setStringSet("hidden_catalogues", setOf("1"))
        val sources = listOf(
            source(LocalMangaSource.ID, "", "Local source"),
            source(1L, "en", "Asura Scans"),
        )

        val result = interactor(store, sources).subscribe().first()

        assertEquals(listOf(0L), result.map { it.id })
    }

    @Test
    fun `local source is kept even with no enabled languages`() = runTest {
        val store = FakePreferenceStore()
        store.setStringSet("source_languages", emptySet())
        val sources = listOf(
            source(LocalMangaSource.ID, "", "Local source"),
            source(1L, "en", "Asura Scans"),
        )

        val result = interactor(store, sources).subscribe().first()

        assertEquals(listOf(0L), result.map { it.id })
    }

    /** Minimal [PreferenceStore] backed by state flows, for JVM unit tests. */
    private class FakePreferenceStore : PreferenceStore {

        private val flows = mutableMapOf<String, MutableStateFlow<Any?>>()

        @Suppress("UNCHECKED_CAST")
        private fun <T> flowFor(key: String, defaultValue: T): MutableStateFlow<Any?> {
            return flows.getOrPut(key) { MutableStateFlow(defaultValue) }
        }

        fun setStringSet(key: String, value: Set<String>) {
            flowFor(key, value).value = value
        }

        override fun getString(key: String, defaultValue: String): Preference<String> {
            return FakePreference(key, flowFor(key, defaultValue)) { it as? String ?: defaultValue }
        }

        override fun getLong(key: String, defaultValue: Long): Preference<Long> {
            return FakePreference(key, flowFor(key, defaultValue)) { it as? Long ?: defaultValue }
        }

        override fun getInt(key: String, defaultValue: Int): Preference<Int> {
            return FakePreference(key, flowFor(key, defaultValue)) { it as? Int ?: defaultValue }
        }

        override fun getFloat(key: String, defaultValue: Float): Preference<Float> {
            return FakePreference(key, flowFor(key, defaultValue)) { it as? Float ?: defaultValue }
        }

        override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> {
            return FakePreference(key, flowFor(key, defaultValue)) { it as? Boolean ?: defaultValue }
        }

        override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> {
            return FakePreference(key, flowFor(key, defaultValue)) { value ->
                (value as? Set<*>)?.filterIsInstance<String>()?.toSet() ?: defaultValue
            }
        }

        override fun <T> getObject(
            key: String,
            defaultValue: T,
            serializer: (T) -> String,
            deserializer: (String) -> T,
        ): Preference<T> {
            return FakePreference(key, flowFor(key, defaultValue)) { it as? T ?: defaultValue }
        }

        override fun getAll(): Map<String, *> = emptyMap<String, Any>()
    }

    private class FakePreference<T>(
        private val key: String,
        private val flow: MutableStateFlow<Any?>,
        private val cast: (Any?) -> T,
    ) : Preference<T> {

        override fun key(): String = key

        override fun get(): T = cast(flow.value)

        override fun set(value: T) {
            flow.value = value
        }

        override fun isSet(): Boolean = flow.value != null

        override fun delete() {
            flow.value = null
        }

        override fun defaultValue(): T = cast(null)

        override fun changes(): Flow<T> {
            return kotlinx.coroutines.flow.map(flow) { cast(it) }
        }

        override fun stateIn(scope: CoroutineScope): StateFlow<T> {
            val state = MutableStateFlow(get())
            scope.launch {
                changes().collect { state.value = it }
            }
            return state
        }
    }
}
