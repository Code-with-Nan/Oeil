package com.example.oeil.model

/** Answer style, chosen from the header pill and handed to [com.example.oeil.data.ReplySource]. */
enum class ReplyMode(val label: String) {
    Auto("Auto"),
    Precise("Precise"),
    Creative("Creative");

    fun next(): ReplyMode = entries[(ordinal + 1) % entries.size]
}
