package com.alphorixy.cashevo

import android.app.Activity
import android.widget.Toast
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

data class TaskItem(
    val id: String,
    val title: String,
    val description: String,
    val reward: Double,
    val icon: ImageVector,
    val type: TaskType,
    val tabCategory: Int
)

data class TaskHistoryItem(
    val userId: String = "",
    val title: String = "",
    val reward: Double = 0.0,
    val timestamp: Long = 0L,
    val type: String = ""
)

enum class TaskType {
    WEEKLY_REWARD,
    REWARDED_VIDEO,
    SURVEY_OFFER,
    COMPLETE_TASK
}

@Composable
fun AdMobBanner(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = "ca-app-pub-2573504616567477/6040596401"
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

@Composable
fun EarnScreen(
    currentBalance: Double,
    currentUserId: String?
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val activeUserId = currentUserId ?: FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val db = FirebaseFirestore.getInstance()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("المكافأة والفيديو", "الاستطلاعات", "إكمال مهام")

    var activeOfferUrl by remember { mutableStateOf<String?>(null) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var taskHistoryList by remember { mutableStateOf<List<TaskHistoryItem>>(emptyList()) }
    var isLoadingHistory by remember { mutableStateOf(false) }

    var lastWeeklyReward by remember { mutableStateOf(0L) }

    LaunchedEffect(activeUserId) {
        if (activeUserId.isNotEmpty()) {
            db.collection("users").document(activeUserId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        lastWeeklyReward = snapshot.getLong("lastWeeklyReward") ?: 0L
                    }
                }
        }
    }

    fun fetchTaskHistory() {
        if (activeUserId.isEmpty()) return
        isLoadingHistory = true
        db.collection("users").document(activeUserId)
            .collection("task_history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapNotNull { doc ->
                    doc.toObject(TaskHistoryItem::class.java)
                }
                taskHistoryList = list
                isLoadingHistory = false
            }
            .addOnFailureListener {
                isLoadingHistory = false
            }
    }

    fun claimWeeklyReward() {
        FirebaseFunctions.getInstance()
            .getHttpsCallable("claimWeeklyReward")
            .call()
            .addOnSuccessListener {
                Toast.makeText(context, "تم استلام المكافأة الأسبوعية بنجاح (+0.06$)", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                val msg = e.message ?: "فشل استلام المكافأة"
                val cleanMsg = if (msg.contains("INTERNAL")) {
                    msg.substringAfterLast(": ").ifEmpty { "لم يحن موعد المكافأة الأسبوعية بعد." }
                } else {
                    msg
                }
                Toast.makeText(context, cleanMsg, Toast.LENGTH_LONG).show()
            }
    }

    // تم إيقاف تحميل الإعلانات بالخلفية مؤقتاً لتوفير البيانات والبطارية
    // val adManager = remember { RewardedAdManager(context) }
    // LaunchedEffect(Unit) {
    //     adManager.loadAd()
    // }

    val tasks = listOf(
        TaskItem("1", "المكافأة الأسبوعية", "احصل على هدية المكافأة الأسبوعية المتجددة", 0.06, Icons.Default.CardGiftcard, TaskType.WEEKLY_REWARD, 0),
        TaskItem("2", "فيديو إعلاني حقيقي", "شاهد إعلان فيديو كاملاً واكسب نقاطاً", 0.05, Icons.Default.PlayCircle, TaskType.REWARDED_VIDEO, 0),
        TaskItem("3", "استطلاعات CPX Research", "أجب عن استطلاعات الرأي واربح أرباحاً حقيقية", 0.50, Icons.Default.Assignment, TaskType.SURVEY_OFFER, 1),
        TaskItem("4", "إكمال مهام التطبيقات", "قم بتثبيت التطبيقات وأتمم التحديات", 1.00, Icons.Default.Task, TaskType.COMPLETE_TASK, 2)
    )

    val filteredTasks = tasks.filter { it.tabCategory == selectedTab }

    if (activeOfferUrl != null) {
        BackHandler {
            activeOfferUrl = null
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundTop)
                .padding(top = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { activeOfferUrl = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)
                ) {
                    Text(text = "إغلاق العروض والعودة", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Text(text = "تضاف الأرباح تلقائياً لحسابك", color = TextMuted, fontSize = 12.sp)
            }

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        loadUrl(activeOfferUrl!!)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
        ) {
            AdMobBanner(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.width(32.dp))
                            Text(text = "الرصيد", color = TextWhite, fontSize = 28.sp)

                            IconButton(
                                onClick = {
                                    fetchTaskHistory()
                                    showHistoryDialog = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "سجل المهام",
                                    tint = Gold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Gold,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$${String.format("%.2f", currentBalance)}",
                                color = Gold,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Gold,
                    edgePadding = 0.dp,
                    indicator = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        val selected = selectedTab == index
                        Tab(
                            selected = selected,
                            onClick = { selectedTab = index },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .background(
                                    color = if (selected) Gold else CardColor,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = title,
                                color = if (selected) Color.Black else TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when (selectedTab) {
                        0 -> "المكافآت والفيديوهات المتاحة"
                        1 -> "استطلاعات الرأي المتاحة"
                        else -> "مهام وتطبيقات إضافية"
                    },
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredTasks) { task ->
                        val isWeeklyTask = task.type == TaskType.WEEKLY_REWARD
                        val nextWeeklyMillis = lastWeeklyReward + (7L * 24 * 60 * 60 * 1000L)
                        val isWeeklyReady = lastWeeklyReward == 0L || System.currentTimeMillis() >= nextWeeklyMillis

                        val nextWeeklyDateFormatted = if (!isWeeklyReady) {
                            SimpleDateFormat("yyyy/MM/dd - hh:mm a", LocalLocale.current.platformLocale).format(Date(nextWeeklyMillis))
                        } else {
                            null
                        }

                        val isEnabled = if (isWeeklyTask) isWeeklyReady else true
                        val actionLabel = if (isWeeklyTask) {
                            if (isWeeklyReady) "استلام (+0.06$)" else "إستلام"
                        } else {
                            "بدأ"
                        }

                        TaskCard(
                            task = task,
                            isEnabled = isEnabled,
                            isDimmed = (task.type == TaskType.REWARDED_VIDEO), // تفعيل التأثير الباهت لمهمة الفيديو فقط
                            actionLabel = actionLabel,
                            nextDateText = nextWeeklyDateFormatted,
                            onAction = onAction@{
                                if (activeUserId.isEmpty()) {
                                    Toast.makeText(context, "جارٍ تسجيل الدخول، أعد المحاولة بعد لحظات", Toast.LENGTH_SHORT).show()
                                    return@onAction
                                }
                                when (task.type) {
                                    TaskType.WEEKLY_REWARD -> {
                                        if (isWeeklyReady) {
                                            claimWeeklyReward()
                                        } else {
                                            Toast.makeText(context, "لم يحن موعد المكافأة الأسبوعية بعد", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    TaskType.REWARDED_VIDEO -> {
                                        Toast.makeText(context, "سوف يتم اضافة مهمة مشاهدة الاعلانات قريبا", Toast.LENGTH_SHORT).show()
                                    }
                                    TaskType.SURVEY_OFFER -> {
                                        activeOfferUrl = "https://offers.cpx-research.com/index.php?app_id=37703&ext_user_id=$activeUserId"
                                    }
                                    TaskType.COMPLETE_TASK -> {
                                        activeOfferUrl = "https://offers.cpx-research.com/index.php?app_id=37703&ext_user_id=$activeUserId"
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            containerColor = CardColor,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "سجل المهام والأرباح", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = { showHistoryDialog = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = TextMuted)
                    }
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoadingHistory) {
                        CircularProgressIndicator(color = Gold)
                    } else if (taskHistoryList.isEmpty()) {
                        Text(text = "لا توجد مهام مسجلة حتى الآن", color = TextMuted, fontSize = 14.sp)
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(taskHistoryList) { historyItem ->
                                HistoryCardItem(item = historyItem)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun TaskCard(
    task: TaskItem,
    isEnabled: Boolean = true,
    isDimmed: Boolean = false, // خاصية المظهر الباهت
    actionLabel: String = "بدأ",
    nextDateText: String? = null,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isDimmed) Modifier.alpha(0.6f) else Modifier), // جعل البطاقة باهتة إذا كانت متوقفة
        colors = CardDefaults.cardColors(
            containerColor = if (isDimmed) Color(0xFF16252C) else CardColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = task.icon,
                contentDescription = null,
                tint = if (isDimmed) Color(0xFF78909C) else Gold,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.title,
                        color = if (isDimmed) TextMuted else TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isDimmed) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(قريباً ⏳)",
                            color = Color(0xFFFFA726),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isDimmed) "هذه المهمة قيد التطوير والتحديث حالياً" else task.description,
                    color = TextMuted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "+$${task.reward}",
                    color = if (isDimmed) TextMuted else SuccessGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                if (!isEnabled && !nextDateText.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = " متاح في: $nextDateText",
                        color = Gold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Button(
                onClick = onAction,
                enabled = isEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDimmed) Color(0xFF263238) else if (isEnabled) Gold else Color(0xFF2C3E50),
                    disabledContainerColor = Color(0xFF2C3E50),
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = actionLabel,
                    color = if (isDimmed) TextMuted else if (isEnabled) Color.Black else TextMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun HistoryCardItem(item: TaskHistoryItem) {
    val formattedDate = remember(item.timestamp) {
        val sdf = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundBottom),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.title, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = formattedDate, color = TextMuted, fontSize = 11.sp)
            }
            Text(
                text = "+$${String.format("%.2f", item.reward)}",
                color = SuccessGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}