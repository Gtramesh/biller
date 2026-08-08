package com.invoicesaver.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFClientAnchor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

data class BillRow(
    val billNumber: String,
    val amount: String,
    val billerName: String,
    val remarks: String,
    val timestamp: String,
    val imageBytes: ByteArray? = null,
    val imageType: Int = XSSFWorkbook.PICTURE_TYPE_JPEG
)

class ExcelManager(private val context: Context) {

    private val excelDir = File(context.filesDir, "excel")

    init {
        excelDir.mkdirs()
    }

    companion object {
        val HEADERS = listOf(
            "Bill Number", "Amount", "Name of Biller", "Remarks", "Date / Time", "Bill Image"
        )
        private const val EMU_PER_PX = 9525L
        private const val MAX_IMAGE_PX = 200
    }

    fun excelFile(fileName: String): File = File(excelDir, fileName)

    fun finishedFiles(currentFileName: String): List<File> =
        excelDir.listFiles { f -> f.isFile && f.name.endsWith(".xlsx") && f.name != currentFileName }
            ?.sortedByDescending { it.lastModified() }
            ?.toList() ?: emptyList()

    fun createNewExcel(fileName: String): File {
        val file = excelFile(fileName)
        XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet("Bills")

            val headerFont = workbook.createFont().apply {
                setBold(true)
                color = IndexedColors.WHITE.index
                fontHeightInPoints = 12
            }
            val headerStyle = workbook.createCellStyle().apply {
                setFont(headerFont)
                fillForegroundColor = IndexedColors.ROYAL_BLUE.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                alignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER
                setBorderTop(BorderStyle.MEDIUM)
                setBorderBottom(BorderStyle.MEDIUM)
                setBorderLeft(BorderStyle.MEDIUM)
                setBorderRight(BorderStyle.MEDIUM)
            }

            val headerRow = sheet.createRow(0)
            headerRow.heightInPoints = 24f
            HEADERS.forEachIndexed { i, header ->
                headerRow.createCell(i).apply {
                    setCellValue(header)
                    cellStyle = headerStyle
                }
            }

            sheet.createFreezePane(0, 1)
            sheet.setAutoFilter(CellRangeAddress(0, 0, 0, HEADERS.size - 1))

            setColumnWidths(sheet)
            writeWorkbook(workbook, file)
        }
        return file
    }

    fun appendRow(file: File, row: BillRow): Int {
        XSSFWorkbook(file).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            val rowIdx = sheet.lastRowNum + 1
            val excelRow = sheet.createRow(rowIdx)
            excelRow.heightInPoints = 24f

            val bodyStyle = workbook.createCellStyle().apply {
                verticalAlignment = VerticalAlignment.CENTER
                setBorderTop(BorderStyle.THIN)
                setBorderBottom(BorderStyle.THIN)
                setBorderLeft(BorderStyle.THIN)
                setBorderRight(BorderStyle.THIN)
            }
            val amountStyle = workbook.createCellStyle().apply {
                verticalAlignment = VerticalAlignment.CENTER
                alignment = HorizontalAlignment.RIGHT
                dataFormat = workbook.createDataFormat().getFormat("#,##0.00")
                setBorderTop(BorderStyle.THIN)
                setBorderBottom(BorderStyle.THIN)
                setBorderLeft(BorderStyle.THIN)
                setBorderRight(BorderStyle.THIN)
            }
            val remarkStyle = workbook.createCellStyle().apply {
                verticalAlignment = VerticalAlignment.CENTER
                wrapText = true
                setBorderTop(BorderStyle.THIN)
                setBorderBottom(BorderStyle.THIN)
                setBorderLeft(BorderStyle.THIN)
                setBorderRight(BorderStyle.THIN)
            }
            val imageStyle = workbook.createCellStyle().apply {
                verticalAlignment = VerticalAlignment.CENTER
                alignment = HorizontalAlignment.CENTER
                setBorderTop(BorderStyle.THIN)
                setBorderBottom(BorderStyle.THIN)
                setBorderLeft(BorderStyle.THIN)
                setBorderRight(BorderStyle.THIN)
            }

            excelRow.createCell(0).apply {
                setCellValue(row.billNumber)
                cellStyle = bodyStyle
            }
            excelRow.createCell(1).apply {
                setCellValue(row.amount.toDoubleOrNull() ?: 0.0)
                cellStyle = amountStyle
            }
            excelRow.createCell(2).apply {
                setCellValue(row.billerName)
                cellStyle = bodyStyle
            }
            excelRow.createCell(3).apply {
                setCellValue(row.remarks)
                cellStyle = remarkStyle
            }
            excelRow.createCell(4).apply {
                setCellValue(row.timestamp)
                cellStyle = bodyStyle
            }
            excelRow.createCell(5).apply {
                setCellValue("Image")
                cellStyle = imageStyle
            }
            if (row.imageBytes != null) {
                embedImage(workbook, sheet, excelRow, rowIdx, row)
            }
            writeWorkbook(workbook, file)
            return sheet.lastRowNum
        }
    }

    fun rowCount(file: File): Int {
        if (!file.exists()) return 0
        XSSFWorkbook(file).use { workbook ->
            return workbook.getSheetAt(0).lastRowNum
        }
    }

    private fun setColumnWidths(sheet: org.apache.poi.ss.usermodel.Sheet) {
        val widths = intArrayOf(16, 14, 22, 36, 20, 18)
        widths.forEachIndexed { i, w -> sheet.setColumnWidth(i, w * 256) }
    }

    private fun embedImage(
        workbook: XSSFWorkbook,
        sheet: org.apache.poi.ss.usermodel.Sheet,
        excelRow: org.apache.poi.ss.usermodel.Row,
        rowIdx: Int,
        row: BillRow
    ) {
        val raw = row.imageBytes ?: return
        val bmp = BitmapFactory.decodeByteArray(raw, 0, raw.size)

        var imgBytes: ByteArray
        var imgType: Int
        var w: Int
        var h: Int

        if (bmp == null) {
            imgBytes = raw
            imgType = row.imageType
            w = MAX_IMAGE_PX
            h = MAX_IMAGE_PX
        } else {
            w = bmp.width
            h = bmp.height
            if (w > MAX_IMAGE_PX || h > MAX_IMAGE_PX) {
                val scale = MAX_IMAGE_PX.toFloat() / maxOf(w, h)
                val scaled = Bitmap.createScaledBitmap(
                    bmp,
                    (w * scale).toInt().coerceAtLeast(1),
                    (h * scale).toInt().coerceAtLeast(1),
                    true
                )
                w = scaled.width
                h = scaled.height
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.PNG, 90, out)
                if (scaled !== bmp) scaled.recycle()
                bmp.recycle()
                imgBytes = out.toByteArray()
                imgType = XSSFWorkbook.PICTURE_TYPE_PNG
            } else {
                imgBytes = raw
                imgType = row.imageType
            }
        }

        val pictureIdx = workbook.addPicture(imgBytes, imgType)
        val drawing = sheet.createDrawingPatriarch()
        val anchor = XSSFClientAnchor(
            0, 0, (w * EMU_PER_PX).toInt(), (h * EMU_PER_PX).toInt(), 6, rowIdx, 6, rowIdx
        )
        drawing.createPicture(anchor, pictureIdx)
        excelRow.heightInPoints = (h * 0.75f) + 10f
    }

    private fun writeWorkbook(workbook: XSSFWorkbook, file: File) {
        val tmp = File(file.parentFile, "${file.name}.tmp")
        try {
            FileOutputStream(tmp).use { workbook.write(it) }
            tmp.renameTo(file)
        } finally {
            if (tmp.exists()) tmp.delete()
        }
    }
}
