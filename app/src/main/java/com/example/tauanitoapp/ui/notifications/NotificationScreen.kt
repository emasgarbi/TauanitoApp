package com.example.tauanitoapp.ui.notifications

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tauanitoapp.R
import com.example.tauanitoapp.data.repository.NotificationItem
import com.example.tauanitoapp.data.repository.NotificationRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    val notifications by NotificationRepository.notifications.collectAsState()
    
    // Segna tutto come letto all'apertura
    LaunchedEffect(Unit) {
        NotificationRepository.markAllAsRead()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.immaginesfondo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        
        val overlayColor = if (isDarkMode) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.75f)
        val contentColor = if (isDarkMode) Color.White else Color.Black
        
        Box(modifier = Modifier.matchParentSize().background(overlayColor))

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Centro Notifiche", color = contentColor) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = contentColor)
                        }
                    },
                    actions = {
                        if (notifications.isNotEmpty()) {
                            IconButton(onClick = { NotificationRepository.clearAll() }) {
                                Icon(Icons.Default.Delete, contentDescription = "Svuota tutto", tint = contentColor)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            if (notifications.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = contentColor.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Nessuna notifica presente",
                        color = contentColor.copy(alpha = 0.5f),
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notifications) { notification ->
                        NotificationCard(notification, isDarkMode, contentColor)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: NotificationItem,
    isDarkMode: Boolean,
    contentColor: Color
) {
    val cardBg = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)
    val aiAccent = Color(0xFF00ACC1)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Pallino rosso per notifiche non lette
                if (!notification.isRead) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp, end = 12.dp)
                            .size(8.dp)
                            .background(Color.Red, CircleShape)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = notification.title,
                            color = if (isDarkMode) Color(0xFF4CAF50) else Color(0xFF1B5E20),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = notification.timestamp,
                            color = contentColor.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = notification.body,
                        color = contentColor,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
            
            // Insight IA
            if (notification.aiAdvice != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(aiAccent.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = null,
                                tint = aiAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Tauanito AI Insight",
                                color = aiAccent,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = notification.aiAdvice,
                            color = contentColor.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
