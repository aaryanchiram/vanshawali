package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Environment
import android.view.View
import androidx.core.content.FileProvider
import androidx.compose.ui.geometry.Offset
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TreeExporter {

    /**
     * Serializes all members, folders, and reminders of a specific folder to JSON
     */
    fun exportTreeToJson(
        folder: FamilyFolder,
        members: List<FamilyMember>,
        reminders: List<Reminder>,
        userPin: String
    ): String {
        val root = JSONObject()
        
        // Metadata
        root.put("app", "VanshVriksh")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        // Folder
        val folderObj = JSONObject().apply {
            put("id", folder.id)
            put("name", folder.name)
            put("description", folder.description)
            put("referenceYear", folder.referenceYear)
            put("certificatePath", folder.certificatePath)
        }
        root.put("folder", folderObj)

        // Members (Encrypt notes, email, mobile if not already encrypted)
        val membersArray = JSONArray()
        members.forEach { m ->
            val mObj = JSONObject().apply {
                put("id", m.id)
                put("firstName", m.firstName)
                put("lastName", m.lastName)
                put("gender", m.gender)
                put("birthDate", m.birthDate)
                put("deathDate", m.deathDate)
                put("birthPlace", m.birthPlace)
                put("occupation", m.occupation)
                
                // Keep encrypted values secure
                put("notes", m.notes)
                put("mobile", m.mobile)
                put("email", m.email)
                
                put("photoPath", m.photoPath)
                put("documentPath", m.documentPath)
                put("fatherId", m.fatherId ?: -1L)
                put("motherId", m.motherId ?: -1L)
                put("spouseId", m.spouseId ?: -1L)
                put("isStarred", m.isStarred)
            }
            membersArray.put(mObj)
        }
        root.put("members", membersArray)

        // Reminders
        val remindersArray = JSONArray()
        reminders.forEach { r ->
            val rObj = JSONObject().apply {
                put("id", r.id)
                put("title", r.title)
                put("date", r.date)
                put("notes", r.notes)
            }
            remindersArray.put(rObj)
        }
        root.put("reminders", remindersArray)

        return root.toString(4)
    }

    /**
     * Parses JSON back to structure and inserts it into database
     */
    suspend fun importTreeFromJson(
        jsonStr: String,
        dao: FamilyDao
    ): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            if (!root.has("app") || root.getString("app") != "VanshVriksh") {
                return false
            }

            // Step 1: Create new Folder
            val folderObj = root.getJSONObject("folder")
            val newFolder = FamilyFolder(
                name = folderObj.getString("name") + " (Imported)",
                description = folderObj.optString("description", ""),
                referenceYear = folderObj.optString("referenceYear", ""),
                certificatePath = folderObj.optString("certificatePath", "")
            )
            val newFolderId = dao.insertFolder(newFolder)

            // Step 2: Parse members
            val membersArray = root.getJSONArray("members")
            val oldToNewIdMap = mutableMapOf<Long, Long>()

            // We do 2 passes to link relationships correctly
            // Pass 1: Insert core profiles
            val tempMembersList = mutableListOf<Triple<Long, FamilyMember, Triple<Long, Long, Long>>>()
            for (i in 0 until membersArray.length()) {
                val mObj = membersArray.getJSONObject(i)
                val oldId = mObj.getLong("id")
                
                val fId = mObj.optLong("fatherId", -1L)
                val mId = mObj.optLong("motherId", -1L)
                val sId = mObj.optLong("spouseId", -1L)

                val member = FamilyMember(
                    folderId = newFolderId,
                    firstName = mObj.getString("firstName"),
                    lastName = mObj.optString("lastName", ""),
                    gender = mObj.getString("gender"),
                    birthDate = mObj.optString("birthDate", ""),
                    deathDate = mObj.optString("deathDate", ""),
                    birthPlace = mObj.optString("birthPlace", ""),
                    occupation = mObj.optString("occupation", ""),
                    notes = mObj.optString("notes", ""),
                    photoPath = mObj.optString("photoPath", ""),
                    documentPath = mObj.optString("documentPath", ""),
                    isStarred = mObj.optBoolean("isStarred", false)
                )
                
                tempMembersList.add(Triple(oldId, member, Triple(fId, mId, sId)))
            }

            // Insert to DB and get new IDs
            for (item in tempMembersList) {
                val oldId = item.first
                val memberToInsert = item.second
                val dbId = dao.insertMember(memberToInsert)
                oldToNewIdMap[oldId] = dbId
            }

            // Pass 2: Update relationship pointers using the map
            for (item in tempMembersList) {
                val oldId = item.first
                val newId = oldToNewIdMap[oldId] ?: continue
                val relations = item.third
                
                val oldFatherId = relations.first
                val oldMotherId = relations.second
                val oldSpouseId = relations.third

                val newFatherId = if (oldFatherId != -1L) oldToNewIdMap[oldFatherId] else null
                val newMotherId = if (oldMotherId != -1L) oldToNewIdMap[oldMotherId] else null
                val newSpouseId = if (oldSpouseId != -1L) oldToNewIdMap[oldSpouseId] else null

                val insertedMember = dao.getMemberById(newId)
                if (insertedMember != null) {
                    val updatedMember = insertedMember.copy(
                        fatherId = newFatherId,
                        motherId = newMotherId,
                        spouseId = newSpouseId
                    )
                    dao.updateMember(updatedMember)
                }
            }

            // Step 3: Insert reminders
            if (root.has("reminders")) {
                val remindersArray = root.getJSONArray("reminders")
                for (i in 0 until remindersArray.length()) {
                    val rObj = remindersArray.getJSONObject(i)
                    // Pick a random member from the imported list or associate generally
                    val firstImportedMemberId = oldToNewIdMap.values.firstOrNull() ?: continue
                    val firstImportedMemberName = dao.getMemberById(firstImportedMemberId)?.fullName ?: "Member"
                    
                    val reminder = Reminder(
                        folderId = newFolderId,
                        memberId = firstImportedMemberId,
                        memberName = firstImportedMemberName,
                        title = rObj.getString("title"),
                        date = rObj.getString("date"),
                        notes = rObj.optString("notes", "")
                    )
                    dao.insertReminder(reminder)
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Share exported JSON string as a backup file
     */
    fun shareBackupFile(context: Context, jsonStr: String, folderName: String) {
        try {
            val fileName = "VanshVriksh_Backup_${folderName.replace(" ", "_")}.json"
            val backupFile = File(context.cacheDir, fileName)
            val writer = FileWriter(backupFile)
            writer.write(jsonStr)
            writer.flush()
            writer.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "VanshVriksh Offline Backup: $folderName")
                putExtra(Intent.EXTRA_TEXT, "यह VanshVriksh ऐप का ऑफलाइन फैमली ट्री बैकअप फ़ाइल है।")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "बैकअप शेयर करें  (Share Backup)"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Generates a PDF for an individual family member's details and shares it.
     */
    fun shareIndividualMemberPdf(context: Context, member: FamilyMember) {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(1200, 1700, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        
        val titlePaint = Paint().apply {
            textSize = 50f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            color = android.graphics.Color.BLACK
        }
        val detailPaint = Paint().apply {
            textSize = 35f
            color = android.graphics.Color.DKGRAY
        }
        
        canvas.drawText("पारिवारिक सदस्य विवरण (Member Details)", 100f, 150f, titlePaint)
        canvas.drawText("नाम: ${member.fullName}", 100f, 250f, detailPaint)
        canvas.drawText("आयु: ${member.age}", 100f, 300f, detailPaint)
        canvas.drawText("व्यवसाय: ${member.occupation.ifBlank { "N/A" }}", 100f, 350f, detailPaint)
        canvas.drawText("जन्म स्थान: ${member.birthPlace.ifBlank { "N/A" }}", 100f, 400f, detailPaint)
        
        pdfDocument.finishPage(page)
        
        try {
            val fileName = "Member_${member.firstName.replace(" ", "_")}.pdf"
            val file = File(context.cacheDir, fileName)
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Member Details: ${member.fullName}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "सदस्य विवरण शेयर करें"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Share a Jetpack Compose drawing rendered as a bitmap
     */
    fun shareBitmapImage(context: Context, bitmap: Bitmap, treeName: String) {
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val stream = FileOutputStream("$cachePath/vanshvriksh_tree.png")
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val imageFile = File(cachePath, "vanshvriksh_tree.png")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "VanshVriksh Family Tree Image: $treeName")
                putExtra(Intent.EXTRA_TEXT, "$treeName का सुंदर डिजिटल फैमिली ट्री चित्र!")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "चित्र शेयर करें (Share Family Tree Image)"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Reconstructs scalable tree coordinates deterministically for drawing outside UI
     */
    fun calculateTreeCoordinates(members: List<FamilyMember>): Map<Long, Offset> {
        val levels = mutableMapOf<Long, Int>()
        
        fun calculateRawLevel(member: FamilyMember, visited: MutableSet<Long>): Int {
            if (visited.contains(member.id)) return levels[member.id] ?: 0
            visited.add(member.id)
            levels[member.id]?.let { return it }

            val fLevel = member.fatherId?.let { fid ->
                members.find { it.id == fid }?.let { calculateRawLevel(it, visited) }
            } ?: -1

            val mLevel = member.motherId?.let { mid ->
                members.find { it.id == mid }?.let { calculateRawLevel(it, visited) }
            } ?: -1

            val computed = maxOf(fLevel, mLevel) + 1
            levels[member.id] = computed
            return computed
        }

        members.forEach { m ->
            calculateRawLevel(m, mutableSetOf())
        }

        // Align spouse levels to the maximum of the two spouse levels to keep couples on same line,
        // and recursively push childrens' levels below them
        var changed = true
        var passes = 0
        while (changed && passes < 15) {
            changed = false
            passes++
            members.forEach { m ->
                val mLevel = levels[m.id] ?: 0
                if (m.spouseId != null) {
                    val sLevel = levels[m.spouseId] ?: 0
                    if (mLevel != sLevel) {
                        val maxL = maxOf(mLevel, sLevel)
                        levels[m.id] = maxL
                        levels[m.spouseId] = maxL
                        changed = true
                    }
                }
                val fL = m.fatherId?.let { levels[it] } ?: -1
                val mL = m.motherId?.let { levels[it] } ?: -1
                val minChildLevel = maxOf(fL, mL) + 1
                val currentL = levels[m.id] ?: 0
                if (currentL < minChildLevel) {
                    levels[m.id] = minChildLevel
                    changed = true
                }
            }
        }

        val nodeWidthDp = 145f
        val spouseCardGap = 25f
        val siblingGap = 30f
        val verticalSpacingDp = 190f

        val coords = mutableMapOf<Long, Offset>()
        val nextXAtLevel = mutableMapOf<Int, Float>()
        val positioned = mutableSetOf<Long>()

        // Helper to recursively shift coordinates of a node and its descendants
        fun shiftSubtree(mId: Long, dx: Float, visited: MutableSet<Long> = mutableSetOf()) {
            if (visited.contains(mId)) return
            visited.add(mId)
            coords[mId] = coords[mId]?.let { Offset(it.x + dx, it.y) } ?: Offset(dx, 0f)
            
            val member = members.find { it.id == mId } ?: return
            val spouse = member.spouseId?.let { sId -> members.find { it.id == sId } }
            if (spouse != null && !visited.contains(spouse.id)) {
                visited.add(spouse.id)
                coords[spouse.id] = coords[spouse.id]?.let { Offset(it.x + dx, it.y) } ?: Offset(dx, 0f)
            }
            
            val children = members.filter { 
                (it.fatherId == member.id || it.motherId == member.id) || 
                (spouse != null && (it.fatherId == spouse.id || it.motherId == spouse.id))
            }.distinctBy { it.id }
            
            children.forEach { child ->
                shiftSubtree(child.id, dx, visited)
            }
        }

        // Helper to recompute contours for all levels based on current positions
        fun recomputeContours() {
            nextXAtLevel.clear()
            coords.forEach { (mId, offset) ->
                val l = levels[mId] ?: 0
                val rightEdge = offset.x + nodeWidthDp + siblingGap
                nextXAtLevel[l] = maxOf(nextXAtLevel[l] ?: 100f, rightEdge)
            }
        }

        // Recursive layout function using beauty contour algorithm
        fun layoutSubtree(mId: Long, level: Int) {
            if (positioned.contains(mId)) return
            positioned.add(mId)

            val member = members.find { it.id == mId } ?: return
            val spouse = member.spouseId?.let { sId -> members.find { it.id == sId } }
            if (spouse != null) {
                positioned.add(spouse.id)
            }

            // 1. Recursively layout children first
            val children = members.filter { 
                (it.fatherId == member.id || it.motherId == member.id) || 
                (spouse != null && (it.fatherId == spouse.id || it.motherId == spouse.id))
            }.distinctBy { it.id }

            children.forEach { child ->
                val childL = levels[child.id] ?: (level + 1)
                layoutSubtree(child.id, childL)
            }

            // 2. Position this couple or member
            val y = level * verticalSpacingDp + 120f

            if (spouse != null) {
                val coupleWidth = nodeWidthDp * 2 + spouseCardGap
                val childCoords = children.mapNotNull { coords[it.id] }
                
                val coupleStart = if (childCoords.isNotEmpty()) {
                    val minChildX = childCoords.minOf { it.x }
                    val maxChildX = childCoords.maxOf { it.x }
                    val midChildrenX = (minChildX + maxChildX + nodeWidthDp) / 2f
                    midChildrenX - coupleWidth / 2f
                } else {
                    nextXAtLevel[level] ?: 100f
                }

                val currentContour = nextXAtLevel[level] ?: 100f
                val finalStart = maxOf(coupleStart, currentContour)

                val x1 = finalStart
                val x2 = finalStart + nodeWidthDp + spouseCardGap

                coords[member.id] = Offset(x1, y)
                coords[spouse.id] = Offset(x2, y)

                // Recompute contours immediately
                recomputeContours()

                // If shifted to the right, shift children as well
                if (finalStart > coupleStart && children.isNotEmpty()) {
                    val shiftAmount = finalStart - coupleStart
                    val visited = mutableSetOf<Long>()
                    children.forEach { child ->
                        shiftSubtree(child.id, shiftAmount, visited)
                    }
                    recomputeContours()
                }
            } else {
                val childCoords = children.mapNotNull { coords[it.id] }
                val idealX = if (childCoords.isNotEmpty()) {
                    val minChildX = childCoords.minOf { it.x }
                    val maxChildX = childCoords.maxOf { it.x }
                    val midChildrenX = (minChildX + maxChildX + nodeWidthDp) / 2f
                    midChildrenX - nodeWidthDp / 2f
                } else {
                    nextXAtLevel[level] ?: 100f
                }

                val currentContour = nextXAtLevel[level] ?: 100f
                val finalX = maxOf(idealX, currentContour)

                coords[member.id] = Offset(finalX, y)
                recomputeContours()

                if (finalX > idealX && children.isNotEmpty()) {
                    val shiftAmount = finalX - idealX
                    val visited = mutableSetOf<Long>()
                    children.forEach { child ->
                        shiftSubtree(child.id, shiftAmount, visited)
                    }
                    recomputeContours()
                }
            }
        }

        // Trace and position root ancestors side-by-side
        val roots = members.filter { m ->
            (m.fatherId == null || members.none { it.id == m.fatherId }) &&
            (m.motherId == null || members.none { it.id == m.motherId })
        }

        val primaryRoots = roots.filter { m ->
            val spouse = members.find { it.id == m.spouseId }
            spouse == null || m.id < spouse.id || !roots.contains(spouse)
        }

        primaryRoots.forEach { root ->
            layoutSubtree(root.id, level = levels[root.id] ?: 0)
        }

        // Leftovers loop for single disconnected nodes or alternative families
        members.forEach { m ->
            if (!positioned.contains(m.id)) {
                layoutSubtree(m.id, level = levels[m.id] ?: 0)
            }
        }

        return coords
    }

    /**
     * Renders a beautiful visual family tree directly on a high-res bitmap
     */
    fun drawTreeToBitmap(
        context: Context,
        members: List<FamilyMember>,
        treeName: String,
        coordinates: Map<Long, Offset>,
        referenceYear: String = ""
    ): Bitmap {
        val nodeWidth = 145f
        val nodeHeight = 100f

        val minX = coordinates.values.minOfOrNull { it.x } ?: 0f
        val maxX = coordinates.values.maxOfOrNull { it.x } ?: 1000f
        val minY = coordinates.values.minOfOrNull { it.y } ?: 0f
        val maxY = coordinates.values.maxOfOrNull { it.y } ?: 800f

        val marginX = 120f
        val marginTop = 190f
        val marginBottom = 160f

        val treeWidth = maxX - minX + nodeWidth
        val treeHeight = maxY - minY + nodeHeight

        // Measure title and subtitle to ensure the canvas is wide enough for them
        val measureHeaderPaint = Paint().apply {
            textSize = 42f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val measureSubPaint = Paint().apply {
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val titleText = "पारिवारिक वंशवृक्ष (VanshVriksh): $treeName"
        val measureSubTitle = if (referenceYear.isNotBlank()) {
            "भावी पीढ़ी के लिए वंशावली सन $referenceYear के अनुसार"
        } else {
            "भावी पीढ़ियों हेतु वंशक्रम विवरण संकलन"
        }
        val titleWidth = measureHeaderPaint.measureText(titleText)
        val subtitleWidth = measureSubPaint.measureText(measureSubTitle)
        val maxTextWidth = maxOf(titleWidth, subtitleWidth)

        // Adjust canvas width dynamically based on both tree width and text requirements
        val requiredWidthByTree = treeWidth + 2 * marginX
        val requiredWidthByText = maxTextWidth + 2 * marginX
        val baseWidth = maxOf(requiredWidthByTree, requiredWidthByText).toInt().coerceAtLeast(1200)
        val baseHeight = (treeHeight + marginTop + marginBottom).toInt().coerceAtLeast(1000)

        // Increase quality multiplier (Resolution Upscaler: 4.0x DPI)
        val exportScale = 4.0f
        val bitmapWidth = (baseWidth * exportScale).toInt()
        val bitmapHeight = (baseHeight * exportScale).toInt()

        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(exportScale, exportScale)

        val unscaledWidth = baseWidth.toFloat()
        val unscaledHeight = baseHeight.toFloat()

        // Light background for excellent printability
        canvas.drawColor(android.graphics.Color.parseColor("#FCFDF9"))

        // Fine classic border
        val borderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#CFD8DC")
            strokeWidth = 6f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRect(20f, 20f, unscaledWidth - 20f, unscaledHeight - 20f, borderPaint)

        val translateX = -minX + (unscaledWidth - treeWidth) / 2f
        val translateY = -minY + marginTop

        fun getCanvasOffset(coord: Offset): Offset {
            return Offset(coord.x + translateX, coord.y + translateY)
        }

        // Align spouse levels and identify focus member for right badge
        val levels = mutableMapOf<Long, Int>()
        fun calculateRawLevel(member: FamilyMember, visited: MutableSet<Long>): Int {
            if (visited.contains(member.id)) return levels[member.id] ?: 0
            visited.add(member.id)
            levels[member.id]?.let { return it }
            val fLevel = member.fatherId?.let { fid ->
                members.find { it.id == fid }?.let { calculateRawLevel(it, visited) }
            } ?: -1
            val mLevel = member.motherId?.let { mid ->
                members.find { it.id == mid }?.let { calculateRawLevel(it, visited) }
            } ?: -1
            val computed = maxOf(fLevel, mLevel) + 1
            levels[member.id] = computed
            return computed
        }
        members.forEach { m -> calculateRawLevel(m, mutableSetOf()) }

        val leafNodes = members.filter { m ->
            members.none { it.fatherId == m.id || it.motherId == m.id }
        }
        val focusMember = if (leafNodes.isEmpty()) {
            members.maxByOrNull { m -> (levels[m.id] ?: 0) * 1000000L + m.id }
        } else {
            leafNodes.maxByOrNull { m -> (levels[m.id] ?: 0) * 1000000L + m.id }
        }

        // Draw connecting branches with gender colors
        val parentLinePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#8D6E63") // Moss trunk brown
            strokeWidth = 5f
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val daughterLinePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#E91E63") // Classic Daughter Pink
            strokeWidth = 5f
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val sonLinePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#0288D1") // Classic Son Blue-Green
            strokeWidth = 5f
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val spousePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#D32F2F") // Vibrant Red Couple Connection
            strokeWidth = 6f
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        members.forEach { child ->
            val mPoint = coordinates[child.id]?.let { getCanvasOffset(it) } ?: return@forEach
            val dadPoint = (child.fatherId?.let { coordinates[it] } ?: child.motherId?.let { momId ->
                val mother = members.find { it.id == momId }
                mother?.spouseId?.let { coordinates[it] }
            })?.let { getCanvasOffset(it) }

            val momPoint = (child.motherId?.let { coordinates[it] } ?: child.fatherId?.let { dadId ->
                val father = members.find { it.id == dadId }
                father?.spouseId?.let { coordinates[it] }
            })?.let { getCanvasOffset(it) }

            val startPoint = when {
                dadPoint != null && momPoint != null -> {
                    Offset(
                        (dadPoint.x + momPoint.x + nodeWidth) / 2f,
                        dadPoint.y + nodeHeight / 2f
                    )
                }
                dadPoint != null -> {
                    Offset(
                        dadPoint.x + nodeWidth / 2f,
                        dadPoint.y + nodeHeight
                    )
                }
                momPoint != null -> {
                    Offset(
                        momPoint.x + nodeWidth / 2f,
                        momPoint.y + nodeHeight
                    )
                }
                else -> null
            }

            startPoint?.let { start ->
                val end = Offset(
                    mPoint.x + nodeWidth / 2f,
                    mPoint.y
                )
                val midY = (start.y + end.y) / 2f

                val childLinePaint = if (child.gender == "Female") daughterLinePaint else sonLinePaint

                canvas.drawLine(start.x, start.y, start.x, midY, parentLinePaint)
                canvas.drawLine(start.x, midY, end.x, midY, childLinePaint)
                canvas.drawLine(end.x, midY, end.x, end.y, childLinePaint)
            }

            child.spouseId?.let { spouseId ->
                coordinates[spouseId]?.let { sp ->
                    val spousePoint = getCanvasOffset(sp)
                    if (child.id < spouseId) {
                        val leftX = minOf(mPoint.x, spousePoint.x)
                        val rightX = maxOf(mPoint.x, spousePoint.x)
                        val start = Offset(leftX + nodeWidth, mPoint.y + nodeHeight / 2f)
                        val end = Offset(rightX, spousePoint.y + nodeHeight / 2f)
                        canvas.drawLine(start.x, start.y, end.x, end.y, spousePaint)
                    }
                }
            }
        }

        // Draw Cards
        members.forEach { m ->
            val p = coordinates[m.id]?.let { getCanvasOffset(it) } ?: return@forEach

            val isMale = m.gender == "Male"
            val isFemale = m.gender == "Female"
            
            val borderCol = when {
                isMale -> android.graphics.Color.parseColor("#2E7D32") // forest
                isFemale -> android.graphics.Color.parseColor("#C2185B") // rose
                else -> android.graphics.Color.parseColor("#7B1FA2") // other
            }

            val rectPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#FFFFFF")
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            val cardBorderPaint = Paint().apply {
                color = borderCol
                strokeWidth = 3f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }

            val cardLeft = p.x
            val cardTop = p.y
            val cardRight = p.x + nodeWidth
            val cardBottom = p.y + nodeHeight
            val radius = 24f

            // Shadow / Card base
            canvas.drawRoundRect(cardLeft, cardTop, cardRight, cardBottom, radius, radius, rectPaint)
            canvas.drawRoundRect(cardLeft, cardTop, cardRight, cardBottom, radius, radius, cardBorderPaint)

            // Circular Avatar Frame (Perfect Centering)
            val avatarRadius = 20f
            val avatarCenterX = p.x + (nodeWidth / 2f)
            val avatarCenterY = p.y + 32f

            val avatarCirclePaint = Paint().apply {
                color = borderCol
                style = Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }
            val avatarBgPaint = Paint().apply {
                color = borderCol
                alpha = 25
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(avatarCenterX, avatarCenterY, avatarRadius, avatarBgPaint)
            canvas.drawCircle(avatarCenterX, avatarCenterY, avatarRadius, avatarCirclePaint)

            var photoLoaded = false
            if (m.photoPath.isNotBlank()) {
                try {
                    val file = File(m.photoPath)
                    if (file.exists()) {
                        val bmp = BitmapFactory.decodeFile(file.absolutePath)
                        if (bmp != null) {
                            // Decode avatar with 4x high density so it remains crystal clear
                            val targetAvatarSize = (avatarRadius * 2 * exportScale).toInt()
                            val scaled = Bitmap.createScaledBitmap(bmp, targetAvatarSize, targetAvatarSize, true)
                            val shader = android.graphics.BitmapShader(scaled, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP)
                            val paint = Paint().apply {
                                isAntiAlias = true
                                setShader(shader)
                            }
                            val matrix = android.graphics.Matrix()
                            // Scale down shader coordinate space by 1/exportScale to fit unscaled circle dimensions
                            matrix.postScale(1f / exportScale, 1f / exportScale)
                            matrix.postTranslate(avatarCenterX - avatarRadius, avatarCenterY - avatarRadius)
                            shader.setLocalMatrix(matrix)

                            canvas.drawCircle(avatarCenterX, avatarCenterY, avatarRadius, paint)
                            photoLoaded = true
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (!photoLoaded) {
                val signPaint = Paint().apply {
                    color = borderCol
                    textSize = 20f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                val sign = if (isMale) "♂" else if (isFemale) "♀" else "👤"
                val fMetrics = signPaint.fontMetrics
                val offsetVal = (fMetrics.descent + fMetrics.ascent) / 2f
                canvas.drawText(sign, avatarCenterX, avatarCenterY - offsetVal, signPaint)
            }

            if (focusMember != null && focusMember.id == m.id) {
                // Symmetrical green verified check badge for focus descendant
                val badgeBgPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#2E7D32")
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                val badgeBorderPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    strokeWidth = 1f
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                }
                val checkTextPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                val badgeRadius = 7f
                val badgeX = avatarCenterX + 13f
                val badgeY = avatarCenterY + 13f
                canvas.drawCircle(badgeX, badgeY, badgeRadius, badgeBgPaint)
                canvas.drawCircle(badgeX, badgeY, badgeRadius, badgeBorderPaint)
                val mFontMetrics = checkTextPaint.fontMetrics
                val offsetText = (mFontMetrics.descent + mFontMetrics.ascent) / 2f
                canvas.drawText("✓", badgeX, badgeY - offsetText, checkTextPaint)
            }

            // Star/Favorite symmetrical position on the top-left
            if (m.isStarred) {
                val starPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#E53935")
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawCircle(p.x + 18f, p.y + 18f, 4.5f, starPaint)
            }

            // Symmetrical Circular Gender Indicator dot on top-right
            val genderDotPaint = Paint().apply {
                color = borderCol
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(p.x + nodeWidth - 18f, p.y + 18f, 4.5f, genderDotPaint)

            // Names (Positioned cleanly below centered avatar)
            val namePaint = Paint().apply {
                color = android.graphics.Color.parseColor("#212121")
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText(m.firstName, p.x + (nodeWidth / 2f), p.y + 68f, namePaint)

            val lNamePaint = Paint().apply {
                color = android.graphics.Color.parseColor("#616161")
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText(m.lastName, p.x + (nodeWidth / 2f), p.y + 83f, lNamePaint)

            // Years
            val yearsPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#558B2F") // leaf green
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val birthYear = if (m.birthDate.length >= 4) m.birthDate.substring(0, 4) else "????"
            val deathYear = if (m.deathDate.isNotBlank()) {
                if (m.deathDate.length >= 4) m.deathDate.substring(0, 4) else "????"
            } else {
                "सक्रिय"
            }
            canvas.drawText("$birthYear - $deathYear", p.x + (nodeWidth / 2f), p.y + 98f, yearsPaint)
        }

        // Title and header
        val headerPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#1B2C1F")
            textSize = 42f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("पारिवारिक वंशवृक्ष (VanshVriksh): $treeName", unscaledWidth / 2f, 80f, headerPaint)

        val subPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#757575")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val subTitle = if (referenceYear.isNotBlank()) {
            "भावी पीढ़ी के लिए वंशावली सन $referenceYear के अनुसार"
        } else {
            "भावी पीढ़ियों हेतु वंशक्रम विवरण संकलन"
        }
        canvas.drawText(subTitle, unscaledWidth / 2f, 122f, subPaint)

        // THE USER REQUESTED COPYRIGHT WATERMARK AT BOTTOM
        val watermarkPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#C62828") // Beautiful deep red
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("© सर्वाधिकार: आर्यन चिराम", unscaledWidth / 2f, unscaledHeight - 95f, watermarkPaint)

        val baseAppPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#757575")
            textSize = 18f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("सुरक्षित डिजिटल वंशवृक्ष प्रणाली - VanshVriksh App", unscaledWidth / 2f, unscaledHeight - 55f, baseAppPaint)

        return bitmap
    }

    /**
     * Generates a REAL high-quality multi-page PDF Document report and shares/prints it.
     * Stamped with copyright of 'आर्यन चिराम' at bottom.
     */
    fun sharePdfReport(context: Context, folder: FamilyFolder, members: List<FamilyMember>) {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        try {
            // Reconstruct coordinates first
            val coordinates = calculateTreeCoordinates(members)
            val treeBmp = drawTreeToBitmap(context, members, folder.name, coordinates, folder.referenceYear)
            
            // Page 1: Graphical Visual Tree page (matches our custom bitmap width/height exactly)
            val pageInfo1 = android.graphics.pdf.PdfDocument.PageInfo.Builder(treeBmp.width, treeBmp.height, 1).create()
            val page1 = pdfDocument.startPage(pageInfo1)
            val canvas1 = page1.canvas
            canvas1.drawBitmap(treeBmp, 0f, 0f, null)
            pdfDocument.finishPage(page1)

            // Page 2+: Detailed A4 report page
            val widthA4 = 1200
            val heightA4 = 1700
            val borderPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#44593E")
                strokeWidth = 6f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }

            var pageIndex = 2
            var pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(widthA4, heightA4, pageIndex).create()
            var activePage = pdfDocument.startPage(pageInfo)
            var canvas = activePage.canvas

            // Page 2: Certified Declaration Form Page
            val targetSubject = members.find { it.isTargetSubject } ?: members.firstOrNull()
            if (targetSubject != null) {
                val certFather = members.find { it.id == targetSubject.fatherId }
                val certGrandfather = certFather?.let { f -> members.find { it.id == f.fatherId } }
                val certGreatGrandfather = certGrandfather?.let { g -> members.find { it.id == g.fatherId } }
                val certGreatGreatGrandfather = certGreatGrandfather?.let { gg -> members.find { it.id == gg.fatherId } }

                val subjectFullName = "${targetSubject.firstName} ${targetSubject.lastName}".trim()
                val fatherName = certFather?.let { "${it.firstName} ${it.lastName}".trim() } ?: "अविदित"
                val grandfatherName = certGrandfather?.let { "${it.firstName} ${it.lastName}".trim() } ?: "अविदित"
                val greatGrandfatherName = certGreatGrandfather?.let { "${it.firstName} ${it.lastName}".trim() } ?: "अविदित"
                val greatGreatGrandfatherName = certGreatGreatGrandfather?.let { "${it.firstName} ${it.lastName}".trim() } ?: "अविदित"

                val refYear = if (folder.referenceYear.isNotBlank()) folder.referenceYear else "1920"

                // Gold frame for declaration page
                val goldFramePaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#8C7853")
                    strokeWidth = 3f
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                }
                
                // Paint background cream/white
                canvas.drawColor(android.graphics.Color.parseColor("#FCFDF9"))
                canvas.drawRect(40f, 40f, widthA4 - 40f, heightA4 - 40f, borderPaint)
                canvas.drawRect(48f, 48f, widthA4 - 48f, heightA4 - 48f, goldFramePaint)

                // Headline
                val titlePaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#1B2C1F")
                    textSize = 38f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                
                val subtitlePaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#44593E")
                    textSize = 24f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }

                canvas.drawText("वंशावली स्व-घोषणा सत्यापन प्रमाण-पत्र", widthA4 / 2f, 150f, titlePaint)
                canvas.drawText("(राजस्व रिकॉर्ड वर्ष: $refYear के अनुसार)", widthA4 / 2f, 200f, subtitlePaint)

                // Ornamental middle design line
                val goldLinePaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#8C7853")
                    strokeWidth = 4f
                }
                canvas.drawLine(150f, 230f, widthA4 - 150f, 230f, goldLinePaint)

                // Paragraph Text Content
                val paraText = "प्रमाणित किया जाता है कि यह वंशावली रिकॉर्ड राजस्व वर्ष $refYear के अनुसार श्री $subjectFullName (पिता: श्री $fatherName, दादा: श्री $grandfatherName, परदादा: श्री $greatGrandfatherName एवं पर-परदादा: श्री $greatGreatGrandfatherName) के अभिलेखीय प्रमाणों एवं वंश-वृक्ष के रिकॉर्ड के अनुसार पूर्णतः वैध रूप से तैयार किया गया है।"

                val paragraphPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#263238")
                    textSize = 26f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                    isAntiAlias = true
                }

                // Wrap text helper
                fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
                    val words = text.split(" ")
                    val lines = mutableListOf<String>()
                    var currentLine = ""
                    for (word in words) {
                        val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                        if (paint.measureText(testLine) > maxWidth) {
                            lines.add(currentLine)
                            currentLine = word
                        } else {
                            currentLine = testLine
                        }
                    }
                    if (currentLine.isNotEmpty()) {
                        lines.add(currentLine)
                    }
                    return lines
                }

                val wrappedTextLines = wrapText(paraText, paragraphPaint, widthA4 - 200f)
                var textY = 320f
                wrappedTextLines.forEach { line ->
                    canvas.drawText(line, 100f, textY, paragraphPaint)
                    textY += 45f
                }

                textY += 50f

                // Disclaimer box drawing
                val warningBoxPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#FFEBEE") // Very soft light red background
                    style = Paint.Style.FILL
                }
                val warningBorderPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#EF5350") // Light Red border
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                val warningTitlePaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#C62828")
                    textSize = 24f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                val warningTextPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#D32F2F")
                    textSize = 20f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    isAntiAlias = true
                }

                // Draw disclaimer container box
                canvas.drawRect(100f, textY, widthA4 - 100f, textY + 280f, warningBoxPaint)
                canvas.drawRect(100f, textY, widthA4 - 100f, textY + 280f, warningBorderPaint)

                canvas.drawText("⚠️ महत्वपूर्ण कानूनी स्व-घोषणा / उत्तरदायित्व:", 130f, textY + 50f, warningTitlePaint)

                val disclaimer1 = "यदि इस वंशावली पत्र में प्रदान की गई कोई भी जानकारी त्रुटिपूर्ण, असत्य या जाली पायी जाती है, तो इसकी सम्पूर्ण वैधानिक, नागरिक एवं आपराधिक जिम्मेदारी वंशावली बनाते समय जानकारी भरने वाले व्यक्ति (आवेदक) की होगी।"
                val disclaimer2 = "इस वंशवृक्ष के सत्यापन में किसी भी प्रकार की विसंगति के लिए ऐप निर्माता अथवा प्रकाशक उत्तरदायी नहीं होंगे।"
                
                var discY = textY + 100f
                wrapText(disclaimer1, warningTextPaint, widthA4 - 260f).forEach { line ->
                    canvas.drawText(line, 130f, discY, warningTextPaint)
                    discY += 35f
                }
                discY += 15f
                wrapText(disclaimer2, warningTextPaint, widthA4 - 260f).forEach { line ->
                    canvas.drawText(line, 130f, discY, warningTextPaint)
                    discY += 35f
                }

                textY += 390f

                // Draw signatures and verification at the bottom
                val signTitlePaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#263238")
                    textSize = 22f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val signSubPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#455A64")
                    textSize = 20f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    isAntiAlias = true
                }

                canvas.drawText("आवेदक/वंशावली लेखक के हस्ताक्षर", 100f, textY + 40f, signTitlePaint)
                canvas.drawText("(हस्ताक्षरकर्ता / परिवार प्रमुख)", 100f, textY + 80f, signSubPaint)

                val issuerText = if (folder.issuerName.isNotBlank()) folder.issuerName else "आर्यन चिराम"
                canvas.drawText("जारीकर्ता/सत्यापनकर्ता प्राधिकारी", widthA4 - 450f, textY + 40f, signTitlePaint)
                canvas.drawText("नाम (Issuer): $issuerText", widthA4 - 450f, textY + 80f, signSubPaint)

                // Dynamically stamp the uploaded signature image if present!
                if (folder.signaturePath.isNotBlank()) {
                    try {
                        val sigFile = File(folder.signaturePath)
                        if (sigFile.exists()) {
                            val sigBmp = BitmapFactory.decodeFile(sigFile.absolutePath)
                            if (sigBmp != null) {
                                val scaledSig = Bitmap.createScaledBitmap(sigBmp, 250, 100, true)
                                canvas.drawBitmap(scaledSig, widthA4 - 440f, textY - 60f, null)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val wmPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#C62828")
                    textSize = 26f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("© सर्वाधिकार: आर्यन चिराम", widthA4 / 2f, heightA4 - 100f, wmPaint)
                pdfDocument.finishPage(activePage)

                // Start page 3 for detailed member catalog
                pageIndex++
                pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(widthA4, heightA4, pageIndex).create()
                activePage = pdfDocument.startPage(pageInfo)
                canvas = activePage.canvas
            }

            // Paint background cream/white
            canvas.drawColor(android.graphics.Color.parseColor("#FCFDF9"))
            canvas.drawRect(40f, 40f, widthA4 - 40f, heightA4 - 40f, borderPaint)

            // Write report headers
            val mainTitlePaint = Paint().apply {
                color = android.graphics.Color.parseColor("#1B2C1F")
                textSize = 38f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("पारिवारिक वंशवृक्ष कुलवृत्तांत इतिहास रिपोर्ट", 80f, 100f, mainTitlePaint)

            val headDetailsPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#424242")
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }
            canvas.drawText("फैमली ट्री नाम (Tree): ${folder.name}", 80f, 150f, headDetailsPaint)
            if (folder.description.isNotBlank()) {
                canvas.drawText("विवरण (Description): ${folder.description}", 80f, 185f, headDetailsPaint)
            }
            canvas.drawText("कुल पंजीकृत सदस्य (Total Members): ${members.size} | दिनांक (Date): ${SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())}", 80f, 220f, headDetailsPaint)

            val sepPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#B0BEC5")
                strokeWidth = 3f
            }
            canvas.drawLine(80f, 250f, widthA4 - 80f, 250f, sepPaint)

            var currentY = 300f

            // Add Family History and Chronicles Section if present
            if (folder.history.isNotBlank()) {
                val historyHeaderPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#44593E")
                    textSize = 24f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
                    isAntiAlias = true
                }
                canvas.drawText("पारिवारिक इतिहास एवं कुलवृत्तांत (Family History)", 80f, currentY, historyHeaderPaint)
                currentY += 40f

                val historyPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#1C1C1C")
                    textSize = 20f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    isAntiAlias = true
                }
                val quoteLinePaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#8C7853") // Golden bronze vertical line
                    strokeWidth = 5f
                    isAntiAlias = true
                }

                // Let's split user's history text into wrapped lines of max width
                val lines = mutableListOf<String>()
                // Split by newline first to preserve paragraph transitions!
                val paragraphs = folder.history.split("\n")
                paragraphs.forEach { paragraph ->
                    val words = paragraph.split(Regex("\\s+")).filter { it.isNotBlank() }
                    if (words.isNotEmpty()) {
                        var currentLine = StringBuilder()
                        val maxLineWidth = widthA4 - 220f
                        
                        for (word in words) {
                            if (currentLine.isEmpty()) {
                                currentLine.append(word)
                            } else {
                                val testLine = "${currentLine} $word"
                                if (historyPaint.measureText(testLine) > maxLineWidth) {
                                    lines.add(currentLine.toString())
                                    currentLine = StringBuilder(word)
                                } else {
                                    currentLine.append(" ").append(word)
                                }
                            }
                        }
                        if (currentLine.isNotEmpty()) {
                            lines.add(currentLine.toString())
                        }
                    }
                    lines.add("") // Add space after paragraph/empty line
                }

                lines.forEach { line ->
                    if (line.isNotEmpty()) {
                        if (currentY > heightA4 - 150f) {
                            // Draw watermark and end page
                            val wmPaint = Paint().apply {
                                color = android.graphics.Color.parseColor("#C62828")
                                textSize = 26f
                                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                textAlign = Paint.Align.CENTER
                                isAntiAlias = true
                            }
                            canvas.drawText("© सर्वाधिकार: आर्यन चिराम", widthA4 / 2f, heightA4 - 100f, wmPaint)
                            pdfDocument.finishPage(activePage)

                            // Open next page
                            pageIndex++
                            pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(widthA4, heightA4, pageIndex).create()
                            activePage = pdfDocument.startPage(pageInfo)
                            canvas = activePage.canvas

                            canvas.drawColor(android.graphics.Color.parseColor("#FCFDF9"))
                            canvas.drawRect(40f, 40f, widthA4 - 40f, heightA4 - 40f, borderPaint)
                            currentY = 100f
                        }

                        // Draw golden bronze quote line and line text
                        canvas.drawLine(85f, currentY - 18f, 85f, currentY + 12f, quoteLinePaint)
                        canvas.drawText(line, 110f, currentY, historyPaint)
                        currentY += 32f
                    } else {
                        currentY += 15f // space between paragraphs
                    }
                }
                currentY += 30f // padding after section
            }
            val labelFont = Paint().apply {
                color = android.graphics.Color.parseColor("#263238")
                textSize = 21f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val textFont = Paint().apply {
                color = android.graphics.Color.parseColor("#212121")
                textSize = 21f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }
            val dividerPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#ECEFF1")
                strokeWidth = 2f
            }

            members.forEachIndexed { i, m ->
                if (currentY > heightA4 - 200f) {
                    // Draw Watermark at bottom of current page before closing it
                    val wmPaint = Paint().apply {
                        color = android.graphics.Color.parseColor("#C62828")
                        textSize = 26f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    canvas.drawText("© सर्वाधिकार: आर्यन चिराम", widthA4 / 2f, heightA4 - 100f, wmPaint)
                    pdfDocument.finishPage(activePage)

                    // Open next page
                    pageIndex++
                    pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(widthA4, heightA4, pageIndex).create()
                    activePage = pdfDocument.startPage(pageInfo)
                    canvas = activePage.canvas

                    canvas.drawColor(android.graphics.Color.parseColor("#FCFDF9"))
                    canvas.drawRect(40f, 40f, widthA4 - 40f, heightA4 - 40f, borderPaint)
                    currentY = 100f
                }

                // Member Name Block
                canvas.drawText("${i + 1}. सदस्य का पूरा नाम (Full Name):", 80f, currentY, labelFont)
                canvas.drawText(m.fullName, 420f, currentY, textFont)
                currentY += 35f

                // Gender Name
                val gStr = if (m.gender == "Male") "पुरुष (Male)" else if (m.gender == "Female") "महिला (Female)" else "अन्य (Other)"
                canvas.drawText("लिंग (Gender):", 120f, currentY, labelFont)
                canvas.drawText(gStr, 420f, currentY, textFont)
                currentY += 35f

                // Birth Date & Place
                val bDate = m.birthDate.ifBlank { "अविदित/अज्ञात" }
                val bPlace = if (m.birthPlace.isNotBlank()) m.birthPlace else "अज्ञात"
                canvas.drawText("जन्मतिथि (Birthdate):", 120f, currentY, labelFont)
                canvas.drawText("$bDate   |  जन्मस्थान: $bPlace", 420f, currentY, textFont)
                currentY += 35f

                // Death Date
                if (m.deathDate.isNotBlank()) {
                    canvas.drawText("स्वर्गवास तिथि (Passed Away):", 120f, currentY, labelFont)
                    canvas.drawText(m.deathDate, 420f, currentY, textFont)
                    currentY += 35f
                }

                // Occupation
                if (m.occupation.isNotBlank()) {
                    canvas.drawText("पेशा/कार्यकाल (Occupation):", 120f, currentY, labelFont)
                    canvas.drawText(m.occupation, 420f, currentY, textFont)
                    currentY += 35f
                }

                canvas.drawLine(80f, currentY + 10f, widthA4 - 80f, currentY + 10f, dividerPaint)
                currentY += 55f
            }

            // Finish the last page with watermarks
            val wmPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#C62828")
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("© सर्वाधिकार: आर्यन चिराम", widthA4 / 2f, heightA4 - 100f, wmPaint)
            pdfDocument.finishPage(activePage)

            // Cache file output
            val fileName = "VanshVriksh_PDF_${folder.name.replace(" ", "_")}.pdf"
            val reportFile = File(context.cacheDir, fileName)
            val output = FileOutputStream(reportFile)
            pdfDocument.writeTo(output)
            output.flush()
            output.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                reportFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "VanshVriksh Family PDF: ${folder.name}")
                putExtra(Intent.EXTRA_TEXT, "यह ${folder.name} का सचित्र डिजिटल पारिवारिक इतिहास एवं पीढ़ियों की सूची का पीडीएफ (PDF) दस्तावेज है। \n\n© सर्वाधिकार: आर्यन चिराम")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "पीडीएफ शेयर/प्रिंट करें (Share/Print PDF Document)"))

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Saves JSON string to the offline autosaves folder
     */
    fun saveLocalAutoSave(context: Context, folderId: Long, folderName: String, jsonStr: String): File? {
        return try {
            val autosavesDir = File(context.filesDir, "autosaves")
            if (!autosavesDir.exists()) {
                autosavesDir.mkdirs()
            }
            val fileName = "autosave_folder_${folderId}.json"
            val file = File(autosavesDir, fileName)
            val writer = FileWriter(file)
            writer.write(jsonStr)
            writer.flush()
            writer.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Lists all auto-saved backup files
     */
    fun getAutoSaveFiles(context: Context): List<File> {
        return try {
            val autosavesDir = File(context.filesDir, "autosaves")
            if (autosavesDir.exists() && autosavesDir.isDirectory) {
                autosavesDir.listFiles { file -> file.name.startsWith("autosave_folder_") && file.name.endsWith(".json") }
                    ?.sortedByDescending { it.lastModified() }
                    ?.toList() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Parses the name and edited timestamp from an auto-save file
     */
    fun getFolderAndDateFromAutoSave(file: File): Pair<String, String> {
        return try {
            val jsonStr = file.readText()
            val root = JSONObject(jsonStr)
            val folderObj = root.optJSONObject("folder")
            val folderName = folderObj?.optString("name") ?: "अज्ञात ट्री (Unknown Tree)"
            
            val timestamp = root.optLong("exportedAt", file.lastModified())
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
            val dateStr = sdf.format(java.util.Date(timestamp))
            Pair(folderName, dateStr)
        } catch (e: Exception) {
            Pair(file.name, "अज्ञात तिथि (Unknown date)")
        }
    }
}
