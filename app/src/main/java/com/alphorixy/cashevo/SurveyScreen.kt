package com.alphorixy.cashevo

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// نموذج بيانات استطلاع الرأي
data class SurveyItem(
    val id: String,
    val title: String,
    val duration: String,
    val reward: Double,
    val surveyUrl: String
)

@Composable
fun SurveyScreen(
    currentBalance: Double,
    onSurveyCompleted: (Double) -> Unit
) {
    val context = LocalContext.current

    // قائمة استطلاعات رأي حقيقية كمثال (يمكن ربطها لاحقاً بشبكات مثل CPX Research)
    val surveys = listOf(
        SurveyItem(
            id = "1",
            title = "استطلاع رأي المستهلك الرقمي",
            duration = "أضف 5 دقائق",
            reward = 0.60,
            surveyUrl = "https://www.google.com" // رابط الاستطلاع أو شاشة الـ Offerwall
        ),
        SurveyItem(
            id = "2",
            title = "استبيان تقنيات الجوال الذكية",
            duration = "أضف 10 دقائق",
            reward = 1.20,
            surveyUrl = "https://www.google.com"
        ),
        SurveyItem(
            id = "3",
            title = "رأي العملاء في خدمات الدفع الإلكتروني",
            duration = "أضف 7 دقائق",
            reward = 0.85,
            surveyUrl = "https://www.google.com"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(16.dp)
    ) {
        // بطاقة الرصيد العلوية
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
                Text(text = "رصيدك السحابي الحالي", color = TextMuted, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$${String.format("%.2f", currentBalance)}",
                    color = Gold,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "استطلاعات الرأي المتاحة لك",
            color = TextWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // قائمة الاستطلاعات
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(surveys) { survey ->
                SurveyCard(
                    survey = survey,
                    onStartSurvey = {
                        // فتح رابط الاستطلاع في المتصفح أو محاكاة إكماله
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(survey.surveyUrl))
                        context.startActivity(intent)

                        // إضافة المكافأة مباشرة وتحديثها في سرفر Firestore
                        onSurveyCompleted(survey.reward)
                    }
                )
            }
        }
    }
}

@Composable
fun SurveyCard(survey: SurveyItem, onStartSurvey: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Assignment,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(text = survey.title, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Gold, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = survey.duration, color = TextMuted, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "+$${survey.reward}", color = SuccessGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onStartSurvey,
                colors = ButtonDefaults.buttonColors(containerColor = Gold),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "ابدأ", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}