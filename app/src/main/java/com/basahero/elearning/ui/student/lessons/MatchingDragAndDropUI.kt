package com.basahero.elearning.ui.student.lessons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basahero.elearning.data.model.MiniQuestion

@Composable
fun MatchingDragAndDropUI(
    question: MiniQuestion,
    isTablet: Boolean,
    isReviewMode: Boolean,
    showFeedback: Boolean,
    onMatchesChanged: (Map<String, String>) -> Unit
) {
    val half = question.choices.size / 2
    val leftSide = remember(question.id) { question.choices.take(half) }
    val rightSide = remember(question.id) { 
        if (isReviewMode) question.choices.drop(half)
        else question.choices.drop(half).shuffled()
    }
    
    // Track matches: Left ID -> Right ID
    var matches by remember(question.id) { mutableStateOf(mapOf<String, String>()) }
    
    // Vibrant rope colors mapped to the left indices
    val ropeColors = listOf(
        Color(0xFFEF4444), // Red
        Color(0xFF3B82F6), // Blue
        Color(0xFF10B981), // Green
        Color(0xFFF59E0B), // Amber
        Color(0xFF8B5CF6), // Purple
        Color(0xFFEC4899)  // Pink
    )
    
    // For drag state
    var activeDragId by remember { mutableStateOf<String?>(null) }
    var currentDragPosition by remember { mutableStateOf<Offset?>(null) }
    
    // To calculate coordinates, we need the window coordinates of the root box
    var rootOffset by remember { mutableStateOf(Offset.Zero) }
    
    // Bounding boxes of items in window space
    val leftBounds = remember { mutableStateMapOf<String, Rect>() }
    val rightBounds = remember { mutableStateMapOf<String, Rect>() }
    
    LaunchedEffect(matches) {
        onMatchesChanged(matches)
    }

    LaunchedEffect(isReviewMode, showFeedback) {
        if (isReviewMode) {
            val perfectMatches = mutableMapOf<String, String>()
            leftSide.forEachIndexed { i, leftItem ->
                perfectMatches[leftItem.id] = rightSide[i].id
            }
            matches = perfectMatches
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { rootOffset = it.positionInWindow() }
    ) {
        // 1. Draw Ropes Canvas
        Canvas(modifier = Modifier.matchParentSize()) {
            // Helper to convert Window Rect to local Canvas Offset (center of the rect)
            fun getLocalCenter(rect: Rect?, isRight: Boolean = false): Offset? {
                if (rect == null) return null
                // We want the rope to attach to the right side of the left item, and left side of the right item
                val attachX = if (isRight) rect.left else rect.right
                return Offset(
                    x = attachX - rootOffset.x,
                    y = rect.center.y - rootOffset.y
                )
            }
            
            // Draw established matches
            leftSide.forEachIndexed { index, leftItem ->
                val matchedRightId = matches[leftItem.id]
                if (matchedRightId != null) {
                    val start = getLocalCenter(leftBounds[leftItem.id], false)
                    val end = getLocalCenter(rightBounds[matchedRightId], true)
                    if (start != null && end != null) {
                        val curveX = if (isTablet) 80.dp.toPx() else 40.dp.toPx()
                        val path = Path().apply {
                            moveTo(start.x, start.y)
                            cubicTo(
                                start.x + curveX, start.y,
                                end.x - curveX, end.y,
                                end.x, end.y
                            )
                        }
                        drawPath(
                            path = path,
                            color = ropeColors[index % ropeColors.size],
                            style = Stroke(width = if (isTablet) 6.dp.toPx() else 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }
            
            // Draw active drag
            if (activeDragId != null && currentDragPosition != null) {
                val leftIndex = leftSide.indexOfFirst { it.id == activeDragId }
                if (leftIndex >= 0) {
                    val start = getLocalCenter(leftBounds[activeDragId], false)
                    val end = Offset(
                        currentDragPosition!!.x - rootOffset.x,
                        currentDragPosition!!.y - rootOffset.y
                    )
                    if (start != null) {
                        val curveX = if (isTablet) 80.dp.toPx() else 40.dp.toPx()
                        val path = Path().apply {
                            moveTo(start.x, start.y)
                            cubicTo(
                                start.x + curveX, start.y,
                                end.x - curveX, end.y,
                                end.x, end.y
                            )
                        }
                        drawPath(
                            path = path,
                            color = ropeColors[leftIndex % ropeColors.size].copy(alpha = 0.7f),
                            style = Stroke(width = if (isTablet) 6.dp.toPx() else 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }
        
        // 2. The Columns
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(if (isTablet) 48.dp else 24.dp) // Responsive gap
        ) {
            // Left Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp)
            ) {
                leftSide.forEachIndexed { index, item ->
                    val isMatched = matches.containsKey(item.id)
                    val color = ropeColors[index % ropeColors.size]
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { leftBounds[item.id] = it.boundsInWindow() }
                            .pointerInput(isReviewMode, showFeedback) {
                                if (isReviewMode || showFeedback) return@pointerInput
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        activeDragId = item.id
                                        val newMatches = matches.toMutableMap()
                                        newMatches.remove(item.id)
                                        matches = newMatches
                                    },
                                    onDragEnd = {
                                        val dropPos = currentDragPosition
                                        if (dropPos != null) {
                                            var matchedRight: String? = null
                                            for ((rId, rRect) in rightBounds) {
                                                val hitBox = rRect.inflate(40f) // Generous hit area
                                                if (hitBox.contains(dropPos)) {
                                                    matchedRight = rId
                                                    break
                                                }
                                            }
                                            if (matchedRight != null) {
                                                val newMatches = matches.toMutableMap()
                                                newMatches.entries.removeIf { it.value == matchedRight }
                                                newMatches[item.id] = matchedRight
                                                matches = newMatches
                                            }
                                        }
                                        activeDragId = null
                                        currentDragPosition = null
                                    },
                                    onDragCancel = {
                                        activeDragId = null
                                        currentDragPosition = null
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val itemRect = leftBounds[item.id]
                                        if (itemRect != null) {
                                            currentDragPosition = Offset(
                                                itemRect.left + change.position.x,
                                                itemRect.top + change.position.y
                                            )
                                        }
                                    }
                                )
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isMatched || activeDragId == item.id) (if (isTablet) 3.dp else 2.dp) else 1.dp,
                            color = if (isMatched || activeDragId == item.id) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        shadowElevation = if (activeDragId == item.id) 8.dp else 2.dp
                    ) {
                        Row(
                            Modifier.padding(if (isTablet) 16.dp else 12.dp), 
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = com.basahero.elearning.util.TextUtil.parseBoldText(item.choiceText),
                                fontSize = if (isTablet) 15.sp else 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            // Connection dot on the right side
                            Box(
                                modifier = Modifier
                                    .size(if (isTablet) 16.dp else 12.dp)
                                    .background(if (isMatched || activeDragId == item.id) color else Color.LightGray, shape = androidx.compose.foundation.shape.CircleShape)
                            )
                        }
                    }
                }
            }
            
            // Right Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp)
            ) {
                rightSide.forEach { item ->
                    val matchedLeftId = matches.entries.find { it.value == item.id }?.key
                    val isMatched = matchedLeftId != null
                    val leftIndex = if (matchedLeftId != null) leftSide.indexOfFirst { it.id == matchedLeftId } else -1
                    val color = if (leftIndex >= 0) ropeColors[leftIndex % ropeColors.size] else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { rightBounds[item.id] = it.boundsInWindow() },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isMatched) (if (isTablet) 3.dp else 2.dp) else 1.dp,
                            color = color
                        ),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            Modifier.padding(if (isTablet) 16.dp else 12.dp), 
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Connection dot on the left side
                            Box(
                                modifier = Modifier
                                    .size(if (isTablet) 16.dp else 12.dp)
                                    .background(if (isMatched) color else Color.LightGray, shape = androidx.compose.foundation.shape.CircleShape)
                            )
                            Spacer(Modifier.width(if (isTablet) 8.dp else 6.dp))
                            Text(
                                text = com.basahero.elearning.util.TextUtil.parseBoldText(item.choiceText),
                                fontSize = if (isTablet) 15.sp else 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
