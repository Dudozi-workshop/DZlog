package com.dudoziworkshop.dzlog.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExposurePlus1
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.TextFields

/** The same Material Filled symbols identify cell types in every editor. */
object DDZCellTypeIcons {
    val Text get() = Icons.Filled.TextFields
    val Number get() = Icons.Filled.Pin
    val AutoNumber get() = Icons.Filled.ExposurePlus1
    val Date get() = Icons.Filled.DateRange
    val Time get() = Icons.Filled.AccessTime
    val RotatingText get() = Icons.Filled.Autorenew
}
