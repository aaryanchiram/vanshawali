package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FamilyMember
import com.example.data.SecurityHelper

enum class MilestoneType {
    BIRTH,
    DEATH,
    MIGRATION,
    ANNIVERSARY,
    CUSTOM
}

data class FamilyMilestone(
    val year: Int,
    val dateDisplay: String,
    val member: FamilyMember,
    val type: MilestoneType,
    val title: String,
    val description: String
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FamilyTimelinePane(
    members: List<FamilyMember>,
    userPin: String,
    selectedMemberId: Long?,
    onChooseMember: (FamilyMember) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTypeFilter by remember { mutableStateOf<MilestoneType?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Extract milestones
    val allMilestones = remember(members, userPin) {
        extractMilestones(members, userPin)
    }

    // Filter milestones
    val filteredMilestones = remember(allMilestones, selectedTypeFilter, searchQuery) {
        allMilestones.filter { milestone ->
            val matchType = selectedTypeFilter == null || milestone.type == selectedTypeFilter
            val matchSearch = searchQuery.isBlank() || 
                milestone.member.fullName.contains(searchQuery, ignoreCase = true) ||
                milestone.title.contains(searchQuery, ignoreCase = true) ||
                milestone.description.contains(searchQuery, ignoreCase = true) ||
                milestone.year.toString().contains(searchQuery)
            matchType && matchSearch
        }
    }

    val listState = rememberLazyListState()

    // Automatically scroll to highlight selected member milestone when changed
    LaunchedEffect(selectedMemberId) {
        if (selectedMemberId != null) {
            val idx = filteredMilestones.indexOfFirst { it.member.id == selectedMemberId }
            if (idx >= 0) {
                listState.animateScrollToItem(idx)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        // Timeline Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Route,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "ऐतिहासिक घटनाक्रम (Timeline)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "${filteredMilestones.size} घटनाएं",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("घटनाक्रम खोजें... (उदा: १९९५, दिल्ली, रमेश)", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, null, modifier = Modifier.size(18.dp)) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Clear, null, modifier = Modifier.size(16.dp))
                    }
                }
            } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filters Flow Row
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedTypeFilter == null,
                onClick = { selectedTypeFilter = null },
                label = { Text("सभी", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                border = null
            )

            listOf(
                MilestoneType.BIRTH to "जन्म (Birth)",
                MilestoneType.DEATH to "स्वर्गवास (Death)",
                MilestoneType.MIGRATION to "स्थानांतरण (Migration)",
                MilestoneType.ANNIVERSARY to "विवाह (Anniversary)"
            ).forEach { (type, HindiLabel) ->
                FilterChip(
                    selected = selectedTypeFilter == type,
                    onClick = { selectedTypeFilter = type },
                    label = { Text(HindiLabel, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = getMilestoneColor(type),
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = null
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredMilestones.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "कोई घटनाक्रम नहीं मिला!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "टिप: सदस्य सम्पादन में टिप्पणी (Notes) वाले बॉक्स में कोई विशेष वर्ष दर्ज करें जैसे: \"१९९५: वाराणसी से अहमदाबाद प्रस्थान\" या \"२००२: स्वर्ण विवाह उत्सव\" तो ऐतिहासिक घटनाक्रम यहां स्वतः सिंक होकर दिखाई देगा।",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        lineHeight = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                itemsIndexed(filteredMilestones) { index, milestone ->
                    val isSelected = selectedMemberId == milestone.member.id
                    TimelineRow(
                        milestone = milestone,
                        isLast = index == filteredMilestones.lastIndex,
                        isSelected = isSelected,
                        onClick = { onChooseMember(milestone.member) }
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineRow(
    milestone: FamilyMilestone,
    isLast: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val themeColor = getMilestoneColor(milestone.type)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isSelected) themeColor.copy(alpha = 0.12f) else Color.Transparent)
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Vertical Timeline line and type icon
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(themeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getMilestoneIcon(milestone.type),
                    contentDescription = milestone.title,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(54.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                )
            }
        }

        // Details Container
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                }
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = milestone.year.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = themeColor
                    )
                    Text(
                        text = milestone.title,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColor,
                        modifier = Modifier
                            .background(themeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = milestone.member.fullName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = milestone.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

// Color coding for event types
fun getMilestoneColor(type: MilestoneType): Color {
    return when (type) {
        MilestoneType.BIRTH -> Color(0xFF2E7D32)      // Green for birth
        MilestoneType.DEATH -> Color(0xFFC62828)      // Red for death remembrance
        MilestoneType.MIGRATION -> Color(0xFF1565C0)  // Blue for migration/travel
        MilestoneType.ANNIVERSARY -> Color(0xFFD84315)  // Terracotta sunset / Orange for Marriage
        MilestoneType.CUSTOM -> Color(0xFF6A1B9A)     // Purple for custom
    }
}

// Icons for each type
fun getMilestoneIcon(type: MilestoneType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        MilestoneType.BIRTH -> Icons.Filled.Cake
        MilestoneType.DEATH -> Icons.Filled.FavoriteBorder
        MilestoneType.MIGRATION -> Icons.Filled.FlightTakeoff
        MilestoneType.ANNIVERSARY -> Icons.Filled.CallMerge
        MilestoneType.CUSTOM -> Icons.Filled.Star
    }
}

// Extraction logic to scan dates and notes to form highly accurate chronological lists
fun extractMilestones(members: List<FamilyMember>, userPin: String): List<FamilyMilestone> {
    val milestones = mutableListOf<FamilyMilestone>()

    // Normalize Devnagari digits to Western Arabic digits for standard parsing
    fun normalizeDigits(input: String): String {
        var output = input
        val hindiDigits = charArrayOf('०', '१', '२', '३', '४', '५', '६', '७', '८', '९')
        for (i in 0..9) {
            output = output.replace(hindiDigits[i], '0' + i)
        }
        return output
    }

    // Look for standard or non-standard years (between 1500 and 2100)
    fun findYearInText(text: String): Int? {
        val normalized = normalizeDigits(text)
        val regex = "(\\d{4})".toRegex()
        val matches = regex.findAll(normalized)
        for (match in matches) {
            val yr = match.value.toIntOrNull()
            if (yr != null && yr in 1500..2100) {
                return yr
            }
        }
        return null
    }

    for (m in members) {
        // 1. Birth
        if (m.birthDate.isNotBlank()) {
            val yr = findYearInText(m.birthDate)
            if (yr != null) {
                val loc = if (m.birthPlace.isNotBlank()) "जन्म स्थान: ${m.birthPlace}" else "जन्म स्थान: अन्य"
                milestones.add(
                    FamilyMilestone(
                        year = yr,
                        dateDisplay = m.birthDate,
                        member = m,
                        type = MilestoneType.BIRTH,
                        title = "जन्म (Birth)",
                        description = "वंशावली प्रारंभ - $loc"
                    )
                )
            }
        }

        // 2. Death
        if (m.deathDate.isNotBlank()) {
            val yr = findYearInText(m.deathDate)
            if (yr != null) {
                milestones.add(
                    FamilyMilestone(
                        year = yr,
                        dateDisplay = m.deathDate,
                        member = m,
                        type = MilestoneType.DEATH,
                        title = "स्वर्गवास (Passing)",
                        description = "${m.fullName} का सलोक गमन / स्मरण दिवस"
                    )
                )
            }
        }

        // 4. Parse custom migration / anniversary notes from notes field
        if (m.notes.isNotBlank()) {
            try {
                val decrypted = SecurityHelper.decrypt(m.notes, userPin)
                if (decrypted.isNotBlank()) {
                    val lines = decrypted.split("\n", "\r")
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.isBlank()) continue
                        val yr = findYearInText(trimmed)
                        if (yr != null) {
                            val lower = trimmed.lowercase()
                            val type = when {
                                lower.contains("migrate") || lower.contains("moved") || lower.contains("settled") ||
                                trimmed.contains("स्थानांतरण") || trimmed.contains("प्रस्थान") || trimmed.contains("चले") || trimmed.contains("आकर बसे") || trimmed.contains("विस्थापित") || trimmed.contains("सैटल") -> {
                                    MilestoneType.MIGRATION
                                }
                                lower.contains("marriage") || lower.contains("wedding") || lower.contains("anniversary") ||
                                trimmed.contains("शादी") || trimmed.contains("विवाह") || trimmed.contains("गठजोड़") || trimmed.contains("वर्षगांठ") || trimmed.contains("वरमाला") -> {
                                    MilestoneType.ANNIVERSARY
                                }
                                else -> MilestoneType.CUSTOM
                            }

                            val title = when (type) {
                                MilestoneType.MIGRATION -> "स्थानांतरण (Migration)"
                                MilestoneType.ANNIVERSARY -> "विवाह गठजोड़ (Marriage)"
                                else -> "परिवारिक मील का पत्थर"
                            }

                            // clean up the year string from description to read nicely
                            var desc = trimmed.replace(yr.toString(), "").replace(normalizeDigits(yr.toString()), "")
                            desc = desc.trim { it <= ' ' || it == ':' || it == '-' || it == ',' || it == '—' }
                            if (desc.isBlank()) {
                                desc = trimmed
                            }

                            milestones.add(
                                FamilyMilestone(
                                    year = yr,
                                    dateDisplay = yr.toString(),
                                    member = m,
                                    type = type,
                                    title = title,
                                    description = desc
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // fall through if notes aren't blockable / decryption not ready
            }
        }
    }

    return milestones.sortedWith(compareBy({ it.year }, { it.member.firstName }))
}
