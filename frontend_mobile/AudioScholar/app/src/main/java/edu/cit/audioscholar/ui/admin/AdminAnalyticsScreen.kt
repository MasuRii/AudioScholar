package edu.cit.audioscholar.ui.admin

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import edu.cit.audioscholar.R
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAnalyticsScreen(
    navController: NavController,
    viewModel: AdminAnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_admin_analytics)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_navigate_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = uiState.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadAnalytics() }) {
                        Text("Retry")
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Activity Stats
                    uiState.activityStats?.let { stats ->
                        AnalyticsSection(title = "New Users (Last 30 Days)") {
                            SimpleBarChart(
                                data = stats.newUsersLast30Days,
                                barColor = MaterialTheme.colorScheme.primary
                            )
                        }

                        AnalyticsSection(title = "New Recordings (Last 30 Days)") {
                            SimpleBarChart(
                                data = stats.newRecordingsLast30Days,
                                barColor = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    // User Distribution
                    uiState.userDistribution?.let { dist ->
                        AnalyticsSection(title = "User Distribution by Provider") {
                            SimplePieChart(
                                data = dist.usersByProvider,
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.tertiary,
                                    MaterialTheme.colorScheme.error,
                                    Color.Cyan,
                                    Color.Magenta
                                )
                            )
                        }

                        AnalyticsSection(title = "User Distribution by Role") {
                            SimplePieChart(
                                data = dist.usersByRole,
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    MaterialTheme.colorScheme.tertiaryContainer,
                                    Color.Green
                                )
                            )
                        }
                    }

                    // Content Engagement
                    if (uiState.contentEngagement.isNotEmpty()) {
                        AnalyticsSection(title = "Top Engaging Content (Favorites)") {
                            // Prepare data for bar chart
                            val engagementData = uiState.contentEngagement.associate {
                                (if (it.title.length > 10) it.title.take(10) + "..." else it.title) to it.favoriteCount.toLong()
                            }
                            SimpleBarChart(
                                data = engagementData,
                                barColor = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun AnalyticsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun SimplePieChart(
    data: Map<String, Long>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val total = data.values.sum()
    if (total == 0L) {
        Text("No data available", style = MaterialTheme.typography.bodyMedium)
        return
    }

    val keys = data.keys.toList()
    val values = data.values.toList()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pie Chart
        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartDiameter = size.minDimension
                val radius = chartDiameter / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                
                var startAngle = -90f

                values.forEachIndexed { index, value ->
                    val sweepAngle = (value.toFloat() / total) * 360f
                    val color = colors[index % colors.size]
                    
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(chartDiameter, chartDiameter)
                    )
                    startAngle += sweepAngle
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Legend
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            keys.forEachIndexed { index, key ->
                val count = values[index]
                val percentage = (count.toFloat() / total * 100).toInt()
                val color = colors[index % colors.size]

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$count ($percentage%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleBarChart(
    data: Map<String, Long>,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Text("No data available", style = MaterialTheme.typography.bodyMedium)
        return
    }

    val maxValue = data.values.maxOrNull() ?: 0L
    val effectiveMax = max(maxValue, 5L) // Ensure at least some height
    val keys = data.keys.toList()
    val values = data.values.toList()
    
    // Text paint for axis labels
    val textPaint = remember {
        Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 30f
            textAlign = Paint.Align.CENTER
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val bottomPadding = 80f // Space for labels
            val chartHeight = canvasHeight - bottomPadding
            
            val barWidth = (canvasWidth / values.size) * 0.6f
            val spacing = (canvasWidth / values.size) * 0.4f
            
            values.forEachIndexed { index, value ->
                val barHeight = (value.toFloat() / effectiveMax) * chartHeight
                val left = index * (barWidth + spacing) + (spacing / 2)
                val top = chartHeight - barHeight
                val right = left + barWidth
                val bottom = chartHeight
                
                // Draw Bar
                drawRect(
                    color = barColor,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight)
                )
                
                // Draw Value above bar if enough space and not too many bars
                if (values.size < 15) {
                   drawContext.canvas.nativeCanvas.drawText(
                       value.toString(),
                       left + barWidth / 2,
                       top - 10f,
                       textPaint
                   )
                }

                // Draw Label (simplified - maybe just first letter or index if too crowded)
                // For dates, maybe show every 5th or just Start/End if crowded
                val shouldShowLabel = if (values.size > 15) index % 5 == 0 else true
                
                if (shouldShowLabel) {
                    val label = keys[index]
                    val shortLabel = if (label.length > 5) label.take(3) + ".." else label
                    
                    drawContext.canvas.nativeCanvas.drawText(
                        shortLabel,
                        left + barWidth / 2,
                        canvasHeight - 20f, // Near bottom
                        textPaint
                    )
                }
            }
            
            // Draw baseline
            drawLine(
                color = Color.Gray,
                start = Offset(0f, chartHeight),
                end = Offset(canvasWidth, chartHeight),
                strokeWidth = 2f
            )
        }
    }
}