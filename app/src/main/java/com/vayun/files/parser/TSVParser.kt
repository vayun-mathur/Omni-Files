package com.vayun.files.parser

import androidx.compose.ui.unit.dp
import com.vayun.files.fileview.CSVRow
import com.vayun.files.fileview.Cell
import com.vayun.files.fileview.CellBorder
import com.vayun.files.fileview.CellStyle
import com.vayun.files.fileview.Table

object TSVParser: Parser<Table>() {
    override fun parse(input: String): Table {
        val rows = mutableListOf<CSVRow>()
        val currentRow = mutableListOf<Cell>()
        val currentField = StringBuilder()
        val cellBorder = CellBorder(true, true, true, true)

        var i = 0
        while (i < input.length) {
            val c = input[i]
            when (c) {
                '\\' -> {
                    // Backslash escape sequences
                    if (i + 1 < input.length) {
                        when (input[i + 1]) {
                            'n' -> currentField.append('\n')
                            't' -> currentField.append('\t')
                            'r' -> currentField.append('\r')
                            '\\' -> currentField.append('\\')
                            else -> currentField.append(c) // Unrecognized escape, treat as literal
                        }
                        i++ // Skip the character after backslash
                    } else {
                        currentField.append(c) // Backslash at the end of the string
                    }
                }
                '\t' -> {
                    // Tab delimiter
                    currentRow.add(Cell(currentField.toString(), rows.size, currentRow.size, CellStyle(cellBorder)))
                    currentField.clear()
                }
                '\n' -> {
                    // End of row (newline)
                    currentRow.add(Cell(currentField.toString(), rows.size, currentRow.size, CellStyle(cellBorder)))
                    currentField.clear()
                    rows.add(CSVRow(currentRow.toList(), rows.size))
                    currentRow.clear()
                    // Handle CRLF (Windows) line endings
                    if (i + 1 < input.length && input[i + 1] == '\r') {
                        i++
                    }
                }
                '\r' -> {
                    // End of row (carriage return)
                    currentRow.add(Cell(currentField.toString(), rows.size, currentRow.size, CellStyle(cellBorder)))
                    currentField.clear()
                    rows.add(CSVRow(currentRow.toList(), rows.size))
                    currentRow.clear()
                    // Handle CRLF (Windows) line endings
                    if (i + 1 < input.length && input[i + 1] == '\n') {
                        i++
                    }
                }
                else -> {
                    currentField.append(c)
                }
            }
            i++
        }

        // Add last field/row if file doesn't end with a row delimiter
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(Cell(currentField.toString(), rows.size, currentRow.size, CellStyle(cellBorder)))
            rows.add(CSVRow(currentRow.toList(), rows.size))
        }

        val columnCount = rows.maxOfOrNull { it.row.size } ?: 0

        return Table(rows, columnCount, List(columnCount) { 120.dp }, List(rows.size) { 50.dp })
    }
}