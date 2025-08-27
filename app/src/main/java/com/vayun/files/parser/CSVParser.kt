package com.vayun.files.parser

import androidx.compose.ui.unit.dp
import com.vayun.files.fileview.CSVRow
import com.vayun.files.fileview.Cell
import com.vayun.files.fileview.CellBorder
import com.vayun.files.fileview.CellStyle
import com.vayun.files.fileview.Table

object CSVParser: Parser<Table>() {
    override fun parse(input: String): Table {
        val rows = mutableListOf<CSVRow>()
        val currentRow = mutableListOf<Cell>()
        val currentField = StringBuilder()

        val cellBorder = CellBorder(true, true, true, true)

        var inQuotes = false
        var i = 0
        while (i < input.length) {
            val c = input[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < input.length && input[i + 1] == '"') {
                        // Escaped quote
                        currentField.append('"')
                        i++ // skip next quote
                    } else {
                        // Toggle inQuotes
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    currentRow.add(Cell(currentField.toString(), rows.size, currentRow.size, CellStyle(cellBorder)))
                    currentField.clear()
                }
                (c == '\n' || (c == '\r' && i + 1 < input.length && input[i + 1] == '\n')) && !inQuotes -> {
                    // End of row
                    if (c == '\r') i++ // skip \n in CRLF
                    currentRow.add(Cell(currentField.toString(), rows.size, currentRow.size, CellStyle(cellBorder)))
                    currentField.clear()
                    rows.add(CSVRow(currentRow.toList(), rows.size))
                    currentRow.clear()
                }
                else -> {
                    currentField.append(c)
                }
            }
            i++
        }

        // Add last field/row if file doesn't end with newline
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(Cell(currentField.toString(), rows.size, currentRow.size, CellStyle(cellBorder)))
            rows.add(CSVRow(currentRow.toList(), rows.size))
        }

        val columnCount = rows.maxOfOrNull { it.row.size } ?: 0

        return Table(rows, columnCount, List(columnCount) { 120.dp }, List(rows.size) { 50.dp })
    }
}