package com.vayun.files.fileview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.vayun.files.MimeType
import com.vayun.files.ShareFAB
import com.vayun.files.getMimeType
import com.vayun.files.parser.CSVParser
import com.vayun.files.parser.ODSParser
import com.vayun.files.parser.TSVParser
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.serialization.UnknownChildHandler
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlConfig
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.math.max
import kotlin.math.min

data class CellBorder(val top: Boolean, val bottom: Boolean, val left: Boolean, val right: Boolean)
data class CellStyle(
    val border: CellBorder = CellBorder(false, false, false, false),
    val textAlign: TextAlign = TextAlign.Left,
    val backgroundColor: Color = Color.Unspecified,
    val textColor: Color = Color.Unspecified,
    val fontSize: TextUnit = 11.sp,
    val fontWeight: FontWeight = FontWeight.Normal
)
data class Cell(val value: String, val row_id: Int, val col_id: Int, val style: CellStyle, val span: Int = 1)
data class CSVRow(val row: List<Cell>, val row_id: Int)
data class TableStyle(val backgroundColor: Color = Color.Unspecified)
data class Table(val rows: List<CSVRow>, val columns: Int, val columnWidths: List<Dp>, val rowHeights: List<Dp>, val style: TableStyle = TableStyle())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpreadsheetPage(navController: NavController, path: String) {
    val currentFile = File(path)
    val mimeType = getMimeType(currentFile)
    val contentResolver = LocalContext.current.contentResolver
    val table = remember {
        val parser = when(mimeType) {
            MimeType.CSV -> CSVParser
            MimeType.ODS -> ODSParser
            MimeType.TSV -> TSVParser
            else -> error("Not a spreadsheet")
        }
        parser.parse(contentResolver.openInputStream(path.toContentUri())!!)
    }

    Scaffold(
        topBar = { TopAppBar({Text(currentFile.name)}) },
        floatingActionButton = { ShareFAB(path) }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            CsvTable(table)
        }
    }
}

@OptIn(ExperimentalXmlUtilApi::class)
val xml = XML {
    autoPolymorphic = true
    defaultPolicy {
        unknownChildHandler = XmlConfig.IGNORING_UNKNOWN_CHILD_HANDLER
    }
}

fun parseFontWeight(foFontWeight: String?): FontWeight {
    if(foFontWeight == null) return FontWeight.Normal
    return when(foFontWeight) {
        "bold" -> FontWeight.Bold
        else -> FontWeight.Normal
    }
}

fun parseFontSizeString(string: String): TextUnit {
    if(Regex("[0-9]*pt").matches(string)) {
        val fontSize = string.split("pt")[0].toInt()
        return fontSize.sp
    }
    error("invalid font size string")
}

@OptIn(ExperimentalPagingApi::class)
@Composable
private fun CsvTable(table: Table) {
    val data = table.rows

    val cellList = data.map { it.row }.flatten()

    val pager: Pager<Int, Cell> = remember { Pager(
        PagingConfig(100),
        pagingSourceFactory = {
            object: PagingSource<Int, Cell>() {
                override fun getRefreshKey(state: PagingState<Int, Cell>): Int? {
                    return state.anchorPosition
                }

                override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Cell> {
                    val key = params.key ?: 0
                    val prevKey = if(key <= 0) null else key - params.loadSize
                    val nextKey = if(key+params.loadSize >= cellList.size) null else key + params.loadSize
                    return LoadResult.Page(cellList.subList(max(key, 0), min(key+params.loadSize, cellList.size)), prevKey, nextKey)
                }
            }
        }
    ) }
    val lazyPagingItems = pager.flow.collectAsLazyPagingItems()

    Box(Modifier
        .horizontalScroll(rememberScrollState())
        .background(table.style.backgroundColor)) {
        LazyVerticalGrid(GridCells.Fixed(table.columns), Modifier.width(table.columnWidths.sumOf { it.value.toDouble() }.dp)) {
            items(
                lazyPagingItems.itemCount,
                key = lazyPagingItems.itemKey { it.row_id*table.columns+it.col_id },
                span = { it: Int ->
                    lazyPagingItems.itemKey { GridItemSpan(it.span) }(it) as GridItemSpan
                }
            ) { index ->
                val cell = lazyPagingItems[index] ?: return@items

                val width = table.columnWidths.subList(cell.col_id, cell.col_id+cell.span).sumOf { it.value.toDouble() }.dp

                val borderShape = GenericShape { size, layout ->
                    moveTo(0f, 0f)
                    if (cell.style.border.top)      lineTo(size.width, 0f)
                    else                            moveTo(size.width, 0f)
                    if (cell.style.border.right)    lineTo(size.width, size.height)
                    else                            moveTo(size.width, size.height)
                    if (cell.style.border.bottom)   lineTo(0f, size.height)
                    else                            moveTo(0f, size.height)
                    if (cell.style.border.left)     lineTo(0f, 0f)
                    else                            moveTo(0f, 0f)
                    moveTo(0f, 0f)
                    moveTo(size.width, size.height)
                }

                Box (Modifier
                    .border(
                        1.dp,
                        if (table.style.backgroundColor == Color.Unspecified) Color.White else Color.Black,
                        shape = borderShape
                    )
                    .background(cell.style.backgroundColor)
                    .width(width)
                    .height(table.rowHeights[cell.row_id])) {
                    Box(Modifier.padding(8.dp)) {
                        Text(cell.value, Modifier.fillMaxSize(), textAlign = when(cell.style.textAlign){
                            TextAlign.Unspecified -> {
                                if(cell.value.toDoubleOrNull() != null) TextAlign.End
                                else TextAlign.Start
                            }
                            else -> cell.style.textAlign
                        }, style = TextStyle(color = cell.style.textColor), fontSize = cell.style.fontSize, fontWeight = cell.style.fontWeight)
                    }
                }
            }
        }
    }
}






//fun byteArrayToInt(byteArray: ByteArray): Int {
//    require(byteArray.size >= 4) { "ByteArray must have at least 4 bytes" }
//    return (byteArray[0].toInt() and 0xFF shl 24) or
//            (byteArray[1].toInt() and 0xFF shl 16) or
//            (byteArray[2].toInt() and 0xFF shl 8) or
//            (byteArray[3].toInt() and 0xFF)
//}
//
//private fun parseXlsx(currentFile: File): Table {
//    val workbook = XSSFWorkbook(currentFile);
//    val sheet = workbook.getSheetAt(0);
//    val rowIterator = sheet.rowIterator()
//
//    val spans = sheet.mergedRegions.associate {
//        (it.firstRow to it.firstColumn) to (it.lastRow - it.firstRow+1 to it.lastColumn - it.firstColumn+1)
//    }
//
//    val sparseCells = mutableListOf<Cell>()
//
//    var columnCount = 0
//
//    for(row in rowIterator.asSequence()) {
//        val cellIterator = row.cellIterator()
//
//        for(cell in cellIterator) {
//            val value = when(cell.cellType) {
//                CellType.NUMERIC -> cell.numericCellValue.toString()
//                CellType.STRING -> cell.stringCellValue
//                else -> ""
//            }
//            val span = spans[cell.address.row to cell.address.column] ?: (1 to 1)
//            if(spans[cell.address.row to cell.address.column] == null && spans.any{
//                it.key.first <= cell.address.row && it.key.second <= cell.address.column
//                    && it.key.first + it.value.first > cell.address.row && it.key.second + it.value.second > cell.address.column
//                }) continue
//            val border = CellBorder(cell.cellStyle.borderTop != BorderStyle.NONE, cell.cellStyle.borderBottom != BorderStyle.NONE, cell.cellStyle.borderLeft != BorderStyle.NONE, cell.cellStyle.borderRight != BorderStyle.NONE)
//            val textAlign = when(cell.cellStyle.alignment) {
//                HorizontalAlignment.GENERAL -> TextAlign.Auto
//                HorizontalAlignment.LEFT -> TextAlign.Left
//                HorizontalAlignment.CENTER -> TextAlign.Center
//                HorizontalAlignment.RIGHT -> TextAlign.Right
//                HorizontalAlignment.FILL -> TextAlign.Left
//                HorizontalAlignment.JUSTIFY -> TextAlign.Left
//                HorizontalAlignment.CENTER_SELECTION -> TextAlign.Left
//                HorizontalAlignment.DISTRIBUTED -> TextAlign.Left
//            }
//            val bg = try {
//                (cell.cellStyle.fillBackgroundColorColor as ExtendedColor?)?.argb?.let {
//                    Color(byteArrayToInt(it))
//                } ?: Color.Unspecified
//            } catch(e: NoClassDefFoundError) {
//                Color.Unspecified
//            }
//            if(bg != Color.Unspecified) {
//                println(bg)
//            }
//            sparseCells += Cell(value, cell.address.row, cell.address.column, CellStyle(border, textAlign, bg), span.second)
//            columnCount = max(columnCount, cell.address.column + span.second)
//        }
//    }
//
//    val sortedSparseCells = sparseCells.groupBy { it.row_id }.mapValues { it.value.sortedBy { it.col_id } }
//
//    val rows = mutableListOf<CSVRow>()
//
//    for((row_id, rowSparseCells) in sortedSparseCells.toSortedMap()) {
//        while(rows.size < row_id) {
//            rows += CSVRow(List(columnCount) { Cell("", rows.size, it, CellStyle()) }, rows.size)
//        }
//        val cells = mutableListOf<Cell>()
//        var rowwidth = 0
//        rowSparseCells.forEach { cell ->
//            while(rowwidth < cell.col_id) {
//                cells += Cell("", row_id, rowwidth, CellStyle())
//                rowwidth++
//            }
//            cells += cell
//            rowwidth += cell.span
//        }
//        rows += CSVRow(cells, row_id)
//    }
//
//    val columnWidths = List(columnCount) {
//        120.dp
//    }
//
//    val rowHeights = List(rows.size) {
//        50.dp
//    }
//
//    return Table(rows, columnCount, columnWidths, rowHeights)
//}
