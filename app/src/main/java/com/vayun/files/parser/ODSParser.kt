package com.vayun.files.parser

import android.content.ContentResolver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import com.vayun.files.fileview.CSVRow
import com.vayun.files.fileview.Cell
import com.vayun.files.fileview.CellBorder
import com.vayun.files.fileview.CellStyle
import com.vayun.files.fileview.Table
import com.vayun.files.fileview.TableStyle
import com.vayun.files.fileview.parseFontSizeString
import com.vayun.files.fileview.parseFontWeight
import com.vayun.files.fileview.xml
import kotlinx.serialization.SerialName
import nl.adaptivity.xmlutil.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

fun String.toColorInt2(): Int {
    if(this == "transparent") return Color.Transparent.toArgb()
    return toColorInt()
}

object ODSParser: Parser<Table>() {
    override fun parse(f: InputStream): Table {
        var officeDocumentContent: OfficeDocumentContent? = null
        ZipInputStream(f).use { zipInputStream ->
            var entry: ZipEntry?
            while (zipInputStream.nextEntry.also { entry = it } != null) {
                if(entry?.isDirectory?: true) continue
                val content = zipInputStream.readBytes().toString(StandardCharsets.UTF_8)
                if(entry.name == "content.xml") {
                    println(content)
                    officeDocumentContent = xml.decodeFromString<OfficeDocumentContent>(content)
                }
                zipInputStream.closeEntry()
            }
        }
        if(officeDocumentContent == null) error("Spreadsheet not found")

        var columnCount = 1
        val table = officeDocumentContent.body!!.spreadsheet.table
        table.tableColumns.dropLast(1).forEach {
            columnCount += it.numberColumnsRepeated ?: 1
        }
        val rows = table.tableRows.mapIndexed { rowIdx, row ->
            val newRow = mutableListOf<Cell>()
            var columnsCounted = 0

            row.cellContent.forEachIndexed { idx, cell ->
                if(cell is CoveredTableCell) {
                    columnsCounted += cell.numberColumnsRepeated ?: 1
                    return@forEachIndexed
                }
                if(cell !is TableCell) return@forEachIndexed
                val text =  cell.text?.content ?: ""
                val styleName = cell.tableStyleName
                val style = officeDocumentContent.automaticStyles.styles.firstOrNull { it.styleName == styleName }
                val textStyle = style?.paragraphProperties?.foTextAlign ?: TextAlign.AUTO
                val translatedTextAlign = when(textStyle) {
                    TextAlign.AUTO -> androidx.compose.ui.text.style.TextAlign.Unspecified
                    TextAlign.LEFT -> androidx.compose.ui.text.style.TextAlign.Left
                    TextAlign.CENTER -> androidx.compose.ui.text.style.TextAlign.Center
                    TextAlign.RIGHT -> androidx.compose.ui.text.style.TextAlign.Right
                }

                val textColor = Color(style?.textProperties?.foColor?.trim()?.toColorInt2() ?: Color.Unspecified.toArgb())

                val fontSize = parseFontSizeString(style?.textProperties?.foFontSize ?: "11pt")
                val fontWeight = parseFontWeight(style?.textProperties?.foFontWeight)

                val tableCellProperties = style?.tableCellProperties
                val backgroundColor = Color(tableCellProperties?.foBackgroundColor?.trim()?.toColorInt2() ?: Color.Unspecified.toArgb())

                val borderAll = tableCellProperties?.foBorder !in listOf(null, "none")
                val borderTop = tableCellProperties?.foBorderTop.let { it !in listOf(null, "none") || it == null && borderAll }
                val borderBottom = tableCellProperties?.foBorderBottom.let { it !in listOf(null, "none") || it == null && borderAll }
                val borderLeft = tableCellProperties?.foBorderLeft.let { it !in listOf(null, "none") || it == null && borderAll }
                val borderRight = tableCellProperties?.foBorderRight.let { it !in listOf(null, "none") || it == null && borderAll }

                newRow.add(Cell(text, rowIdx, columnsCounted, CellStyle(CellBorder(borderTop, borderBottom, borderLeft, borderRight), translatedTextAlign, backgroundColor, textColor, fontSize, fontWeight), span = cell.tableNumberColumnsSpanned ?: 1))
                columnsCounted += if(idx == row.cellContent.lastIndex) cell.tableNumberColumnsSpanned ?: 1 else 1
            }
            while(columnsCounted < columnCount) {
                newRow.add(Cell("", rowIdx, columnsCounted, CellStyle(), span = 1))
                columnsCounted += 1
            }

            CSVRow(newRow, rowIdx)
        }

        val columnWidths = List(columnCount) { 120.dp }

        val rowHeights = List(rows.size) { 50.dp }

        return Table(rows, columnCount, columnWidths, rowHeights, TableStyle(Color.White))
    }

    // Namespace Constants
    private const val N_TABLE = "urn:oasis:names:tc:opendocument:xmlns:table:1.0"
    private const val P_TABLE = "table"
    private const val N_OFFICE = "urn:oasis:names:tc:opendocument:xmlns:office:1.0"
    private const val P_OFFICE = "office"
    private const val N_TEXT = "urn:oasis:names:tc:opendocument:xmlns:text:1.0"
    private const val P_TEXT = "text"
    private const val N_STYLE = "urn:oasis:names:tc:opendocument:xmlns:style:1.0"
    private const val P_STYLE = "style"
    private const val N_DRAW = "urn:oasis:names:tc:opendocument:xmlns:drawing:1.0"
    private const val P_DRAW = "draw"
    private const val N_FO = "urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0"
    private const val P_FO = "fo"
    private const val N_XLINK = "http://www.w3.org/1999/xlink"
    private const val P_XLINK = "xlink"
    private const val N_DC = "http://purl.org/dc/elements/1.1/"
    private const val P_DC = "dc"
    private const val N_NUMBER = "urn:oasis:names:tc:opendocument:xmlns:datastyle:1.0"
    private const val P_NUMBER = "number"
    private const val N_SVG = "urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0"
    private const val P_SVG = "svg"
    private const val N_OF = "urn:oasis:names:tc:opendocument:xmlns:of:1.2"
    private const val P_OF = "of"


    @Serializable
    @XmlSerialName("document-content", N_OFFICE, P_OFFICE)
    data class OfficeDocumentContent(
        // The office:version attribute, mapped with @XmlElement(false)
        @XmlSerialName("version", N_OFFICE, P_OFFICE)
        @XmlElement(false)
        val officeVersion: String,

        // Nested font-face-decls element
        @XmlSerialName("font-face-decls", N_OFFICE, P_OFFICE)
        val fontFaceDecls: OfficeFontFaceDecls? = null,

        // Nested automatic-styles element
        @XmlSerialName("automatic-styles", N_OFFICE, P_OFFICE)
        val automaticStyles: OfficeAutomaticStyles = OfficeAutomaticStyles(listOf()),

        // Nested body element
        @XmlSerialName("body", N_OFFICE, P_OFFICE)
        val body: OfficeBody? = null
    )

    @Serializable
    @XmlSerialName("font-face-decls", N_OFFICE, P_OFFICE)
    data class OfficeFontFaceDecls(
        // List of font-face elements, correctly mapped as child elements
        @XmlSerialName("font-face", N_STYLE, P_STYLE)
        @XmlElement(true)
        val fontFaces: List<StyleFontFace>
    )

    @Serializable
    @XmlSerialName("font-face", N_STYLE, P_STYLE)
    data class StyleFontFace(
        // style:name attribute, mapped with @XmlElement(false)
        @XmlSerialName("name", N_STYLE, P_STYLE)
        @XmlElement(false)
        val styleName: String,

        // svg:font-family attribute, mapped with @XmlElement(false)
        @XmlSerialName("font-family", N_SVG, P_SVG)
        @XmlElement(false)
        val svgFontFamily: String
    )

    @Serializable
    @XmlSerialName("automatic-styles", N_OFFICE, P_OFFICE)
    data class OfficeAutomaticStyles(
        // List of style elements, correctly mapped as child elements
        @XmlSerialName("style", N_STYLE, P_STYLE)
        @XmlElement(true)
        val styles: List<Style>
    )

    @Serializable
    @XmlSerialName("style", N_STYLE, P_STYLE)
    data class Style(
        // Attributes, mapped with @XmlElement(false)
        @XmlSerialName("name", N_STYLE, P_STYLE)
        @XmlElement(false)
        val styleName: String,
        @XmlSerialName("family", N_STYLE, P_STYLE)
        @XmlElement(false)
        val styleFamily: String,
        @XmlSerialName("parent-style-name", N_STYLE, P_STYLE)
        @XmlElement(false)
        val styleParentStyleName: String? = null,
        @XmlSerialName("data-style-name", N_STYLE, P_STYLE)
        @XmlElement(false)
        val styleDataStyleName: String? = null,
        @XmlSerialName("master-page-name", N_STYLE, P_STYLE)
        @XmlElement(false)
        val styleMasterPageName: String? = null,

        // Style properties, which are child elements and can be nullable
        @XmlSerialName("text-properties", N_STYLE, P_STYLE)
        val textProperties: StyleTextProperties = StyleTextProperties(),
        @XmlSerialName("table-cell-properties", N_STYLE, P_STYLE)
        val tableCellProperties: StyleTableCellProperties = StyleTableCellProperties(),
        @XmlSerialName("paragraph-properties", N_STYLE, P_STYLE)
        val paragraphProperties: StyleParagraphProperties = StyleParagraphProperties(),
        @XmlSerialName("table-column-properties", N_STYLE, P_STYLE)
        val tableColumnProperties: StyleTableColumnProperties = StyleTableColumnProperties(),
        @XmlSerialName("table-row-properties", N_STYLE, P_STYLE)
        val tableRowProperties: StyleTableRowProperties = StyleTableRowProperties(),
        @XmlSerialName("table-properties", N_STYLE, P_STYLE)
        val tableProperties: StyleTableProperties = StyleTableProperties()
    )

    @Serializable
    @XmlSerialName("text-properties", N_STYLE, P_STYLE)
    data class StyleTextProperties(
        // Attributes, mapped with @XmlElement(false)
        @XmlSerialName("font-name", N_STYLE, P_STYLE)
        @XmlElement(false)
        val styleFontName: String? = null,
        @XmlSerialName("font-name-asian", N_STYLE, P_STYLE)
        @XmlElement(false)
        val styleFontNameAsian: String? = null,
        @XmlSerialName("font-name-complex", N_STYLE, P_STYLE)
        @XmlElement(false)
        val styleFontNameComplex: String? = null,
        @XmlSerialName("color", N_FO, P_FO)
        @XmlElement(false)
        val foColor: String? = null,
        @XmlSerialName("font-size", N_FO, P_FO)
        @XmlElement(false)
        val foFontSize: String? = null,
        @XmlSerialName("font-size-asian", N_STYLE, P_STYLE)
        @XmlElement(false)
        val styleFontSizeAsian: String? = null,
        @XmlSerialName("font-size-complex", N_STYLE, P_STYLE)
        @XmlElement(false)
        val styleFontSizeComplex: String? = null,
        @XmlSerialName("font-weight", N_FO, P_FO)
        @XmlElement(false)
        val foFontWeight: String? = null,
        @XmlSerialName("font-weight-asian", N_STYLE, P_STYLE)
        @XmlElement(false)
        val styleFontWeightAsian: String? = null,
        @XmlSerialName("font-weight-complex", N_STYLE, P_STYLE)
        @XmlElement(false)
        val styleFontWeightComplex: String? = null
    )

    @Serializable
    @XmlSerialName("table-cell-properties", N_STYLE, P_STYLE)
    data class StyleTableCellProperties(
        // Attributes, mapped with @XmlElement(false)
        @XmlSerialName("border", N_FO, P_FO)
        @XmlElement(false)
        val foBorder: String? = null,
        @XmlSerialName("border-top", N_FO, P_FO)
        @XmlElement(false)
        val foBorderTop: String? = null,
        @XmlSerialName("border-bottom", N_FO, P_FO)
        @XmlElement(false)
        val foBorderBottom: String? = null,
        @XmlSerialName("border-left", N_FO, P_FO)
        @XmlElement(false)
        val foBorderLeft: String? = null,
        @XmlSerialName("border-right", N_FO, P_FO)
        @XmlElement(false)
        val foBorderRight: String? = null,
        @XmlSerialName("vertical-align", N_STYLE, P_STYLE)
        @XmlElement(false)
        val styleVerticalAlign: String? = null,
        @XmlSerialName("background-color", N_FO, P_FO)
        @XmlElement(false)
        val foBackgroundColor: String? = null,
        @XmlSerialName("repeat-content", N_STYLE, P_STYLE)
        @XmlElement(false)
        val styleRepeatContent: String? = null
    )

    enum class TextAlign
    {
        @SerialName("auto")
        AUTO,
        @SerialName("left")
        LEFT,
        @SerialName("center")
        CENTER,
        @SerialName("right")
        RIGHT,
    }

    @Serializable
    @XmlSerialName("paragraph-properties", N_STYLE, P_STYLE)
    data class StyleParagraphProperties(
        // Attributes, mapped with @XmlElement(false)
        @XmlSerialName("text-align", N_FO, P_FO)
        @XmlElement(false)
        val foTextAlign: TextAlign = TextAlign.AUTO
    )

    @Serializable
    @XmlSerialName("table-column-properties", N_STYLE, P_STYLE)
    data class StyleTableColumnProperties(
        // Attributes, mapped with @XmlElement(false)
        @XmlSerialName("break-before", N_FO, P_FO)
        @XmlElement(false)
        val foBreakBefore: String? = null,
        @XmlSerialName("column-width", N_STYLE, P_STYLE)
        @XmlElement(false)
        val styleColumnWidth: String? = null,
        @XmlSerialName("use-optimal-column-width", N_STYLE, P_STYLE)
        @XmlElement(false)
        val styleUseOptimalColumnWidth: Boolean? = null
    )

    @Serializable
    @XmlSerialName("table-row-properties", N_STYLE, P_STYLE)
    data class StyleTableRowProperties(
        // Attributes, mapped with @XmlElement(false)
        @XmlSerialName("row-height", N_STYLE, P_STYLE)
        @XmlElement(false)
        val styleRowHeight: String? = null,
        @XmlSerialName("use-optimal-row-height", N_STYLE, P_STYLE)
        @XmlElement(false)
        val styleUseOptimalRowHeight: Boolean? = null,
        @XmlSerialName("break-before", N_FO, P_FO)
        @XmlElement(false)
        val foBreakBefore: String? = null
    )

    @Serializable
    @XmlSerialName("table-properties", N_STYLE, P_STYLE)
    data class StyleTableProperties(
        // Attributes, mapped with @XmlElement(false)
        @XmlSerialName("display", N_TABLE, P_TABLE)
        @XmlElement(false)
        val tableDisplay: Boolean? = null,
        @XmlSerialName("writing-mode", N_STYLE, P_STYLE)
        @XmlElement(false)
        val styleWritingMode: String? = null
    )

// New Data Classes for <office:body> and its children

    @Serializable
    @XmlSerialName("body", N_OFFICE, P_OFFICE)
    data class OfficeBody(
        @XmlSerialName("spreadsheet", N_OFFICE, P_OFFICE)
        val spreadsheet: OfficeSpreadsheet
    )

    @Serializable
    @XmlSerialName("spreadsheet", N_OFFICE, P_OFFICE)
    data class OfficeSpreadsheet(
        @XmlSerialName("calculation-settings", N_TABLE, P_TABLE)
        val calculationSettings: TableCalculationSettings? = null, // Optional element
        @XmlSerialName("table", N_TABLE, P_TABLE)
        val table: Table2
    )

    @Serializable
    @XmlSerialName("calculation-settings", N_TABLE, P_TABLE)
    data class TableCalculationSettings(
        // Attributes, mapped with @XmlElement(false)
        @XmlSerialName("case-sensitive", N_TABLE, P_TABLE)
        @XmlElement(false)
        val caseSensitive: Boolean,
        @XmlSerialName("search-criteria-must-apply-to-whole-cell", N_TABLE, P_TABLE)
        @XmlElement(false)
        val searchCriteriaMustApplyToWholeCell: Boolean,
        @XmlSerialName("use-wildcards", N_TABLE, P_TABLE)
        @XmlElement(false)
        val useWildcards: Boolean,
        @XmlSerialName("use-regular-expressions", N_TABLE, P_TABLE)
        @XmlElement(false)
        val useRegularExpressions: Boolean,
        @XmlSerialName("automatic-find-labels", N_TABLE, P_TABLE)
        @XmlElement(false)
        val automaticFindLabels: Boolean
    )

    // Updated Table data class to include columns
    @Serializable
    @XmlSerialName("table", N_TABLE, P_TABLE)
    data class Table2(
        @XmlSerialName("name", N_TABLE, P_TABLE)
        @XmlElement(false) // Attribute, mapped with @XmlElement(false)
        val name: String,
        @XmlSerialName("style-name", N_TABLE, P_TABLE)
        @XmlElement(false) // Attribute, mapped with @XmlElement(false)
        val styleName: String,

        @XmlSerialName("table-column", N_TABLE, P_TABLE)
        @XmlElement(true) // List of child elements
        val tableColumns: List<TableColumn> = listOf(),

        // This property will capture both table:table-cell and table:covered-table-cell
        @XmlSerialName("table-row", N_TABLE, P_TABLE)
        @XmlElement(true) // List of child elements
        val tableRows: List<TableRow> = listOf()
    )

    @Serializable
    @XmlSerialName("table-column", N_TABLE, P_TABLE)
    data class TableColumn(
        // Attributes, mapped with @XmlElement(false)
        @XmlSerialName("style-name", N_TABLE, P_TABLE)
        @XmlElement(false)
        val styleName: String,
        @XmlSerialName("default-cell-style-name", N_TABLE, P_TABLE)
        @XmlElement(false)
        val defaultCellStyleName: String,
        @XmlSerialName("number-columns-repeated", N_TABLE, P_TABLE)
        @XmlElement(false)
        val numberColumnsRepeated: Int? = null // Optional attribute
    )

    @Serializable
    @XmlSerialName("table-row", N_TABLE, P_TABLE)
    data class TableRow(
        @XmlSerialName("style-name", N_TABLE, P_TABLE)
        @XmlElement(false) // Attribute, mapped with @XmlElement(false)
        val styleName: String,

        @XmlSerialName("number-rows-repeated", N_TABLE, P_TABLE)
        @XmlElement(false)
        val numberRowsRepeated: Int? = null,

        // This sealed interface allows for different types of cell content within a row
        @XmlElement(true) // Indicates a list of child elements
        val cellContent: List<CellContent>
    )

    @Serializable
    sealed interface CellContent // Common interface for different cell types

    @Serializable
    @XmlSerialName("table-cell", N_TABLE, P_TABLE)
    data class TableCell(
        // Attributes, mapped with @XmlElement(false)
        @XmlSerialName("number-columns-repeated", N_TABLE, P_TABLE)
        @XmlElement(false)
        val numberColumnsRepeated: Int? = null,
        @XmlSerialName("value-type", N_OFFICE, P_OFFICE)
        @XmlElement(false)
        val officeValueType: String? = null,
        @XmlSerialName("value", N_OFFICE, P_OFFICE)
        @XmlElement(false)
        val officeValue: String? = null, // Can be float or percentage value
        @XmlSerialName("date-value", N_OFFICE, P_OFFICE)
        @XmlElement(false)
        val officeDateValue: String? = null,
        @XmlSerialName("number-columns-spanned", N_TABLE, P_TABLE)
        @XmlElement(false)
        val tableNumberColumnsSpanned: Int? = null,
        @XmlSerialName("number-rows-spanned", N_TABLE, P_TABLE)
        @XmlElement(false)
        val tableNumberRowsSpanned: Int? = null,
        @XmlSerialName("style-name", N_TABLE, P_TABLE)
        @XmlElement(false)
        val tableStyleName: String? = null,

        // Nested text element
        @XmlSerialName("p", N_TEXT, P_TEXT)
        val text: Text? = null
    ) : CellContent

    @Serializable
    @XmlSerialName("covered-table-cell", N_TABLE, P_TABLE)
    data class CoveredTableCell(
        // Attributes, mapped with @XmlElement(false)
        @XmlSerialName("number-columns-repeated", N_TABLE, P_TABLE)
        @XmlElement(false)
        val numberColumnsRepeated: Int? = null
    ) : CellContent

    @Serializable
    @XmlSerialName("p", N_TEXT, P_TEXT)
    data class Text(
        @XmlValue
        val content: String
    )
}