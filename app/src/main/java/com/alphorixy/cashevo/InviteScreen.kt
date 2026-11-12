package com.alphorixy.cashevo

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

@Composable
fun InviteScreen() {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "test_user_1"

    var referralCode by remember { mutableStateOf("CE-...") }
    var referredCount by remember { mutableStateOf(0) }
    var referralEarnings by remember { mutableStateOf(0.0) }
    var hasUsedReferral by remember { mutableStateOf(false) }
    var inputCode by remember { mutableStateOf("") }
    var isApplyingCode by remember { mutableStateOf(false) }

    LaunchedEffect(currentUserId) {
        val userRef = db.collection("users").document(currentUserId)

        userRef.addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                val code = snapshot.getString("referralCode")
                if (!code.isNullOrBlank()) {
                    referralCode = code
                } else {
                    val generatedCode = "CE-" + currentUserId.take(6).uppercase(Locale.getDefault())
                    userRef.update("referralCode", generatedCode)
                    referralCode = generatedCode
                }
                referralEarnings = snapshot.getDouble("referralEarnings") ?: 0.0
                referredCount = snapshot.getLong("referralCount")?.toInt() ?: 0
                hasUsedReferral = snapshot.getBoolean("hasUsedReferral") ?: false
            }
        }
    }

    val referralLink = "https://cashevo.app/invite?ref=$referralCode"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "دعوة الأصدقاء",
                color = TextWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
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
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(BackgroundBottom),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = Gold,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "دعوة أصدقائك واربح 10%",
                        color = TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "احصل على عمولة بنسبة 10% من كافة الأرباح التي يحققها أصدقاؤك عند التسجيل عبر رابط الإحالة الخاص بك.",
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // بطاقة إدخال رمز دعوة صديق (تظهر فقط إذا لم يسبق للمستخدم إدخال رمز من قبل)
        if (!hasUsedReferral) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "هل تمت دعوتك من قِبل صديق؟",
                            color = TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "أدخل رمز دعوة صديقك لتحصل على المكافأة وتفعيل النظام بنجاح",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputCode,
                                onValueChange = { inputCode = it.uppercase() },
                                placeholder = { Text("رمز الدعوة (مثل: CE-ABC123)", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Gold,
                                    unfocusedBorderColor = TextMuted,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (inputCode.isBlank() || isApplyingCode) return@Button
                                    isApplyingCode = true

                                    db.collection("users")
                                        .whereEqualTo("referralCode", inputCode.trim())
                                        .get()
                                        .addOnSuccessListener { query ->
                                            if (query.isEmpty) {
                                                isApplyingCode = false
                                                Toast.makeText(context, "رمز الدعوة غير صحيح!", Toast.LENGTH_SHORT).show()
                                                return@addOnSuccessListener
                                            }
                                            val referrerDoc = query.documents[0]
                                            val referrerId = referrerDoc.id
                                            if (referrerId == currentUserId) {
                                                isApplyingCode = false
                                                Toast.makeText(context, "لا يمكنك استخدام رمز الدعوة الخاص بك!", Toast.LENGTH_SHORT).show()
                                                return@addOnSuccessListener
                                            }

                                            val currentUserRef = db.collection("users").document(currentUserId)
                                            val referrerRef = db.collection("users").document(referrerId)

                                            db.runTransaction { tx ->
                                                tx.update(referrerRef, "referralCount", com.google.firebase.firestore.FieldValue.increment(1))
                                                tx.update(currentUserRef, "invitedBy", referrerId)
                                                tx.update(currentUserRef, "hasUsedReferral", true)
                                            }.addOnSuccessListener {
                                                isApplyingCode = false
                                                hasUsedReferral = true
                                                Toast.makeText(context, "تم تفعيل رمز الدعوة بنجاح!", Toast.LENGTH_SHORT).show()
                                            }.addOnFailureListener {
                                                isApplyingCode = false
                                                Toast.makeText(context, "فشل تفعيل الرمز: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .addOnFailureListener {
                                            isApplyingCode = false
                                            Toast.makeText(context, "تعذر التحقق من الرمز", Toast.LENGTH_SHORT).show()
                                        }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Gold),
                                enabled = !isApplyingCode
                            ) {
                                Text("تفعيل", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(text = "رمز الدعوة الخاص بك", color = TextMuted, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = referralCode,
                            color = Gold,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Referral Code", referralCode)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ رمز الدعوة بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BackgroundBottom),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "نسخ الرمز", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Button(
                onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "انضم إلي الآن في تطبيق Cash Evo واجمع الأرباح معنا! استخدم رابط الدعوة الخاص بي: $referralLink")
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Gold),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(
                    text = "مشاركة رابط الدعوة",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text(
                text = "إحصائيات الإحالات",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(title = "الأصدقاء المدعوون", value = referredCount.toString(), modifier = Modifier.weight(1f))
                StatCard(title = "أرباح الإحالات", value = "$${String.format(LocalLocale.current.platformLocale, "%.2f", referralEarnings)}", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = CardColor
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = Gold,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
