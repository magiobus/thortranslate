# Ollama 与自定义 OpenAI 兼容节点 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在现有翻译模型基础上新增 Ollama 和自定义 OpenAI 兼容节点两种模型，支持动态拉取模型列表和视觉/文字两种输入模式。

**Architecture:** 两者共用 `callOpenAICompatible()` 方法，参数化 endpoint/apiKey/model/vision。AppSettings 新增 7 个字段存储配置。设置页 AI Model 区域扩展为两行按钮，选中后展示对应配置项。

**Tech Stack:** Kotlin, Jetpack Compose, OkHttp, SharedPreferences, ML Kit Text Recognition

---

### Task 1: AppSettings 新增字段和常量

**Files:**
- Modify: `app/src/main/java/com/kanjilens/data/models/AppSettings.kt`

**Step 1: 在 companion object 中新增模型常量**

在 `MODEL_MLKIT_OFFLINE_AUTO = 3` 后面加：

```kotlin
const val MODEL_OLLAMA = 4
const val MODEL_CUSTOM = 5
```

**Step 2: 新增 SharedPreferences key 常量**

在现有 KEY 常量区域加：

```kotlin
private const val KEY_OLLAMA_URL = "ollama_url"
private const val KEY_OLLAMA_MODEL = "ollama_model"
private const val KEY_OLLAMA_VISION = "ollama_vision"
private const val KEY_CUSTOM_URL = "custom_url"
private const val KEY_CUSTOM_API_KEY = "custom_api_key"
private const val KEY_CUSTOM_MODEL = "custom_model"
private const val KEY_CUSTOM_VISION = "custom_vision"
```

**Step 3: 新增 StateFlow 属性**

在现有 `_geminiApiKey` 等属性后面加：

```kotlin
private val _ollamaUrl = MutableStateFlow(prefs.getString(KEY_OLLAMA_URL, "http://192.168.1.x:11434") ?: "http://192.168.1.x:11434")
val ollamaUrl: StateFlow<String> = _ollamaUrl

private val _ollamaModel = MutableStateFlow(prefs.getString(KEY_OLLAMA_MODEL, "") ?: "")
val ollamaModel: StateFlow<String> = _ollamaModel

private val _ollamaVision = MutableStateFlow(prefs.getBoolean(KEY_OLLAMA_VISION, true))
val ollamaVision: StateFlow<Boolean> = _ollamaVision

private val _customUrl = MutableStateFlow(prefs.getString(KEY_CUSTOM_URL, "") ?: "")
val customUrl: StateFlow<String> = _customUrl

private val _customApiKey = MutableStateFlow(prefs.getString(KEY_CUSTOM_API_KEY, "") ?: "")
val customApiKey: StateFlow<String> = _customApiKey

private val _customModel = MutableStateFlow(prefs.getString(KEY_CUSTOM_MODEL, "") ?: "")
val customModel: StateFlow<String> = _customModel

private val _customVision = MutableStateFlow(prefs.getBoolean(KEY_CUSTOM_VISION, true))
val customVision: StateFlow<Boolean> = _customVision
```

**Step 4: 更新 activeApiKey**

```kotlin
val activeApiKey: String
    get() = when (_aiModel.value) {
        MODEL_GEMINI_FLASH -> _geminiApiKey.value
        MODEL_MLKIT_OFFLINE, MODEL_MLKIT_OFFLINE_AUTO -> ""
        MODEL_OLLAMA -> ""
        MODEL_CUSTOM -> _customApiKey.value
        else -> _openaiApiKey.value
    }
```

**Step 5: 新增 setter 方法**

```kotlin
fun setOllamaUrl(url: String) {
    _ollamaUrl.value = url
    prefs.edit().putString(KEY_OLLAMA_URL, url).apply()
}

fun setOllamaModel(model: String) {
    _ollamaModel.value = model
    prefs.edit().putString(KEY_OLLAMA_MODEL, model).apply()
}

fun setOllamaVision(vision: Boolean) {
    _ollamaVision.value = vision
    prefs.edit().putBoolean(KEY_OLLAMA_VISION, vision).apply()
}

fun setCustomUrl(url: String) {
    _customUrl.value = url
    prefs.edit().putString(KEY_CUSTOM_URL, url).apply()
}

fun setCustomApiKey(key: String) {
    _customApiKey.value = key
    prefs.edit().putString(KEY_CUSTOM_API_KEY, key).apply()
}

fun setCustomModel(model: String) {
    _customModel.value = model
    prefs.edit().putString(KEY_CUSTOM_MODEL, model).apply()
}

fun setCustomVision(vision: Boolean) {
    _customVision.value = vision
    prefs.edit().putBoolean(KEY_CUSTOM_VISION, vision).apply()
}
```

**Step 6: Commit**

```bash
git add app/src/main/java/com/kanjilens/data/models/AppSettings.kt
git commit -m "feat: add Ollama and Custom model constants and settings fields"
```

---

### Task 2: ScreenTranslator 新增翻译逻辑

**Files:**
- Modify: `app/src/main/java/com/kanjilens/translate/OpenAITranslator.kt`

**Step 1: 新增 `callOpenAICompatible()` 方法**

在 `callGemini()` 方法后面加：

```kotlin
private fun callOpenAICompatible(
    base64Image: String,
    endpoint: String,
    apiKey: String,
    model: String,
    vision: Boolean,
    systemPrompt: String,
    ocrText: String? = null,
): String? {
    val userContent = if (vision) {
        JSONArray().apply {
            put(JSONObject().apply {
                put("type", "text")
                put("text", "Translate this game screen.")
            })
            put(JSONObject().apply {
                put("type", "image_url")
                put("image_url", JSONObject().apply {
                    put("url", "data:image/jpeg;base64,$base64Image")
                    put("detail", "low")
                })
            })
        }
    } else {
        ocrText ?: return null
    }

    val body = JSONObject().apply {
        put("model", model)
        put("max_tokens", 1000)
        put("messages", JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
            put(JSONObject().apply {
                put("role", "user")
                if (vision) {
                    put("content", userContent)
                } else {
                    put("content", "Translate this game screen text:\n\n$userContent")
                }
            })
        })
    }

    val requestBuilder = Request.Builder()
        .url(endpoint)
        .addHeader("Content-Type", "application/json")
        .post(body.toString().toRequestBody("application/json".toMediaType()))

    if (apiKey.isNotEmpty()) {
        requestBuilder.addHeader("Authorization", "Bearer $apiKey")
    }

    val response = client.newCall(requestBuilder.build()).execute()
    val responseBody = response.body?.string() ?: return null
    if (!response.isSuccessful) return null

    val json = JSONObject(responseBody)
    val choices = json.optJSONArray("choices") ?: return null
    return if (choices.length() > 0) {
        choices.getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    } else null
}
```

**Step 2: 新增模型列表拉取方法**

```kotlin
suspend fun fetchOllamaModels(baseUrl: String): List<String> = withContext(Dispatchers.IO) {
    try {
        val request = Request.Builder()
            .url("$baseUrl/api/tags")
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext emptyList()
        if (!response.isSuccessful) return@withContext emptyList()
        val json = JSONObject(body)
        val models = json.optJSONArray("models") ?: return@withContext emptyList()
        (0 until models.length()).map { models.getJSONObject(it).getString("name") }
    } catch (e: Exception) {
        emptyList()
    }
}

suspend fun fetchOpenAIModels(baseUrl: String, apiKey: String): List<String> = withContext(Dispatchers.IO) {
    try {
        val requestBuilder = Request.Builder().url("$baseUrl/v1/models")
        if (apiKey.isNotEmpty()) requestBuilder.addHeader("Authorization", "Bearer $apiKey")
        val response = client.newCall(requestBuilder.build()).execute()
        val body = response.body?.string() ?: return@withContext emptyList()
        if (!response.isSuccessful) return@withContext emptyList()
        val json = JSONObject(body)
        val data = json.optJSONArray("data") ?: return@withContext emptyList()
        (0 until data.length()).map { data.getJSONObject(it).getString("id") }
    } catch (e: Exception) {
        emptyList()
    }
}
```

**Step 3: 在 `translateScreen()` 中新增 Ollama 和 Custom 分支**

在 `if (model == AppSettings.MODEL_MLKIT_OFFLINE)` 块后面，`val base64Image = bitmapToBase64(bitmap)` 之前加：

```kotlin
if (model == AppSettings.MODEL_OLLAMA || model == AppSettings.MODEL_CUSTOM) {
    val endpoint = if (model == AppSettings.MODEL_OLLAMA) {
        "${ollamaUrl.trimEnd('/')}/v1/chat/completions"
    } else {
        "${customUrl.trimEnd('/')}/v1/chat/completions"
    }
    val key = if (model == AppSettings.MODEL_CUSTOM) customApiKey else ""
    val modelName = if (model == AppSettings.MODEL_OLLAMA) ollamaModel else customModel
    val vision = if (model == AppSettings.MODEL_OLLAMA) ollamaVision else customVision
    val prompt = getSystemPrompt(style, outputLanguage)

    val base64 = bitmapToBase64(bitmap)
    val ocrText = if (!vision) textRecognizer.recognizeText(bitmap) else null

    val result = callOpenAICompatible(base64, endpoint, key, modelName, vision, prompt, ocrText)
    return@withContext if (result != null) {
        TranslateResult.Success(result)
    } else {
        TranslateResult.Error("Translation failed. Check your endpoint and model settings.")
    }
}
```

**Step 4: `translateScreen()` 签名新增参数**

在函数签名中新增（在 `outputLanguage` 后面）：

```kotlin
ollamaUrl: String = "",
ollamaModel: String = "",
ollamaVision: Boolean = true,
customUrl: String = "",
customApiKey: String = "",
customModel: String = "",
customVision: Boolean = true,
```

**Step 5: Commit**

```bash
git add app/src/main/java/com/kanjilens/translate/OpenAITranslator.kt
git commit -m "feat: add callOpenAICompatible, fetchOllamaModels, fetchOpenAIModels methods"
```

---

### Task 3: MainActivity 传递新参数

**Files:**
- Modify: `app/src/main/java/com/kanjilens/MainActivity.kt`

**Step 1: 找到 `translateScreen()` 调用处，新增参数传递**

在调用 `screenTranslator.translateScreen(...)` 时补充：

```kotlin
ollamaUrl = settings.ollamaUrl.value,
ollamaModel = settings.ollamaModel.value,
ollamaVision = settings.ollamaVision.value,
customUrl = settings.customUrl.value,
customApiKey = settings.customApiKey.value,
customModel = settings.customModel.value,
customVision = settings.customVision.value,
```

**Step 2: Commit**

```bash
git add app/src/main/java/com/kanjilens/MainActivity.kt
git commit -m "feat: pass Ollama and Custom settings to translateScreen"
```

---

### Task 4: SettingsScreen UI 更新

**Files:**
- Modify: `app/src/main/java/com/kanjilens/ui/screens/SettingsScreen.kt`

**Step 1: 新增 collectAsState 收集新字段**

在现有 `val aiModel by settings.aiModel.collectAsState()` 后面加：

```kotlin
val ollamaUrl by settings.ollamaUrl.collectAsState()
val ollamaModel by settings.ollamaModel.collectAsState()
val ollamaVision by settings.ollamaVision.collectAsState()
val customUrl by settings.customUrl.collectAsState()
val customApiKey by settings.customApiKey.collectAsState()
val customModel by settings.customModel.collectAsState()
val customVision by settings.customVision.collectAsState()
```

**Step 2: 新增本地 UI 状态**

```kotlin
var ollamaUrlInput by remember { mutableStateOf(ollamaUrl) }
var ollamaModelList by remember { mutableStateOf<List<String>>(emptyList()) }
var ollamaModelLoading by remember { mutableStateOf(false) }
var ollamaModelError by remember { mutableStateOf(false) }
var ollamaModelInput by remember { mutableStateOf(ollamaModel) }
var ollamaModelMenuExpanded by remember { mutableStateOf(false) }

var customUrlInput by remember { mutableStateOf(customUrl) }
var customApiKeyInput by remember { mutableStateOf(customApiKey) }
var customModelList by remember { mutableStateOf<List<String>>(emptyList()) }
var customModelLoading by remember { mutableStateOf(false) }
var customModelError by remember { mutableStateOf(false) }
var customModelInput by remember { mutableStateOf(customModel) }
var customModelMenuExpanded by remember { mutableStateOf(false) }
var showCustomApiKey by remember { mutableStateOf(false) }
```

需要 `rememberCoroutineScope()`：

```kotlin
val scope = rememberCoroutineScope()
```

**Step 3: 更新 AI Model 区域为两行**

将现有三个按钮的 `Row` 替换为：

```kotlin
Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SettingsOption(
            label = "Offline",
            selected = aiModel == AppSettings.MODEL_MLKIT_OFFLINE,
            onClick = { settings.setAiModel(AppSettings.MODEL_MLKIT_OFFLINE) },
            modifier = Modifier.weight(1f),
        )
        SettingsOption(
            label = "Gemini",
            selected = aiModel == AppSettings.MODEL_GEMINI_FLASH,
            onClick = { settings.setAiModel(AppSettings.MODEL_GEMINI_FLASH) },
            modifier = Modifier.weight(1f),
        )
        SettingsOption(
            label = "GPT-4o",
            selected = aiModel == AppSettings.MODEL_GPT4O_MINI,
            onClick = { settings.setAiModel(AppSettings.MODEL_GPT4O_MINI) },
            modifier = Modifier.weight(1f),
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SettingsOption(
            label = "Ollama",
            selected = aiModel == AppSettings.MODEL_OLLAMA,
            onClick = { settings.setAiModel(AppSettings.MODEL_OLLAMA) },
            modifier = Modifier.weight(1f),
        )
        SettingsOption(
            label = "Custom",
            selected = aiModel == AppSettings.MODEL_CUSTOM,
            onClick = { settings.setAiModel(AppSettings.MODEL_CUSTOM) },
            modifier = Modifier.weight(1f),
        )
    }
}
```

**Step 4: 新增 Ollama 配置区域**

在 AI Model section 的 `if (aiModel == AppSettings.MODEL_MLKIT_OFFLINE ...)` 块后面加：

```kotlin
if (aiModel == AppSettings.MODEL_OLLAMA) {
    OutlinedTextField(
        value = ollamaUrlInput,
        onValueChange = { ollamaUrlInput = it; settings.setOllamaUrl(it) },
        label = { Text("Ollama 地址") },
        placeholder = { Text("http://192.168.1.x:11434") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        ),
    )

    // 模型选择
    Box {
        val displayModel = if (ollamaModel.isEmpty()) "选择模型..." else ollamaModel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable {
                    if (!ollamaModelLoading) {
                        ollamaModelLoading = true
                        ollamaModelError = false
                        scope.launch {
                            val list = screenTranslator.fetchOllamaModels(ollamaUrlInput)
                            ollamaModelLoading = false
                            if (list.isEmpty()) {
                                ollamaModelError = true
                            } else {
                                ollamaModelList = list
                                ollamaModelMenuExpanded = true
                            }
                        }
                    }
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (ollamaModelLoading) "加载中..." else displayModel,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("▼", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(
            expanded = ollamaModelMenuExpanded,
            onDismissRequest = { ollamaModelMenuExpanded = false },
        ) {
            ollamaModelList.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        settings.setOllamaModel(name)
                        ollamaModelInput = name
                        ollamaModelMenuExpanded = false
                    },
                )
            }
        }
    }
    if (ollamaModelError) {
        OutlinedTextField(
            value = ollamaModelInput,
            onValueChange = { ollamaModelInput = it; settings.setOllamaModel(it) },
            label = { Text("手动输入模型名") },
            placeholder = { Text("llava") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            ),
        )
        Text(
            text = "无法连接到 Ollama，请手动输入模型名",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.error,
        )
    }

    // 视觉模式开关
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("视觉模式", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = if (ollamaVision) "直接发送截图（需要视觉模型）" else "OCR 提取文字后翻译",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        androidx.compose.material3.Switch(
            checked = ollamaVision,
            onCheckedChange = { settings.setOllamaVision(it) },
        )
    }
}
```

**Step 5: 新增 Custom 配置区域**

在 Ollama 配置区域后面，同样在 AI Model section 内加：

```kotlin
if (aiModel == AppSettings.MODEL_CUSTOM) {
    OutlinedTextField(
        value = customUrlInput,
        onValueChange = { customUrlInput = it; settings.setCustomUrl(it) },
        label = { Text("Base URL") },
        placeholder = { Text("https://api.example.com") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
    OutlinedTextField(
        value = customApiKeyInput,
        onValueChange = { customApiKeyInput = it; settings.setCustomApiKey(it) },
        label = { Text("API Key（可选）") },
        singleLine = true,
        visualTransformation = if (showCustomApiKey) VisualTransformation.None else PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
    if (customApiKeyInput.isNotEmpty()) {
        Text(
            text = if (showCustomApiKey) "隐藏" else "显示",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { showCustomApiKey = !showCustomApiKey }.padding(top = 2.dp),
        )
    }

    // 模型选择（与 Ollama 相同结构，调 /v1/models）
    Box {
        val displayModel = if (customModel.isEmpty()) "选择模型..." else customModel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable {
                    if (!customModelLoading) {
                        customModelLoading = true
                        customModelError = false
                        scope.launch {
                            val list = screenTranslator.fetchOpenAIModels(customUrlInput, customApiKeyInput)
                            customModelLoading = false
                            if (list.isEmpty()) {
                                customModelError = true
                            } else {
                                customModelList = list
                                customModelMenuExpanded = true
                            }
                        }
                    }
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (customModelLoading) "加载中..." else displayModel,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("▼", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(
            expanded = customModelMenuExpanded,
            onDismissRequest = { customModelMenuExpanded = false },
        ) {
            customModelList.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        settings.setCustomModel(name)
                        customModelInput = name
                        customModelMenuExpanded = false
                    },
                )
            }
        }
    }
    if (customModelError) {
        OutlinedTextField(
            value = customModelInput,
            onValueChange = { customModelInput = it; settings.setCustomModel(it) },
            label = { Text("手动输入模型名") },
            placeholder = { Text("deepseek-chat") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            ),
        )
        Text(
            text = "无法获取模型列表，请手动输入模型名",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.error,
        )
    }

    // 视觉模式开关
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("视觉模式", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = if (customVision) "直接发送截图（需要视觉模型）" else "OCR 提取文字后翻译",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        androidx.compose.material3.Switch(
            checked = customVision,
            onCheckedChange = { settings.setCustomVision(it) },
        )
    }
}
```

**Step 6: SettingsScreen 签名新增 screenTranslator 参数**

```kotlin
fun SettingsScreen(
    settings: AppSettings,
    screenTranslator: ScreenTranslator,
    onBack: () -> Unit,
)
```

**Step 7: Commit**

```bash
git add app/src/main/java/com/kanjilens/ui/screens/SettingsScreen.kt
git commit -m "feat: add Ollama and Custom model UI in SettingsScreen"
```

---

### Task 5: MainActivity 传递 screenTranslator 给 SettingsScreen

**Files:**
- Modify: `app/src/main/java/com/kanjilens/MainActivity.kt`

**Step 1: 找到 SettingsScreen 调用处，新增参数**

```kotlin
SettingsScreen(
    settings = settings,
    screenTranslator = screenTranslator,
    onBack = { ... },
)
```

**Step 2: Commit**

```bash
git add app/src/main/java/com/kanjilens/MainActivity.kt
git commit -m "feat: pass screenTranslator to SettingsScreen"
```

---

### Task 6: 验证构建

**Step 1: 构建**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL，无编译错误。

**Step 2: 如有错误，修复后重新构建**

**Step 3: Commit（如有修复）**

```bash
git add -A
git commit -m "fix: resolve compilation errors for Ollama/Custom model support"
```
