package com.argon.ph03n1x

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.argon.ph03n1x.data.ArgonRepository
import com.argon.ph03n1x.data.QuotaManager
import com.argon.ph03n1x.ui.ChatMessage
import com.argon.ph03n1x.ui.ChatUiState
import com.argon.ph03n1x.ui.ChatViewModel
import com.argon.ph03n1x.ui.ChatViewModelFactory

object ArgonColors {
    val PureBlack = Color(0xFF050505)
    val Surface = Color(0xFF111111)
    val Cyan = Color(0xFF00FFE0)
    val SlateUser = Color(0xFF1A2332)
    val TextPrimary = Color(0xFFE0E0E0)
    val TextSecondary = Color(0xFF8A8A8A)
    val Error = Color(0xFFFF4D6A)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val quotaManager = QuotaManager(applicationContext)
        val repository = ArgonRepository(quotaManager)

        setContent {
            ArgonTheme {
                val viewModel: ChatViewModel = viewModel(
                    factory = ChatViewModelFactory(repository)
                )
                ChatScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ArgonTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = ArgonColors.PureBlack,
            surface = ArgonColors.Surface,
            primary = ArgonColors.Cyan,
            onPrimary = ArgonColors.PureBlack,
            onBackground = ArgonColors.TextPrimary,
            onSurface = ArgonColors.TextPrimary
        ),
        content = content
    )
}

@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val quotaBadge by viewModel.quotaBadge.collectAsState()
    val showDialog by viewModel.showApiKeyDialog.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is ChatUiState.ApiKeyRequired) {
            viewModel.forceShowApiKeyDialog()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArgonColors.PureBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            StatusHeader(badge = quotaBadge)

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(message = msg)
                }

                if (uiState is ChatUiState.Loading) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            if (uiState is ChatUiState.Error) {
                Text(
                    text = (uiState as ChatUiState.Error).message,
                    color = ArgonColors.Error,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            InputBar(
                value = inputText,
                onValueChange = { inputText = it },
                enabled = uiState !is ChatUiState.Loading && uiState !is ChatUiState.ApiKeyRequired,
                onSend = {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                }
            )
        }

        if (showDialog) {
            ApiKeyModalDialog(
                onSubmit = { key -> viewModel.submitApiKey(key) },
                onDismiss = { viewModel.dismissApiKeyDialog() }
            )
        }
    }
}

@Composable
fun StatusHeader(badge: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ArgonColors.Surface)
            .border(1.dp, ArgonColors.Cyan.copy(alpha = 0.3f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "PH03N!X",
            color = ArgonColors.Cyan,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = badge,
            color = ArgonColors.Cyan,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isUser) ArgonColors.SlateUser else ArgonColors.Surface)
                .then(
                    if (!isUser) Modifier.border(1.dp, ArgonColors.Cyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    else Modifier
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                color = ArgonColors.TextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ArgonColors.Surface)
            .border(1.dp, ArgonColors.Cyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "ARGON 4.8 ▌",
            color = ArgonColors.Cyan,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp
        )
    }
}

@Composable
fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ArgonColors.Surface)
            .border(1.dp, ArgonColors.Cyan.copy(alpha = 0.25f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            textStyle = TextStyle(
                color = ArgonColors.TextPrimary,
                fontSize = 15.sp
            ),
            cursorBrush = SolidColor(ArgonColors.Cyan),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (enabled && value.isNotBlank()) onSend() }),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = if (enabled) "Transmit to Argon 4.8…" else "FREE TIER LOCKED",
                            color = ArgonColors.TextSecondary,
                            fontSize = 15.sp
                        )
                    }
                    inner()
                }
            }
        )

        Button(
            onClick = onSend,
            enabled = enabled && value.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = ArgonColors.Cyan,
                contentColor = ArgonColors.PureBlack,
                disabledContainerColor = ArgonColors.Cyan.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = "SEND",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun ApiKeyModalDialog(
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var keyInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ArgonColors.Surface)
                .border(1.dp, ArgonColors.Cyan, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "FREE TIER EXHAUSTED (20/20)",
                color = ArgonColors.Cyan,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ENTER ARGON 4.8 API KEY",
                color = ArgonColors.TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(20.dp))

            BasicTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ArgonColors.PureBlack)
                    .border(1.dp, ArgonColors.Cyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(14.dp),
                textStyle = TextStyle(
                    color = ArgonColors.TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                cursorBrush = SolidColor(ArgonColors.Cyan),
                decorationBox = { inner ->
                    Box {
                        if (keyInput.isEmpty()) {
                            Text(
                                text = "sk-argon-••••••••",
                                color = ArgonColors.TextSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                        }
                        inner()
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onSubmit(keyInput) },
                enabled = keyInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ArgonColors.Cyan,
                    contentColor = ArgonColors.PureBlack
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "ACTIVATE KEY",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}