package com.android.purebilibili.feature.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.components.IOSClickableItem
import com.android.purebilibili.core.ui.components.IOSDivider
import com.android.purebilibili.core.ui.components.IOSGroup
import com.android.purebilibili.core.ui.components.IOSSearchBar
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Gearshape

@Composable
internal fun SettingsSearchBarSection(
    query: String,
    onQueryChange: (String) -> Unit
) {
    IOSSearchBar(
        query = query,
        onQueryChange = onQueryChange,
        placeholder = "搜索设置功能",
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
internal fun SettingsSearchResultsSection(
    results: List<SettingsSearchResult>,
    onResultClick: (SettingsSearchTarget) -> Unit
) {
    SettingsCategoryHeader("搜索结果")
    IOSGroup {
        if (results.isEmpty()) {
            IOSClickableItem(
                icon = CupertinoIcons.Outlined.Gearshape,
                title = "未找到匹配项",
                subtitle = "试试其他关键词",
                onClick = null,
                showChevron = false,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            results.forEachIndexed { index, result ->
                val visual = resolveSettingsEntryVisual(result.target)
                IOSClickableItem(
                    icon = visual.icon,
                    iconPainter = visual.iconResId?.let { painterResource(id = it) },
                    title = result.title,
                    subtitle = result.subtitle,
                    value = result.section,
                    onClick = { onResultClick(result.target) },
                    iconTint = visual.iconTint
                )
                if (index != results.lastIndex) {
                    IOSDivider(startIndent = 66.dp)
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}
