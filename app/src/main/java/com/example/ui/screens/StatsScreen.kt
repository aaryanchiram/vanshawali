package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FamilyMember
import com.example.ui.viewmodel.FamilyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: FamilyViewModel,
    modifier: Modifier = Modifier
) {
    val isHindi by viewModel.isHindi.collectAsState()
    val allMembers by viewModel.allMembers.collectAsState()

    val scrollState = rememberScrollState()

    // Dynamically calculate statistics
    val totalCount = allMembers.size
    val livingCount = allMembers.count { !it.isDeceased }
    val deceasedCount = allMembers.count { it.isDeceased }
    
    val generations = allMembers.map { it.generation }.distinct().sorted()
    val generationCount = generations.size

    val maleCount = allMembers.count { it.gender.equals("Male", ignoreCase = true) }
    val femaleCount = allMembers.count { it.gender.equals("Female", ignoreCase = true) }

    // Generational breakdown distribution
    val membersPerGeneration = remember(allMembers) {
        allMembers.groupBy { it.generation }
            .mapValues { it.value.size }
            .toSortedMap()
    }
    val maxGenSize = remember(membersPerGeneration) {
        membersPerGeneration.values.maxOrNull() ?: 1
    }

    // Careers breakdown distribution
    val occupationsDistribution = remember(allMembers) {
        allMembers
            .map { it.occupation?.trim()?.ifBlank { null } }
            .filterNotNull()
            .groupBy { it }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(5)
    }
    val maxOccSize = remember(occupationsDistribution) {
        occupationsDistribution.firstOrNull()?.value ?: 1
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isHindi) "कुल पारिवारिक आँकड़े" else "Family Insights",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleLanguage() }) {
                        Text(
                            text = if (isHindi) "EN" else "हिं",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (allMembers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Equalizer,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isHindi) "आँकड़े अभी उपलब्ध नहीं हैं" else "No Stats Available",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = if (isHindi) "आँकड़े प्राप्त करने के लिए पहले सदस्य जोड़ें।" else "Add family members first to see insights.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- GRID CARDS: NUMERICAL STATS ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = if (isHindi) "कुल सदस्य" else "Total Members",
                        value = totalCount.toString(),
                        icon = Icons.Default.Groups,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = if (isHindi) "कुल पीढ़ियां" else "Generations",
                        value = generationCount.toString(),
                        icon = Icons.Default.AccountTree,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = if (isHindi) "जीवित सदस्य" else "Living Members",
                        value = livingCount.toString(),
                        icon = Icons.Default.Favorite,
                        color = Color(0xFFDCFCE7), // Soft Green
                        textColor = Color(0xFF15803D),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = if (isHindi) "स्वर्गवासी" else "Deceased",
                        value = deceasedCount.toString(),
                        icon = Icons.Default.NightsStay,
                        color = Color(0xFFFEE2E2), // Soft Red
                        textColor = Color(0xFFB91C1C),
                        modifier = Modifier.weight(1f)
                    )
                }

                // --- GENDER DISTRIBUTION ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isHindi) "लिंग अनुपात वितरण" else "Gender Diversity Ratio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        val totalGender = (maleCount + femaleCount).toFloat()
                        val malePercent = if (totalGender > 0) (maleCount / totalGender) else 0.5f
                        val femalePercent = if (totalGender > 0) (femaleCount / totalGender) else 0.5f

                        // Bar Segment Indicator
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray.copy(alpha = 0.3f))
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                // Male part
                                if (malePercent > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(malePercent)
                                            .background(Color(0xFF1D4ED8)) // Blue
                                    )
                                }
                                // Female part
                                if (femalePercent > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(femalePercent)
                                            .background(Color(0xFFBE185D)) // Pink
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(12.dp).background(Color(0xFF1D4ED8), CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${if (isHindi) "पुरुष" else "Male"}: $maleCount (${String.format("%.0f", malePercent * 100)}%)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(12.dp).background(Color(0xFFBE185D), CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${if (isHindi) "महिला" else "Female"}: $femaleCount (${String.format("%.0f", femalePercent * 100)}%)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // --- GENERATIONAL INSIGHTS CHART ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isHindi) "पीढ़ी वार सदस्य संख्या" else "Members by Generation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            membersPerGeneration.forEach { (gen, count) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${if (isHindi) "पीढ़ी" else "Gen"} $gen",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.width(55.dp)
                                    )

                                    val fillRatio = count.toFloat() / maxGenSize
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(20.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(fillRatio)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }

                                    Text(
                                        text = count.toString(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier
                                            .width(35.dp)
                                            .padding(start = 8.dp),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }
                }

                // --- CAREER DISTRIBUTION ---
                if (occupationsDistribution.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (isHindi) "शीर्ष ५ पारिवारिक व्यवसाय" else "Top 5 Occupations",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                occupationsDistribution.forEach { (occ, count) ->
                                    val localOcc = if (isHindi) {
                                        when (occ.lowercase()) {
                                            "teacher" -> "शिक्षक (Teacher)"
                                            "farmer" -> "कृषक (Farmer)"
                                            "engineer" -> "अभियंता (Engineer)"
                                            "business" -> "व्यवसाय (Business)"
                                            "software engineer" -> "तकनीकी विशेषज्ञ / कोडर"
                                            "home maker" -> "गृहणी (Home Maker)"
                                            "government service" -> "शासकीय सेवा"
                                            "college student" -> "महाविद्यालय छात्र"
                                            else -> occ
                                        }
                                    } else occ

                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = localOcc,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = count.toString(),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { count.toFloat() / maxOccSize },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor.copy(alpha = 0.8f)
                )
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(textColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
