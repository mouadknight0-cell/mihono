package eu.kanade.tachiyomi.ui.player.settings

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.aniyomi.AYMR

enum class SubtitlesBorderStyle(
    val value: String,
    val titleRes: StringResource,
) {
    OutlineAndShadow("outline-and-shadow", AYMR.strings.player_sheets_subtitles_border_style_outline_and_shadow),
    OpaqueBox("opaque-box", AYMR.strings.player_sheets_subtitles_border_style_opaque_box),
    BackgroundBox("background-box", AYMR.strings.player_sheets_subtitles_border_style_background_box),
}
