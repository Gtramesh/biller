package com.invoicesaver.app

import android.app.Application
import com.invoicesaver.app.data.ExcelManager

class InvoiceSaverApp : Application() {

    lateinit var excelManager: ExcelManager
        private set

    override fun onCreate() {
        super.onCreate()
        System.setProperty("java.io.tmpdir", cacheDir.absolutePath)
        excelManager = ExcelManager(this)
    }
}
