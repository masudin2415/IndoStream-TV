
package com.example.tvstream

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

// 1. Model Data Channel
data class Channel(
    val id: String,
    val name: String,
    val logoUrl: String,
    val webUrl: String
)

// List Channel Menggunakan URL Halaman Resmi Vidio
val channelList = listOf(
    Channel(
        id = "indosiar",
        name = "Indosiar",
        logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ad/Indosiar_logo_2015.svg/1200px-Indosiar_logo_2015.svg.png",
        webUrl = "https://www.vidio.com/live/205-indosiar"
    ),
    Channel(
        id = "sctv",
        name = "SCTV",
        logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/05/SCTV_logo_2017.svg/1200px-SCTV_logo_2017.svg.png",
        webUrl = "https://www.vidio.com/live/204-sctv"
    ),
    Channel(
        id = "moji",
        name = "Moji TV",
        logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/Moji_logo_2022.svg/1200px-Moji_logo_2022.svg.png",
        webUrl = "https://www.vidio.com/live/206-moji"
    )
)

enum class ScreenState { SPLASH, HOME, WEB_PLAYER }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainApp()
        }
    }
}

@Composable
fun MainApp() {
    var currentScreen by remember { mutableStateOf(ScreenState.SPLASH) }
    var selectedChannel by remember { mutableStateOf(channelList[0]) }

    when (currentScreen) {
        ScreenState.SPLASH -> SplashScreen { currentScreen = ScreenState.HOME }
        ScreenState.HOME -> HomeScreen(
            channels = channelList,
            onChannelSelect = { channel ->
                selectedChannel = channel
                currentScreen = ScreenState.WEB_PLAYER
            }
        )
        ScreenState.WEB_PLAYER -> VidioWebViewScreen(
            channel = selectedChannel,
            onBackPressed = { currentScreen = ScreenState.HOME }
        )
    }
}

// --- 1. SPLASH SCREEN ---
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000)
        onTimeout()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "INDOSSTREAM TV",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(color = Color(0xFF22C55E))
        }
    }
}

// --- 2. HOME SCREEN ---
@Composable
fun HomeScreen(channels: List<Channel>, onChannelSelect: (Channel) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .padding(32.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text(
                text = "LIVE TV",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Red
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "STREAM",
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = Color.White
            )
        }

        Text(
            text = "Pilih Saluran TV Resmi",
            fontSize = 18.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(channels) { channel ->
                ChannelCard(channel = channel, onClick = { onChannelSelect(channel) })
            }
        }
    }
}

// --- CHANNEL CARD (Support D-Pad Remote) ---
@Composable
fun ChannelCard(channel: Channel, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(180.dp)
            .height(110.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) Color(0xFF1E293B) else Color(0xFF151C2C))
            .border(
                width = if (isFocused) 3.dp else 1.dp,
                color = if (isFocused) Color(0xFF38BDF8) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = channel.name,
                modifier = Modifier
                    .height(50.dp)
                    .fillMaxWidth(),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = channel.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// --- 3. WEBVIEW PLAYER SCREEN (VIDIO EMBED) ---
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VidioWebViewScreen(channel: Channel, onBackPressed: () -> Unit) {
    // Tombol Back pada remote/HP akan kembali ke Home Screen
    BackHandler {
        onBackPressed()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    // Pengaturan WebView
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        // User Agent Desktop/Chrome modern agar pemutar Vidio berjalan optimal
                        userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    }

                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            url: String?
                        ): Boolean {
                            return false // Tetap buka di dalam WebView
                        }
                    }

                    // Muat halaman resmi Vidio
                    loadUrl(channel.webUrl)
                }
            },
            update = { webView ->
                webView.loadUrl(channel.webUrl)
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
