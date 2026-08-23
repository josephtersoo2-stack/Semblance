package app.semblance.ui.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.semblance.ui.theme.AccentAmber
import app.semblance.ui.theme.AccentCyan
import app.semblance.ui.theme.AccentGreen
import app.semblance.ui.theme.AccentPurple
import app.semblance.ui.theme.AccentRed
import app.semblance.ui.theme.ConsoleBg
import app.semblance.ui.theme.ConsoleBorder
import app.semblance.ui.theme.ConsoleBorderBright
import app.semblance.ui.theme.ConsoleSurface
import app.semblance.ui.theme.ConsoleSurfaceElevated
import app.semblance.ui.theme.ConsoleSurfaceVariant
import app.semblance.ui.theme.TextMuted
import app.semblance.ui.theme.TextPrimary
import app.semblance.ui.theme.TextSecondary
import app.semblance.ui.theme.Typography

@Composable
fun WizardScreen(
    viewModel: WizardViewModel,
    onNavigateBack: () -> Unit,
    onProfileCreated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val step by viewModel.currentStep.collectAsState()
    val formState by viewModel.formState.collectAsState()

    Scaffold(
        containerColor = ConsoleBg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ConsoleSurface)
                    .border(1.dp, ConsoleBorder)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PROFILE SYNTHESIS WIZARD",
                        style = Typography.titleMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "STEP $step / 6",
                        style = Typography.labelMedium.copy(color = AccentCyan, fontWeight = FontWeight.Bold)
                    )
                }

                // Step Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    for (s in 1..6) {
                        val isCurrent = s == step
                        val isPast = s < step
                        val color = if (isCurrent) AccentCyan else if (isPast) AccentGreen else ConsoleBorder
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 3.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(color)
                                .clickable { viewModel.setStep(s) }
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Navigation bottom bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ConsoleSurface)
                    .border(1.dp, ConsoleBorder)
                    .padding(12.dp)
            ) {
                if (step > 1) {
                    OutlinedButton(
                        onClick = { viewModel.prevStep() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("PREVIOUS", style = Typography.labelMedium)
                    }
                } else {
                    Spacer(modifier = Modifier.width(80.dp))
                }

                if (step < 6) {
                    Button(
                        onClick = { viewModel.nextStep() },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = ConsoleBg),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("NEXT: STEP ${step + 1}", style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.createProfile(onSuccess = onProfileCreated)
                        },
                        enabled = formState.consistencyReport.allValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGreen,
                            contentColor = ConsoleBg,
                            disabledContainerColor = ConsoleSurfaceElevated,
                            disabledContentColor = TextMuted
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Create", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SYNTHESIZE PROFILE", style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (step) {
                1 -> Step1Identity(formState = formState, viewModel = viewModel)
                2 -> Step2Device(formState = formState, viewModel = viewModel)
                3 -> Step3Network(formState = formState, viewModel = viewModel)
                4 -> Step4Rhythm(formState = formState, viewModel = viewModel)
                5 -> Step5Interests(formState = formState, viewModel = viewModel)
                6 -> Step6Review(formState = formState, viewModel = viewModel)
            }
        }
    }
}

// ------------------- STEP 1: IDENTITY -------------------
@Composable
fun Step1Identity(formState: WizardFormState, viewModel: WizardViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SectionTitle(title = "1. PERSONA IDENTITY & PSYCHOMETRICS", subtitle = "Define the simulated human persona, schedule timezone & voice characteristics.")

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = { viewModel.generateRandomIdentity() },
            colors = ButtonDefaults.buttonColors(containerColor = ConsoleSurfaceElevated, contentColor = AccentPurple),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AccentPurple.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = "Randomize", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("GENERATE REALISTIC PERSONA", style = Typography.labelLarge)
        }

        Spacer(modifier = Modifier.height(16.dp))

        WizardTextField(
            label = "Persona Alias / Handle",
            value = formState.alias,
            onValueChange = { viewModel.updateIdentity(it, formState.age, formState.tz, formState.voice) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        WizardTextField(
            label = "Age",
            value = formState.age.toString(),
            onValueChange = {
                val parsed = it.toIntOrNull() ?: 20
                viewModel.updateIdentity(formState.alias, parsed, formState.tz, formState.voice)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        WizardTextField(
            label = "Circadian Timezone",
            value = formState.tz,
            onValueChange = { viewModel.updateIdentity(formState.alias, formState.age, it, formState.voice) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        WizardTextField(
            label = "Persona Voice / Typing Style",
            value = formState.voice,
            onValueChange = { viewModel.updateIdentity(formState.alias, formState.age, formState.tz, it) }
        )
    }
}

// ------------------- STEP 2: DEVICE & CONSISTENCY -------------------
@Composable
fun Step2Device(formState: WizardFormState, viewModel: WizardViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SectionTitle(title = "2. HARDWARE FINGERPRINT & DEVICE PRESET", subtitle = "Select a verified hardware profile. All telemetry layers must agree.")

        Spacer(modifier = Modifier.height(14.dp))

        Text(text = "DEVICE LIBRARY (CONSISTENT PRESETS):", style = Typography.labelMedium.copy(color = TextSecondary))
        Spacer(modifier = Modifier.height(6.dp))

        // Presets chips / list
        DeviceLibrary.presets.forEach { preset ->
            val isSelected = formState.selectedPresetId == preset.id
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) ConsoleSurfaceElevated else ConsoleSurface)
                    .border(
                        width = if (isSelected) 1.5.dp else 0.5.dp,
                        color = if (isSelected) AccentCyan else ConsoleBorder,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { viewModel.applyPreset(preset.id) }
                    .padding(10.dp)
            ) {
                Icon(
                    Icons.Default.PhoneAndroid,
                    contentDescription = preset.name,
                    tint = if (isSelected) AccentCyan else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.name,
                        style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                    Text(
                        text = "Android ${preset.androidVersion} | Chrome ${preset.chromeVersion} | ${preset.screenWidth}x${preset.screenHeight} (${preset.screenDensity}x DPR) | ${preset.gpu}",
                        style = Typography.bodySmall.copy(color = TextSecondary, fontSize = 9.sp)
                    )
                }
                if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = AccentCyan, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "LIVE CONSISTENCY VALIDATOR:", style = Typography.labelMedium.copy(color = TextSecondary))
        Spacer(modifier = Modifier.height(6.dp))

        formState.consistencyReport.items.forEach { item ->
            ValidationCard(item = item)
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

// ------------------- STEP 3: NETWORK -------------------
@Composable
fun Step3Network(formState: WizardFormState, viewModel: WizardViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SectionTitle(title = "3. RESIDENTIAL PROXY & NETWORK BINDING", subtitle = "Bind a dedicated sticky residential proxy port to isolate profile IP reputation.")

        Spacer(modifier = Modifier.height(14.dp))

        WizardTextField(
            label = "Proxy Host / Gateway",
            value = formState.proxyHost,
            onValueChange = { viewModel.updateProxy(it, formState.proxyPort, formState.proxyUser, formState.proxyPass, formState.proxySticky) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        WizardTextField(
            label = "Proxy Port",
            value = formState.proxyPort.toString(),
            onValueChange = {
                val port = it.toIntOrNull() ?: 8080
                viewModel.updateProxy(formState.proxyHost, port, formState.proxyUser, formState.proxyPass, formState.proxySticky)
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        WizardTextField(
            label = "Proxy Username",
            value = formState.proxyUser,
            onValueChange = { viewModel.updateProxy(formState.proxyHost, formState.proxyPort, it, formState.proxyPass, formState.proxySticky) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        WizardTextField(
            label = "Proxy Password",
            value = formState.proxyPass,
            onValueChange = { viewModel.updateProxy(formState.proxyHost, formState.proxyPort, formState.proxyUser, it, formState.proxySticky) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = { viewModel.testProxy() },
            enabled = !formState.proxyTestRunning,
            colors = ButtonDefaults.buttonColors(containerColor = ConsoleSurfaceElevated, contentColor = AccentCyan),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AccentCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
        ) {
            if (formState.proxyTestRunning) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AccentCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("TESTING UPSTREAM LATENCY...", style = Typography.labelLarge)
            } else {
                Icon(Icons.Default.NetworkCheck, contentDescription = "Test Proxy", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("TEST PROXY CONNECTIVITY & ASN", style = Typography.labelLarge)
            }
        }

        formState.proxyTestResult?.let { result ->
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(AccentGreen.copy(alpha = 0.15f))
                    .border(1.dp, AccentGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = result,
                    style = Typography.bodySmall.copy(color = AccentGreen, fontFamily = FontFamily.Monospace)
                )
            }
        }
    }
}

// ------------------- STEP 4: RHYTHM -------------------
@Composable
fun Step4Rhythm(formState: WizardFormState, viewModel: WizardViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SectionTitle(title = "4. CIRCADIAN RHYTHM & LIFECYCLE", subtitle = "Define active daily hours, Poisson session frequency, and engagement rates.")

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Active Hours Window: ${formState.activeHoursStart}:00 -> ${formState.activeHoursEnd}:00",
            style = Typography.labelMedium.copy(color = TextPrimary)
        )
        Slider(
            value = formState.activeHoursStart.toFloat(),
            onValueChange = { viewModel.updateRhythm(it.toInt(), formState.activeHoursEnd, formState.sessionsPerDay, formState.commentRate) },
            valueRange = 0f..23f,
            steps = 23,
            colors = SliderDefaults.colors(thumbColor = AccentCyan, activeTrackColor = AccentCyan)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Target Sessions Per Day: ${formState.sessionsPerDay}",
            style = Typography.labelMedium.copy(color = TextPrimary)
        )
        Slider(
            value = formState.sessionsPerDay.toFloat(),
            onValueChange = { viewModel.updateRhythm(formState.activeHoursStart, formState.activeHoursEnd, it.toInt(), formState.commentRate) },
            valueRange = 1f..10f,
            steps = 9,
            colors = SliderDefaults.colors(thumbColor = AccentAmber, activeTrackColor = AccentAmber)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Comment Probability Rate: ${(formState.commentRate * 100).toInt()}%",
            style = Typography.labelMedium.copy(color = TextPrimary)
        )
        Slider(
            value = formState.commentRate,
            onValueChange = { viewModel.updateRhythm(formState.activeHoursStart, formState.activeHoursEnd, formState.sessionsPerDay, it) },
            valueRange = 0.01f..0.30f,
            colors = SliderDefaults.colors(thumbColor = AccentPurple, activeTrackColor = AccentPurple)
        )
    }
}

// ------------------- STEP 5: INTERESTS -------------------
@Composable
fun Step5Interests(formState: WizardFormState, viewModel: WizardViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SectionTitle(title = "5. INTEREST GRAPH & CONTENT TOPICS", subtitle = "Initial seed topics that drive autonomous organic exploration & query refinement.")

        Spacer(modifier = Modifier.height(14.dp))

        val interestTopics = listOf(
            "technology" to "Technology & Hardware",
            "gaming" to "Gaming & Esports",
            "science" to "Space & Science",
            "music" to "Music & Audio",
            "travel" to "Travel & Adventure",
            "cooking" to "Culinary & Cooking"
        )

        interestTopics.forEach { (key, label) ->
            val weight = formState.interests[key] ?: 0f
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(ConsoleSurface)
                    .border(0.5.dp, ConsoleBorder, RoundedCornerShape(6.dp))
                    .padding(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = label, style = Typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.Medium))
                    Text(text = "${(weight * 100).toInt()}%", style = Typography.labelSmall.copy(color = AccentCyan))
                }
                Slider(
                    value = weight,
                    onValueChange = { newWeight ->
                        val updated = formState.interests.toMutableMap()
                        updated[key] = newWeight
                        viewModel.updateInterests(updated)
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = AccentGreen, activeTrackColor = AccentGreen)
                )
            }
        }
    }
}

// ------------------- STEP 6: REVIEW -------------------
@Composable
fun Step6Review(formState: WizardFormState, viewModel: WizardViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SectionTitle(title = "6. REVIEW & CONSISTENCY AUDIT", subtitle = "Verify all identity layers before spawning worker and persisting to Room database.")

        Spacer(modifier = Modifier.height(14.dp))

        // Summary Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(ConsoleSurface)
                .border(1.dp, ConsoleBorderBright, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            ReviewRow(label = "ALIAS", value = "@${formState.alias}")
            ReviewRow(label = "DEVICE MODEL", value = formState.deviceModel)
            ReviewRow(label = "OS / CHROMIUM", value = "Android ${formState.androidVersion} | Chrome ${formState.chromeVersion}")
            ReviewRow(label = "TIMEZONE", value = formState.tz)
            ReviewRow(label = "PROXY GATEWAY", value = "${formState.proxyHost}:${formState.proxyPort}")
            ReviewRow(label = "INITIAL PHASE", value = "WARMUP (warmth = 0)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "CONSISTENCY AUDIT REPORT:", style = Typography.labelMedium.copy(color = TextSecondary))
        Spacer(modifier = Modifier.height(6.dp))

        formState.consistencyReport.items.forEach { item ->
            ValidationCard(item = item)
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (!formState.consistencyReport.allValid) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(AccentRed.copy(alpha = 0.15f))
                    .border(1.dp, AccentRed, RoundedCornerShape(6.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "BLOCKED: Fix consistency violations in Step 2 before creating profile.",
                    style = Typography.bodySmall.copy(color = AccentRed, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun ReviewRow(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(text = label, style = Typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp))
        Text(text = value, style = Typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 11.sp))
    }
}

@Composable
fun ValidationCard(item: ValidationItem) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(ConsoleSurfaceElevated)
            .border(
                0.5.dp,
                if (item.isValid) AccentGreen.copy(alpha = 0.4f) else AccentRed.copy(alpha = 0.5f),
                RoundedCornerShape(6.dp)
            )
            .padding(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (item.isValid) AccentGreen.copy(alpha = 0.2f) else AccentRed.copy(alpha = 0.2f))
        ) {
            Icon(
                imageVector = if (item.isValid) Icons.Default.Check else Icons.Default.Close,
                contentDescription = if (item.isValid) "Pass" else "Fail",
                tint = if (item.isValid) AccentGreen else AccentRed,
                modifier = Modifier.size(12.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = Typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 11.sp)
            )
            Text(
                text = item.details,
                style = Typography.bodySmall.copy(
                    color = if (item.isValid) AccentGreen else AccentRed,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(text = title, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = AccentCyan))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = subtitle, style = Typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
    }
}

@Composable
fun WizardTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = label.uppercase(), style = Typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp))
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = Typography.bodyMedium.copy(color = TextPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ConsoleSurface,
                unfocusedContainerColor = ConsoleSurface,
                focusedBorderColor = AccentCyan,
                unfocusedBorderColor = ConsoleBorder,
                cursorColor = AccentCyan
            ),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
