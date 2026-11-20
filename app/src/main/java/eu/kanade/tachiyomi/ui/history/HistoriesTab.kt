package eu.kanade.tachiyomi.ui.history

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.domain.ui.model.NavTab
import eu.kanade.presentation.components.TabbedScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.history.manga.MangaHistoryScreenModel
import eu.kanade.tachiyomi.ui.history.manga.mangaHistoryTab
import eu.kanade.tachiyomi.ui.history.manga.resumeLastChapterReadEvent
import eu.kanade.tachiyomi.ui.main.MainActivity
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

data object HistoriesTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_history_enter)
            return TabOptions(
                index = 5u,
                title = stringResource(MR.strings.history),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        resumeLastChapterReadEvent.send(Unit)
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val enabledTabs = currentBottomNavTabs()
        val fromMore = NavTab.History.prefKey !in enabledTabs
        val mangaHistoryScreenModel = rememberScreenModel { MangaHistoryScreenModel() }
        val mangaSearchQuery by mangaHistoryScreenModel.query.collectAsState()

        val tabs = persistentListOf(mangaHistoryTab(context, fromMore))

        TabbedScreen(
            titleRes = MR.strings.label_recent_manga,
            tabs = tabs,
            state = rememberPagerState { tabs.size },
            mangaSearchQuery = mangaSearchQuery,
            onChangeMangaSearchQuery = mangaHistoryScreenModel::search,
            animeSearchQuery = mangaSearchQuery,
            onChangeAnimeSearchQuery = mangaHistoryScreenModel::search,
        )

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
        }
    }
}
