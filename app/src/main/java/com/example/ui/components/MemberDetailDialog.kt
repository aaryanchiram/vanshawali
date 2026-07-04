package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventType
import coil.compose.AsyncImage
import com.example.data.FamilyMember
import com.example.data.Reminder
import com.example.data.SecurityHelper
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDetailDialog(
    member: FamilyMember?, // Null means "Add Mode"
    allMembers: List<FamilyMember>,
    userPin: String,
    onDismiss: () -> Unit,
    onSave: (
        firstName: String,
        lastName: String,
        gender: String,
        birthDate: String,
        deathDate: String,
        birthPlace: String,
        occupation: String,
        decryptedNotes: String,
        photoPath: String,
        documentPath: String,
        fatherId: Long?,
        motherId: Long?,
        spouseId: Long?,
        marriageDate: String,
        isTargetSubject: Boolean,
        decryptedMobile: String,
        decryptedEmail: String,
        isStarred: Boolean,
        addReminderTitle: String? // "Birthday", "Anniversary" or null
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isEditMode = member != null

    // Input States
    var firstName by remember { mutableStateOf(member?.firstName ?: "") }
    var lastName by remember { mutableStateOf(member?.lastName ?: "") }
    var gender by remember { mutableStateOf(member?.gender ?: "Male") }
    var birthDate by remember { mutableStateOf(member?.birthDate ?: "1990-01-01") }
    var deathDate by remember { mutableStateOf(member?.deathDate ?: "") }
    var birthPlace by remember { mutableStateOf(member?.birthPlace ?: "") }
    var occupation by remember { mutableStateOf(member?.occupation ?: "") }
    
    // Transparent Decryption on setup
    var decryptedNotes by remember {
        mutableStateOf(
            if (member != null) SecurityHelper.decrypt(member.notes, userPin) else ""
        )
    }
    var decryptedMobile by remember {
        mutableStateOf(
            if (member != null) SecurityHelper.decrypt(member.mobile, userPin) else ""
        )
    }
    var decryptedEmail by remember {
        mutableStateOf(
            if (member != null) SecurityHelper.decrypt(member.email, userPin) else ""
        )
    }

    var photoPath by remember { mutableStateOf(member?.photoPath ?: "") }
    var documentPath by remember { mutableStateOf(member?.documentPath ?: "") }
    var fatherId by remember { mutableStateOf(member?.fatherId) }
    var motherId by remember { mutableStateOf(member?.motherId) }
    var spouseId by remember { mutableStateOf(member?.spouseId) }
    var marriageDate by remember { mutableStateOf(member?.marriageDate ?: "") }
    var isTargetSubject by remember { mutableStateOf(member?.isTargetSubject ?: false) }
    var isStarred by remember { mutableStateOf(member?.isStarred ?: false) }

    // Reminders option
    var autoCreateReminder by remember { mutableStateOf(false) }
    var reminderType by remember { mutableStateOf("जन्मदिन") }

    // Relationship dropdown states
    var showFatherDropdown by remember { mutableStateOf(false) }
    var showMotherDropdown by remember { mutableStateOf(false) }
    var showSpouseDropdown by remember { mutableStateOf(false) }

    // Safely copy picked photo/document into the isolated private app files directory
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val copied = SecurityHelper.copyFileToPrivateSandbox(context, it, "photos")
            if (copied != null) {
                photoPath = copied
            }
        }
    }

    val docLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val copied = SecurityHelper.copyFileToPrivateSandbox(context, it, "documents")
            if (copied != null) {
                documentPath = copied
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Filled.Edit else Icons.Filled.PersonAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = if (isEditMode) "सदस्य विवरण सम्पादन" else "नया पारिवारिक सदस्य",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // PHOTO / AVATAR UPLOAD CONTAINER
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clickable { photoLauncher.launch("image/*") },
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (photoPath.isNotBlank()) {
                                AsyncImage(
                                    model = File(photoPath),
                                    contentDescription = "Member profile picture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        // Circular Overlay Camera Icon Button
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = "Upload Photo",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // SUBTITLE SEGMENT
                Text(
                    text = "बुनियादी जानकारी (Basic Details)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )

                // FIRST & LAST NAME SIDE-BY-SIDE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("पहला नाम (First)*") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("उपनाम (Last)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // MODERN GENDER SELECTOR CHIPS
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "लिंग / Gender:", 
                        fontWeight = FontWeight.SemiBold, 
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Male", "Female", "Other").forEach { g ->
                            val isSelected = (gender == g)
                            val (gText, gColor) = when (g) {
                                "Male" -> Pair("पुरुष", Color(0xFF2E7D32))
                                "Female" -> Pair("महिला", Color(0xFFC2185B))
                                else -> Pair("अन्य", Color(0xFF7B1FA2))
                            }
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) gColor.copy(alpha = 0.12f) 
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) gColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { gender = g }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(if (isSelected) gColor else Color.Gray.copy(alpha = 0.5f), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = gText,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = if (isSelected) gColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }

                // DATES SIDE-BY-SIDE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = birthDate,
                        onValueChange = { birthDate = it },
                        label = { Text("जन्मतिथि*") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.CalendarMonth, null, modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            IconButton(onClick = {
                                autoCreateReminder = true
                                reminderType = "जन्मदिन"
                                Toast.makeText(context, "जन्मदिन उत्सव रिमाइंडर सेट किया गया!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = "Set Birthday Reminder",
                                    tint = if (autoCreateReminder && reminderType == "जन्मदिन") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .onDoubleTapSpy {
                                autoCreateReminder = true
                                reminderType = "जन्मदिन"
                                Toast.makeText(context, "डबल-क्लिक: जन्मदिन उत्सव रिमाइंडर सेट किया गया!", Toast.LENGTH_SHORT).show()
                            }
                    )
                    OutlinedTextField(
                        value = deathDate,
                        onValueChange = { deathDate = it },
                        label = { Text("स्वर्गवास") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.HeartBroken, null, modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            IconButton(onClick = {
                                autoCreateReminder = true
                                reminderType = "पुण्यतिथि"
                                Toast.makeText(context, "पुण्यतिथि स्मरण रिमाइंडर सेट किया गया!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = "Set Death Reminder",
                                    tint = if (autoCreateReminder && reminderType == "पुण्यतिथि") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .onDoubleTapSpy {
                                autoCreateReminder = true
                                reminderType = "पुण्यतिथि"
                                Toast.makeText(context, "डबल-क्लिक: पुण्यतिथि स्मरण रिमाइंडर सेट किया गया!", Toast.LENGTH_SHORT).show()
                            }
                    )
                }

                // BIRTHPLACE & OCCUPATION SIDE-BY-SIDE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = birthPlace,
                        onValueChange = { birthPlace = it },
                        label = { Text("जन्मस्थान") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Place, null, modifier = Modifier.size(16.dp)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = occupation,
                        onValueChange = { occupation = it },
                        label = { Text("पेशा/कार्य") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Work, null, modifier = Modifier.size(16.dp)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // ENCRYPTED DIRECT PERSONAL SENSITIVE DATA
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.VerifiedUser, "Secure", tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("पूर्ण एन्क्रिप्शन सुरक्षा (E2E Encrypted Field Shield)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = decryptedMobile,
                                onValueChange = { decryptedMobile = it },
                                label = { Text("मोबाइल (Secure)") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Filled.Phone, null, modifier = Modifier.size(16.dp)) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = decryptedEmail,
                                onValueChange = { decryptedEmail = it },
                                label = { Text("ईमेल (Secure)") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Filled.Email, null, modifier = Modifier.size(16.dp)) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // CERTIFICATES & ATTACHMENTS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("सर्टिफिकेट / दस्तावेज", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            if (documentPath.isNotBlank()) "✓ फ़ाइल लिंक कर दी गयी है" else "कोई दस्तावेज अपलोड नहीं है",
                            fontSize = 11.sp,
                            color = if (documentPath.isNotBlank()) Color(0xFF2E7D32) else Color.Gray,
                            fontWeight = if (documentPath.isNotBlank()) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Button(
                        onClick = { docLauncher.launch("*/*") },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer, 
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Filled.AttachFile, null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (documentPath.isNotBlank()) "बदलें" else "अपलोड", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // RELATIONSHIP SELECTORS DESCRIPTION
                Text(
                    text = "पारिवारिक सम्बन्ध (Set Family Linkages)", 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 12.sp, 
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), 
                    modifier = Modifier.padding(top = 4.dp)
                )

                val otherMembers = remember(allMembers, member) {
                    allMembers.filter { it.id != member?.id }
                }

                // FATHER LINK
                ExposedRelationMenu(
                    label = "पिता (Father): " + (otherMembers.find { it.id == fatherId }?.fullName ?: "लिंक करें / Link"),
                    expanded = showFatherDropdown,
                    onToggle = { showFatherDropdown = !showFatherDropdown },
                    onDismiss = { showFatherDropdown = false },
                    items = otherMembers.filter { it.gender == "Male" },
                    onSelect = { fId ->
                        fatherId = fId
                        showFatherDropdown = false
                    },
                    onClear = {
                        fatherId = null
                        showFatherDropdown = false
                    }
                )

                // MOTHER LINK
                ExposedRelationMenu(
                    label = "माता (Mother): " + (otherMembers.find { it.id == motherId }?.fullName ?: "लिंक करें / Link"),
                    expanded = showMotherDropdown,
                    onToggle = { showMotherDropdown = !showMotherDropdown },
                    onDismiss = { showMotherDropdown = false },
                    items = otherMembers.filter { it.gender == "Female" },
                    onSelect = { mId ->
                        motherId = mId
                        showMotherDropdown = false
                    },
                    onClear = {
                        motherId = null
                        showMotherDropdown = false
                    }
                )

                // SPOUSE LINK
                ExposedRelationMenu(
                    label = "जीवनसाथी (Spouse): " + (otherMembers.find { it.id == spouseId }?.fullName ?: "लिंक करें / Link"),
                    expanded = showSpouseDropdown,
                    onToggle = { showSpouseDropdown = !showSpouseDropdown },
                    onDismiss = { showSpouseDropdown = false },
                    items = otherMembers,
                    onSelect = { sId ->
                        spouseId = sId
                        showSpouseDropdown = false
                    },
                    onClear = {
                        spouseId = null
                        showSpouseDropdown = false
                    }
                )

                // CONDITIONAL MARRIAGE DATE FIELD IF SPOUSE IS SELECTED
                if (spouseId != null) {
                    OutlinedTextField(
                        value = marriageDate,
                        onValueChange = { marriageDate = it },
                        label = { Text("शादी की तारीख (Marriage Date)*") },
                        placeholder = { Text("YYYY-MM-DD (e.g. 1920-05-15)") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Celebration, null, modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            IconButton(onClick = {
                                autoCreateReminder = true
                                reminderType = "विवाह वर्षगांठ"
                                Toast.makeText(context, "विवाह वर्षगांठ रिमाइंडर सेट किया गया!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = "Set Marriage Reminder",
                                    tint = if (autoCreateReminder && reminderType == "विवाह वर्षगांठ") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .onDoubleTapSpy {
                                autoCreateReminder = true
                                reminderType = "विवाह वर्षगांठ"
                                Toast.makeText(context, "डबल-क्लिक: विवाह वर्षगांठ रिमाइंडर सेट किया गया!", Toast.LENGTH_SHORT).show()
                            }
                    )
                }

                // PERSONALIZED NOTES TEXT AREA
                OutlinedTextField(
                    value = decryptedNotes,
                    onValueChange = { decryptedNotes = it },
                    label = { Text("पारिवारिक इतिहास (Notes - ENCRYPTED)") },
                    minLines = 3,
                    leadingIcon = { Icon(Icons.Filled.HistoryEdu, null, modifier = Modifier.size(18.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // REMINDERS OPTION (FOR ADD MODE - upgraded to elegant switch)
                if (!isEditMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("उत्सव रिमाइंडर (Add Reminder)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("वंशावली कैलेंडर में स्मरण तिथि जोड़ें", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                            Switch(
                                checked = autoCreateReminder,
                                onCheckedChange = { autoCreateReminder = it }
                            )
                        }

                        if (autoCreateReminder) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth().padding(start = 30.dp)
                            ) {
                                listOf("जन्मदिन", "विवाह वर्षगांठ", "पुण्यतिथि").forEach { type ->
                                    val isTypeSelected = (reminderType == type)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { reminderType = type }
                                            .background(
                                                if (isTypeSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) 
                                                else Color.Transparent
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        RadioButton(
                                            selected = isTypeSelected,
                                            onClick = { reminderType = type }
                                        )
                                        Text(type, fontSize = 12.sp, fontWeight = if (isTypeSelected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    }
                }

                // STARRED HIGHLIGHT OPTION (upgraded to elegant switch)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .clickable { isStarred = !isStarred }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isStarred) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = null,
                            tint = if (isStarred) Color(0xFFFFB300) else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("प्रमुख सदस्य (Highlight Profile)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("वंशावली चार्ट में इन्हें चिन्हित किया जाएगा", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                    Switch(
                        checked = isStarred,
                        onCheckedChange = { isStarred = it }
                    )
                }

                // TARGET SUBJECT FOR PDF CERTIFICATE
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .clickable { isTargetSubject = !isTargetSubject }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isTargetSubject) Icons.Filled.Verified else Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = if (isTargetSubject) Color(0xFF2E7D32) else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("वंशावली का मुख्य सदस्य (Target Subject)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("इन्हीं के अनुसार स्व-घोषणा प्रमाण-पत्र ऑटो-फिल होगा", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                    Switch(
                        checked = isTargetSubject,
                        onCheckedChange = { isTargetSubject = it }
                    )
                }

                // DELETE BUTTON
                if (isEditMode && onDelete != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(Icons.Filled.DeleteForever, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("सदस्य हटाएं (Delete Profile)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (firstName.isNotBlank()) {
                        onSave(
                            firstName,
                            lastName,
                            gender,
                            birthDate,
                            deathDate,
                            birthPlace,
                            occupation,
                            decryptedNotes,
                            photoPath,
                            documentPath,
                            fatherId,
                            motherId,
                            spouseId,
                            marriageDate,
                            isTargetSubject,
                            decryptedMobile,
                            decryptedEmail,
                            isStarred,
                            if (autoCreateReminder) reminderType else null
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text("सहेजें (Save)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text("रद्द करें")
            }
        }
    )
}

@Composable
fun ExposedRelationMenu(
    label: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    items: List<FamilyMember>,
    onSelect: (Long) -> Unit,
    onClear: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable { onToggle() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 13.sp)
            Icon(
                imageVector = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = Color.Gray
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            DropdownMenuItem(
                text = { Text("कोई नहीं / None (Clear Relation)", fontWeight = FontWeight.Bold, color = Color.Red) },
                onClick = onClear
            )
            items.forEach { m ->
                DropdownMenuItem(
                    text = { Text(m.fullName) },
                    onClick = { onSelect(m.id) }
                )
            }
        }
    }
}

fun Modifier.onDoubleTapSpy(onDoubleTap: () -> Unit): Modifier = this.pointerInput(onDoubleTap) {
    awaitPointerEventScope {
        var lastTapTime = 0L
        while (true) {
            val event = awaitPointerEvent()
            if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Press) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTapTime < 300) {
                    onDoubleTap()
                }
                lastTapTime = currentTime
            }
        }
    }
}

