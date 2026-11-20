package eu.kanade.tachiyomi.util.episode

import eu.kanade.domain.items.episode.model.applyFilters
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.model.Episode

/**
 * Gets next unseen episode with filters and sorting applied
 */
fun List<Episode>.getNextUnseen(anime: Anime): Episode? {
    return applyFilters(anime).let { episodes ->
        if (anime.sortDescending()) {
            episodes.findLast { !it.seen }
        } else {
            episodes.find { !it.seen }
        }
    }
}
