package app.galeria.data

data class CollageCell(
    val index: Int,
    val row: Int,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)

data class CollageGeometry(val columns: Int, val rows: Int, val cells: List<CollageCell>)

object CollageLayout {
    fun forCount(count: Int): CollageGeometry {
        require(count in 2..9) { "A collage requires between 2 and 9 images" }
        val columns = if (count <= 4) 2 else 3
        val rows = (count + columns - 1) / columns
        val cells = List(count) { index ->
            val row = index / columns
            val column = index % columns
            CollageCell(
                index = index,
                row = row,
                left = column.toFloat() / columns,
                top = row.toFloat() / rows,
                width = 1f / columns,
                height = 1f / rows,
            )
        }
        return CollageGeometry(columns, rows, cells)
    }
}
