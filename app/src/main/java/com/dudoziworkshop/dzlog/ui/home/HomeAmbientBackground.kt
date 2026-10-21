package com.dudoziworkshop.dzlog.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.dudoziworkshop.dzlog.R

@Composable
internal fun HomeAmbientBackground(modifier: Modifier = Modifier) {
    HomeAmbientContent(painter = painterResource(R.drawable.home_leaf_shadow_c), modifier = modifier)
}
