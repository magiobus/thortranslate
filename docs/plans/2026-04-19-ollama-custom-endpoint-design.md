# 设计文档：Ollama 与自定义 OpenAI 兼容节点支持

## 概述

新增两种翻译模型：Ollama（本地 LLM 服务）和自定义 OpenAI 兼容节点。两者共用同一套 OpenAI 兼容 API 逻辑，分别独立配置。

## 数据模型（AppSettings）

新增模型常量：
```kotlin
const val MODEL_OLLAMA = 4
const val MODEL_CUSTOM = 5
```

新增 SharedPreferences 字段：

| 字段 | 类型 | 默认值 |
|------|------|--------|
| `ollama_url` | String | `http://192.168.1.x:11434` |
| `ollama_model` | String | `""` |
| `ollama_vision` | Boolean | `true` |
| `custom_url` | String | `""` |
| `custom_api_key` | String | `""` |
| `custom_model` | String | `""` |
| `custom_vision` | Boolean | `true` |

`activeApiKey`：Ollama 返回空字符串，Custom 返回 `customApiKey`。

## 设置页 UI

AI Model 区域扩展为两行：
```
[ Offline ] [ Gemini ] [ GPT-4o ]
[ Ollama  ] [ Custom ]
```

选中 Ollama 时展示：
- 地址输入框（默认 `http://192.168.1.x:11434`）
- 模型下拉（点击拉取 `/api/tags`，支持 loading 状态和失败 fallback 手动输入）
- 视觉模式开关

选中 Custom 时展示：
- Base URL 输入框
- API Key 输入框（密码遮罩）
- 模型下拉（点击拉取 `/v1/models`，同上 fallback）
- 视觉模式开关

## 翻译逻辑（ScreenTranslator）

新增共用方法：
```kotlin
private fun callOpenAICompatible(
    base64Image: String,
    endpoint: String,
    apiKey: String,
    model: String,
    vision: Boolean,
    systemPrompt: String,
    ocrText: String? = null,
): String?
```

- `vision = true`：图片 base64 编码放入 messages（与现有 GPT-4o 逻辑一致）
- `vision = false`：使用传入的 `ocrText` 作为纯文本发送

`translateScreen()` 新增分支：
```kotlin
MODEL_OLLAMA -> callOpenAICompatible(..., ollamaUrl + "/v1/chat/completions", apiKey = "", ...)
MODEL_CUSTOM -> callOpenAICompatible(..., customUrl + "/v1/chat/completions", apiKey = customApiKey, ...)
```

非视觉模式下，调用前先执行 `TextRecognizer.recognizeText()` 提取文字。

新增模型列表拉取方法：
- `fetchOllamaModels(url): List<String>` — `GET /api/tags`，解析 `models[].name`
- `fetchOpenAIModels(url, apiKey): List<String>` — `GET /v1/models`，解析 `data[].id`
