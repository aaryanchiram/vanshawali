package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.animation.core.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import coil.compose.AsyncImage
import com.example.data.FamilyMember
import com.example.data.TreeExporter
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun FamilyTreeCanvas(
    members: List<FamilyMember>,
    treeName: String,
    referenceYear: String = "",
    onMemberSelected: (FamilyMember) -> Unit,
    onAddRelation: (FamilyMember, String) -> Unit, // relations: "Father", "Mother", "Spouse", "Child"
    onEditMember: (FamilyMember) -> Unit,
    onToggleStar: (FamilyMember) -> Unit,
    onAddMember: () -> Unit = {},
    onPrintReport: () -> Unit = {},
    onSavePdfReport: () -> Unit = {},
    selectedMemberId: Long? = null
) {
    val theme = com.example.ui.theme.LocalTreeTheme.current
    val activeStylePreference = theme.styleId
    val fontSize = theme.fontSize.value
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Active Selected Family Member for bottom detail box
    var activeSelectedMemberId by remember { mutableStateOf<Long?>(null) }
    var activeStyle by remember(activeStylePreference) { mutableStateOf(activeStylePreference) } // 1: राजशाही स्वर्ण, 2: हरित वन, 3: आधुनिक नील, 4: गुलाबी क्वार्ट्ज, 5: कास्मिक स्लेट

    LaunchedEffect(selectedMemberId) {
        if (selectedMemberId != null) {
            activeSelectedMemberId = selectedMemberId
        }
    }
    val activeSelectedMember = remember(members, activeSelectedMemberId) {
        members.find { it.id == activeSelectedMemberId }
    }

    // Interactive zoom and pan states
    var scale by remember { mutableFloatStateOf(1.0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Generational Layout Calculator
    // Step 1: Assign each member to a proper aligned generational level
    val memberLevels = remember(members) {
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
        levels
    }

    // Step 2: Assign coordinates purely in scalable DP based on a professional recursive tree layout
    val nodeWidthDp = 145f
    val nodeHeightDp = 100f
    val spouseCardGap = 25f
    val siblingGap = 30f
    val verticalSpacingDp = 190f

    val focusMember = remember(members, memberLevels) {
        val leafNodes = members.filter { m ->
            members.none { it.fatherId == m.id || it.motherId == m.id }
        }
        if (leafNodes.isEmpty()) {
            members.maxByOrNull { m -> (memberLevels[m.id] ?: 0) * 1000000L + m.id }
        } else {
            leafNodes.maxByOrNull { m -> (memberLevels[m.id] ?: 0) * 1000000L + m.id }
        }
    }

    val memberCoordinates = remember(members, memberLevels) {
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
                val l = memberLevels[mId] ?: 0
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
                val childL = memberLevels[child.id] ?: (level + 1)
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
            layoutSubtree(root.id, level = memberLevels[root.id] ?: 0)
        }

        // Leftovers loop for single disconnected nodes or alternative families
        members.forEach { m ->
            if (!positioned.contains(m.id)) {
                layoutSubtree(m.id, level = memberLevels[m.id] ?: 0)
            }
        }

        coords
    }

    // Capture bitmap sharing trigger
    var triggerShare by remember { mutableStateOf(false) }

    LaunchedEffect(triggerShare) {
        if (triggerShare) {
            triggerShare = false
            val b = TreeExporter.drawTreeToBitmap(context, members, treeName, memberCoordinates, referenceYear)
            TreeExporter.shareBitmapImage(context, b, treeName)
        }
    }

    val backgroundBrush = remember(activeStyle, isDark) {
        when (activeStyle) {
            1 -> if (isDark) {
                Brush.radialGradient(colors = listOf(Color(0xFF2C1E12), Color(0xFF0F0B06)), center = Offset(500f, 500f), radius = 1200f)
            } else {
                Brush.radialGradient(colors = listOf(Color(0xFFFDFBF7), Color(0xFFF3ECE0)), center = Offset(500f, 500f), radius = 1200f)
            }
            2 -> if (isDark) {
                Brush.radialGradient(colors = listOf(Color(0xFF122216), Color(0xFF070E09)), center = Offset(500f, 500f), radius = 1200f)
            } else {
                Brush.radialGradient(colors = listOf(Color(0xFFF5FAF6), Color(0xFFE1ECEB)), center = Offset(500f, 500f), radius = 1200f)
            }
            3 -> if (isDark) {
                Brush.radialGradient(colors = listOf(Color(0xFF0A192F), Color(0xFF020C1B)), center = Offset(500f, 500f), radius = 1200f)
            } else {
                Brush.radialGradient(colors = listOf(Color(0xFFF4F7F9), Color(0xFFD6E4ED)), center = Offset(500f, 500f), radius = 1200f)
            }
            4 -> if (isDark) {
                Brush.radialGradient(colors = listOf(Color(0xFF2A0D15), Color(0xFF110306)), center = Offset(500f, 500f), radius = 1200f)
            } else {
                Brush.radialGradient(colors = listOf(Color(0xFFFFF7F8), Color(0xFFFBDDE3)), center = Offset(500f, 500f), radius = 1200f)
            }
            5 -> if (isDark) {
                Brush.radialGradient(colors = listOf(Color(0xFF140D24), Color(0xFF07040C)), center = Offset(500f, 500f), radius = 1200f)
            } else {
                Brush.radialGradient(colors = listOf(Color(0xFFF7F4FA), Color(0xFFE4DCEE)), center = Offset(500f, 500f), radius = 1200f)
            }
            else -> if (isDark) {
                Brush.radialGradient(colors = listOf(Color(0xFF1B2C1F), Color(0xFF0C140E)), center = Offset(500f, 500f), radius = 1200f)
            } else {
                Brush.radialGradient(colors = listOf(Color(0xFFFCFDF9), Color(0xFFEFEFD0)), center = Offset(500f, 500f), radius = 1200f)
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.3f, 5.0f)
                    offset += pan
                }
            }
    ) {
        val viewWidthDp = maxWidth
        val viewHeightDp = maxHeight
        val density = androidx.compose.ui.platform.LocalDensity.current
        LaunchedEffect(selectedMemberId, memberCoordinates, viewWidthDp, viewHeightDp) {
            if (selectedMemberId != null) {
                val point = memberCoordinates[selectedMemberId]
                if (point != null) {
                    with(density) {
                        val viewWidthPx = viewWidthDp.toPx()
                        val viewHeightPx = viewHeightDp.toPx()
                        val midXPx = point.x.dp.toPx() + (nodeWidthDp.dp.toPx() / 2f)
                        val midYPx = point.y.dp.toPx() + (nodeHeightDp.dp.toPx() / 2f)
                        
                        val targetX = (viewWidthPx / 2f) - (midXPx * scale)
                        val targetY = (viewHeightPx / 2f) - (midYPx * scale)
                        
                        offset = Offset(targetX, targetY)
                    }
                }
            }
        }
        // Draw connection lines at the back
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        ) {
            val strokeWidthVal = 4.dp.toPx()

            members.forEach { child ->
                val mPoint = memberCoordinates[child.id] ?: return@forEach
                val dadPoint = child.fatherId?.let { memberCoordinates[it] } ?: child.motherId?.let { momId ->
                    val mother = members.find { it.id == momId }
                    mother?.spouseId?.let { memberCoordinates[it] }
                }
                val momPoint = child.motherId?.let { memberCoordinates[it] } ?: child.fatherId?.let { dadId ->
                    val father = members.find { it.id == dadId }
                    father?.spouseId?.let { memberCoordinates[it] }
                }

                val startPoint = when {
                    dadPoint != null && momPoint != null -> {
                        // Draw descend connection purely from parents midpoint starting from the spouse link line to avoid any visual gap
                        Offset(
                            (dadPoint.x + momPoint.x + nodeWidthDp).dp.toPx() / 2f,
                            (dadPoint.y + nodeHeightDp / 2f).dp.toPx()
                        )
                    }
                    dadPoint != null -> {
                        Offset(
                            (dadPoint.x + nodeWidthDp / 2f).dp.toPx(),
                            (dadPoint.y + nodeHeightDp).dp.toPx()
                        )
                    }
                    momPoint != null -> {
                        Offset(
                            (momPoint.x + nodeWidthDp / 2f).dp.toPx(),
                            (momPoint.y + nodeHeightDp).dp.toPx()
                        )
                    }
                    else -> null
                }

                startPoint?.let { start ->
                    val end = Offset(
                        (mPoint.x + nodeWidthDp / 2f).dp.toPx(),
                        mPoint.y.dp.toPx()
                    )
                    val midY = (start.y + end.y) / 2f

                    // Color customization based on activeStyle
                    val parentTrunkColor = when (activeStyle) {
                        1 -> if (isDark) Color(0xFFFFD700) else Color(0xFF8B0000) // Gold / Maroon
                        2 -> if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32) // Soft Green / Forest
                        3 -> if (isDark) Color(0xFF4FC3F7) else Color(0xFF0288D1) // Bright Blue / Slate
                        4 -> if (isDark) Color(0xFFF06292) else Color(0xFFC2185B) // Delicate Rose / Magenta
                        5 -> if (isDark) Color(0xFFB085F5) else Color(0xFF673AB7) // Cosmic Lavender / Deep Purple
                        else -> if (isDark) Color(0xFF8D6E63) else Color(0xFF6D4C41)
                    }

                    val childLineColor = if (child.gender == "Female") {
                        when (activeStyle) {
                            1 -> Color(0xFFE57373)
                            2 -> Color(0xFFD81B60)
                            3 -> Color(0xFF3F51B5)
                            4 -> Color(0xFFE91E63)
                            5 -> Color(0xFFE040FB)
                            else -> Color(0xFFE91E63)
                        }
                    } else {
                        when (activeStyle) {
                            1 -> Color(0xFFFFB300)
                            2 -> Color(0xFF00796B)
                            3 -> Color(0xFF00B0FF)
                            4 -> Color(0xFFF06292)
                            5 -> Color(0xFF00E5FF)
                            else -> Color(0xFF0288D1)
                        }
                    }

                    val pathEffect = when (activeStyle) {
                        3 -> PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f) // Tech Dash
                        5 -> PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)    // Space Dots
                        else -> null
                    }

                    if (activeStyle == 2 || activeStyle == 4) {
                        // Drawing an elegant organic cubic Bezier S-curve from start to end
                        val path = Path().apply {
                            moveTo(start.x, start.y)
                            cubicTo(
                                start.x, (start.y + end.y) / 2f,
                                end.x, (start.y + end.y) / 2f,
                                end.x, end.y
                            )
                        }
                        drawPath(
                            path = path,
                            color = childLineColor,
                            style = Stroke(width = strokeWidthVal, cap = StrokeCap.Round)
                        )
                    } else {
                        drawLine(
                            color = parentTrunkColor,
                            start = start,
                            end = Offset(start.x, midY),
                            strokeWidth = strokeWidthVal,
                            cap = StrokeCap.Round,
                            pathEffect = pathEffect
                        )
                        drawLine(
                            color = childLineColor,
                            start = Offset(start.x, midY),
                            end = Offset(end.x, midY),
                            strokeWidth = strokeWidthVal,
                            cap = StrokeCap.Round,
                            pathEffect = pathEffect
                        )
                        drawLine(
                            color = childLineColor,
                            start = Offset(end.x, midY),
                            end = end,
                            strokeWidth = strokeWidthVal,
                            cap = StrokeCap.Round,
                            pathEffect = pathEffect
                        )
                    }
                }

                // Spouse red line (straight horizontal link)
                child.spouseId?.let { spouseId ->
                    memberCoordinates[spouseId]?.let { spousePoint ->
                        // Only draw link once to avoid duplicate overlaps
                        if (child.id < spouseId) {
                            val leftX = minOf(mPoint.x, spousePoint.x)
                            val rightX = maxOf(mPoint.x, spousePoint.x)
                            
                            val start = Offset(
                                (leftX + nodeWidthDp).dp.toPx(),
                                (mPoint.y + nodeHeightDp / 2f).dp.toPx()
                            )
                            val end = Offset(
                                rightX.dp.toPx(),
                                (spousePoint.y + nodeHeightDp / 2f).dp.toPx()
                            )

                            val spouseLineColor = when (activeStyle) {
                                1 -> Color(0xFFB71C1C) // Deep Gold Maroon
                                2 -> Color(0xFF1B5E20) // Deep Botanical Green
                                3 -> Color(0xFF0D47A1) // Rich Electric Blue
                                4 -> Color(0xFF880E4F) // Love sweet maroon Rose
                                5 -> Color(0xFFD500F9) // Stellar Galactic Neon Purple
                                else -> Color(0xFFD32F2F)
                            }
                            
                            val spousePathEffect = when (activeStyle) {
                                3 -> PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                                5 -> PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)
                                else -> null
                            }
                            
                            if (activeStyle == 2 || activeStyle == 4) {
                                val spousePath = Path().apply {
                                    moveTo(start.x, start.y)
                                    quadraticBezierTo(
                                        (start.x + end.x) / 2f,
                                        start.y - 12f, // Gentle upward arc representing marriage bond
                                        end.x,
                                        end.y
                                    )
                                }
                                drawPath(
                                    path = spousePath,
                                    color = spouseLineColor,
                                    style = Stroke(width = strokeWidthVal + 1f, cap = StrokeCap.Round)
                                )
                            } else {
                                drawLine(
                                    color = spouseLineColor,
                                    start = start,
                                    end = end,
                                    strokeWidth = strokeWidthVal + 1f,
                                    cap = StrokeCap.Square,
                                    pathEffect = spousePathEffect
                                )
                            }
                        }
                    }
                }
            }
        }

        // Draw Interactive Member Nodes in precise DP offsets
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        ) {
            members.forEach { member ->
                val point = memberCoordinates[member.id] ?: Offset.Zero
                
                Box(
                    modifier = Modifier
                        .offset(point.x.dp, point.y.dp)
                        .width(nodeWidthDp.dp)
                        .height(nodeHeightDp.dp)
                ) {
                    FamilyMemberNode(
                        member = member,
                        isActive = (activeSelectedMember?.id == member.id),
                        isFocusMember = (focusMember?.id == member.id),
                        activeStyle = activeStyle,
                        onNodeClick = {
                            activeSelectedMemberId = member.id
                            onMemberSelected(member)
                        },
                        onNodeDoubleClick = {
                            onEditMember(member)
                        },
                        onToggleStar = { onToggleStar(member) }
                    )
                }
            }
        }

        // Bottom Panel - Legend & Selected Member Details Side-by-Side (Clash-free and centered)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // 1. Legend Box (Left Card)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(175.dp)
                    .background(
                        color = if (isDark) Color(0xEE18221B) else Color(0xEEFCFDF5),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isDark) Color(0x33FFFFFF) else Color(0x332E7D32),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "info",
                            tint = if (isDark) Color(0xFFAAED7A) else Color(0xFF2E7D32),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "संकेत सूची (Legend)",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Male representation
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF2E7D32), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "पुरुष सदस्य (Male)",
                                fontSize = 8.sp,
                                color = if (isDark) Color.LightGray else Color.Black
                            )
                        }

                        // Female representation
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFFC2185B), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "महिला सदस्य (Female)",
                                fontSize = 8.sp,
                                color = if (isDark) Color.LightGray else Color.Black
                            )
                        }

                        // Spouse relation (लाल लकीर)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp, 2.5.dp)
                                    .background(Color(0xFFD32F2F))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "दम्पति संबंध (Spouse)",
                                fontSize = 8.sp,
                                color = Color(0xFFD32F2F),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Son line (नीली लकीर)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp, 2.5.dp)
                                    .background(Color(0xFF0288D1))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "पुत्र संबंध (Son Line)",
                                fontSize = 8.sp,
                                color = if (isDark) Color.LightGray else Color.Black
                            )
                        }

                        // Daughter line (पिंक लकीर)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp, 2.5.dp)
                                    .background(Color(0xFFE91E63))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "पुत्री संबंध (Daughter)",
                                fontSize = 8.sp,
                                color = if (isDark) Color.LightGray else Color.Black
                            )
                        }

                        // Focus member badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFF2E7D32), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✓", color = Color.White, fontSize = 6.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "मुख्य वंशज (Focus)",
                                fontSize = 8.sp,
                                color = if (isDark) Color.LightGray else Color.Black
                            )
                        }
                    }

                    Text(
                        text = "💡 ज़ूम/पैन: दो उंगली स्पर्श करें",
                        fontSize = 7.5.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // 2. Selected Member Details Box (Right Card - Centered Alignment)
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .height(175.dp)
                    .background(
                        if (isDark) Color(0xEE111E16) else Color(0xEEF5F7F2),
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        if (isDark) Color(0x33FFFFFF) else Color(0x332E7D32),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(8.dp)
            ) {
                if (activeSelectedMember == null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "सदस्य विवरण (Details)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isDark) Color.LightGray else Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "विवरण देखने हेतु सदस्य पर क्लिक करें",
                            fontSize = 8.5.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "डबल-क्लिक से सदस्य संपादित करें",
                            fontSize = 7.5.sp,
                            color = Color.Gray.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        val borderCol = when (activeSelectedMember.gender) {
                            "Male" -> Color(0xFF2E7D32)
                            "Female" -> Color(0xFFC2185B)
                            else -> Color(0xFF7B1FA2)
                        }

                        // Avatar (Centered, Compact)
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(borderCol.copy(alpha = 0.15f))
                                .border(1.2.dp, borderCol, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (activeSelectedMember.photoPath.isNotBlank()) {
                                AsyncImage(
                                    model = java.io.File(activeSelectedMember.photoPath),
                                    contentDescription = "Profile",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = "Default avatar",
                                    tint = borderCol,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Star / Highlight Badge (Centered, Compact)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (activeSelectedMember.isStarred) Color(0x1FDD2C00) else Color(0x0F000000))
                                .clickable { onToggleStar(activeSelectedMember) }
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Icon(
                                imageVector = if (activeSelectedMember.isStarred) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Star",
                                tint = if (activeSelectedMember.isStarred) Color.Red else Color.Gray,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = if (activeSelectedMember.isStarred) "प्रमुख सदस्य" else "सामान्य सदस्य",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (activeSelectedMember.isStarred) Color.Red else Color.Gray
                            )
                        }

                        // Text Details Column (Centered perfectly)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = activeSelectedMember.fullName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isDark) Color.White else Color.Black,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            // Age and details
                            Text(
                                text = "आयु: ${activeSelectedMember.age}",
                                fontSize = 10.sp,
                                color = if (isDark) Color.LightGray else Color.DarkGray
                            )

                            IconButton(onClick = { 
                                com.example.data.TreeExporter.shareIndividualMemberPdf(context, activeSelectedMember) 
                            }) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = "Export PDF", tint = borderCol)
                            }

                            val birthYear = if (activeSelectedMember.birthDate.length >= 4) activeSelectedMember.birthDate.substring(0, 4) else "अज्ञात"
                            val deathYear = if (activeSelectedMember.deathDate.isNotBlank()) {
                                if (activeSelectedMember.deathDate.length >= 4) activeSelectedMember.deathDate.substring(0, 4) else "अज्ञात"
                            } else null

                            if (deathYear != null) {
                                Text(
                                    text = "जन्म: $birthYear | स्वर्गवास: $deathYear",
                                    fontSize = 9.5.sp,
                                    color = Color.Red.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = "जन्म: $birthYear | सक्रिय (Present)",
                                    fontSize = 9.5.sp,
                                    color = if (isDark) Color(0xFFAAED7A) else Color(0xFF2E7D32),
                                    textAlign = TextAlign.Center
                                )
                            }

                            if (activeSelectedMember.birthPlace.isNotBlank()) {
                                Text(
                                    text = "स्थान: ${activeSelectedMember.birthPlace}",
                                    fontSize = 8.5.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "डबल-क्लिक से संपादन करें",
                                fontSize = 7.5.sp,
                                color = Color.Gray.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }



        // Top Right Zoom Controls & Premium Action Buttons
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Add Member (सदस्य जोड़ें)
            IconButton(
                onClick = { onAddMember() },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape).border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = "सदस्य जोड़ें (Add Member)")
            }

            // 2. Print (प्रिंट)
            IconButton(
                onClick = { onPrintReport() },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape).border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Filled.Print, contentDescription = "प्रिंट करें (Print)")
            }

            // 3. Save PDF (सेव pdf)
            IconButton(
                onClick = { onSavePdfReport() },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape).border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Filled.PictureAsPdf, contentDescription = "पीडीएफ सेव करें (Save PDF)")
            }

            // 4. Save Image (सेव इमेज)
            IconButton(
                onClick = { triggerShare = true },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape).border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Filled.Image, contentDescription = "इमेज सेव करें (Save Image)")
            }

            // 5. Zoom In
            IconButton(
                onClick = { scale = (scale + 0.1f).coerceAtMost(5.0f) },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape).border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom In")
            }

            // 6. Zoom Out
            IconButton(
                onClick = { scale = (scale - 0.1f).coerceAtLeast(0.3f) },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape).border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom Out")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FamilyMemberNode(
    member: FamilyMember,
    isActive: Boolean,
    isFocusMember: Boolean = false,
    activeStyle: Int = 1,
    onNodeClick: () -> Unit,
    onNodeDoubleClick: () -> Unit,
    onToggleStar: () -> Unit
) {
    var showTooltip by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    
    // Choose profile frame outline based on gender and active style
    val borderCol = when (member.gender) {
        "Male" -> when (activeStyle) {
            1 -> Color(0xFFE5A93C) // Warm Gold for Royal male
            2 -> Color(0xFF2E7D32) // Forest emerald green
            3 -> Color(0xFF1565C0) // Deep tech blue
            4 -> Color(0xFFD81B60) // Rose Pink style male
            5 -> Color(0xFF9575CD) // Lavender light purple male
            else -> Color(0xFF2E7D32)
        }
        "Female" -> when (activeStyle) {
            1 -> Color(0xFFD32F2F) // Ruby Red for Royal female
            2 -> Color(0xFFC2185B) // Classic rose green
            3 -> Color(0xFF00E5FF) // Cyber Cyan for female
            4 -> Color(0xFFEC407A) // Sweet pink female
            5 -> Color(0xFFEA40C2) // Starry Neon Magenta female
            else -> Color(0xFFC2185B)
        }
        else -> when (activeStyle) {
            1 -> Color(0xFFFFD700)
            2 -> Color(0xFF7B1FA2)
            3 -> Color(0xFF00B0FF)
            4 -> Color(0xFFF48FB1)
            5 -> Color(0xFFEA80FC)
            else -> Color(0xFF7B1FA2)
        }
    }

    val cardShape = when (activeStyle) {
        1 -> RoundedCornerShape(14.dp) // Royal Rounded
        2 -> RoundedCornerShape(24.dp) // Eco Organic Rounded
        3 -> CutCornerShape(8.dp)       // Futuristic Cyber Cut
        4 -> RoundedCornerShape(12.dp)  // Sweet classic
        5 -> RoundedCornerShape(topEnd = 16.dp, bottomStart = 16.dp) // Cosmic offset arcs
        else -> RoundedCornerShape(12.dp)
    }

    val cardBgColor = if (isDark) {
        when (activeStyle) {
            1 -> if (isActive) Color(0xFF331D1D) else Color(0xFF221111) // Royal Dark Maroon
            2 -> if (isActive) Color(0xFF142E1B) else Color(0xFF0F1E13) // Forest Dark Green
            3 -> if (isActive) Color(0xFF0D253E) else Color(0xFF071424) // Corporate Tech Dark Blue
            4 -> if (isActive) Color(0xFF3E1220) else Color(0xFF2C0A15) // Wine Red Love
            5 -> if (isActive) Color(0xFF221435) else Color(0xFF130A21) // Cyber violet dark space
            else -> if (isActive) Color(0xFF1D281F) else Color(0xFF141F17)
        }
    } else {
        when (activeStyle) {
            1 -> if (isActive) Color(0xFFFFF7E6) else Color(0xFFFCF6EB) // Parchment Cream
            2 -> if (isActive) Color(0xFFEBF5EA) else Color(0xFFF1F8E9) // Herb Mint Green
            3 -> if (isActive) Color(0xFFE1F5FE) else Color(0xFFECEFF1) // Ice blue silver
            4 -> if (isActive) Color(0xFFFFEBEE) else Color(0xFFFFF1F2) // Peach Rose Pink
            5 -> if (isActive) Color(0xFFF3EAFB) else Color(0xFFEDE7F6) // Celestial Lavender
            else -> if (isActive) Color(0xFFFFFDE7) else Color(0xFFFBFBF6)
        }
    }

    val borderThickness = if (isActive) 4.dp else 2.dp
    val computedBorderColor = if (isActive) {
        when (activeStyle) {
            1 -> Color(0xFFFFD700) // Shining Pure Gold
            2 -> Color(0xFF00E676) // Glowing Grass Green
            3 -> Color(0xFF00E5FF) // Flash Tech Neon Cyan
            4 -> Color(0xFFFF4081) // Hot Pink Rose
            5 -> Color(0xFFD500F9) // Stellar Galactic Ultraviolet
            else -> Color(0xFFFFD700)
        }
    } else {
        borderCol
    }

    val avatarShape = when (activeStyle) {
        1 -> CircleShape // Circular with gold ring
        2 -> RoundedCornerShape(topStart = 11.dp, bottomEnd = 11.dp, topEnd = 3.dp, bottomStart = 3.dp) // Eco Leaf shape
        3 -> RoundedCornerShape(8.dp) // Squircle Tech
        4 -> RoundedCornerShape(12.dp) // Sweet flower feel
        5 -> CircleShape // Starry circle
        else -> CircleShape
    }

    val styleBadgeIcon = when (activeStyle) {
        1 -> Icons.Filled.Star // Gold Star for Premium Royal
        2 -> Icons.Filled.Spa  // Eco Branch Leaf for Forest
        3 -> Icons.Filled.Bolt // Tech Bolt for Cyber Indigo
        4 -> Icons.Filled.Favorite // Heart for Rose Quartz family love
        5 -> Icons.Filled.Info // Info/Star badge for Starry Dark
        else -> null
    }

    val styleBadgeColor = when (activeStyle) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFF4CAF50)
        3 -> Color(0xFF00E5FF)
        4 -> Color(0xFFEC407A)
        5 -> Color(0xFFEA80FC)
        else -> Color.Transparent
    }

    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = cardBgColor
        ),
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = { onNodeClick() },
                onDoubleClick = { onNodeDoubleClick() },
                onLongClick = { showTooltip = true }
            )
            .border(
                width = borderThickness,
                color = computedBorderColor,
                shape = cardShape
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 8.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // First Row: Star icon, Avatar placeholder, Focus check badge, Gender indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onToggleStar() },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = if (member.isStarred) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Starred",
                        tint = if (member.isStarred) Color.Red else Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(avatarShape)
                            .background(borderCol.copy(alpha = 0.15f))
                            .border(
                                width = if (isActive) 2.dp else 1.dp,
                                color = computedBorderColor,
                                shape = avatarShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (member.photoPath.isNotBlank()) {
                            AsyncImage(
                                model = File(member.photoPath),
                                contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Default avatar",
                                tint = borderCol,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Style-specific visual overlay/badge
                    if (styleBadgeIcon != null) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(styleBadgeColor, CircleShape)
                                .border(0.5.dp, Color.White, CircleShape)
                                .align(Alignment.TopEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = styleBadgeIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(7.dp)
                            )
                        }
                    }

                    if (isFocusMember) {
                        // Beautiful green verified focus-member check mark badge
                        Box(
                            modifier = Modifier
                                .size(13.dp)
                                .background(Color(0xFF2E7D32), CircleShape)
                                .border(1.dp, Color.White, CircleShape)
                                .align(Alignment.BottomEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "✓",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 8.sp
                            )
                        }
                    }
                }
                
                // Gender indicator dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(borderCol, CircleShape)
                )
            }

            // Name
            Text(
                text = "${member.firstName}\n${member.lastName}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (isDark) Color.White else Color.Black
            )

            // Year range
            val yearsText = remember(member.birthDate, member.deathDate) {
                val birthYear = if (member.birthDate.length >= 4) member.birthDate.substring(0, 4) else "????"
                val deathYear = if (member.deathDate.isNotBlank()) {
                    if (member.deathDate.length >= 4) member.deathDate.substring(0, 4) else "????"
                } else {
                    "सक्रिय (Present)"
                }
                "$birthYear - $deathYear"
            }
            
            val ageDisplay = "(${member.age})"

            val yearsColor = if (isDark) {
                when (activeStyle) {
                    1 -> Color(0xFFFFD54F) // Gold tint
                    2 -> Color(0xFFA5D6A7) // Mint tint
                    3 -> Color(0xFF81D4FA) // Cyan tint
                    4 -> Color(0xFFF48FB1) // Rose tint
                    5 -> Color(0xFFB39DDB) // Lavender violet tint
                    else -> Color(0xAABAE16C)
                }
            } else {
                when (activeStyle) {
                    2 -> Color(0xFF2E7D32)
                    3 -> Color(0xFF0288D1)
                    4 -> Color(0xFFC2185B)
                    5 -> Color(0xFF673AB7)
                    else -> Color(0xFF558B2F)
                }
            }

            Text(
                text = "$yearsText $ageDisplay",
                fontSize = 9.sp,
                fontWeight = FontWeight.Light,
                color = yearsColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (showTooltip) {
        Popup(
            onDismissRequest = { showTooltip = false },
            alignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "विवरण (Details)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "जीवनी (Biography): ${member.notes.ifBlank { "N/A" }}", fontSize = 12.sp)
                    Text(text = "व्यवसाय (Occupation): ${member.occupation.ifBlank { "N/A" }}", fontSize = 12.sp)
                    Text(text = "जन्म स्थान (Birth Place): ${member.birthPlace.ifBlank { "N/A" }}", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showTooltip = false }) { Text("बंद करें (Close)") }
                }
            }
        }
    }
}
