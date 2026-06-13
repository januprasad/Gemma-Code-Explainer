package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ExplanationEntity
import com.example.ui.theme.*
import com.example.viewmodel.ExplanationUiState
import com.example.viewmodel.ExplanationViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold"),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    GemmaAppDashboard(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun GemmaAppDashboard(
    modifier: Modifier = Modifier,
    viewModel: ExplanationViewModel = viewModel()
) {

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val codeSnippet by viewModel.codeSnippet.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val selectedAction by viewModel.selectedAction.collectAsState()
    val selectedModel by viewModel.selectedModelName.collectAsState()
    val temperature by viewModel.temperature.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val history by viewModel.historyList.collectAsState()
    val searchTerm by viewModel.searchTerm.collectAsState()
    val favoritesOnly by viewModel.favoritesOnly.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val stats by viewModel.statsFlow.collectAsState()

    var activeTab by remember { mutableStateOf("COMPILE") }

    // Dropdown States
    var langExpanded by remember { mutableStateOf(false) }
    var actionExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .background(TerminalBg)
            .fillMaxSize()
    ) {
        // App Terminal Header
        TerminalHeader(
            activeTab = activeTab,
            onTabSelected = { activeTab = it }
        )

        Divider(color = TerminalBorder, modifier = Modifier.fillMaxWidth())

        if (activeTab == "COMPILE") {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp, top = 16.dp)
            ) {
                // Preset Demos Selection
                item {
                    Column {
                        Text(
                            text = "PRESET SNIPPETS",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                fontSize = 11.sp,
                                letterSpacing = 1.2.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PresetChip("Kotlin channel", onClick = {
                                viewModel.selectedLanguage.value = "Kotlin"
                                viewModel.loadPreset("coroutine")
                            })
                            PresetChip("Java stream", onClick = {
                                viewModel.selectedLanguage.value = "Java"
                                viewModel.loadPreset("stream")
                            })
                            PresetChip("Python wrapper", onClick = {
                                viewModel.selectedLanguage.value = "Python"
                                viewModel.loadPreset("pydeco")
                            })
                        }
                    }
                }

                // Selector settings panel (Language, Engine Spec, Actions)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
                            .background(TerminalSurface)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "COMPILATION PROFILES",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                fontSize = 11.sp,
                                letterSpacing = 1.2.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Row 1: Language Dropdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lang / Spec:",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = TextLight
                                ),
                                modifier = Modifier.width(96.dp)
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                Button(
                                    onClick = { langExpanded = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TerminalBorder,
                                        contentColor = TextLight
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.fillMaxWidth().height(40.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = selectedLanguage, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Language", modifier = Modifier.size(16.dp))
                                    }
                                }
                                DropdownMenu(
                                    expanded = langExpanded,
                                    onDismissRequest = { langExpanded = false },
                                    modifier = Modifier.background(TerminalSurface).border(1.dp, TerminalBorder)
                                ) {
                                    viewModel.availableLanguages.forEach { language ->
                                        DropdownMenuItem(
                                            text = { Text(language, fontFamily = FontFamily.Monospace, color = TextLight, fontSize = 13.sp) },
                                            onClick = {
                                                viewModel.selectedLanguage.value = language
                                                langExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Row 2: Action Dropdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Gemma Opt:",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = TextLight
                                ),
                                modifier = Modifier.width(96.dp)
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                Button(
                                    onClick = { actionExpanded = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TerminalBorder,
                                        contentColor = TextLight
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.fillMaxWidth().height(40.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = selectedAction, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Action", modifier = Modifier.size(16.dp))
                                    }
                                }
                                DropdownMenu(
                                    expanded = actionExpanded,
                                    onDismissRequest = { actionExpanded = false },
                                    modifier = Modifier.background(TerminalSurface).border(1.dp, TerminalBorder)
                                ) {
                                    viewModel.availableActions.forEach { act ->
                                        DropdownMenuItem(
                                            text = { Text(act, fontFamily = FontFamily.Monospace, color = TextLight, fontSize = 13.sp) },
                                            onClick = {
                                                viewModel.selectedAction.value = act
                                                actionExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Row 3: Model Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LLM Core:",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = TextLight
                                ),
                                modifier = Modifier.width(96.dp)
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                Button(
                                    onClick = { modelExpanded = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TerminalBorder,
                                        contentColor = GemmaCyan
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.fillMaxWidth().height(40.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = selectedModel, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Model", modifier = Modifier.size(16.dp))
                                    }
                                }
                                DropdownMenu(
                                    expanded = modelExpanded,
                                    onDismissRequest = { modelExpanded = false },
                                    modifier = Modifier.background(TerminalSurface).border(1.dp, TerminalBorder)
                                ) {
                                    viewModel.availableModels.forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text(item, fontFamily = FontFamily.Monospace, color = TextLight, fontSize = 12.sp) },
                                            onClick = {
                                                viewModel.selectedModelName.value = item
                                                modelExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Row 4: Temperature Slider
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Temp (Creativity):",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = TextLight
                                )
                                Text(
                                    text = String.format("%.1f", temperature),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = GemmaTeal,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = temperature,
                                onValueChange = { viewModel.temperature.value = it },
                                valueRange = 0.0f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = GemmaCyan,
                                    activeTrackColor = GemmaTeal,
                                    inactiveTrackColor = TerminalBorder
                                ),
                                modifier = Modifier.height(16.dp)
                            )
                        }
                    }
                }

                // Interactive Terminal Code Field
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
                            .background(TerminalSurface)
                    ) {
                        // Title bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(TerminalBorder)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(GemmaTeal, RoundedCornerShape(5.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "code_editor.${selectedLanguage.lowercase()}",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextLight
                                    )
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Paste button
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Clear Code",
                                    tint = TextMuted,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { viewModel.codeSnippet.value = "" }
                                )
                                ButtonPasteShortcut(onPasted = {
                                    viewModel.codeSnippet.value = it
                                })
                            }
                        }

                        // Code editing container with code line numbers
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 160.dp, max = 220.dp)
                                .padding(vertical = 8.dp)
                        ) {
                            // Line numbers panel
                            val lineCount = maxOf(1, codeSnippet.split("\n").size)
                            val numbersText = (1..lineCount).joinToString("\n") { "%2d".format(it) }
                            Text(
                                text = numbersText,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    lineHeight = 20.sp,
                                    color = TextMuted
                                ),
                                modifier = Modifier
                                    .padding(start = 12.dp, end = 8.dp)
                                    .drawBehind {
                                        drawLine(
                                            color = TerminalBorder,
                                            start = Offset(size.width + 8.dp.toPx(), 0f),
                                            end = Offset(size.width + 8.dp.toPx(), size.height),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                    }
                            )

                            // Main Editor Field
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                            ) {
                                if (codeSnippet.isEmpty()) {
                                    Text(
                                        text = "// Paste your $selectedLanguage code snippet here...",
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            lineHeight = 20.sp,
                                            color = TextMuted
                                        )
                                    )
                                }
                                BasicTextField(
                                    value = codeSnippet,
                                    onValueChange = { viewModel.codeSnippet.value = it },
                                    textStyle = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        lineHeight = 20.sp,
                                        color = GemmaCyan
                                    ),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("code_input_editor"),
                                    cursorBrush = SolidColor(GemmaCyan)
                                )
                            }
                        }
                    }
                }

                // Analyze Trigger button
                item {
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.makeExplanationRequest()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GemmaCyan,
                            contentColor = TerminalBg
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("analyze_trigger_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Run")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EXECUTE GEMMA ANALYSIS",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                    }
                }

                // Output Result Pane: Idle / Loading / Success / Error
                item {
                    AnimatedContent(
                        targetState = uiState,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        }
                    ) { state ->
                        when (state) {
                            is ExplanationUiState.Idle -> {
                                TerminalFeedbackCard(
                                    title = "COMPILATION CONSOLE IDLE",
                                    body = "Select a profile, paste/edit your code snippet, and click compile. Gemma 4 will parse structural syntax trees instantly.",
                                    color = TextMuted
                                )
                            }
                            is ExplanationUiState.Loading -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, GemmaTeal, RoundedCornerShape(8.dp))
                                        .background(TerminalSurface)
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = GemmaCyan)
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "PINGING GEMMA 4 CLUSTERS...",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GemmaCyan
                                    )
                                    Text(
                                        text = "Refactoring syntax grids & evaluating logical dependencies.",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            is ExplanationUiState.Success -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, GemmaTeal, RoundedCornerShape(8.dp))
                                        .background(TerminalSurface)
                                        .padding(16.dp)
                                ) {
                                    // Utility bar inside result
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(GemmaCyan, RoundedCornerShape(4.dp))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "GEMMA_4_RESULT",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GemmaCyan
                                            )
                                        }

                                        // Play Voice, Copy, Bookmark Actions
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            IconButton(
                                                onClick = {
                                                    if (isSpeaking) {
                                                        viewModel.stopSpeaking()
                                                    } else {
                                                        viewModel.speakText(state.response, context)
                                                    }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isSpeaking) Icons.Default.Close else Icons.Default.PlayArrow,
                                                    contentDescription = "Read Aloud",
                                                    tint = if (isSpeaking) Pink80 else GemmaCyan,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    val clip = ClipData.newPlainText("Gemma Explanation", state.response)
                                                    clipboard.setPrimaryClip(clip)
                                                    Toast.makeText(context, "Copied analysis to clipboard!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = "Copy text",
                                                    tint = GemmaTeal,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    Divider(color = TerminalBorder, modifier = Modifier.padding(bottom = 12.dp))

                                    // Render custom formatted Markdown content
                                    MarkdownResultView(
                                        text = state.response,
                                        onCopy = { copiedCode ->
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Gemma Code Block", copiedCode)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Copied code block to clipboard!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    
                                    // Extraction Caution Tip
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "⚠ Prototype API calls execute client-side. Protect backend secrets before shipping production releases.",
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            color = TextMuted
                                        ),
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                            is ExplanationUiState.Error -> {
                                TerminalFeedbackCard(
                                    title = "CONNECTION / EXCEPTION ERROR",
                                    body = state.message,
                                    color = Color(0xFFEF5350)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // History Tab layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Statistics & Info header
                DynamicStatsDashboard(stats = stats)

                // Search box
                OutlinedTextField(
                    value = searchTerm,
                    onValueChange = { viewModel.searchTerm.value = it },
                    placeholder = { Text("Search logs...", fontFamily = FontFamily.Monospace, color = TextMuted, fontSize = 13.sp) },
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = TextLight, fontSize = 13.sp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon", tint = TextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GemmaCyan,
                        unfocusedBorderColor = TerminalBorder,
                        focusedContainerColor = TerminalSurface,
                        unfocusedContainerColor = TerminalSurface
                    )
                )

                // Favorite filter checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = favoritesOnly,
                        onCheckedChange = { viewModel.favoritesOnly.value = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = GemmaCyan,
                            uncheckedColor = TextMuted
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Show starred bookmarks only",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextLight
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearAllHistory() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear History", tint = Pink80)
                        }
                    }
                }

                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
                            .background(TerminalSurface)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Warning, contentDescription = "Empty", tint = TextMuted, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "NO EXPLANATION ARCHIVES FOUND",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (favoritesOnly) "Try toggling off 'starred bookmarks' filter." else "Analyze code in the compiler, and reports will archive here database-safely.",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(history) { record ->
                            HistoryItemCard(
                                record = record,
                                onFavoriteToggle = { viewModel.toggleFavorite(record) },
                                onDelete = { viewModel.deleteExplanation(record) },
                                onInspect = {
                                    viewModel.loadAndExplainRecord(record)
                                    activeTab = "COMPILE"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalHeader(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Purple dynamic bolt logo from design HTML
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF6750A4), Color(0xFFD0BCFF)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Build, // representing bolt
                        contentDescription = "Gemma Bolt",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Gemma 4",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = "Advanced Code Assistant",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
            
            // "JD" Avatar from design HTML
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFEADDFF))
                    .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "JD",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF21005D),
                        fontSize = 14.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation tab selector: beautiful, clean minimal rounded pill outline
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF3EDF7), RoundedCornerShape(24.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (activeTab == "COMPILE") Color(0xFF6750A4) else Color.Transparent)
                    .clickable { onTabSelected("COMPILE") },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "COMPILE CODE",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (activeTab == "COMPILE") Color.White else Color(0xFF49454F)
                    )
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (activeTab == "HISTORY") Color(0xFF6750A4) else Color.Transparent)
                    .clickable { onTabSelected("HISTORY") },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "EXPLAIN ARCHIVE",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (activeTab == "HISTORY") Color.White else Color(0xFF49454F)
                    )
                )
            }
        }
    }
}

@Composable
fun DynamicStatsDashboard(
    stats: Triple<Int, Int, Map<String, Int>>
) {
    val (total, favs, counts) = stats
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
            .background(TerminalSurface)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Text("TOTAL RUNS", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextMuted)
            Text("$total", fontFamily = FontFamily.Monospace, fontSize = 16.sp, color = GemmaCyan, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Text("STARRED", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextMuted)
            Text("$favs", fontFamily = FontFamily.Monospace, fontSize = 16.sp, color = Pink80, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Text("KOTLIN", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextMuted)
            Text("${counts["Kotlin"] ?: 0}", fontFamily = FontFamily.Monospace, fontSize = 16.sp, color = GemmaTeal, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PresetChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(TerminalSurface)
            .border(1.dp, TerminalBorder, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = "+ $label",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = GemmaCyan
            )
        )
    }
}

@Composable
fun ButtonPasteShortcut(
    onPasted: (String) -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(TerminalSurface)
            .clickable {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = clipboard.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val pasted = clipData.getItemAt(0).text
                    if (!pasted.isNullOrEmpty()) {
                        onPasted(pasted.toString())
                        Toast.makeText(context, "Pasted from clipboard!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Clipboard is empty!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "No clipdata found on device!", Toast.LENGTH_SHORT).show()
                }
            }
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = "PASTE",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = GemmaCyan,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TerminalFeedbackCard(
    title: String,
    body: String,
    color: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .background(TerminalSurface)
            .padding(14.dp)
    ) {
        Text(
            text = "> $title",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = color,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = body,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = TextLight
            )
        )
    }
}

@Composable
fun MarkdownResultView(text: String, onCopy: (String) -> Unit) {
    // Splits text dynamically into code block and literal text chunks
    val blocks = remember(text) {
        val list = mutableListOf<MarkdownBlock>()
        val parts = text.split("```")
        for (i in parts.indices) {
            val part = parts[i]
            if (i % 2 == 1) { // Code block split
                val lines = part.lines()
                val lang = lines.firstOrNull()?.trim() ?: "code"
                val codeContent = lines.drop(1).joinToString("\n").trim()
                if (codeContent.isNotEmpty()) {
                    list.add(MarkdownBlock.Code(language = lang, content = codeContent))
                }
            } else {
                if (part.isNotBlank()) {
                    list.add(MarkdownBlock.Text(content = part.trim()))
                }
            }
        }
        list
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Code -> {
                    CodeBlockCard(language = block.language, content = block.content, onCopy = onCopy)
                }
                is MarkdownBlock.Text -> {
                    TextLineWithFormatting(block.content)
                }
            }
        }
    }
}

sealed interface MarkdownBlock {
    data class Text(val content: String) : MarkdownBlock
    data class Code(val language: String, val content: String) : MarkdownBlock
}

@Composable
fun TextLineWithFormatting(text: String) {
    val annotatedString = remember(text) {
        val builder = AnnotatedString.Builder()
        var currentIndex = 0
        val regex = Regex("(\\*\\*.*?\\*\\*|`.*?`)")
        val matches = regex.findAll(text)

        for (match in matches) {
            val start = match.range.first
            val end = match.range.last + 1

            if (start > currentIndex) {
                builder.append(text.substring(currentIndex, start))
            }

            val matchText = match.value
            if (matchText.startsWith("**") && matchText.endsWith("**")) {
                val boldContent = matchText.substring(2, matchText.length - 2)
                builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))) {
                    append(boldContent)
                }
            } else if (matchText.startsWith("`") && matchText.endsWith("`")) {
                val inlineCode = matchText.substring(1, matchText.length - 1)
                builder.withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF21005D),
                        background = Color(0xFFEADDFF)
                    )
                ) {
                    append(inlineCode)
                }
            }
            currentIndex = end
        }
        if (currentIndex < text.length) {
            builder.append(text.substring(currentIndex))
        }
        builder.toAnnotatedString()
    }

    Text(
        text = annotatedString,
        style = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = Color(0xFF1D1B20)
        )
    )
}

@Composable
fun CodeBlockCard(
    language: String,
    content: String,
    onCopy: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF21005D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1B004B))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (language.isNotEmpty()) language.uppercase() else "CODE SNIPPET",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEADDFF)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF21005D))
                    .clickable { onCopy(content) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "COPY",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(
            text = content,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = Color(0xFFEADDFF)
            ),
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        )
    }
}

@Composable
fun HistoryItemCard(
    record: ExplanationEntity,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit,
    onInspect: () -> Unit
) {
    val dateString = remember(record.timestamp) {
        val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        format.format(Date(record.timestamp))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable { onInspect() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFEADDFF))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = record.language,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF21005D),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = record.actionType,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.sp,
                    color = Color(0xFF49454F),
                    fontWeight = FontWeight.Bold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (record.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Starred Check",
                    tint = if (record.isFavorite) Color(0xFF6750A4) else Color(0xFF79747E),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onFavoriteToggle() }
                )
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove Record",
                    tint = Color(0xFF79747E),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onDelete() }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = record.title,
            fontFamily = FontFamily.SansSerif,
            fontSize = 14.sp,
            color = Color(0xFF21005D),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (record.explanation.length > 110) record.explanation.take(110) + "..." else record.explanation,
            fontFamily = FontFamily.SansSerif,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = Color(0xFF49454F)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateString,
                fontFamily = FontFamily.SansSerif,
                fontSize = 10.sp,
                color = Color(0xFF79747E)
            )
            Text(
                text = "Load & Recompile >",
                fontFamily = FontFamily.SansSerif,
                fontSize = 11.sp,
                color = Color(0xFF6750A4),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

