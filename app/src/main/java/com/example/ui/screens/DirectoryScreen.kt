package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FamilyMember
import com.example.ui.viewmodel.FamilyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryScreen(
    viewModel: FamilyViewModel,
    onNavigateToAddEdit: (Int) -> Unit,
    onFocusInTree: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isHindi by viewModel.isHindi.collectAsState()
    val rawList by viewModel.allMembers.collectAsState()
    val filteredList by viewModel.filteredMembers.collectAsState()

    val query by viewModel.searchQuery.collectAsState()
    val selectedGen by viewModel.selectedGeneration.collectAsState()
    val selectedGenText = if (selectedGen != null) "${if (isHindi) "पीढ़ी" else "Gen"} $selectedGen" else null
    
    val selectedGender by viewModel.selectedGender.collectAsState()
    val selectedGenderText = when (selectedGender) {
        "Male" -> if (isHindi) "पुरुष" else "Male"
        "Female" -> if (isHindi) "महिला" else "Female"
        else -> null
    }

    val livingFilter by viewModel.livingFilter.collectAsState()
    val livingFilterText = when (livingFilter) {
        true -> if (isHindi) "जीवित" else "Living"
        false -> if (isHindi) "स्वर्गवासी" else "Deceased"
        null -> null
    }

    var showDeleteConfirmDialogFor by remember { mutableStateOf<FamilyMember?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isHindi) "पारिवारिक निर्देशिका" else "Family Directory",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleLanguage() }) {
                        Text(
                            text = if (isHindi) "EN" else "ヒं",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAddEdit(0) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag("fab_add_member")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Member")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Input Block
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("directory_search_input"),
                placeholder = { Text(if (isHindi) "नाम, काम या फोन से खोजें..." else "Search by name, occupation, phone...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Horizontal Filters Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Generation filter dropdown mock
                item {
                    val generations = rawList.map { it.generation }.distinct().sorted()
                    var expandedGen by remember { mutableStateOf(false) }
                    Box {
                        FilterChip(
                            selected = selectedGen != null,
                            onClick = { expandedGen = true },
                            label = { Text(selectedGenText ?: (if (isHindi) "सभी पीढियां" else "All Generations")) },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
                        )
                        DropdownMenu(expanded = expandedGen, onDismissRequest = { expandedGen = false }) {
                            DropdownMenuItem(
                                text = { Text(if (isHindi) "सभी पीढियां" else "All Generations") },
                                onClick = {
                                    viewModel.filterByGeneration(null)
                                    expandedGen = false
                                }
                            )
                            generations.forEach { g ->
                                DropdownMenuItem(
                                    text = { Text("${if (isHindi) "पीढ़ी" else "Gen"} $g") },
                                    onClick = {
                                        viewModel.filterByGeneration(g)
                                        expandedGen = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Gender Filter chip
                item {
                    var expandedGen by remember { mutableStateOf(false) }
                    Box {
                        FilterChip(
                            selected = selectedGender != null,
                            onClick = { expandedGen = true },
                            label = { Text(selectedGenderText ?: (if (isHindi) "सभी लिंग" else "All Genders")) },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
                        )
                        DropdownMenu(expanded = expandedGen, onDismissRequest = { expandedGen = false }) {
                            DropdownMenuItem(
                                text = { Text(if (isHindi) "सभी लिंग" else "All Genders") },
                                onClick = {
                                    viewModel.filterByGender(null)
                                    expandedGen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isHindi) "पुरुष" else "Male") },
                                onClick = {
                                    viewModel.filterByGender("Male")
                                    expandedGen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isHindi) "महिला" else "Female") },
                                onClick = {
                                    viewModel.filterByGender("Female")
                                    expandedGen = false
                                }
                            )
                        }
                    }
                }

                // Status Filter chip (Living / Deceased)
                item {
                    var expandedGen by remember { mutableStateOf(false) }
                    Box {
                        FilterChip(
                            selected = livingFilter != null,
                            onClick = { expandedGen = true },
                            label = { Text(livingFilterText ?: (if (isHindi) "सभी स्थिति" else "All Status")) },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
                        )
                        DropdownMenu(expanded = expandedGen, onDismissRequest = { expandedGen = false }) {
                            DropdownMenuItem(
                                text = { Text(if (isHindi) "सभी स्थिति" else "All Status") },
                                onClick = {
                                    viewModel.filterByLivingStatus(null)
                                    expandedGen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isHindi) "जीवित" else "Living Only") },
                                onClick = {
                                    viewModel.filterByLivingStatus(true)
                                    expandedGen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isHindi) "स्वर्गवासी" else "Deceased Only") },
                                onClick = {
                                    viewModel.filterByLivingStatus(false)
                                    expandedGen = false
                                }
                            )
                        }
                    }
                }

                // Clear button
                if (selectedGen != null || selectedGender != null || livingFilter != null) {
                    item {
                        TextButton(
                            onClick = { viewModel.clearFilters() },
                            modifier = Modifier.testTag("clear_filters_btn")
                        ) {
                            Icon(Icons.Default.FilterListOff, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHindi) "साफ़ करें" else "Clear")
                        }
                    }
                }
            }

            // ListView
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isHindi) "कोई मेल नहीं मिला" else "No matching members",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = if (isHindi) "सक्रीय फ़िल्टर या खोज शब्द बदलें।" else "Try altering active search query or filters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { member ->
                        DirectoryMemberRow(
                            member = member,
                            isHindi = isHindi,
                            onFocus = {
                                viewModel.selectMember(member)
                                onFocusInTree()
                            },
                            onEdit = { onNavigateToAddEdit(member.id) },
                            onDelete = { showDeleteConfirmDialogFor = member },
                            onCall = { phone ->
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Handle missing dialer exception gracefully
                                }
                            }
                        )
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        showDeleteConfirmDialogFor?.let { target ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialogFor = null },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteMember(target)
                            showDeleteConfirmDialogFor = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(if (isHindi) "हाँ, हटाएँ" else "Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialogFor = null }) {
                        Text(if (isHindi) "रद्द करें" else "Cancel")
                    }
                },
                title = { Text(if (isHindi) "क्या आप निश्चित हैं?" else "Confirm Deletion") },
                text = {
                    Text(
                        if (isHindi) 
                            "क्या आप सच में '${target.getName(true)}' को हटाना चाहते हैं? इसके हटाने से इससे जुडी सभी रिश्तेदारियां भी प्रभाव विहीन हो जायेंगी।" 
                            else "Are you sure you want to delete '${target.getName(false)}'? This will also clear relational references targeting this person."
                    )
                }
            )
        }
    }
}

@Composable
fun DirectoryMemberRow(
    member: FamilyMember,
    isHindi: Boolean,
    onFocus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCall: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colored Avatar badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = Color(android.graphics.Color.parseColor(member.profileColorHex)),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.getName(isHindi).take(1).uppercase(),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Mid Core Information
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = member.getName(isHindi),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Status mark
                        if (member.isDeceased) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = if (isHindi) "स्वर्गवासी" else "Passed",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${if (isHindi) "पीढ़ी" else "Gen"} ${member.generation} • " +
                                (member.occupation ?: (if (isHindi) "व्यवसाय दर्ज नहीं" else "No Occ")),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (!member.phone.isNullOrBlank()) {
                        Text(
                            text = "📞 ${member.phone}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Relational icon shortcuts
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onFocus,
                        modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape).size(36.dp)
                    ) {
                        Icon(Icons.Default.AccountTree, contentDescription = "Focus Tree", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), CircleShape).size(36.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), CircleShape).size(36.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Secondary expansion - show notes or address if they exist
            if (!member.address.isNullOrBlank() || !member.notes.isNullOrBlank()) {
                Divider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = Color.LightGray.copy(alpha = 0.3f)
                )

                if (!member.address.isNullOrBlank()) {
                    Text(
                        text = "📍 ${member.address}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                if (!member.notes.isNullOrBlank()) {
                    Text(
                        text = "📝 ${member.notes}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
