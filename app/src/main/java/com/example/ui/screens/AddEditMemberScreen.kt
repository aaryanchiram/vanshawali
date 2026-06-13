package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FamilyMember
import com.example.ui.viewmodel.FamilyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMemberScreen(
    viewModel: FamilyViewModel,
    memberId: Int,
    initialFatherId: Int? = null,
    initialMotherId: Int? = null,
    initialSpouseId: Int? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isHindi by viewModel.isHindi.collectAsState()
    val allMembers by viewModel.allMembers.collectAsState()

    // Determine lock editing person
    val targetConfig = remember(memberId, allMembers) {
        allMembers.find { it.id == memberId }
    }

    // Form states
    var nameEnglish by remember { mutableStateOf("") }
    var nameHindi by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var generationString by remember { mutableStateOf("3") }
    var fatherId by remember { mutableStateOf<Int?>(null) }
    var motherId by remember { mutableStateOf<Int?>(null) }
    var spouseId by remember { mutableStateOf<Int?>(null) }
    var birthDate by remember { mutableStateOf("") }
    var deathDate by remember { mutableStateOf("") }
    var isDeceased by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var occupation by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#4F46E5") }

    // Init values
    LaunchedEffect(targetConfig) {
        if (targetConfig != null) {
            nameEnglish = targetConfig.nameEnglish
            nameHindi = targetConfig.nameHindi
            gender = targetConfig.gender
            generationString = targetConfig.generation.toString()
            fatherId = targetConfig.fatherId
            motherId = targetConfig.motherId
            spouseId = targetConfig.spouseId
            birthDate = targetConfig.birthDate ?: ""
            deathDate = targetConfig.deathDate ?: ""
            isDeceased = targetConfig.isDeceased
            phone = targetConfig.phone ?: ""
            address = targetConfig.address ?: ""
            occupation = targetConfig.occupation ?: ""
            notes = targetConfig.notes ?: ""
            selectedColorHex = targetConfig.profileColorHex
        } else {
            // Apply quick links from tree UI
            if (initialFatherId != null) fatherId = initialFatherId
            if (initialMotherId != null) motherId = initialMotherId
            if (initialSpouseId != null) spouseId = initialSpouseId
            
            // Auto generation deduction
            val father = allMembers.find { it.id == initialFatherId }
            val mother = allMembers.find { it.id == initialMotherId }
            val spouse = allMembers.find { it.id == initialSpouseId }
            if (father != null) {
                generationString = (father.generation + 1).toString()
            } else if (mother != null) {
                generationString = (mother.generation + 1).toString()
            } else if (spouse != null) {
                generationString = spouse.generation.toString()
            }
        }
    }

    val scrollState = rememberScrollState()

    // Validations
    var nameError by remember { mutableStateOf(false) }
    var generationError by remember { mutableStateOf(false) }

    // Available relations candidates (exclude self to avoid loops)
    val eligibleFathers = remember(memberId, allMembers) {
        allMembers.filter { it.id != memberId && it.gender == "Male" }
    }
    val eligibleMothers = remember(memberId, allMembers) {
        allMembers.filter { it.id != memberId && it.gender == "Female" }
    }
    val eligibleSpouses = remember(memberId, allMembers) {
        allMembers.filter { it.id != memberId }
    }

    val colorsList = listOf(
        "#4F46E5", // Indigo
        "#1D4ED8", // Blue
        "#047857", // Green
        "#0E7490", // Cyan
        "#7C3AED", // Purple
        "#BE185D", // Pink
        "#B45309", // Amber
        "#DB2777", // Rose
        "#0F172A"  // Slate
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (targetConfig != null) {
                            if (isHindi) "विवरण संपादित करें" else "Edit Details"
                        } else {
                            if (isHindi) "नया सदस्य जोड़ें" else "Add Family Member"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_btn")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // --- PERSONAL DETAILS CARD ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (isHindi) "व्यक्तिगत जानकारी" else "Personal Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // English Name
                    OutlinedTextField(
                        value = nameEnglish,
                        onValueChange = {
                            nameEnglish = it
                            nameError = false
                        },
                        label = { Text(if (isHindi) "पूरा नाम (अंग्रेजी में)" else "Full Name (In English)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_name_eng"),
                        isError = nameError,
                        placeholder = { Text("e.g., Aaryan Chiram") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Hindi Name
                    OutlinedTextField(
                        value = nameHindi,
                        onValueChange = { nameHindi = it },
                        label = { Text(if (isHindi) "पूरा नाम (हिंदी में)" else "Full Name (In Hindi)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_name_hindi"),
                        placeholder = { Text("उदाहरण: आर्यन चिराम") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Gender Row selection
                    Column {
                        Text(
                            text = if (isHindi) "लिंग" else "Gender",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val genders = listOf("Male", "Female")
                            genders.forEach { option ->
                                val selected = gender == option
                                OutlinedButton(
                                    onClick = { gender = option },
                                    modifier = Modifier.weight(1f).testTag("input_gender_$option"),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                    ),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        width = if (selected) 2.dp else 1.dp
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (option == "Male") Icons.Default.Male else Icons.Default.Female,
                                        contentDescription = null,
                                        tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isHindi) (if (option == "Male") "पुरुष" else "महिला") else option,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Generation input
                    OutlinedTextField(
                        value = generationString,
                        onValueChange = {
                            generationString = it
                            generationError = false
                        },
                        label = { Text(if (isHindi) "पीढ़ी संख्या (जैसे: दादाजी = 1, पिता = 2, आप = 3)" else "Generation Number (e.g., Gp=1, Father=2, Self=3)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_generation"),
                        isError = generationError,
                        placeholder = { Text("e.g., 3") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Profile Color Selector
                    Column {
                        Text(
                            text = if (isHindi) "प्रोफ़ाइल रंग चुनें" else "Choose Visual Theme Color",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            colorsList.forEach { col ->
                                val parsed = Color(android.graphics.Color.parseColor(col))
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(parsed)
                                        .clickable { selectedColorHex = col },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selectedColorHex == col) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- RELATIONSHIPS CARD ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (isHindi) "पारिवारिक संबंध" else "Family Connections",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // 1. Father Selector
                    var fatherExp by remember { mutableStateOf(false) }
                    val fatherLabelAndVal = remember(fatherId, allMembers) {
                        val dad = allMembers.find { it.id == fatherId }
                        dad?.getName(isHindi) ?: if (isHindi) "कोई नहीं / खाली" else "None"
                    }
                    Box {
                        OutlinedTextField(
                            value = fatherLabelAndVal,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (isHindi) "पिता का चयन करें" else "Select Father") },
                            modifier = Modifier.fillMaxWidth().testTag("select_father"),
                            trailingIcon = {
                                IconButton(onClick = { fatherExp = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                        DropdownMenu(expanded = fatherExp, onDismissRequest = { fatherExp = false }) {
                            DropdownMenuItem(
                                text = { Text(if (isHindi) "कोई नहीं / रिफरेंस हटाएँ" else "None / Clear link") },
                                onClick = {
                                    fatherId = null
                                    fatherExp = false
                                }
                            )
                            eligibleFathers.forEach { dad ->
                                DropdownMenuItem(
                                    text = { Text("${dad.getName(isHindi)} (Gen ${dad.generation})") },
                                    onClick = {
                                        fatherId = dad.id
                                        fatherExp = false
                                        // Auto adjust generation if child's gen is too small
                                        val curGen = generationString.toIntOrNull() ?: 0
                                        if (curGen <= dad.generation) {
                                            generationString = (dad.generation + 1).toString()
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // 2. Mother Selector
                    var motherExp by remember { mutableStateOf(false) }
                    val motherLabelAndVal = remember(motherId, allMembers) {
                        val mom = allMembers.find { it.id == motherId }
                        mom?.getName(isHindi) ?: if (isHindi) "कोई नहीं / खाली" else "None"
                    }
                    Box {
                        OutlinedTextField(
                            value = motherLabelAndVal,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (isHindi) "माता का चयन करें" else "Select Mother") },
                            modifier = Modifier.fillMaxWidth().testTag("select_mother"),
                            trailingIcon = {
                                IconButton(onClick = { motherExp = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                        DropdownMenu(expanded = motherExp, onDismissRequest = { motherExp = false }) {
                            DropdownMenuItem(
                                text = { Text(if (isHindi) "कोई नहीं / रिफरेंस हटाएँ" else "None / Clear link") },
                                onClick = {
                                    motherId = null
                                    motherExp = false
                                }
                            )
                            eligibleMothers.forEach { mom ->
                                DropdownMenuItem(
                                    text = { Text("${mom.getName(isHindi)} (Gen ${mom.generation})") },
                                    onClick = {
                                        motherId = mom.id
                                        motherExp = false
                                        val curGen = generationString.toIntOrNull() ?: 0
                                        if (curGen <= mom.generation) {
                                            generationString = (mom.generation + 1).toString()
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // 3. Spouse Selector
                    var spouseExp by remember { mutableStateOf(false) }
                    val spouseLabelAndVal = remember(spouseId, allMembers) {
                        val sp = allMembers.find { it.id == spouseId }
                        sp?.getName(isHindi) ?: if (isHindi) "कोई नहीं / खाली" else "None"
                    }
                    Box {
                        OutlinedTextField(
                            value = spouseLabelAndVal,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (isHindi) "जीवनसाथी का चयन करें" else "Select Spouse") },
                            modifier = Modifier.fillMaxWidth().testTag("select_spouse"),
                            trailingIcon = {
                                IconButton(onClick = { spouseExp = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                        DropdownMenu(expanded = spouseExp, onDismissRequest = { spouseExp = false }) {
                            DropdownMenuItem(
                                text = { Text(if (isHindi) "कोई नहीं / रिफरेंस हटाएँ" else "None / Clear link") },
                                onClick = {
                                    spouseId = null
                                    spouseExp = false
                                }
                            )
                            eligibleSpouses.forEach { sp ->
                                DropdownMenuItem(
                                    text = { Text("${sp.getName(isHindi)} (Gen ${sp.generation})") },
                                    onClick = {
                                        spouseId = sp.id
                                        spouseExp = false
                                        // Auto match spouse generation
                                        generationString = sp.generation.toString()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- DECEASED VS ALIVE CARD ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isHindi) "जीवनकाल स्थिति" else "Life Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isHindi) "क्या सदस्य स्वर्गवासी हैं?" else "Is this member deceased?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isDeceased,
                            onCheckedChange = { isDeceased = it },
                            modifier = Modifier.testTag("switch_deceased")
                        )
                    }

                    if (isDeceased) {
                        OutlinedTextField(
                            value = deathDate,
                            onValueChange = { deathDate = it },
                            label = { Text(if (isHindi) "स्वर्गवास वर्ष / दिनांक" else "Year / Date of Passing") },
                            modifier = Modifier.fillMaxWidth().testTag("input_death_date"),
                            placeholder = { Text("e.g., 2021-02-12 or 2021") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- LIFETIME & BIOGRAPHY DETAILS ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (isHindi) "संपर्क व व्यावसायिक जानकारी" else "Bio & Contact Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Phone or Mobile text
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(if (isHindi) "फ़ोन / मोबाइल नंबर" else "Mobile Number") },
                        modifier = Modifier.fillMaxWidth().testTag("input_phone"),
                        placeholder = { Text("+919876543210") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Address text
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(if (isHindi) "पता" else "Home Address") },
                        modifier = Modifier.fillMaxWidth().testTag("input_address"),
                        placeholder = { Text(if (isHindi) "रायपुर, छत्तीसगढ़" else "Raipur, Chhattisgarh") },
                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Birth details
                    OutlinedTextField(
                        value = birthDate,
                        onValueChange = { birthDate = it },
                        label = { Text(if (isHindi) "जन्म तिथि / संवत" else "Birth Date / Year") },
                        modifier = Modifier.fillMaxWidth().testTag("input_birth_date"),
                        placeholder = { Text("e.g. 1970-07-22") },
                        leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Occupation/Work
                    OutlinedTextField(
                        value = occupation,
                        onValueChange = { occupation = it },
                        label = { Text(if (isHindi) "व्यवसाय / पेशा (जैसे: शिक्षक, कृषक)" else "Occupation / Profession (e.g., Teacher)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_occupation"),
                        placeholder = { Text("e.g., Business") },
                        leadingIcon = { Icon(Icons.Default.Work, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Short Biography / notes
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(if (isHindi) "विशेष टिप्पणी / जीवन परिचय" else "Biographical Notes") },
                        modifier = Modifier.fillMaxWidth().height(100.dp).testTag("input_notes"),
                        placeholder = { Text(if (isHindi) "उनके बारे में कुछ विशेष..." else "Interesting story, achievements or legacy details...") },
                        maxLines = 4,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SAVE PROGRESS BUTTON ---
            Button(
                onClick = {
                    // Validations
                    var hasErr = false
                    if (nameEnglish.isBlank()) {
                        nameError = true
                        hasErr = true
                    }
                    val genVal = generationString.trim().toIntOrNull()
                    if (genVal == null) {
                        generationError = true
                        hasErr = true
                    }

                    if (!hasErr && genVal != null) {
                        val updated = FamilyMember(
                            id = memberId,
                            nameEnglish = nameEnglish.trim(),
                            nameHindi = nameHindi.trim(),
                            gender = gender,
                            generation = genVal,
                            fatherId = fatherId,
                            motherId = motherId,
                            spouseId = spouseId,
                            birthDate = birthDate.trim().ifBlank { null },
                            deathDate = if (isDeceased) deathDate.trim().ifBlank { null } else null,
                            isDeceased = isDeceased,
                            phone = phone.trim().ifBlank { null },
                            address = address.trim().ifBlank { null },
                            occupation = occupation.trim().ifBlank { null },
                            notes = notes.trim().ifBlank { null },
                            profileColorHex = selectedColorHex
                        )
                        viewModel.saveMember(updated)
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_member_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isHindi) "सहेजें" else "Save Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
