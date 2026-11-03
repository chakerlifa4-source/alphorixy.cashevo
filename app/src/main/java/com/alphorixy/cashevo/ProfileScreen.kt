package com.alphorixy.cashevo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.InputStream

// دالة مساعدة لقراءة النصوص من ملفات مجلد res/raw
fun readRawResource(context: Context, resId: Int): String {
    return try {
        val inputStream: InputStream = context.resources.openRawResource(resId)
        inputStream.bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        "تعذر تحميل النص."
    }
}

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    // التحقق مما إذا كان المستخدم ضيفاً (غير مسجل بحساب دائم)
    val isGuest = currentUser == null || currentUser.isAnonymous

    var activeDialogContent by remember { mutableStateOf<String?>(null) }

    var userName by remember { mutableStateOf(currentUser?.displayName ?: if (isGuest) "زائر (ضيف)" else "مستخدم Cash Evo") }
    var userEmail by remember { mutableStateOf(currentUser?.email ?: if (isGuest) "غير مسجل" else "user@cashevo.app") }
    var userId by remember { mutableStateOf(currentUser?.uid ?: "ID: #GUEST") }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            if (!currentUser.isAnonymous) {
                db.collection("users").document(uid).get().addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("fullName") ?: document.getString("name")
                        if (!name.isNullOrBlank()) userName = name
                        val email = document.getString("email")
                        if (!email.isNullOrBlank()) userEmail = email
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ملفي الشخصي",
                    color = TextWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                // إذا لم يكن مسجلاً (ضيف)، نعرض زر تسجيل الدخول في الأعلى مكان الإعدادات مع تلميح كبير
                if (isGuest) {
                    Button(
                        onClick = {
                            val intent = Intent(context, LoginScreen::class.java)
                            context.startActivity(intent)
                            (context as? Activity)?.finish()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Login,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تسجيل الدخول ⚡",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardColor),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(BackgroundBottom),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Gold,
                            modifier = Modifier.size(45.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = userName,
                        color = TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = userEmail,
                        color = TextMuted,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BackgroundBottom)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isGuest) "حساب ضيف (غير مسجل)" else "ID: #${userId.take(8)}",
                            color = Gold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            Text(
                text = "الحساب والدعم والسياسات",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    ProfileMenuItem(
                        icon = Icons.Default.Help,
                        title = "الدعم الفني",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://email/alphorixy.support@gmail.com"))
                            context.startActivity(intent)
                        }
                    )
                    Divider(color = BackgroundBottom, thickness = 1.dp)

                    ProfileMenuItem(
                        icon = Icons.Default.Security,
                        title = "سياسة الخصوصية",
                        onClick = { activeDialogContent = "privacy" }
                    )
                    Divider(color = BackgroundBottom, thickness = 1.dp)

                    ProfileMenuItem(
                        icon = Icons.Default.Description,
                        title = "شروط الاستخدام",
                        onClick = { activeDialogContent = "terms" }
                    )
                    Divider(color = BackgroundBottom, thickness = 1.dp)

                    ProfileMenuItem(
                        icon = Icons.Default.Cookie,
                        title = "سياسة ملفات تعريف الارتباط",
                        onClick = { activeDialogContent = "cookies" }
                    )
                    Divider(color = BackgroundBottom, thickness = 1.dp)

                    ProfileMenuItem(
                        icon = Icons.Default.Info,
                        title = "عن التطبيق والشركة",
                        onClick = { activeDialogContent = "about" }
                    )

                    // زر تسجيل الخروج يظهر فقط إذا كان المستخدم مسجلاً دخوله بالفعل
                    if (!isGuest) {
                        Divider(color = BackgroundBottom, thickness = 1.dp)
                        ProfileMenuItem(
                            icon = Icons.Default.ExitToApp,
                            title = "تسجيل الخروج",
                            textColor = Color(0xFFFF6B6B),
                            iconColor = Color(0xFFFF6B6B),
                            onClick = {
                                auth.signOut()
                                val intent = Intent(context, LoginScreen::class.java)
                                context.startActivity(intent)
                                (context as? Activity)?.finish()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SocialCircleIcon(iconResId = R.drawable.ic_x) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://x.com/CashEvoSupport")))
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    SocialCircleIcon(iconResId = R.drawable.ic_facebook) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://facebook.com/cashevo.suportt")))
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    SocialCircleIcon(iconResId = R.drawable.ic_instagram) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/cashevo.app")))
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    SocialCircleIcon(iconResId = R.drawable.ic_reddit) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://reddit.com/r/CashEvo")))
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    SocialCircleIcon(iconResId = R.drawable.ic_discord) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/cashevosupport")))
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    SocialCircleIcon(iconResId = R.drawable.ic_whatsapp) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/213552254852")))
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    SocialCircleIcon(iconResId = R.drawable.ic_telegram) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/cashevo_support")))
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                Text(
                    text = "© Cash Evo 2026. جميع الحقوق محفوظة.",
                    color = TextMuted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "إصدار التطبيق: v1.0.0",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // النافذة التفاعلية لملء الشاشة مع التمرير
    if (activeDialogContent != null) {
        val titleText = when (activeDialogContent) {
            "privacy" -> "سياسة الخصوصية"
            "terms" -> "شروط الاستخدام"
            "cookies" -> "سياسة ملفات تعريف الارتباط"
            "about" -> "عن التطبيق والشركة"
            else -> "معلومات"
        }

        val bodyText = when (activeDialogContent) {
            "privacy" -> readRawResource(context, R.raw.privacy_policy)
            "terms" -> readRawResource(context, R.raw.terms_of_service)
            "cookies" -> readRawResource(context, R.raw.cookies_policy)
            "about" -> readRawResource(context, R.raw.app_info)
            else -> ""
        }

        Dialog(
            onDismissRequest = { activeDialogContent = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
            dialogWindow?.setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppBackground),
                color = Color.Transparent
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardColor)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = titleText,
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(onClick = { activeDialogContent = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = TextMuted
                            )
                        }
                    }

                    Divider(color = BackgroundBottom, thickness = 1.dp)

                    val scrollState = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = bodyText,
                            color = TextWhite,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    textColor: Color = TextWhite,
    iconColor: Color = Gold,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SocialCircleIcon(iconResId: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(CardColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            colorFilter = ColorFilter.tint(Color.White)
        )
    }
}