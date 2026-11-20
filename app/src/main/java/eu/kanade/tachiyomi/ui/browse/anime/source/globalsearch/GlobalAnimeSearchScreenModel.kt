package eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch

class GlobalAnimeSearchScreenModel(
    initialQuery: String = "",
    initialExtensionFilter: String? = null,
) : AnimeSearchScreenModel(
    State(
        searchQuery = initialQuery,
        sourceFilter = AnimeSourceFilter.All,
    ),
) {

    init {
        extensionFilter = initialExtensionFilter
        if (initialQuery.isNotBlank() || !initialExtensionFilter.isNullOrBlank()) {
            search()
        }
    }
}
