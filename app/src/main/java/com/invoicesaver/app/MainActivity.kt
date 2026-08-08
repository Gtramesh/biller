package com.invoicesaver.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.invoicesaver.app.ui.InvoiceSaverRoot
import com.invoicesaver.app.ui.theme.InvoiceSaverTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InvoiceSaverTheme {
                InvoiceSaverRoot()
            }
        }
    }
}
