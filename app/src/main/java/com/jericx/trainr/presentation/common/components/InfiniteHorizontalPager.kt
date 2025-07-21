package com.jericx.trainr.presentation.common.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> InfiniteHorizontalPager(
    modifier: Modifier = Modifier,
    items: List<T>,
    initialIndex: Int = 0,
    onPageChanged: (T) -> Unit = {},
    pageContent: @Composable (T) -> Unit
) {
    if (items.isEmpty()) return

    val startPage = remember(initialIndex, items.size) {
        val center = Int.MAX_VALUE / 2
        center - (center % items.size) + initialIndex
    }

    val pagerState = rememberPagerState(
        initialPage = if (items.size > 1) startPage else 0,
        pageCount = { if (items.size > 1) Int.MAX_VALUE else 1 }
    )

    LaunchedEffect(pagerState.currentPage) {
        val actualIndex = Math.floorMod(pagerState.currentPage, items.size)
        onPageChanged(items[actualIndex])
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        beyondViewportPageCount = 2,
        contentPadding = PaddingValues(0.dp),
        pageSpacing = 0.dp
    ) { page ->
        val actualIndex = Math.floorMod(page, items.size)
        pageContent(items[actualIndex])
    }
}