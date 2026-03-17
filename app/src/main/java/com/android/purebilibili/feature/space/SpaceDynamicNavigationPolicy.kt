package com.android.purebilibili.feature.space

import com.android.purebilibili.data.model.response.SpaceDynamicItem
import com.android.purebilibili.feature.dynamic.components.DynamicCardPrimaryAction
import com.android.purebilibili.feature.dynamic.components.resolveDynamicCardPrimaryAction

internal sealed interface SpaceDynamicClickAction {
    data class OpenVideo(val bvid: String) : SpaceDynamicClickAction
    data class OpenDynamicDetail(val dynamicId: String) : SpaceDynamicClickAction
    data object None : SpaceDynamicClickAction
}

internal fun resolveSpaceDynamicClickAction(dynamic: SpaceDynamicItem): SpaceDynamicClickAction {
    return when (val action = resolveDynamicCardPrimaryAction(resolveSpaceDynamicCardItem(dynamic))) {
        is DynamicCardPrimaryAction.OpenVideo -> SpaceDynamicClickAction.OpenVideo(action.bvid)
        is DynamicCardPrimaryAction.OpenDynamicDetail -> SpaceDynamicClickAction.OpenDynamicDetail(action.dynamicId)
        else -> SpaceDynamicClickAction.None
    }
}
