package com.example.myapplication

import com.example.myapplication.ui.theme.MyApplicationTheme
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.PrintWriter
import java.net.Socket

class DrawMobil : ComponentActivity() {

    // Ağ bağlantımızı tutacak değişken
    private var soketYazici: PrintWriter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val coroutineScope = rememberCoroutineScope()

                // Uygulama açıldığında bir kere çalışıp bilgisayara bağlanmayı dener
                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        try {
                            val socket = Socket("127.0.0.1", 8080)
                            soketYazici = PrintWriter(socket.getOutputStream(), true)
                            Log.d("BAGLANTI", "Bilgisayara bağlandı!")
                        } catch (e: Exception) {
                            Log.e("BAGLANTI", "Hata: ${e.message}")
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.first()

                                    if (change.type == PointerType.Stylus) {
                                        val x = change.position.x
                                        val y = change.position.y
                                        val basinc = change.pressure

                                        // Veriyi arka planda Python'a yolla
                                        coroutineScope.launch(Dispatchers.IO) {
                                            soketYazici?.println("X:$x,Y:$y,P:$basinc")
                                        }
                                    }
                                }
                            }
                        },
                    color = Color.White
                ) {
                    // Boş tuval
                }
            }
        }
    }
}