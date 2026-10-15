package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Locale

class DrawMobil : ComponentActivity() {

    // Verileri sıraya koyup tek bir thread üzerinden sokete basacak kanal
    private val usbDataChannel = Channel<ByteArray>(Channel.CONFLATED)
    
    private var clientSocket: Socket? = null
    private var outputStream: OutputStream? = null

    // UI Durumu
    private var connectionStatus by mutableStateOf("Başlatılıyor...")
    private var debugInfo by mutableStateOf("")
    private var toolTypeInfo by mutableStateOf("Dokunmatik")
    private var lastX by mutableFloatStateOf(0f)
    private var lastY by mutableFloatStateOf(0f)
    private var lastP by mutableFloatStateOf(0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("DrawMobil", "Uygulama başlatıldı (v4.5-Opt)")

        // PC'ye bağlanmaya başla
        startSocketClient()

        // Sokete veri yazan worker coroutine - Lifecycle'a bağlı
        lifecycleScope.launch(Dispatchers.IO) {
            for (data in usbDataChannel) {
                try {
                    outputStream?.let {
                        it.write(data)
                        it.flush()
                    }
                } catch (e: Exception) {
                    outputStream = null
                    clientSocket = null
                }
            }
        }

        setContent {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Arka planda çizim alanı
                @OptIn(ExperimentalComposeUiApi::class)
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInteropFilter { event ->
                            val action = event.actionMasked
                            for (i in 0 until event.pointerCount) {
                                val toolType = event.getToolType(i)
                                val x = event.getX(i)
                                val y = event.getY(i)
                                val basinc = event.getPressure(i)
                                
                                // UI Güncelleme (Hızlı takip için)
                                lastX = x
                                lastY = y
                                lastP = basinc
                                toolTypeInfo = when(toolType) {
                                    MotionEvent.TOOL_TYPE_STYLUS -> "KALEM"
                                    MotionEvent.TOOL_TYPE_ERASER -> "SİLGİ"
                                    else -> "DOKUNMATİK"
                                }

                                // Kalem veya yüksek hassasiyetli dokunuş
                                val isStylus = toolType == MotionEvent.TOOL_TYPE_STYLUS || 
                                              toolType == MotionEvent.TOOL_TYPE_ERASER ||
                                              basinc > 0.001f

                                if (isStylus) {
                                    when (action) {
                                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                            val mesaj = String.format(Locale.US, "X:%.2f,Y:%.2f,P:%.3f,T:%d\n", x, y, basinc, toolType)
                                            usbDataChannel.trySend(mesaj.toByteArray())
                                        }
                                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                            usbDataChannel.trySend("UP\n".toByteArray())
                                        }
                                    }
                                }
                            }
                            true
                        },
                    color = Color.White
                ) { }

                // Bilgi Ekranı
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = connectionStatus, fontSize = 22.sp, color = Color.DarkGray)
                    Text(text = "Mod: $toolTypeInfo", fontSize = 14.sp, color = if(toolTypeInfo == "KALEM") Color(0xFF388E3C) else Color.Gray)
                    
                    if (debugInfo.isNotEmpty()) {
                        Text(text = debugInfo, fontSize = 11.sp, color = Color.Red)
                    }
                    
                    // Canlı Veri İzleyici (Daha kompakt)
                    Column(modifier = Modifier.padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = String.format(Locale.US, "X: %.0f | Y: %.0f | P: %.2f", lastX, lastY, lastP), fontSize = 14.sp, color = Color.Black)
                    }
                }
            }
        }
    }

    private fun startSocketClient() {
        lifecycleScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    withContext(Dispatchers.Main) {
                        connectionStatus = "PC Bekleniyor..."
                        debugInfo = "127.0.0.1:5990"
                    }

                    val socket = Socket()
                    socket.connect(InetSocketAddress("127.0.0.1", 5990), 5000)
                    socket.tcpNoDelay = true
                    
                    withContext(Dispatchers.Main) {
                        connectionStatus = "BAĞLANDI!"
                        debugInfo = "PC Aktif"
                    }
                    
                    clientSocket = socket
                    outputStream = socket.getOutputStream()

                    // Ekran boyutlarını gönder
                    val dm = resources.displayMetrics
                    usbDataChannel.trySend("SIZE:${dm.widthPixels},${dm.heightPixels}\n".toByteArray())

                    // Bağlantı kopana kadar bekle
                    val inputStream = socket.getInputStream()
                    while (true) {
                        if (inputStream.read() == -1) break
                    }
                    throw Exception("Koptu")

                } catch (e: Exception) {
                    clientSocket = null
                    outputStream = null
                    delay(2000)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            outputStream?.close()
            clientSocket?.close()
        } catch (e: Exception) {
            // Sessiz kapat
        }
    }
}
