package com.basahero.elearning.ui.student.quiz

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.basahero.elearning.data.model.QuizQuestion
import com.basahero.elearning.ui.common.AnimatedAnswerCard
import com.basahero.elearning.ui.common.ShakingTextField
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// 1. MCQ Color-Fill + Reveal
//    AnimatedVisibility color fill on tap, correct/wrong reveal on submit.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AnimatedMcqQuestion(
    question: QuizQuestion,
    selectedChoiceId: String?,
    isSubmitted: Boolean,
    onChoiceSelected: (String) -> Unit
) {
    // Shuffle once per question id so order is stable across recompositions
    val shuffledChoices = remember(question.id) { question.choices.shuffled() }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        shuffledChoices.forEach { choice ->
            val isSelected = selectedChoiceId == choice.id
            val isCorrectChoice = question.correctAnswerIds.contains(choice.id)

            // On submit, reveal the selected card AND the correct card
            val showReveal = isSubmitted && (isSelected || isCorrectChoice)

            AnimatedAnswerCard(
                isRevealed = showReveal,
                isCorrect = isCorrectChoice,
                modifier = Modifier.fillMaxWidth()
            ) {
                Card(
                    onClick = { if (!isSubmitted) onChoiceSelected(choice.id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected && !isSubmitted)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = choice.choiceText,
                            fontSize = 15.sp,
                            color = if (isSelected && !isSubmitted)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Fill-in Shake on Wrong Answer
//    Shakes the text field on incorrect submission via ShakingTextField from
//    CommonComponents.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AnimatedFillInQuestion(
    question: QuizQuestion,
    currentText: String,
    isSubmitted: Boolean,
    onTextChanged: (String) -> Unit
) {
    val correctAnswer = question.correctAnswerIds.firstOrNull() ?: ""
    val isWrong = isSubmitted && !currentText.equals(correctAnswer, ignoreCase = true)

    Column {
        ShakingTextField(
            value = currentText,
            onValueChange = { if (!isSubmitted) onTextChanged(it) },
            label = "Type your answer",
            isWrong = isWrong,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        if (!isSubmitted) {
            Text(
                text = "Type your answer in the box above.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Sequencing — Draggable Cards + Snap on Drop
//    Long-press the drag handle, drag vertically to reorder, snaps on release.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DragDropSequencingQuestion(
    question: QuizQuestion,
    currentOrder: List<String>,
    isSubmitted: Boolean,
    onOrderChanged: (List<String>) -> Unit
) {
    var listState by remember(currentOrder) { mutableStateOf(currentOrder) }
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    Column(Modifier.fillMaxWidth()) {
        Text(
            text = if (isSubmitted) "Correct sequence:" else "Long press the ≡ handle to drag and reorder:",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        listState.forEachIndexed { index, id ->
            val choice = question.choices.find { it.id == id }
            val isBeingDragged = draggedItemIndex == index
            val zIndex = if (isBeingDragged) 1f else 0f
            val yOffset = if (isBeingDragged) dragOffset.roundToInt() else 0

            // Submit reveal: green if correct slot, red if wrong slot
            val isCorrectSlot = isSubmitted && question.correctAnswerIds.getOrNull(index) == id
            val bgColor = when {
                isSubmitted && isCorrectSlot -> MaterialTheme.colorScheme.tertiaryContainer
                isSubmitted && !isCorrectSlot -> MaterialTheme.colorScheme.errorContainer
                isBeingDragged -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .zIndex(zIndex)
                    .offset { IntOffset(0, yOffset) }
                    .pointerInput(isSubmitted) {
                        if (isSubmitted) return@pointerInput
                        detectDragGesturesAfterLongPress(
                            onDragStart = { draggedItemIndex = index },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount.y

                                // Snap: swap when dragged past the neighbouring card's midpoint
                                val targetIndex = (index + (dragOffset / size.height).roundToInt())
                                    .coerceIn(0, listState.size - 1)
                                if (targetIndex != index) {
                                    val newList = listState.toMutableList()
                                    newList.removeAt(index)
                                    newList.add(targetIndex, id)
                                    listState = newList
                                    draggedItemIndex = targetIndex
                                    dragOffset = 0f
                                    onOrderChanged(newList)
                                }
                            },
                            onDragEnd = { draggedItemIndex = null; dragOffset = 0f },
                            onDragCancel = { draggedItemIndex = null; dragOffset = 0f }
                        )
                    },
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isBeingDragged) 10.dp else 1.dp
                ),
                colors = CardDefaults.cardColors(containerColor = bgColor)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Position number badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            isSubmitted && isCorrectSlot -> MaterialTheme.colorScheme.tertiary
                            isSubmitted && !isCorrectSlot -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        }
                    ) {
                        Text(
                            text = "${index + 1}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = choice?.choiceText ?: "",
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )

                    if (!isSubmitted) {
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = "Drag to reorder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Matching — Two-Column Layout with Canvas-Drawn Connecting Lines
//    Tap a left item → tap a right item to draw a Bézier line between them.
//    On submit, lines turn green (correct) or red (wrong).
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CanvasMatchingQuestion(
    question: QuizQuestion,
    connections: List<String>, // Flattened [leftId, rightId, leftId, rightId …]
    isSubmitted: Boolean,
    onConnectionMade: (List<String>) -> Unit
) {
    val leftItems = question.choices.take(question.choices.size / 2)
    val rightItems = question.choices.drop(question.choices.size / 2)

    var leftAnchors by remember { mutableStateOf(mutableMapOf<String, Offset>()) }
    var rightAnchors by remember { mutableStateOf(mutableMapOf<String, Offset>()) }
    var selectedLeft by remember { mutableStateOf<String?>(null) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val correctColor = MaterialTheme.colorScheme.tertiary

    if (!isSubmitted) {
        Text(
            text = "Tap a word on the left, then tap its match on the right.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }

    Box(modifier = Modifier
        .fillMaxWidth()
        .height(360.dp)
    ) {
        // ── Canvas layer: draw Bézier curves for each connection ────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            connections.chunked(2).forEach { pair ->
                if (pair.size == 2) {
                    val start = leftAnchors[pair[0]]
                    val end = rightAnchors[pair[1]]
                    if (start != null && end != null) {
                        val isPairCorrect = question.correctAnswerIds.chunked(2).contains(pair)
                        val lineColor = when {
                            isSubmitted && isPairCorrect -> correctColor
                            isSubmitted && !isPairCorrect -> errorColor
                            else -> primaryColor
                        }
                        val path = Path().apply {
                            moveTo(start.x, start.y)
                            cubicTo(start.x + 100f, start.y, end.x - 100f, end.y, end.x, end.y)
                        }
                        drawPath(path = path, color = lineColor, style = Stroke(width = 6f))
                        // Draw dots at endpoints
                        drawCircle(lineColor, radius = 10f, center = start)
                        drawCircle(lineColor, radius = 10f, center = end)
                    }
                }
            }
        }

        // ── UI Layer: left and right columns ───────────────────────────────
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween) {

            // Left column
            Column(
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            ) {
                leftItems.forEach { choice ->
                    val isSelected = selectedLeft == choice.id
                    val isConnected = connections.filterIndexed { i, _ -> i % 2 == 0 }
                        .contains(choice.id)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = when {
                                    isSelected -> primaryColor
                                    isConnected -> primaryColor.copy(alpha = 0.5f)
                                    else -> Color.Gray.copy(alpha = 0.4f)
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(
                                color = if (isSelected)
                                    primaryColor.copy(alpha = 0.12f)
                                else
                                    MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable(enabled = !isSubmitted) { selectedLeft = choice.id }
                            .padding(12.dp)
                            .onGloballyPositioned { coords ->
                                leftAnchors = leftAnchors.toMutableMap().also {
                                    it[choice.id] = Offset(
                                        coords.positionInParent().x + coords.size.width.toFloat(),
                                        coords.positionInParent().y + coords.size.height / 2f
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = choice.choiceText,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            // Right column
            Column(
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                rightItems.forEach { choice ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .border(1.dp, Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .clickable(enabled = !isSubmitted) {
                                val left = selectedLeft ?: return@clickable
                                val current = connections.toMutableList()
                                // Remove any existing connection for this left item
                                val existingIdx = current.indexOfFirst { it == left }
                                if (existingIdx != -1 && existingIdx % 2 == 0) {
                                    current.removeAt(existingIdx)
                                    if (existingIdx < current.size) current.removeAt(existingIdx)
                                }
                                current.add(left)
                                current.add(choice.id)
                                onConnectionMade(current)
                                selectedLeft = null
                            }
                            .padding(12.dp)
                            .onGloballyPositioned { coords ->
                                rightAnchors = rightAnchors.toMutableMap().also {
                                    it[choice.id] = Offset(
                                        coords.positionInParent().x,
                                        coords.positionInParent().y + coords.size.height / 2f
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = choice.choiceText,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    // Clear selection hint
    if (selectedLeft != null && !isSubmitted) {
        Text(
            text = "Now tap a word on the right to connect →",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Passage — Highlighted Words as Clickable AnnotatedString Spans
//    Question provides a short passage in `questionText`; `choices` are the
//    words the student must tap to select. On submit, correct words turn green,
//    incorrect ones red.
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PassageQuestion(
    question: QuizQuestion,
    selectedWordIds: List<String>,
    isSubmitted: Boolean,
    onSelectionChanged: (List<String>) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val correctColor = MaterialTheme.colorScheme.tertiary
    val errorColor = MaterialTheme.colorScheme.error
    val surfaceColor = MaterialTheme.colorScheme.secondaryContainer

    Column {
        if (!isSubmitted) {
            Text(
                text = "Tap the highlighted words in the passage to select them:",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // ── Word chip grid ───────────────────────────────────────────────────
        // We render each choice as a chip. The passage text is already in
        // questionText; the choices are the vocabulary words to identify.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            question.choices.forEach { choice ->
                val isSelected = choice.id in selectedWordIds
                val isCorrect = choice.id in question.correctAnswerIds

                val chipColor = when {
                    isSubmitted && isSelected && isCorrect -> correctColor
                    isSubmitted && isSelected && !isCorrect -> errorColor
                    isSubmitted && !isSelected && isCorrect -> correctColor.copy(alpha = 0.4f)
                    isSelected -> primaryColor
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }

                val textColor = when {
                    isSubmitted && isSelected -> Color.White
                    isSelected -> Color.White
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = chipColor,
                    modifier = Modifier.clickable(enabled = !isSubmitted) {
                        val newList = selectedWordIds.toMutableList()
                        if (isSelected) newList.remove(choice.id) else newList.add(choice.id)
                        onSelectionChanged(newList)
                    }
                ) {
                    Text(
                        text = choice.choiceText,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        if (isSubmitted) {
            Spacer(Modifier.height(16.dp))

            // Build AnnotatedString to show the passage with correct words highlighted
            val annotated = buildAnnotatedString {
                append(question.questionText)
                question.correctAnswerIds.forEach { correctId ->
                    val word = question.choices.find { it.id == correctId }?.choiceText ?: return@forEach
                    var start = question.questionText.indexOf(word)
                    while (start != -1) {
                        addStyle(
                            SpanStyle(
                                color = correctColor,
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline
                            ),
                            start = start,
                            end = start + word.length
                        )
                        start = question.questionText.indexOf(word, start + 1)
                    }
                }
            }

            Text(
                text = "Passage with answers highlighted:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = annotated,
                fontSize = 15.sp,
                lineHeight = 24.sp
            )
        }
    }
}