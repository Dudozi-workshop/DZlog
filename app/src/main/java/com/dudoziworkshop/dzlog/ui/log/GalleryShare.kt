package com.dudoziworkshop.dzlog.ui.log

import android.content.ClipData
import android.content.Context
import android.content.Intent
import com.dudoziworkshop.dzlog.domain.model.MediaImageItem

/** Share exactly the selected files, granting the receiving app temporary URI access. */
internal fun shareGalleryImages(context: Context, items: List<MediaImageItem>) {
    val uris = ArrayList(items.map { it.uri }.distinct())
    require(uris.isNotEmpty()) { "공유할 사진을 선택해 주세요." }
    val intent = Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
        type = "image/*"
        if (uris.size == 1) putExtra(Intent.EXTRA_STREAM, uris.first())
        else putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        clipData = ClipData.newRawUri("DZlog 사진", uris.first()).apply {
            uris.drop(1).forEach { addItem(ClipData.Item(it)) }
        }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "사진 공유"))
}
