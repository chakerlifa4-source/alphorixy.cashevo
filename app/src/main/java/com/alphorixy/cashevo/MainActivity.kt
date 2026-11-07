package com.alphorixy.cashevo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.launch

val BackgroundTop = Color(0xFF0F2027)
val BackgroundMiddle = Color(0xFF203A43)
val BackgroundBottom = Color(0xFF2C5364)
val CardColor = Color(0xFF1E3038)
val Gold = Color(0xFFFFD700)
val TextWhite = Color(0xFFFFFFFF)
val TextMuted = Color(0xFFB0BEC5)
val SuccessGreen = Color(0xFF51CF66)

val AppBackground = Brush.verticalGradient(
    colors = listOf(BackgroundTop, BackgroundMiddle, BackgroundBottom)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this) {}
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Gold,
                    onPrimary = Color.Black,
                    secondary = BackgroundBottom,
                    background = BackgroundTop,
                    surface = CardColor,
                    onSurface = TextWhite
                )
            ) {
                MainDashboardScreen()
            }
        }
    }
}

@Composable
fun MainDashboardScreen() {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    var userBalance by remember { mutableStateOf(0.0000) }
    val minWithdrawal = 5.00

    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    var userId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            userId = currentUser.uid
        } else {
            auth.signInAnonymously()
                .addOnSuccessListener { result ->
                    userId = result.user?.uid
                }
        }
    }

    DisposableEffect(userId) {
        val registration = userId?.let { id ->
            val ref = db.collection("users").document(id)
            ref.addSnapshotListener { doc, _ ->
                if (doc == null) return@addSnapshotListener
                if (doc.exists()) {
                    userBalance = doc.getDouble("balance") ?: 0.0
                } else if (!doc.metadata.isFromCache) {
                    ref.set(mapOf("balance" to 0.0))
                }
            }
        }
        onDispose { registration?.remove() }
    }

    val items = listOf(
        BottomNavItem("يكسب", Icons.Default.AttachMoney),
        BottomNavItem("المحفظة", Icons.Default.AccountBalanceWallet),
        BottomNavItem("دعوة", Icons.Default.Group),
        BottomNavItem("ملفي الشخصي", Icons.Default.Person)
    )

    Scaffold(
        containerColor = BackgroundTop,
        bottomBar = {
            NavigationBar(
                containerColor = BackgroundTop,
                contentColor = TextWhite,
                tonalElevation = 0.dp
            ) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                        icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                        label = { Text(text = item.title, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Gold,
                            selectedTextColor = Gold,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = BackgroundMiddle
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            when (page) {
                0 -> EarnScreen(
                    currentBalance = userBalance,
                    currentUserId = userId
                )

                1 -> WalletScreen(currentBalance = userBalance, minWithdrawal = minWithdrawal)
                2 -> InviteScreen()
                3 -> ProfileScreen()
            }
        }
    }
}

data class BottomNavItem(val title: String, val icon: ImageVector)