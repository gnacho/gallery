package app.galeria.data

data class MediaSelection(val ids: Set<Long> = emptySet()) {
    val count: Int get() = ids.size

    fun contains(id: Long): Boolean = id in ids

    fun toggle(id: Long): MediaSelection = copy(
        ids = if (id in ids) ids - id else ids + id,
    )

    fun selectAll(ids: Iterable<Long>): MediaSelection = copy(ids = this.ids + ids)

    fun clear(): MediaSelection = MediaSelection()
}
