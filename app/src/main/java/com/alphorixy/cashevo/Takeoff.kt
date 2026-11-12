package com.alphorixy.cashevo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Takeoff : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SplashScreen()
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()

    var isConnected by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    suspend fun performCheck() {
        isLoading = true
        val startTime = System.currentTimeMillis()

        val netConnected = withContext(Dispatchers.IO) {
            checkInternetConnection(context)
        }

        val elapsedTime = System.currentTimeMillis() - startTime
        if (elapsedTime < 2000) {
            delay(3000 - elapsedTime)
        }

        isConnected = netConnected
        isLoading = false
    }

    LaunchedEffect(Unit) {
        performCheck()
    }

    LaunchedEffect(isConnected, isLoading) {
        if (!isLoading && isConnected) {
            val currentUser = auth.currentUser
            val targetIntent = if (currentUser != null) {
                Intent(context, MainActivity::class.java)
            } else {
                Intent(context, LoginScreen::class.java)
            }

            context.startActivity(targetIntent)
            (context as? Activity)?.finish()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F2027),
                        Color(0xFF203A43),
                        Color(0xFF2C5364)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Cash Evo",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "إكمال المهام والحصول على مكافآت",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // الشعار بحجم كبير وواضح وبدون أي قص أو إطار شفاف
            Image(
                painter = painterResource(id = R.drawable.alphorixy),
                contentDescription = "شعار التطبيق",
                modifier = Modifier
                    .fillMaxWidth(0.80f)
                    .aspectRatio(1f)
                    .padding(horizontal = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp, start = 32.dp, end = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFFFFD700),
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "جاري التحقق من الاتصال...",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                } else if (!isConnected) {
                    Text(
                        text = "⚠️ لا يوجد اتصال بالإنترنت",
                        color = Color(0xFFFF6B6B),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                performCheck()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD700)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "إعادة المحاولة",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn()
                    ) {
                        Text(
                            text = "✅ تم الاتصال بنجاح",
                            color = Color(0xFF51CF66),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

fun checkInternetConnection(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    } else {
        @Suppress("DEPRECATION")
        val networkInfo = connectivityManager.activeNetworkInfo
        @Suppress("DEPRECATION")
        return networkInfo?.isConnected == true
    }
}