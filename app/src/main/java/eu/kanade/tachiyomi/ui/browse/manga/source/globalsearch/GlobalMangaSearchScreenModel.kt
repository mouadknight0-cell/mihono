package eu.kanade.tachiyomi.ui.browse.manga.source.globalsearch

class GlobalMangaSearchScreenModel(
    initialQuery: String = "",
    initialExtensionFilter: String? = null,
) : MangaSearchScreenModel(
    State(searchQuery = initialQuery),
) {

    init {
        extensionFilter = initialExtensionFilter
        if (initialQuery.isNotBlank() || !initialExtensionFilter.isNullOrBlank()) {
            search()
        }
    }
}
