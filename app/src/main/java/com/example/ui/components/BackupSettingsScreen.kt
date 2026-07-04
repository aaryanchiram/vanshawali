package com.example.ui.components

import com.example.data.TreeExporter
import java.io.File
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(
    currentPin: String,
    onPinUpdated: (String) -> Unit,
    onBack: () -> Unit,
    onExportTrigger: () -> Unit, // shares json
    onImportTrigger: (String) -> Unit, // takes pasted json
    onPdfReportTrigger: () -> Unit,
    isDarkMode: Boolean,
    onThemeChanged: (Boolean) -> Unit,
    activeStylePreference: Int = 1,
    onStyleChanged: (Int) -> Unit = {},
    fontSize: Float = 12f,
    onFontSizeChanged: (Float) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var isPinEnabled by remember { mutableStateOf(currentPin.isNotEmpty()) }
    var changePinDialog by remember { mutableStateOf(false) }
    var newPinCode by remember { mutableStateOf("") }
    
    // Paste JSON Import block
    var showImportDialog by remember { mutableStateOf(false) }
    var pastedJsonText by remember { mutableStateOf("") }
    var importStatusMessage by remember { mutableStateOf("") }

    // Cloud backup simulation
    var isCloudAutoBackup by remember { mutableStateOf(true) }
    var cloudSyncProgress by remember { mutableStateOf(0f) }
    var isSyncing by remember { mutableStateOf(false) }
    var lastSyncedDate by remember { mutableStateOf("9 जून 2026, 12:05 अपराह्न") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("सुरक्षा और बैकअप सेटिंग्स (Security & Settings)", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 0. APP THEME SETTINGS
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Palette, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("एप्लीकेशन थीम (App Theme)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Text(
                        "लाइट थीम पारिवारिक वृक्ष को पारंपरिक और सुस्पष्ट पीले, केसरिया और चंदन रंगों के साथ प्रस्तुत करता है, जिससे विवरण सुन्दरता से दिखते हैं।",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 15.sp
                    )

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                if (isDarkMode) "डार्क मोड (Dark Mode)" else "लाइट मोड (Light Mode)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                if (isDarkMode) "रात के समय उपयोग के लिए उपयुक्त" else "पारंपरिक, सुंदर और सुस्पष्ट अनुभव",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = onThemeChanged
                        )
                    }
                }
            }

            // 0.5. FAMILY TREE DIA-GRAM STYLES
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Style, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("वंशवृक्ष आरेख शैली (Family Tree Styles)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Text(
                        "यहाँ से अपने वंशवृक्ष (Family Diagram) का संपूर्ण रूप-रंग बदलें। प्रत्येक थीम में अनोखी पृष्ठभूमि, विशिष्ट आकृतियाँ, संबंध रेखाएं और चंचल प्रोफाइल एनिमेशन शामिल हैं।",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 15.sp
                    )

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                    Text("फ़ॉन्ट आकार: ${fontSize.toInt()}sp", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Slider(
                        value = fontSize,
                        onValueChange = onFontSizeChanged,
                        valueRange = 8f..20f,
                        steps = 5,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                    val stylesList = listOf(
                        Triple(1, "राजशाही स्वर्ण (Royal Gold)", "राजसी स्वर्ण-मखमली पृष्ठभूमि। आकर्षक गोल कार्ड आकार, और घूमने वाले शाही कांति चक्र चक्राकार एनिमेशन।"),
                        Triple(2, "हरित वन (Forest Green)", "जैविक हरी वनस्पति झलक। पेड़ों से तैरती लहरदार जड़ें, पत्ती के आकार के नोड्स और कोमल श्वसन विस्तार एनिमेशन।"),
                        Triple(3, "आधुनिक नील (Tech/Cyber Blue)", "आधुनिक भविष्यवादी कट-कॉर्नर कार्ड, साइबर डॉटेड कनेक्शन रेखाएं और सूक्ष्म फ़्लोटिंग एनिमेशन।"),
                        Triple(4, "गुलाबी क्वार्ट्ज (Rose Quartz)", "मधुर परिवार स्नेह थीम। नाजुक घुमावदार संबंध रेखाएं और हृदय-गति के अनुकूल धड़कता हुआ प्रोफाइल आकार।"),
                        Triple(5, "कास्मिक स्लेट (Cosmic Slate)", "अंतरिक्षीय गहरे रंग की रहस्यमय रात। चक्राकार घूर्णन रिंग, चमकीली बिंदुदार स्टार्स लाइनें और ब्रह्मांडीय फ्लोट प्रभाव।")
                    )

                    stylesList.forEach { (id, title, desc) ->
                        val isSelected = (activeStylePreference == id)
                        val themeColor = when (id) {
                            1 -> Color(0xFFFFD700)
                            2 -> Color(0xFF2E7D32)
                            3 -> Color(0xFF1565C0)
                            4 -> Color(0xFFEC407A)
                            5 -> Color(0xFF7E57C2)
                            else -> MaterialTheme.colorScheme.primary
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) themeColor else Color.LightGray.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .background(
                                    color = if (isSelected) themeColor.copy(alpha = 0.08f) else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onStyleChanged(id) }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(themeColor, androidx.compose.foundation.shape.CircleShape)
                                        .align(Alignment.CenterVertically)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = title,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = themeColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = desc,
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        lineHeight = 14.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 1. PIN LOCK & SECURITY
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.VpnKey, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("एप्लीकेशन लॉक सुरक्षा", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Text(
                        "फिंगरप्रिंट और PIN लॉक को सक्रिय रखें ताकि आपका पारिवारिक इतिहास और दस्तावेज़ पूरी तरह सुरक्षित रहें।",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 15.sp
                    )

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("पिन लॉक इनेबल करें", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                if (isPinEnabled) "सुरक्षा पिन वर्तमान में सक्रिय है" else "पिन लॉक निष्क्रिय है",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = isPinEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    changePinDialog = true
                                } else {
                                    onPinUpdated("")
                                    isPinEnabled = false
                                }
                            }
                        )
                    }

                    if (isPinEnabled) {
                        Button(
                            onClick = { changePinDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.LockReset, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("सुरक्षा पिन बदलें (Change PIN)")
                        }
                    }
                }
            }

            // 2. OFFLINE BACKUP, EXPORT & DOCUMENTS
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.FolderZip, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("ऑफलाइन डेटा बैकअप और दस्तावेज", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Text(
                        "यह सम्पूर्ण फैमली ट्री डेटा और लिंक किए गए सदस्यों को एन्क्रिप्टेड JSON कोड या दस्तावेज़ रिपोर्ट में ऑफलाइन सेव और शेयर करें।",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 15.sp
                    )

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onExportTrigger,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Share, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ऑफलाइन एक्सपोर्ट", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { showImportDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.FileDownload, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("डेटा रीस्टोर", fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = onPdfReportTrigger,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5A11)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("सुंदर दस्तावेज़/PDF रिपोर्ट प्रिंट करें")
                    }
                }
            }

            // 3. CLOUD BACKUP (GOOGLE DRIVE) SIMULATOR
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CloudUpload, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("क्लाउड बैकअप (Google Drive / Cloud Sync)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Text(
                        "फैमली ट्री को सुरक्षित तरीके से अपने गूगल ड्राइव या सर्वर पर स्टोर करें। इसके लिए डेटा एन्क्रिप्ट होकर ही अपलोड होता है।",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 15.sp
                    )

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ऑटो-क्लाउड सिंक", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("डेटा को स्वतः क्लाउड पर डालें", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = isCloudAutoBackup,
                            onCheckedChange = { isCloudAutoBackup = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("आखरी सिंक (Last Backup):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(lastSyncedDate, fontSize = 11.sp, color = Color.Gray)
                        }
                        Button(
                            onClick = {
                                if (!isSyncing) {
                                    isSyncing = true
                                    cloudSyncProgress = 0.0f
                                    coroutineScope.launch {
                                        for (step in 1..10) {
                                            delay(200)
                                            cloudSyncProgress = step / 10f
                                        }
                                        isSyncing = false
                                        lastSyncedDate = "अभी-अभी (Just Now)"
                                    }
                                }
                            },
                            enabled = !isSyncing,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (isSyncing) {
                                Text("सिंक हो रहा है...")
                            } else {
                                Icon(Icons.Filled.Sync, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("बैकअप लें (Backup)")
                            }
                        }
                    }

                    if (isSyncing) {
                        LinearProgressIndicator(
                            progress = { cloudSyncProgress },
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // 4. AUTOMATIC LOCAL SAVES (AUTO-SAVE HISTORY)
            val settingsContext = androidx.compose.ui.platform.LocalContext.current
            var autoSaveFiles by remember { mutableStateOf(TreeExporter.getAutoSaveFiles(settingsContext)) }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AccountTree, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("ऑटो-सेव रिकॉर्ड्स (Auto-Save History)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Text(
                        "जब भी आप किसी पारिवारिक वृक्ष में संशोधन करते हैं, आपका विवरण स्वतः सुरक्षित हो जाता है। दुर्घटनावश फ़ोल्डर हटने या डेटा हानि होने पर आप इन्हें रिस्टोर कर सकते हैं:",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 15.sp
                    )

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                    if (autoSaveFiles.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("कोई ऑटो-सेव रिकॉर्ड नहीं मिला (No auto-saves yet)", fontSize = 12.sp, color = Color.Gray)
                        }
                    } else {
                        autoSaveFiles.forEach { file ->
                            val parsed = remember(file) { TreeExporter.getFolderAndDateFromAutoSave(file) }
                            val folderTitle = parsed.first
                            val savedTimeDesc = parsed.second

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = folderTitle,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "सुरक्षित समय: $savedTimeDesc",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Share button
                                    IconButton(
                                        onClick = {
                                            try {
                                                TreeExporter.shareBackupFile(settingsContext, file.readText(), folderTitle)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Share,
                                            contentDescription = "Share Auto-Save File",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Restore button
                                    Button(
                                        onClick = {
                                            try {
                                                val json = file.readText()
                                                onImportTrigger(json)
                                                // Refresh list after import
                                                autoSaveFiles = TreeExporter.getAutoSaveFiles(settingsContext)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Text("रिस्टोर", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1. PIN CREATE/CHANGE MODAL
        if (changePinDialog) {
            AlertDialog(
                onDismissRequest = { changePinDialog = false },
                title = { Text("सुरक्षा पिन सेट करें (Set PIN)") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("4 अंकों का सुरक्षा पिन दर्ज करें:")
                        OutlinedTextField(
                            value = newPinCode,
                            onValueChange = { if (it.length <= 4) newPinCode = it },
                            label = { Text("PIN दर्ज करें") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPinCode.length == 4) {
                                onPinUpdated(newPinCode)
                                isPinEnabled = true
                                changePinDialog = false
                                newPinCode = ""
                            }
                        }
                    ) {
                        Text("सहेजें (Save PIN)")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { changePinDialog = false }) {
                        Text("रद्द करें")
                    }
                }
            )
        }

        // 2. OFFLINE RESTORE (PASTE JSON) DIALOG
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = { Text("ऑफलाइन बैकअप रीस्टोर करें") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("निर्यात किया गया बैकअप JSON कोड यहाँ पेस्ट करें और रीस्टोर करने के लिए बटन दबाएं:", fontSize = 12.sp)
                        
                        OutlinedTextField(
                            value = pastedJsonText,
                            onValueChange = { pastedJsonText = it },
                            placeholder = { Text("{ \"app\": \"VanshVriksh\", ... }") },
                            minLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (importStatusMessage.isNotEmpty()) {
                            Text(
                                text = importStatusMessage,
                                color = if (importStatusMessage.contains("सफल")) Color(0xFF2E7D32) else Color.Red,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (pastedJsonText.isNotBlank()) {
                                onImportTrigger(pastedJsonText)
                                importStatusMessage = "डेटा रीस्टोर सफल! ऐप को रीस्टार्ट करें या वापस जाएं।"
                                pastedJsonText = ""
                            } else {
                                importStatusMessage = "कृपया सही कोड पेस्ट करें।"
                            }
                        }
                    ) {
                        Text("रीस्टोर करें (Restore)")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showImportDialog = false
                            importStatusMessage = ""
                        }
                    ) {
                        Text("बंद करें")
                    }
                }
            )
        }
    }
}
