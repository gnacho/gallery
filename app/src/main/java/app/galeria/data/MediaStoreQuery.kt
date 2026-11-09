package app.galeria.data

object MediaStoreQuery {
    val projection = arrayOf(
        "_id",
        "_display_name",
        "mime_type",
        "datetaken",
        "date_added",
        "width",
        "height",
        "_size",
        "bucket_id",
        "bucket_display_name",
        "duration",
        "is_favorite",
        "is_trashed",
    )

    const val sortOrder = "datetaken DESC, date_added DESC, _id DESC"
}
