package com.alphorixy.cashevo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

class LoginScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppBackground),
                    color = Color.Transparent
                ) {
                    LoginContent()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginContent() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val clientId = stringResource(R.string.default_web_client_id)
    var selectedMethod by remember { mutableStateOf("options") } // "options", "email_login", "email_signup", "forgot_password"

    // حالات البريد الإلكتروني وحساب التسجيل
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }

    var agreedToTerms by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    var showLegalDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogContent by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    // التحكم في زر الرجوع بنظام أندرويد (BackHandler)
    BackHandler(enabled = selectedMethod != "options") {
        when (selectedMethod) {
            "email_signup", "forgot_password" -> selectedMethod = "email_login"
            "email_login" -> selectedMethod = "options"
        }
        errorMessage = null
        successMessage = null
    }

    // ميزة اختفاء رسائل الخطأ والنجاح تلقائياً بعد 4 ثوانٍ
    LaunchedEffect(errorMessage, successMessage) {
        if (errorMessage != null || successMessage != null) {
            delay(4000L)
            errorMessage = null
            successMessage = null
        }
    }

    // حفظ بيانات المستخدم في Firestore
    fun saveUserToFirestore(user: FirebaseUser, providerType: String, customName: String? = null) {
        val userRef = db.collection("users").document(user.uid)
        userRef.get().addOnSuccessListener { document ->
            val nameToSave = customName ?: user.displayName ?: "مستخدم Cash Evo"
            val userData = hashMapOf(
                "uid" to user.uid,
                "email" to (user.email ?: ""),
                "phoneNumber" to (user.phoneNumber ?: ""),
                "displayName" to nameToSave,
                "points" to 0,
                "balanceUSD" to 0.0,
                "provider" to providerType,
                "createdAt" to System.currentTimeMillis()
            )
            userRef.set(userData)
        }
    }

    // الانتقال للواجهة الرئيسية
    fun navigateToMain(user: FirebaseUser, provider: String, customName: String? = null) {
        saveUserToFirestore(user, provider, customName)
        val intent = Intent(context, MainActivity::class.java)
        context.startActivity(intent)
        (context as? Activity)?.finish()
    }

    // Google Auth Launcher
    val googleAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            isLoading = true
            auth.signInWithCredential(credential).addOnCompleteListener { authTask ->
                isLoading = false
                if (authTask.isSuccessful) {
                    authTask.result?.user?.let { navigateToMain(it, "google") }
                } else {
                    errorMessage = authTask.exception?.localizedMessage ?: "فشل تسجيل الدخول بواسطة Google"
                }
            }
        } catch (e: ApiException) {
            isLoading = false
            errorMessage = "إلغاء أو خطأ في الاتصال بجوجل (كود: ${e.statusCode})"
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
            )
    ) {
        // 1. محتوى الشاشة الأساسي
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Cash Evo",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "تسجيل الدخول للمتابعة",
                color = Color(0xFFFFD700),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // عرض رسائل الخطأ
            if (errorMessage != null) {
                Surface(
                    color = Color(0xFFFF6B6B).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFFF6B6B),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // عرض رسائل النجاح
            if (successMessage != null) {
                Surface(
                    color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        text = successMessage!!,
                        color = Color(0xFF4CAF50),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            when (selectedMethod) {
                "options" -> {
                    Button(
                        onClick = {
                            if (!agreedToTerms) {
                                errorMessage = "الرجاء الموافقة على سياسة الخصوصية وشروط الاستخدام أولاً"
                            } else {
                                errorMessage = null
                                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestIdToken(clientId)
                                    .requestEmail()
                                    .build()
                                val client = GoogleSignIn.getClient(context, gso)
                                client.signOut().addOnCompleteListener {
                                    googleAuthLauncher.launch(client.signInIntent)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.google),
                                contentDescription = "Google Logo",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "المتابعة باستخدام Google", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            if (!agreedToTerms) {
                                errorMessage = "الرجاء الموافقة على سياسة الخصوصية وشروط الاستخدام أولاً"
                            } else {
                                errorMessage = null
                                selectedMethod = "email_login"
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "البريد الإلكتروني وكلمة المرور", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = {
                            if (!agreedToTerms) {
                                errorMessage = "الرجاء الموافقة على سياسة الخصوصية وشروط الاستخدام أولاً"
                            } else {
                                isLoading = true
                                auth.signInAnonymously().addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        task.result?.user?.let { navigateToMain(it, "guest") }
                                    } else {
                                        errorMessage = task.exception?.localizedMessage ?: "فشل الدخول كضيف"
                                    }
                                }
                            }
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "المتابعة كضيف (بدون حساب)",
                                color = Color(0xFFFFD700),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                textDecoration = TextDecoration.Underline
                            )
                        }
                    }
                }

                "email_login" -> {
                    Text(
                        text = "تسجيل الدخول",
                        color = Color(0xFFFFD700),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("البريد الإلكتروني", color = Color.White.copy(alpha = 0.7f)) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.White.copy(alpha = 0.7f)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة المرور", color = Color.White.copy(alpha = 0.7f)) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White.copy(alpha = 0.7f)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        TextButton(onClick = { selectedMethod = "forgot_password"; errorMessage = null; successMessage = null }) {
                            Text(text = "نسيت كلمة المرور؟", color = Color(0xFFFFD700), fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = "الرجاء إدخال البريد الإلكتروني وكلمة المرور"
                            } else {
                                isLoading = true
                                errorMessage = null
                                auth.signInWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { task ->
                                        isLoading = false
                                        if (task.isSuccessful) {
                                            task.result?.user?.let { navigateToMain(it, "email") }
                                        } else {
                                            errorMessage = task.exception?.localizedMessage ?: "فشل تسجيل الدخول. تأكد من صحة البيانات."
                                        }
                                    }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "تسجيل الدخول", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = { selectedMethod = "email_signup"; errorMessage = null }) {
                        Text(text = "ليس لديك حساب؟ انقر هنا لإنشاء حساب جديد", color = Color(0xFFFFD700), fontSize = 13.sp)
                    }
                }

                "email_signup" -> {
                    Text(
                        text = "إنشاء حساب جديد",
                        color = Color(0xFFFFD700),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("الاسم الكامل", color = Color.White.copy(alpha = 0.7f)) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.7f)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("البريد الإلكتروني", color = Color.White.copy(alpha = 0.7f)) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.White.copy(alpha = 0.7f)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة المرور", color = Color.White.copy(alpha = 0.7f)) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White.copy(alpha = 0.7f)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("تأكيد كلمة المرور", color = Color.White.copy(alpha = 0.7f)) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White.copy(alpha = 0.7f)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (fullName.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                                errorMessage = "الرجاء تعبئة جميع الحقول المطلوبة"
                            } else if (password != confirmPassword) {
                                errorMessage = "كلمتا المرور غير متطابقتين"
                            } else if (password.length < 6) {
                                errorMessage = "كلمة المرور يجب أن تكون 6 أحرف على الأقل"
                            } else {
                                isLoading = true
                                errorMessage = null
                                auth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { task ->
                                        isLoading = false
                                        if (task.isSuccessful) {
                                            val user = task.result?.user
                                            val profileUpdates = UserProfileChangeRequest.Builder()
                                                .setDisplayName(fullName)
                                                .build()
                                            user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                                                user.sendEmailVerification().addOnCompleteListener { verifyTask ->
                                                    if (verifyTask.isSuccessful) {
                                                        successMessage = "تم إنشاء الحساب وإرسال رابط التحقق إلى بريدك الإلكتروني"
                                                    }
                                                    user.let { navigateToMain(it, "email", fullName) }
                                                }
                                            } ?: user?.let {
                                                user.sendEmailVerification()
                                                navigateToMain(it, "email", fullName)
                                            }
                                        } else {
                                            errorMessage = task.exception?.localizedMessage ?: "فشل إنشاء الحساب"
                                        }
                                    }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "إنشاء الحساب", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = { selectedMethod = "email_login"; errorMessage = null }) {
                        Text(text = "لديك حساب بالفعل؟ تسجيل الدخول", color = Color(0xFFFFD700), fontSize = 13.sp)
                    }
                }

                "forgot_password" -> {
                    Text(
                        text = "استعادة كلمة المرور",
                        color = Color(0xFFFFD700),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = "أدخل بريدك الإلكتروني وسنرسل لك رابطاً حقيقياً لإعادة تعيين كلمة المرور",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("البريد الإلكتروني", color = Color.White.copy(alpha = 0.7f)) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.White.copy(alpha = 0.7f)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (email.isBlank()) {
                                errorMessage = "الرجاء إدخال البريد الإلكتروني أولاً"
                            } else {
                                isLoading = true
                                errorMessage = null
                                auth.sendPasswordResetEmail(email)
                                    .addOnCompleteListener { task ->
                                        isLoading = false
                                        if (task.isSuccessful) {
                                            successMessage = "تم إرسال رابط استعادة كلمة المرور الحقيقي إلى بريدك الإلكتروني"
                                        } else {
                                            errorMessage = task.exception?.localizedMessage ?: "فشل إرسال البريد. تحقق من صحة العنوان."
                                        }
                                    }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "إرسال رابط الاستعادة", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = { selectedMethod = "email_login"; errorMessage = null; successMessage = null }) {
                        Text(text = "العودة لتسجيل الدخول", color = Color(0xFFFFD700), fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                color = Color.White.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = agreedToTerms,
                        onCheckedChange = { agreedToTerms = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFFFFD700),
                            uncheckedColor = Color.White.copy(alpha = 0.6f)
                        )
                    )

                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "أوافق على ", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            Text(
                                text = "شروط الاستخدام",
                                color = Color(0xFFFFD700),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable {
                                    dialogTitle = "شروط الاستخدام"
                                    dialogContent = try {
                                        context.resources.openRawResource(R.raw.terms_of_service).bufferedReader().use { it.readText() }
                                    } catch (e: Exception) {
                                        "مرحبًا بك في تطبيق Cash Evo..."
                                    }
                                    showLegalDialog = true
                                }
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "و ", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            Text(
                                text = "سياسة الخصوصية",
                                color = Color(0xFFFFD700),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable {
                                    dialogTitle = "سياسة الخصوصية"
                                    dialogContent = try {
                                        context.resources.openRawResource(R.raw.privacy_policy).bufferedReader().use { it.readText() }
                                    } catch (e: Exception) {
                                        "نحن نحترم خصوصيتك..."
                                    }
                                    showLegalDialog = true
                                }
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(color = Color(0xFFFFD700))
            }
        }

        // 2. زر الرجوع العلوي (تم وضعه هنا ليكون في الطبقة العليا فوق الـ Column ويستجيب للضغط تماماً)
        if (selectedMethod != "options") {
            IconButton(
                onClick = {
                    when (selectedMethod) {
                        "email_signup", "forgot_password" -> selectedMethod = "email_login"
                        "email_login" -> selectedMethod = "options"
                    }
                    errorMessage = null
                    successMessage = null
                },
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 8.dp)
                    .align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = Color.White
                )
            }
        }
    }

    if (showLegalDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showLegalDialog = false }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E293B)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                ) {
                    Text(
                        text = dialogTitle,
                        color = Color(0xFFFFD700),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val dialogScrollState = rememberScrollState()
                    Box(
                        modifier = Modifier.weight(1f).verticalScroll(dialogScrollState)
                    ) {
                        Text(
                            text = dialogContent,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showLegalDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "إغلاق", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}