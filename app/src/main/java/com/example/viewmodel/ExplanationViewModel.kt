package com.example.viewmodel

import ai.nobodywho.Chat
import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.*
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

sealed interface ExplanationUiState {
    object Idle : ExplanationUiState
    object Loading : ExplanationUiState
    data class Success(val response: String) : ExplanationUiState
    data class Error(val message: String) : ExplanationUiState
}

class ExplanationViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ExplanationDatabase.getDatabase(application)
    private val dao = db.explanationDao

    private var chat: Chat? = null

    init {
        // Pre-create the cache directory to avoid "No such file or directory" error during model download
        val modelCachePath = File(application.cacheDir, "nobodywho/models/NobodyWho/Qwen_Qwen3-0.6B-GGUF")
        if (!modelCachePath.exists()) {
            modelCachePath.mkdirs()
        }
    }

    // Input States
    var codeSnippet = MutableStateFlow("")
    var selectedLanguage = MutableStateFlow("Kotlin")
    var selectedAction = MutableStateFlow("General Explain")
    var selectedModelName = MutableStateFlow("Gemma 4 Pro (Advanced Spec)")
    var temperature = MutableStateFlow(0.4f)

    // UI Result State
    private val _uiState = MutableStateFlow<ExplanationUiState>(ExplanationUiState.Idle)
    val uiState: StateFlow<ExplanationUiState> = _uiState.asStateFlow()

    // History & Search State
    val searchTerm = MutableStateFlow("")
    val favoritesOnly = MutableStateFlow(false)

    // Text to Speech
    private var tts: TextToSpeech? = null
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    val historyList: StateFlow<List<ExplanationEntity>> = combine(
        searchTerm,
        favoritesOnly,
        dao.getAllExplanations()
    ) { query, showFavsOnly, allExplanations ->
        allExplanations.filter { item ->
            val matchesSearch = query.isEmpty() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.code.contains(query, ignoreCase = true) ||
                    item.explanation.contains(query, ignoreCase = true) ||
                    item.language.contains(query, ignoreCase = true)

            val matchesFavorite = !showFavsOnly || item.isFavorite
            matchesSearch && matchesFavorite
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Language list
    val availableLanguages = listOf(
        "Kotlin", "Java", "Python", "JavaScript", "TypeScript", 
        "C++", "C#", "Rust", "Go", "Swift", "SQL", "HTML/CSS", "Bash/Shell"
    )

    // Models mappings
    val availableModels = listOf(
        "Gemma 4 Pro (Advanced Spec)",
        "Gemma 4 Fast (Speed Spec)"
    )

    // Actions list
    val availableActions = listOf(
        "General Explain",
        "Summarize Code",
        "Optimize Code",
        "Generate Unit Test",
        "Explain Line-by-Line",
        "Refactor Code"
    )

    // Dynamic stats
    val statsFlow = dao.getAllExplanations().map { list ->
        val total = list.size
        val favs = list.count { it.isFavorite }
        val kotlinCount = list.count { it.language.equals("Kotlin", ignoreCase = true) }
        val pythonCount = list.count { it.language.equals("Python", ignoreCase = true) }
        val optimizeCount = list.count { it.actionType.contains("Optimize", ignoreCase = true) }
        
        Triple(total, favs, mapOf(
            "Kotlin" to kotlinCount,
            "Python" to pythonCount,
            "Optimizations" to optimizeCount
        ))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Triple(0, 0, emptyMap()))

    fun loadPreset(presetKey: String) {
        val snippet = when (presetKey) {
            "coroutine" -> """// Kotlin Coroutines Channel example
suspend fun fetchAndProcessData() = coroutineScope {
    val channel = Channel<Int>()
    launch {
        for (x in 1..5) {
            delay(100)
            channel.send(x * x)
        }
        channel.close()
    }
    launch {
        for (y in channel) {
            println("Received formatted score: " + y)
        }
    }
}"""
            "stream" -> """// Java Stream API filtering and reduction
public int sumOfEvenSquares(List<Integer> numbers) {
    return numbers.stream()
        .filter(n -> n % 2 == 0)
        .map(n -> n * n)
        .reduce(0, Integer::sum);
}"""
            "pydeco" -> """# Python decorator for timing execution
import time

fun time_it(func):
    def wrapper(*args, **kwargs):
        start = time.time()
        result = func(*args, **kwargs)
        end = time.time()
        print(f"{func.__name__} took {end - start:.4f}s")
        return result
    return wrapper"""
            else -> ""
        }
        codeSnippet.value = snippet
    }

    fun makeExplanationRequest() {
        if (codeSnippet.value.trim().isEmpty()) {
            _uiState.value = ExplanationUiState.Error("Code is empty. Please paste or write some code to start analyzing!")
            return
        }

        _uiState.value = ExplanationUiState.Loading
        stopSpeaking()

        viewModelScope.launch {
            try {
                // Initialize chat if not already done
                if (chat == null) {
                    chat = withContext(Dispatchers.IO) {
                        Chat.fromPath(
                            modelPath = "hf://NobodyWho/Qwen_Qwen3-0.6B-GGUF/Qwen_Qwen3-0.6B-Q4_K_M.gguf"
                        )
                    }
                }

                // Construct structural instruction preset
                val actionInstructions = when (selectedAction.value) {
                    "General Explain" -> "Explain how the following ${selectedLanguage.value} code executes and functions."
                    "Summarize Code" -> "Provide a highly structured, concise, and professional developer summary of the following ${selectedLanguage.value} code."
                    "Optimize Code" -> "Analyze the following ${selectedLanguage.value} code for speed/memory efficiency bottleneck. Return an optimized version."
                    "Generate Unit Test" -> "Write a comprehensive suite of unit tests for the following ${selectedLanguage.value} code."
                    "Explain Line-by-Line" -> "Do a sequential, clear line-by-line annotation of the following ${selectedLanguage.value} code."
                    "Refactor Code" -> "Refactor the following ${selectedLanguage.value} code to match modern clean architecture."
                    else -> "Analyze the following ${selectedLanguage.value} code snippet."
                }

                val prompt = """
                    Task: $actionInstructions
                    
                    Code:
                    ```${selectedLanguage.value.lowercase()}
                    ${codeSnippet.value}
                    ```
                    
                    Generate a clear explanation.
                """.trimIndent()

                val resultText = withContext(Dispatchers.IO) {
                    chat?.ask(prompt)?.completed()
                }

                if (resultText != null) {
                    _uiState.value = ExplanationUiState.Success(resultText)
                    
                    // Automatically save to Room History
                    val titleText = getExcerptTitle(codeSnippet.value, selectedAction.value)
                    val historyRecord = ExplanationEntity(
                        title = titleText,
                        code = codeSnippet.value,
                        explanation = resultText,
                        language = selectedLanguage.value,
                        actionType = selectedAction.value,
                        timestamp = System.currentTimeMillis()
                    )
                    
                    withContext(Dispatchers.IO) {
                        dao.insertExplanation(historyRecord)
                    }
                } else {
                    _uiState.value = ExplanationUiState.Error("Received empty response from the local model.")
                }
            } catch (e: Exception) {
                Log.e("ExplanationViewModel", "Chat Error", e)
                _uiState.value = ExplanationUiState.Error("Model Error: ${e.message ?: "Unknown Error"}. Please check if the model is downloaded correctly.")
            }
        }
    }

    fun loadAndExplainRecord(record: ExplanationEntity) {
        codeSnippet.value = record.code
        selectedLanguage.value = record.language
        selectedAction.value = record.actionType
        makeExplanationRequest()
    }

    private fun getExcerptTitle(code: String, action: String): String {
        val cleanCode = code.trim().lines().firstOrNull { it.isNotBlank() } ?: "snippet"
        val trimmed = if (cleanCode.length > 25) cleanCode.take(25) + "..." else cleanCode
        return "$action: $trimmed"
    }

    fun toggleFavorite(entity: ExplanationEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.updateFavoriteStatus(entity.id, !entity.isFavorite)
            }
        }
    }

    fun deleteExplanation(entity: ExplanationEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.deleteExplanation(entity)
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.deleteAllExplanations()
            }
        }
    }

    fun speakText(text: String, context: Context) {
        if (tts == null) {
            _isSpeaking.value = true
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.let { player ->
                        player.language = Locale.US
                        // Strip out markdown symbols (*, `, #) for cleaner pronunciation synthesis
                        val cleanText = text
                            .replace(Regex("[`*#_~]"), " ")
                            .replace(Regex("<[^>]*>"), "")
                        _isSpeaking.value = true
                        player.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "GemmaVoice")
                    }
                } else {
                    _isSpeaking.value = false
                }
            }
        } else {
            val cleanText = text
                .replace(Regex("[`*#_~]"), " ")
                .replace(Regex("<[^>]*>"), "")
            _isSpeaking.value = true
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "GemmaVoice")
        }
    }

    fun stopSpeaking() {
        tts?.let {
            if (it.isSpeaking) {
                it.stop()
            }
        }
        _isSpeaking.value = false
    }

    override fun onCleared() {
        super.onCleared()
        tts?.let {
            it.stop()
            it.shutdown()
        }
    }
}
