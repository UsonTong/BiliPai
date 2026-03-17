package com.android.purebilibili.feature.dynamic

internal fun shouldApplyUserDynamicsResult(
    selectedUid: Long?,
    requestUid: Long,
    activeRequestToken: Long,
    requestToken: Long
): Boolean {
    return selectedUid == requestUid && activeRequestToken == requestToken
}

internal fun shouldReloadSelectedUserDynamics(
    previousUid: Long?,
    nextUid: Long,
    currentItems: List<com.android.purebilibili.data.model.response.DynamicItem>,
    userError: String?
): Boolean {
    return nextUid != previousUid || currentItems.isEmpty() || !userError.isNullOrBlank()
}
