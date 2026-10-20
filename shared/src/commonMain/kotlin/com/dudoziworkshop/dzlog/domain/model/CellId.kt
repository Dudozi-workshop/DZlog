package com.dudoziworkshop.dzlog.domain.model

/** Platform UUID generation; persisted IDs remain unchanged. */
internal expect fun randomCellId(): String

/** Same UUID policy for editor draft elements on both platforms. */
fun newTableElementId(): String = randomCellId()
