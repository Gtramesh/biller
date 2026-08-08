package com.invoicesaver.app.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Tasks
import com.invoicesaver.app.InvoiceSaverApp
import com.invoicesaver.app.R
import com.invoicesaver.app.data.BillRepository
import com.invoicesaver.app.data.BillRow
import com.invoicesaver.app.data.ExcelManager
import com.invoicesaver.app.data.FileSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BillUiState(
    val excelFileName: String = "",
    val rowCount: Int = 0,
    val busy: Boolean = false,
    val uploading: Boolean = false,
    val messageRes: Int? = null,
    val errorRes: Int? = null,
    val lastExcelFile: File? = null,
    val lastExcelName: String = ""
)

class BillViewModel(app: Application) : AndroidViewModel(app) {

    private val excelManager: ExcelManager = (app as InvoiceSaverApp).excelManager
    private val prefs = app.getSharedPreferences("invoice_saver", Context.MODE_PRIVATE)
    private val repository = BillRepository()

    private val _state = MutableStateFlow(BillUiState())
    val state: StateFlow<BillUiState> = _state.asStateFlow()

    init {
        ensureSession()
    }

    fun ensureSession() {
        val name = prefs.getString(KEY_EXCEL, null)
        if (name.isNullOrBlank()) {
            createNewSession()
        } else {
            val file = excelManager.excelFile(name)
            val count = if (file.exists()) excelManager.rowCount(file) else 0
            _state.value = BillUiState(excelFileName = name, rowCount = count)
        }
    }

    fun createNewSession(): String {
        val name = "Bills_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".xlsx"
        excelManager.createNewExcel(name)
        prefs.edit().putString(KEY_EXCEL, name).apply()
        _state.value = BillUiState(excelFileName = name, rowCount = 0)
        return name
    }

    fun addBill(
        billNumber: String,
        amount: String,
        billerName: String,
        remarks: String,
        imageUri: Uri?
    ) {
        val current = _state.value
        if (billNumber.isBlank() || amount.isBlank() || billerName.isBlank()) {
            _state.value = current.copy(errorRes = R.string.fill_required)
            return
        }
        if (amount.toDoubleOrNull() == null) {
            _state.value = current.copy(errorRes = R.string.amount_invalid)
            return
        }
        _state.value = current.copy(busy = true, messageRes = null, errorRes = null)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val imageBytes = imageUri?.let { readImageBytes(it) }
                    val imageType = if (imageUri != null) detectImageType(imageUri) else XSSFWorkbook.PICTURE_TYPE_JPEG
                    val row = BillRow(
                        billNumber = billNumber.trim(),
                        amount = amount.trim(),
                        billerName = billerName.trim(),
                        remarks = remarks.trim(),
                        timestamp = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US).format(Date()),
                        imageBytes = imageBytes,
                        imageType = imageType
                    )
                    val count = excelManager.appendRow(excelManager.excelFile(current.excelFileName), row)
                    Result.success(count)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            result.onSuccess { count ->
                _state.value = BillUiState(
                    excelFileName = current.excelFileName,
                    rowCount = count,
                    messageRes = R.string.bill_added
                )
            }.onFailure {
                _state.value = current.copy(busy = false, errorRes = R.string.upload_failed)
            }
        }
    }

    fun quitExcel() {
        val current = _state.value
        if (current.uploading) return
        val oldFile = excelManager.excelFile(current.excelFileName)
        _state.value = current.copy(uploading = true, messageRes = null, errorRes = null)
        viewModelScope.launch {
            var success = false
            var guestMode = false
            withContext(Dispatchers.IO) {
                FileSaver.saveToDownloads(getApplication(), oldFile)
                try {
                    if (repository.currentUser == null) {
                        guestMode = true
                    } else {
                        Tasks.await(repository.uploadExcel(oldFile, current.excelFileName))
                        success = true
                    }
                } catch (e: Exception) {
                    success = false
                }
            }
            val newName = createNewSession()
            _state.value = _state.value.copy(
                uploading = false,
                excelFileName = newName,
                rowCount = 0,
                lastExcelFile = oldFile,
                lastExcelName = current.excelFileName,
                messageRes = when {
                    guestMode -> R.string.guest_saved
                    success -> R.string.upload_success
                    else -> R.string.upload_failed
                }
            )
        }
    }

    fun downloadLast(context: Context) {
        val file = _state.value.lastExcelFile ?: return
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { FileSaver.saveToDownloads(context, file) }
            _state.value = _state.value.copy(
                messageRes = if (ok) R.string.save_success else R.string.save_failed
            )
        }
    }

    fun shareLast(context: Context) {
        val file = _state.value.lastExcelFile ?: return
        FileSaver.shareFile(context, file)
    }

    fun finishedFiles(): List<File> = excelManager.finishedFiles(_state.value.excelFileName)

    fun downloadFile(context: Context, file: File) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { FileSaver.saveToDownloads(context, file) }
            _state.value = _state.value.copy(
                messageRes = if (ok) R.string.save_success else R.string.save_failed
            )
        }
    }

    fun shareFile(context: Context, file: File) {
        FileSaver.shareFile(context, file)
    }

    fun consumeMessages() {
        _state.value = _state.value.copy(messageRes = null, errorRes = null)
    }

    private fun readImageBytes(uri: Uri): ByteArray? {
        val resolver = getApplication<Application>().contentResolver
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        return if (bytes.size > 10 * 1024 * 1024) null else bytes
    }

    private fun detectImageType(uri: Uri): Int {
        val mime = getApplication<Application>().contentResolver.getType(uri) ?: ""
        return if (mime.contains("png")) XSSFWorkbook.PICTURE_TYPE_PNG else XSSFWorkbook.PICTURE_TYPE_JPEG
    }

    companion object {
        private const val KEY_EXCEL = "current_excel"
    }
}
