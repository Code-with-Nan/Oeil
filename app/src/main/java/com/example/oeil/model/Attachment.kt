package com.example.oeil.model

import android.net.Uri

/** An image picked with the system photo picker, shown as a removable chip in the composer. */
data class Attachment(
    val uri: Uri,
    val label: String
)
