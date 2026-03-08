package com.kanjilens.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Help", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(
                            text = "<",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            HelpSection("What is ThorLens?")
            HelpBody(
                "ThorLens translates foreign-language game screens in real time. " +
                "Built for dual-screen devices like the Ayn Thor — run your game on the top screen " +
                "and ThorLens on the bottom."
            )

            HelpDivider()

            HelpSection("Translate Mode")
            HelpBody(
                "Captures a screenshot and sends it to an AI model that translates " +
                "and explains what's on screen. Works with any language — Japanese, Chinese, Korean, and more."
            )
            HelpBody("Three translation styles:")
            HelpBullet("Auto", "Translates and explains what to do next (recommended)")
            HelpBullet("Translate", "Just translates the text, no extra explanation")
            HelpBullet("Explain", "Full translation with detailed guidance on how to progress")
            Spacer(modifier = Modifier.height(4.dp))
            HelpBody("Requires an internet connection and an API key.")

            HelpDivider()

            HelpSection("JP Dictionary Mode")
            HelpBody(
                "Offline Japanese word-by-word breakdown. Uses on-device OCR to read Japanese text, " +
                "then looks up each word in a 212K-entry dictionary. Shows kanji, reading, meaning, " +
                "and JLPT level. No internet required."
            )

            HelpDivider()

            HelpSection("AI Models")
            HelpBullet("Gemini 2.5 Flash", "By Google. Free tier available — great to get started.")
            HelpBullet("GPT-4o mini", "By OpenAI. Reliable and fast.")
            Spacer(modifier = Modifier.height(4.dp))
            HelpBody("You can switch models in Settings. Each model stores its own API key.")

            HelpDivider()

            HelpSection("How to get an API key")

            HelpSubsection("Google (Gemini Flash) — Free")
            HelpBody("1. Go to aistudio.google.com")
            HelpBody("2. Sign in with your Google account")
            HelpBody("3. Click \"Get API Key\"")
            HelpBody("4. Click \"Create API key\"")
            HelpBody("5. Copy the key (starts with AIza...)")
            HelpBody("6. Paste it in ThorLens Settings")

            Spacer(modifier = Modifier.height(8.dp))

            HelpSubsection("OpenAI (GPT-4o mini)")
            HelpBody("1. Go to platform.openai.com")
            HelpBody("2. Create an account or sign in")
            HelpBody("3. Go to API Keys section")
            HelpBody("4. Click \"Create new secret key\"")
            HelpBody("5. Copy the key (starts with sk-...)")
            HelpBody("6. Paste it in ThorLens Settings")

            HelpDivider()

            Text(
                text = "Made by magiobus",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = "MIT License",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HelpSection(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun HelpSubsection(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun HelpBody(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 20.sp,
    )
}

@Composable
private fun HelpBullet(label: String, description: String) {
    Text(
        text = "\u2022 $label — $description",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 20.sp,
        modifier = Modifier.padding(start = 8.dp),
    )
}

@Composable
private fun HelpDivider() {
    Spacer(modifier = Modifier.height(12.dp))
    androidx.compose.material3.HorizontalDivider(
        color = MaterialTheme.colorScheme.surfaceVariant,
    )
    Spacer(modifier = Modifier.height(4.dp))
}
