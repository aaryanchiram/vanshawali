package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FamilyMember
import com.example.ui.viewmodel.FamilyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeViewScreen(
    viewModel: FamilyViewModel,
    onNavigateToAddEdit: (Int, Int?, Int?, Int?) -> Unit, // id (0 for add), fatherId, motherId, spouseId
    modifier: Modifier = Modifier
) {
    val members by viewModel.allMembers.collectAsState()
    val rawSelected by viewModel.selectedMember.collectAsState()
    val relatives by viewModel.currentRelatives.collectAsState()
    val isHindi by viewModel.isHindi.collectAsState()

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isHindi) "वंश वृक्ष मानचित्र" else "Family Tree Map",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleLanguage() },
                        modifier = Modifier.testTag("lang_toggle")
                    ) {
                        Text(
                            text = if (isHindi) "EN" else "हिं",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (members.isNotEmpty()) {
                        IconButton(onClick = { viewModel.resetData() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (members.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FamilyRestroom,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isHindi) "कोई भी सदस्य नहीं मिला" else "No family members found",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (isHindi) 
                                "वंश वृक्ष को आरंभ करने के लिए अपना पहला सदस्य जोड़ें या नीचे दिए गए बटन पर क्लिक करके तैयार नमूना डेटा आयात करें।"
                                else "Add your first family member to start the tree or click below to import demo sample data.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { viewModel.loadSampleData() },
                            modifier = Modifier.fillMaxWidth().testTag("import_sample_btn")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isHindi) "नमूना वंश वृक्ष आयात करें" else "Import Sample Family Tree")
                        }
                        OutlinedButton(
                            onClick = { onNavigateToAddEdit(0, null, null, null) },
                            modifier = Modifier.fillMaxWidth().testTag("add_first_member_btn")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isHindi) "पहला सदस्य जोड़ें" else "Add First Member")
                        }
                    }
                }
            }
        } else {
            val rel = relatives
            if (rel == null) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Tree Map Scroll Area
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(verticalScrollState)
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        // Drawing connecting lines
                        Canvas(
                            modifier = Modifier
                                .width(900.dp)
                                .height(560.dp)
                        ) {
                            val strokeWidthVal = 4f
                            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

                            // 1. Ancestor connections (Father & Mother to target)
                            // Father pos x=250.dp, y=40.dp
                            // Mother pos x=470.dp, y=40.dp
                            // Target pos x=360.dp, y=200.dp
                            val fatherX = 250.dp.toPx() + 90.dp.toPx()
                            val fatherY = 40.dp.toPx() + 60.dp.toPx()
                            val motherX = 470.dp.toPx() + 90.dp.toPx()
                            val motherY = 40.dp.toPx() + 60.dp.toPx()
                            val targetX = 360.dp.toPx() + 90.dp.toPx()
                            val targetY = 200.dp.toPx()

                            if (rel.father != null || rel.mother != null) {
                                // Draw horizontal connector between parents
                                drawLine(
                                    color = Color.Gray,
                                    start = Offset(fatherX, fatherY - 30f),
                                    end = Offset(motherX, motherY - 30f),
                                    strokeWidth = strokeWidthVal
                                )
                                // Draw vertical down from middle of parents to target
                                val midX = (fatherX + motherX) / 2
                                drawLine(
                                    color = Color.Gray,
                                    start = Offset(midX, fatherY - 30f),
                                    end = Offset(midX, targetY),
                                    strokeWidth = strokeWidthVal
                                )
                            }

                            // 2. Spouse Connection (horizontal relation)
                            // Target pos x=360.dp, y=200.dp
                            // Spouse pos x=590.dp, y=200.dp
                            val spouseX = 590.dp.toPx()
                            val spouseY = 200.dp.toPx() + 30.dp.toPx()
                            if (rel.spouse != null) {
                                drawLine(
                                    color = Color.Magenta.copy(alpha = 0.5f),
                                    start = Offset(targetX + 90.dp.toPx(), targetY + 30.dp.toPx()),
                                    end = Offset(spouseX, spouseY),
                                    strokeWidth = strokeWidthVal + 1,
                                    pathEffect = pathEffect
                                )
                            }

                            // 3. Descendants connector
                            // Target pos x=360.dp, y=200.dp
                            // Children line vertical down and horizontal split
                            val childrenCount = rel.children.size
                            if (childrenCount > 0) {
                                val startChildY = 360.dp.toPx()
                                val midChildTargetY = (200.dp.toPx() + 60.dp.toPx() + startChildY) / 2
                                // Draw vertical down from target
                                drawLine(
                                    color = Color.Gray,
                                    start = Offset(targetX, targetY + 60.dp.toPx()),
                                    end = Offset(targetX, midChildTargetY),
                                    strokeWidth = strokeWidthVal
                                )

                                // Horizontal bar spanning children
                                val firstChildX = 80.dp.toPx() + 90.dp.toPx()
                                val lastChildX = (80.dp.toPx() + (childrenCount - 1) * 220.dp.toPx()) + 90.dp.toPx()
                                drawLine(
                                    color = Color.Gray,
                                    start = Offset(firstChildX, midChildTargetY),
                                    end = Offset(lastChildX, midChildTargetY),
                                    strokeWidth = strokeWidthVal
                                )

                                // Vertical drop lines for each child
                                (0 until childrenCount).forEach { idx ->
                                    val childCenterX = (80.dp.toPx() + idx * 220.dp.toPx()) + 90.dp.toPx()
                                    drawLine(
                                        color = Color.Gray,
                                        start = Offset(childCenterX, midChildTargetY),
                                        end = Offset(childCenterX, startChildY),
                                        strokeWidth = strokeWidthVal
                                    )
                                }
                            }
                        }

                        // Relatives positioning layouts
                        Box(modifier = Modifier.width(900.dp).height(560.dp)) {
                            // --- GENERATION 1: ANCESTORS ---
                            // Father Card
                            Box(modifier = Modifier.offset(x = 250.dp, y = 40.dp)) {
                                if (rel.father != null) {
                                    TreeMemberCard(
                                        member = rel.father,
                                        relationTag = if (isHindi) "पिता" else "FATHER",
                                        isHindi = isHindi,
                                        onClick = { viewModel.selectMember(rel.father) }
                                    )
                                } else {
                                    EmptyRelativeSlot(
                                        label = if (isHindi) "पिता जोडें" else "Add Father",
                                        icon = Icons.Default.Add,
                                        onClick = {
                                            // Add relation setting child's fatherId directly
                                            onNavigateToAddEdit(0, null, null, null)
                                        }
                                    )
                                }
                            }

                            // Mother Card
                            Box(modifier = Modifier.offset(x = 470.dp, y = 40.dp)) {
                                if (rel.mother != null) {
                                    TreeMemberCard(
                                        member = rel.mother,
                                        relationTag = if (isHindi) "माता" else "MOTHER",
                                        isHindi = isHindi,
                                        onClick = { viewModel.selectMember(rel.mother) }
                                    )
                                } else {
                                    EmptyRelativeSlot(
                                        label = if (isHindi) "माता जोडें" else "Add Mother",
                                        icon = Icons.Default.Add,
                                        onClick = { onNavigateToAddEdit(0, null, null, null) }
                                    )
                                }
                            }

                            // --- GENERATION 2: FOCUS MEMBER & SPOUSE / SIBLINGS ---
                            // Siblings list (floating left side)
                            Box(modifier = Modifier.offset(x = 20.dp, y = 175.dp)) {
                                if (rel.siblings.isNotEmpty()) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = if (isHindi) "भाई / बहन" else "Siblings",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.width(220.dp)
                                        ) {
                                            items(rel.siblings) { sib ->
                                                MiniSiblingCard(
                                                    member = sib,
                                                    isHindi = isHindi,
                                                    onClick = { viewModel.selectMember(sib) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Focus Target Card
                            Box(modifier = Modifier.offset(x = 360.dp, y = 190.dp)) {
                                TreeMemberCard(
                                    member = rel.target,
                                    relationTag = if (isHindi) "केंद्र" else "FOCUS",
                                    isCurrentFocus = true,
                                    isHindi = isHindi,
                                    onClick = {}
                                )
                            }

                            // Spouse Card
                            Box(modifier = Modifier.offset(x = 590.dp, y = 190.dp)) {
                                if (rel.spouse != null) {
                                    TreeMemberCard(
                                        member = rel.spouse,
                                        relationTag = if (isHindi) "जीवनसाथी" else "SPOUSE",
                                        isHindi = isHindi,
                                        onClick = { viewModel.selectMember(rel.spouse) }
                                    )
                                } else {
                                    EmptyRelativeSlot(
                                        label = if (isHindi) "जीवनसाथी जोडें" else "Add Spouse",
                                        icon = Icons.Default.Add,
                                        onClick = {
                                            onNavigateToAddEdit(0, null, null, rel.target.id)
                                        }
                                    )
                                }
                            }

                            // --- GENERATION 3: DESCENDANTS ---
                            // Children list
                            Box(modifier = Modifier.offset(x = 40.dp, y = 350.dp)) {
                                if (rel.children.isNotEmpty()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(40.dp)
                                    ) {
                                        rel.children.forEach { child ->
                                            TreeMemberCard(
                                                member = child,
                                                relationTag = if (child.gender == "Male") (if (isHindi) "पुत्र" else "SON") else (if (isHindi) "पुत्री" else "DAUGHTER"),
                                                isHindi = isHindi,
                                                onClick = { viewModel.selectMember(child) }
                                            )
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier.width(820.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        EmptyRelativeSlot(
                                            label = if (isHindi) "संतान जोड़ें" else "Add Child",
                                            icon = Icons.Default.Add,
                                            onClick = {
                                                val fId = if (rel.target.gender == "Male") rel.target.id else rel.spouse?.id
                                                val mId = if (rel.target.gender == "Female") rel.target.id else rel.spouse?.id
                                                onNavigateToAddEdit(0, fId, mId, null)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom info sheet overlays for more details of current focus
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Side: Colored Avatar badge
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        color = Color(android.graphics.Color.parseColor(rel.target.profileColorHex)),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = rel.target.getName(isHindi).take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Middle: text info
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = rel.target.getName(isHindi),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Deceased Badge
                                    if (rel.target.isDeceased) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            modifier = Modifier.padding(2.dp)
                                        ) {
                                            Text(
                                                text = if (isHindi) "स्वर्गवासी" else "Deceased",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${if (isHindi) "पीढ़ी" else "Generation"} ${rel.target.generation} • " +
                                            "${if (isHindi) rel.target.gender else rel.target.gender} • " +
                                            (rel.target.occupation ?: (if (isHindi) "अनिर्दिष्ट" else "Not Specified")),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (!rel.target.phone.isNullOrBlank()) {
                                    Text(
                                        text = "📞 ${rel.target.phone}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Right Side: Quick Action Button to edit
                            IconButton(
                                onClick = { onNavigateToAddEdit(rel.target.id, null, null, null) },
                                modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TreeMemberCard(
    member: FamilyMember,
    relationTag: String,
    isHindi: Boolean,
    onClick: () -> Unit,
    isCurrentFocus: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(180.dp)
            .height(100.dp)
            .border(
                width = if (isCurrentFocus) 3.dp else 1.dp,
                color = if (isCurrentFocus) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentFocus) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentFocus) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with relation marker and generation badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = (if (member.gender == "Male") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = relationTag,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (member.gender == "Male") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "G${member.generation}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Name
            Text(
                text = member.getName(isHindi),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            // Bottom text: Occupation/Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = member.occupation ?: (if (isHindi) "गृहस्थ" else "N/A"),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Gender Icon
                Icon(
                    imageVector = if (member.gender == "Male") Icons.Default.Male else Icons.Default.Female,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (member.gender == "Male") Color(0xFF1D4ED8) else Color(0xFFBE185D)
                )
            }
        }
    }
}

@Composable
fun MiniSiblingCard(
    member: FamilyMember,
    isHindi: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .width(100.dp)
            .height(56.dp)
            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = member.getName(isHindi),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (member.gender == "Male") (if (isHindi) "भाई" else "Bro") else (if (isHindi) "बहन" else "Sis"),
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Icon(
                    imageVector = if (member.gender == "Male") Icons.Default.Male else Icons.Default.Female,
                    contentDescription = null,
                    tint = if (member.gender == "Male") Color(0xFF1D4ED8) else Color(0xFFBE185D),
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyRelativeSlot(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(180.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                color = Color.LightGray.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
