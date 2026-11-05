package com.alphorixy.cashevo

import android.R.attr.fontWeight
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

// نموذج بيانات طلب السحب لعرضه في السجل
data class WithdrawalItem(
    val id: String = "",
    val fullName: String = "",
    val accountNumber: String = "",
    val amount: Double = 0.0,
    val paymentMethod: String = "",
    val status: String = "pending",
    val createdAt: Timestamp? = null
)

@Composable
fun WalletScreen(
    currentBalance: Double,
    minWithdrawal: Double,
    onBalanceUpdated: () -> Unit = {}
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "test_user_1"

    var showWithdrawDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    val progress = (currentBalance / minWithdrawal).toFloat().coerceIn(0f, 1f)

    // حالات الحقول داخل النافذة المنبثقة لطلب السحب
    var fullName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var confirmAccountNumber by remember { mutableStateOf("") } // حقل مطابقة رقم الحساب
    var withdrawAmount by remember { mutableStateOf(currentBalance.toString()) }
    var selectedMethod by remember { mutableStateOf("PayPal") }
    var isLoading by remember { mutableStateOf(false) }

    // قائمة سجل طلبات السحب الخاصة بالمستخدم
    var withdrawalHistory by remember { mutableStateOf<List<WithdrawalItem>>(emptyList()) }

    // جلب سجل المعاملات والطلبات السابقة من فايربيس بشكل حي
    LaunchedEffect(currentUserId) {
        db.collection("withdrawals")
            .whereEqualTo("userId", currentUserId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    withdrawalHistory = snapshot.documents.map { doc ->
                        WithdrawalItem(
                            id = doc.id,
                            fullName = doc.getString("fullName") ?: "",
                            accountNumber = doc.getString("accountNumber") ?: "",
                            amount = doc.getDouble("amount") ?: 0.0,
                            paymentMethod = doc.getString("paymentMethod") ?: "",
                            status = doc.getString("status") ?: "pending",
                            createdAt = doc.getTimestamp("createdAt")
                        )
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
        // 1. عنوان الشاشة
        item {
            Text(
                text = "المحفظة والأرباح",
                color = TextWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 2. بطاقة الرصيد وشريط التقدم للسحب
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardColor),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "الرصيد المتاح للسحب", color = TextMuted, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$${String.format("%.2f", currentBalance)}",
                                color = Gold,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(BackgroundBottom),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Gold,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "الحد الأدنى للسحب: $$minWithdrawal", color = TextMuted, fontSize = 12.sp)
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                color = Gold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Gold,
                            trackColor = BackgroundBottom
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (currentBalance < minWithdrawal) {
                                Toast.makeText(context, "رصيدك أقل من الحد الأدنى للسحب ($$minWithdrawal)", Toast.LENGTH_SHORT).show()
                            } else {
                                withdrawAmount = currentBalance.toString()
                                showWithdrawDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text(
                            text = "طلب سحب الأرباح",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 3. طرق السحب المتاحة
        item {
            Text(
                text = "طرق السحب المتاحة",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PaymentMethodCard(name = "PayPal", icon = "🅿️", modifier = Modifier.weight(1f))
                PaymentMethodCard(name = "USDT", icon = "🪙", modifier = Modifier.weight(1f))
                PaymentMethodCard(name = "BaridiMob", icon = "🇩🇿", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 4. سجل الطلبات والمعاملات السابقة المفصل
        item {
            Text(
                text = "سجل الطلبات والمعاملات السابقة",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (withdrawalHistory.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد معاملات سابقة حتى الآن",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            items(withdrawalHistory) { req ->
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", LocalLocale.current.platformLocale)
                val dateStr = req.createdAt?.toDate()?.let { dateFormat.format(it) } ?: "جاري المزامنة..."

                val statusText = when(req.status) {
                    "approved" -> "مقبول ✅"
                    "rejected" -> "مرفوض ❌"
                    else -> "قيد الانتظار ⏳"
                }
                val statusColor = when(req.status) {
                    "approved" -> SuccessGreen
                    "rejected" -> Color.Red
                    else -> Gold
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = CardColor),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "طريقة الدفع: ${req.paymentMethod}", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = statusText, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text(text = "المبلغ المطلوب: $${String.format("%.2f", req.amount)}", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(text = "الاسم الكامل: ${req.fullName}", color = TextMuted, fontSize = 12.sp)
                        Text(text = "رقم الحساب: ${req.accountNumber}", color = TextMuted, fontSize = 12.sp)
                        Text(text = "تاريخ الطلب: $dateStr", color = TextMuted, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    // النافذة الأولى: إدخال معلومات السحب وطريقة الدفع ومطابقة الحساب
    if (showWithdrawDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoading) showWithdrawDialog = false },
            containerColor = CardColor,
            title = {
                Text(text = "تقديم طلب سحب جديد", color = TextWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "اختر طريقة الدفع وأدخل جميع البيانات المطلوبة بدقة:", color = TextMuted, fontSize = 12.sp)

                    // أزرار اختيار طريقة الدفع
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("PayPal", "USDT", "BaridiMob").forEach { method ->
                            val isSelected = selectedMethod == method
                            OutlinedButton(
                                onClick = { selectedMethod = method },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) Gold else CardColor,
                                    contentColor = if (isSelected) Color.Black else TextWhite
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(4.dp)
                            ) {
                                Text(text = method, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // حقل الاسم الكامل
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("الاسم الكامل لصاحب الحساب") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = TextMuted,
                            focusedLabelColor = Gold,
                            unfocusedLabelColor = TextMuted,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        )
                    )

                    // حقل رقم الحساب الأساسي
                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it },
                        label = { Text("رقم الحساب / البريد الإلكتروني") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = TextMuted,
                            focusedLabelColor = Gold,
                            unfocusedLabelColor = TextMuted,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        )
                    )

                    // حقل تأكيد رقم الحساب الثاني للمطابقة
                    OutlinedTextField(
                        value = confirmAccountNumber,
                        onValueChange = { confirmAccountNumber = it },
                        label = { Text("تأكيد رقم الحساب / البريد مرة أخرى") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = TextMuted,
                            focusedLabelColor = Gold,
                            unfocusedLabelColor = TextMuted,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        )
                    )

                    // حقل المبلغ المراد سحبه
                    OutlinedTextField(
                        value = withdrawAmount,
                        onValueChange = { withdrawAmount = it },
                        label = { Text("المبلغ المراد سحبه ($)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = TextMuted,
                            focusedLabelColor = Gold,
                            unfocusedLabelColor = TextMuted,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = withdrawAmount.toDoubleOrNull() ?: 0.0

                        // 1. لزوم ملء جميع الحقول
                        if (fullName.isBlank() || accountNumber.isBlank() || confirmAccountNumber.isBlank() || amount <= 0) {
                            Toast.makeText(context, "الرجاء تعبئة جميع الحقول وإدخال مبلغ صحيح", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        // 2. التحقق من مطابقة رقم الحساب
                        if (accountNumber != confirmAccountNumber) {
                            Toast.makeText(context, "رقم الحساب غير متطابق! تأكد من إعادة إدخاله بدقة.", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        // 3. التحقق من الحد الأدنى للسحب
                        if (amount < minWithdrawal) {
                            Toast.makeText(context, "المبلغ أقل من الحد الأدنى للسحب ($$minWithdrawal)", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        // 4. التحقق من أن المبلغ لا يتجاوز رصيد المستخدم
                        if (amount > currentBalance) {
                            Toast.makeText(context, "المبلغ المطلوب أكبر من رصيدك الحالي! تم إلغاء الطلب.", Toast.LENGTH_LONG).show()
                            showWithdrawDialog = false
                            return@Button
                        }

                        // الانتقال لنافذة التأكيد النهائية
                        showWithdrawDialog = false
                        showConfirmDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)
                ) {
                    Text("متابعة للمراجعة والتأكيد", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawDialog = false }) {
                    Text("إلغاء", color = TextMuted)
                }
            }
        )
    }

    if (showConfirmDialog) {
        val amount = withdrawAmount.toDoubleOrNull() ?: 0.0
        AlertDialog(
            onDismissRequest = { if (!isLoading) showConfirmDialog = false },
            containerColor = CardColor,
            title = {
                Text(text = "مراجعة وتأكيد طلب السحب", color = TextWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "الرجاء مراجعة معلومات طلبك بعناية قبل التأكيد النهائي:", color = TextMuted, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    DetailRow("طريقة الدفع:", selectedMethod)
                    DetailRow("الاسم الكامل:", fullName)
                    DetailRow("رقم الحساب:", accountNumber)
                    DetailRow("المبلغ المطلوب:", "$${String.format("%.2f", amount)}")
                    DetailRow("حالة الطلب:", "قيد المراجعة اليدوية (Pending)")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isLoading = true

                        val userRef = db.collection("users").document(currentUserId)
                        val withdrawalRef = db.collection("withdrawals").document()

                        db.runTransaction { transaction ->
                            val snapshot = transaction.get(userRef)
                            val currentDbBalance = snapshot.getDouble("balance") ?: 0.0

                            if (currentDbBalance < amount) {
                                throw IllegalStateException("عذراً، رصيدك الحالي غير كافٍ أو تم تغييره.")
                            }

                            val newBalance = currentDbBalance - amount

                            transaction.update(userRef, "balance", newBalance)

                            val withdrawalData = mapOf(
                                "userId" to currentUserId,
                                "fullName" to fullName,
                                "accountNumber" to accountNumber,
                                "amount" to amount,
                                "paymentMethod" to selectedMethod,
                                "status" to "pending",
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                            transaction.set(withdrawalRef, withdrawalData)

                            newBalance
                        }.addOnSuccessListener { _ ->
                            isLoading = false
                            showConfirmDialog = false
                            Toast.makeText(context, "تم إرسال طلب السحب بنجاح وخصم المبلغ من رصيدك!", Toast.LENGTH_LONG).show()
                            onBalanceUpdated()
                        }.addOnFailureListener { e ->
                            isLoading = false
                            Toast.makeText(context, "فشل إتمام العملية: ${e.localizedMessage ?: "حدث خطأ غير معروف"}", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                    } else {
                        Text("تأكيد نهائي وإرسال الطلب", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { if (!isLoading) { showConfirmDialog = false; showWithdrawDialog = true } },
                    enabled = !isLoading
                ) {
                    Text("تعديل البيانات", color = TextMuted)
                }
            }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextMuted, fontSize = 13.sp)
        Text(text = value, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PaymentMethodCard(name: String, icon: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = name,
                color = TextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}