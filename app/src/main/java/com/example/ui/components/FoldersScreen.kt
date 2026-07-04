package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FamilyFolder
import com.example.data.GeminiMember
import com.example.data.GeminiService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    folders: List<FamilyFolder>,
    onFolderSelected: (FamilyFolder) -> Unit,
    onAddFolder: (String, String, String, String, String, String) -> Unit, // Name, Description, Reference Year, Certificate Path, Issuer Name, Signature Path
    onAddScannedFolder: (String, String, String, String, List<GeminiMember>) -> Unit,
    onUpdateFolder: (FamilyFolder) -> Unit,
    onDeleteFolder: (FamilyFolder) -> Unit,
    onOpenGlobalReminders: () -> Unit,
    onOpenGlobalBackup: () -> Unit,
    isDarkMode: Boolean,
    onThemeChanged: (Boolean) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var newFolderName by remember { mutableStateOf("") }
    var newFolderDesc by remember { mutableStateOf("") }
    var newReferenceYear by remember { mutableStateOf("") }
    var certificatePath by remember { mutableStateOf("") }
    var newIssuerName by remember { mutableStateOf("") }
    var newSignaturePath by remember { mutableStateOf("") }
    var activeScannedMembers by remember { mutableStateOf<List<GeminiMember>?>(null) }
    
    val filteredFolders = remember(folders, searchQuery) {
        if (searchQuery.isBlank()) folders
        else folders.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val certLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            val copied = com.example.data.SecurityHelper.copyFileToPrivateSandbox(context, it, "certificates")
            if (copied != null) {
                certificatePath = copied
            }
        }
    }

    val signatureLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            val copied = com.example.data.SecurityHelper.copyFileToPrivateSandbox(context, it, "signatures")
            if (copied != null) {
                newSignaturePath = copied
            }
        }
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var folderToEdit by remember { mutableStateOf<FamilyFolder?>(null) }
    var editFolderName by remember { mutableStateOf("") }
    var editFolderDesc by remember { mutableStateOf("") }
    var editReferenceYear by remember { mutableStateOf("") }
    var editCertificatePath by remember { mutableStateOf("") }
    var editIssuerName by remember { mutableStateOf("") }
    var editSignaturePath by remember { mutableStateOf("") }

    val editCertLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            val copied = com.example.data.SecurityHelper.copyFileToPrivateSandbox(context, it, "certificates")
            if (copied != null) {
                editCertificatePath = copied
            }
        }
    }

    val editSigLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            val copied = com.example.data.SecurityHelper.copyFileToPrivateSandbox(context, it, "signatures")
            if (copied != null) {
                editSignaturePath = copied
            }
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "पारिवारिक रिकॉर्ड्स (My Family Tress)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenGlobalReminders) {
                        BadgedBox(
                            badge = { Badge { Text("!") } }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = "Reminders",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    // Global Theme Toggle
                    IconButton(onClick = { onThemeChanged(!isDarkMode) }) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Filled.WbSunny else Icons.Filled.NightsStay,
                            contentDescription = "Toggle Dark/Light Mode",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onOpenGlobalBackup) {
                        Icon(
                            imageVector = Icons.Filled.Backup,
                            contentDescription = "Cloud backup and Security Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Filled.CreateNewFolder, contentDescription = "New Tree Category") },
                text = { Text("नया परिवार बनाएं") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Summary Header Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .border(1.dp, MaterialTheme.colorScheme.goldAccent(), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountTree,
                            contentDescription = "Tree emblem",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "आपका अपना वंशवृक्ष (VanshVriksh)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            "सुरक्षित ऑफलाइन और क्लाउड एन्क्रिप्शन के साथ अलग-अलग परिवारों के फोल्डर बनाएं।",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("खोजें (Search) ...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp)
            )

            if (filteredFolders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FolderOpen,
                            contentDescription = "No folders",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "कोई पारिवारिक श्रेणी/फोल्डर नहीं मिला!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "अपने माता-पिता, ससुराल पक्ष या अन्य कबीले के लिए ‘नया परिवार बनाएं’ बटन दबाएं।",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredFolders) { folder ->
                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFolderSelected(folder) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Folder,
                                        contentDescription = "Folder category indicator",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(42.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = folder.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (folder.description.isNotBlank()) {
                                            Text(
                                                text = folder.description,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                maxLines = 1,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                        if (folder.referenceYear.isNotBlank()) {
                                            Text(
                                                text = "वंशावली सन: ${folder.referenceYear} के अनुसार",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                        if (folder.certificatePath.isNotBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Verified,
                                                    contentDescription = "Handmade Certificate Attached",
                                                    tint = Color(0xFFD4AF37),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "हस्तलिखित वंशावली दस्तावेज संलग्न",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF2E7D32)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "बनाया गया (Created): ${dateFormat.format(Date(folder.createdAt))}",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }

                                Row {
                                    IconButton(onClick = {
                                        folderToEdit = folder
                                        editFolderName = folder.name
                                        editFolderDesc = folder.description
                                        editReferenceYear = folder.referenceYear
                                        editCertificatePath = folder.certificatePath
                                        editIssuerName = folder.issuerName
                                        editSignaturePath = folder.signaturePath
                                        showEditDialog = true
                                    }) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "Edit family tree details",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    IconButton(onClick = { onDeleteFolder(folder) }) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete family side completely",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Category dialog modal
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showAddDialog = false
                    newFolderName = ""
                    newFolderDesc = ""
                    newReferenceYear = ""
                    certificatePath = ""
                    newIssuerName = ""
                    newSignaturePath = ""
                },
                title = { Text("नया पारिवारिक श्रेणी / फोल्डर") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newFolderName,
                            onValueChange = { newFolderName = it },
                            label = { Text("परिवार का नाम (e.g. चिराम परिवार)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = newFolderDesc,
                            onValueChange = { newFolderDesc = it },
                            label = { Text("संक्षिप्त विवरण (Description)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = newReferenceYear,
                            onValueChange = { newReferenceYear = it },
                            label = { Text("वंशावली का सन / वर्ष (e.g. १९९० / 2026)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = newIssuerName,
                            onValueChange = { newIssuerName = it },
                            label = { Text("जारीकर्ता अधिकारी का नाम (e.g. आर्यन चिराम)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            onClick = { signatureLauncher.launch("image/*") },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Draw,
                                    contentDescription = "Upload Signature",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "जारीकर्ता के हस्ताक्षर अपलोड करें (Signature Image)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        if (newSignaturePath.isBlank()) "चयन करने हेतु यहाँ क्लिक करें" else "संलग्न: ${newSignaturePath.substringAfterLast("/")}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (newSignaturePath.isBlank()) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f) else Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            onClick = { certLauncher.launch("image/*") },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AttachFile,
                                    contentDescription = "Attach Certificate",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "हाथ से बनाया गया सर्टिफिकेट / दस्तावेज",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        if (certificatePath.isBlank()) "चयन करने हेतु यहाँ क्लिक करें (संलग्न करें)" else "संलग्न: ${certificatePath.substringAfterLast("/")}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (certificatePath.isBlank()) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f) else Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }

                        if (certificatePath.isNotBlank()) {
                            var isScanning by remember { mutableStateOf(false) }
                            val scope = rememberCoroutineScope()

                            Button(
                                onClick = {
                                    isScanning = true
                                    scope.launch {
                                        val result = GeminiService.scanLineageTreeImage(context, certificatePath)
                                        isScanning = false
                                        if (result.isNotEmpty()) {
                                            activeScannedMembers = result
                                        } else {
                                            Toast.makeText(context, "स्कैन विफल रहा या कोई वंशावली संबंध नहीं मिले! कृपया स्पष्ट छवि का प्रयोग करें।", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isScanning
                            ) {
                                if (isScanning) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("AI विश्लेषण किया जा रहा है...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Scanner", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("✨ AI स्कैन द्वारा स्वतः वंशवृक्ष बनाएं", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newFolderName.isNotBlank()) {
                                onAddFolder(newFolderName, newFolderDesc, newReferenceYear, certificatePath, newIssuerName, newSignaturePath)
                                newFolderName = ""
                                newFolderDesc = ""
                                newReferenceYear = ""
                                certificatePath = ""
                                newIssuerName = ""
                                newSignaturePath = ""
                                showAddDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("सहेजें (Save)")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showAddDialog = false
                        newFolderName = ""
                        newFolderDesc = ""
                        newReferenceYear = ""
                        certificatePath = ""
                        newIssuerName = ""
                        newSignaturePath = ""
                    }) {
                        Text("रद्द करें")
                    }
                }
            )
        }

        // Edit Category/Folder Dialog modal
        if (showEditDialog && folderToEdit != null) {
            val folder = folderToEdit!!
            AlertDialog(
                onDismissRequest = { 
                    showEditDialog = false
                    folderToEdit = null
                },
                title = { Text("पारिवारिक फोल्डर संपादित करें") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = editFolderName,
                            onValueChange = { editFolderName = it },
                            label = { Text("परिवार का नाम (e.g. चिराम परिवार)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editFolderDesc,
                            onValueChange = { editFolderDesc = it },
                            label = { Text("संक्षिप्त विवरण (Description)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editReferenceYear,
                            onValueChange = { editReferenceYear = it },
                            label = { Text("वंशावली का सन / वर्ष (e.g. १९९० / 2026)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editIssuerName,
                            onValueChange = { editIssuerName = it },
                            label = { Text("जारीकर्ता अधिकारी का नाम (e.g. आर्यन चिराम)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            onClick = { editSigLauncher.launch("image/*") },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Draw,
                                    contentDescription = "Upload Signature",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "जारीकर्ता के हस्ताक्षर संपादित करें (Signature Image)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        if (editSignaturePath.isBlank()) "चयन करने हेतु यहाँ क्लिक करें" else "संलग्न: ${editSignaturePath.substringAfterLast("/")}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (editSignaturePath.isBlank()) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f) else Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            onClick = { editCertLauncher.launch("image/*") },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AttachFile,
                                    contentDescription = "Attach Certificate",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "हाथ से बनाया गया सर्टिफिकेट / दस्तावेज",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        if (editCertificatePath.isBlank()) "चयन करने हेतु यहाँ क्लिक करें (संलग्न करें)" else "संलग्न: ${editCertificatePath.substringAfterLast("/")}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (editCertificatePath.isBlank()) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f) else Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editFolderName.isNotBlank()) {
                                onUpdateFolder(
                                    folder.copy(
                                        name = editFolderName,
                                        description = editFolderDesc,
                                        referenceYear = editReferenceYear,
                                        certificatePath = editCertificatePath,
                                        issuerName = editIssuerName,
                                        signaturePath = editSignaturePath
                                    )
                                )
                                showEditDialog = false
                                folderToEdit = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("सहेजें (Save)")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showEditDialog = false
                        folderToEdit = null
                    }) {
                        Text("रद्द करें")
                    }
                }
            )
        }

        // Active Scanned Members Verification Dialog
        if (activeScannedMembers != null) {
            val scannedList = activeScannedMembers!!
            AlertDialog(
                onDismissRequest = { activeScannedMembers = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "AI", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI पहचानी गई वंशावली समीक्षा", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "AI ने आपकी फोटो से निम्नलिखित सदस्यों और संबंधों को प्राप्त किया है। आप इनके नाम, लिंग या संबंधों को संपादित भी कर सकते हैं:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(scannedList) { index, item ->
                                var firstName by remember(item.tempId) { mutableStateOf(item.firstName) }
                                var lastName by remember(item.tempId) { mutableStateOf(item.lastName) }
                                var gender by remember(item.tempId) { mutableStateOf(item.gender) }
                                var fatherId by remember(item.tempId) { mutableStateOf(item.fatherTempId) }
                                var motherId by remember(item.tempId) { mutableStateOf(item.motherTempId) }
                                var spouseId by remember(item.tempId) { mutableStateOf(item.spouseTempId) }

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "सदस्य #${index + 1} (tempId: ${item.tempId})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            IconButton(
                                                onClick = {
                                                    activeScannedMembers = scannedList.filterIndexed { idx, _ -> idx != index }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Filled.Close, contentDescription = "हटाएं", tint = Color.Red, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedTextField(
                                                value = firstName,
                                                onValueChange = {
                                                    firstName = it
                                                    item.firstName = it
                                                },
                                                label = { Text("पहला नाम", fontSize = 9.sp) },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = lastName,
                                                onValueChange = {
                                                    lastName = it
                                                    item.lastName = it
                                                },
                                                label = { Text("उपनाम", fontSize = 9.sp) },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("लिंग (Gender):", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                            TextButton(
                                                onClick = {
                                                    gender = "Male"
                                                    item.gender = "Male"
                                                },
                                                colors = ButtonDefaults.textButtonColors(
                                                    containerColor = if (gender == "Male") MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                                                )
                                            ) {
                                                Text("पुरुष", fontSize = 11.sp)
                                            }
                                            TextButton(
                                                onClick = {
                                                    gender = "Female"
                                                    item.gender = "Female"
                                                },
                                                colors = ButtonDefaults.textButtonColors(
                                                    containerColor = if (gender == "Female") MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                                                )
                                            ) {
                                                Text("महिला", fontSize = 11.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("पारिवारिक रिश्ते (Relationships):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)

                                        val potentialFathers = scannedList.filter { it.tempId != item.tempId && it.gender == "Male" }
                                        val potentialMothers = scannedList.filter { it.tempId != item.tempId && it.gender == "Female" }
                                        val potentialSpouses = scannedList.filter { it.tempId != item.tempId }

                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                                            RelationDropdownRow(
                                                label = "पिता (Father):",
                                                selectedId = fatherId,
                                                options = scannedList,
                                                onSelected = {
                                                    fatherId = it
                                                    item.fatherTempId = it
                                                }
                                            )

                                            RelationDropdownRow(
                                                label = "माता (Mother):",
                                                selectedId = motherId,
                                                options = scannedList,
                                                onSelected = {
                                                    motherId = it
                                                    item.motherTempId = it
                                                }
                                            )

                                            RelationDropdownRow(
                                                label = "जीवन साथी:",
                                                selectedId = spouseId,
                                                options = scannedList,
                                                onSelected = {
                                                    spouseId = it
                                                    item.spouseTempId = it
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Button(
                            onClick = {
                                val nextTempId = (scannedList.maxOfOrNull { it.tempId } ?: 0L) + 1L
                                activeScannedMembers = scannedList + GeminiMember(
                                    tempId = nextTempId,
                                    firstName = "नया",
                                    lastName = "सदस्य",
                                    gender = "Male"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add Member", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("नया सदस्य जोड़ें (+ Add Member)", fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newFolderName.isNotBlank()) {
                                onAddScannedFolder(newFolderName, newFolderDesc, newReferenceYear, certificatePath, scannedList)
                                newFolderName = ""
                                newFolderDesc = ""
                                newReferenceYear = ""
                                certificatePath = ""
                                activeScannedMembers = null
                                showAddDialog = false
                            } else {
                                Toast.makeText(context, "कृपया पहले परिवार का नाम दर्ज करें।", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("सहेजें और वंशवृक्ष बनाएं", fontSize = 12.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeScannedMembers = null }) {
                        Text("रद्द करें")
                    }
                }
            )
        }
    }
}

@Composable
fun RelationDropdownRow(
    label: String,
    selectedId: Long?,
    options: List<GeminiMember>,
    onSelected: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = options.find { it.tempId == selectedId }?.let { "${it.firstName} ${it.lastName}" } ?: "कोई नहीं (None)"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(100.dp))
        Box {
            Text(
                text = selectedName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("कोई नहीं (None)", fontSize = 11.sp) },
                    onClick = {
                        onSelected(null)
                        expanded = false
                    }
                )
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text("${opt.firstName} ${opt.lastName}", fontSize = 11.sp) },
                        onClick = {
                            onSelected(opt.tempId)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// Support extensions for beautiful custom theme accents inside classes
@Composable
fun ColorScheme.goldAccent(): Color = Color(0xFFD4AF37)
