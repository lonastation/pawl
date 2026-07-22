package com.linn.pawl.ui.navigation

object AppRoutes {
    const val HOME = "home/{tab}"
    const val SETTINGS = "settings"
    const val TRASH = "trash"
    const val SCAN_LOGS = "scan_logs"
    const val VIDEO_DETAIL = "video_detail/{mediaId}"
    const val IMAGE_DETAIL = "image_detail/{mediaId}"

    const val TAB_VIDEO = "video"
    const val TAB_IMAGE = "image"

    fun home(tab: String = TAB_VIDEO) = "home/$tab"
    fun videoDetail(mediaId: Long) = "video_detail/$mediaId"
    fun imageDetail(mediaId: Long) = "image_detail/$mediaId"

    fun tabFromAppTab(tab: AppTab): String = when (tab) {
        AppTab.Video -> TAB_VIDEO
        AppTab.Image -> TAB_IMAGE
    }

    fun appTabFromRoute(tab: String?): AppTab = when (tab) {
        TAB_IMAGE -> AppTab.Image
        else -> AppTab.Video
    }
}
