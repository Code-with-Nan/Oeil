package com.example.oeil.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.oeil.components.ChatHeader
import com.example.oeil.components.ChatInput
import com.example.oeil.components.EmptyState
import com.example.oeil.components.MessageItem
import com.example.oeil.components.TypingIndicator
import com.example.oeil.state.OeilChatState
import com.example.oeil.ui.theme.BackgroundDark
import com.example.oeil.ui.theme.BackgroundDarkAlt
import com.example.oeil.ui.theme.GreenPrimary
import com.example.oeil.ui.theme.TealAccent

/**
 * Root screen of the Oeil app: brand header, message history and composer.
 */
@Composable
fun OeilScreen(modifier: Modifier = Modifier) {
    val chat = remember { OeilChatState() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    AutoScrollToLatest(chat = chat, listState = listState)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BackgroundDarkAlt, BackgroundDark)))
    ) {
        AmbientGlow()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                ChatHeader(
                    onNewChat = chat::startNewChat,
                    onProfile = {}
                )
            },
            bottomBar = {
                ChatInput(
                    value = chat.input,
                    onValueChange = chat::updateInput,
                    onSend = { chat.send(scope) },
                    enabled = !chat.isTyping
                )
            }
        ) { padding ->
            Conversation(
                chat = chat,
                listState = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
    }
}

@Composable
private fun AutoScrollToLatest(chat: OeilChatState, listState: LazyListState) {
    LaunchedEffect(chat.messages.size, chat.isTyping) {
        val latestIndex = if (chat.isTyping) chat.messages.size else chat.messages.size - 1
        if (latestIndex >= 0) {
            listState.animateScrollToItem(latestIndex)
        }
    }
}

@Composable
private fun Conversation(
    chat: OeilChatState,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(chat.messages, key = { it.id }) { message ->
                MessageItem(message = message)
            }

            if (chat.isTyping) {
                item { TypingIndicator() }
            }
        }

        if (chat.messages.isEmpty() && !chat.isTyping) {
            EmptyState(
                modifier = Modifier.align(Alignment.Center),
                onPick = chat::applySuggestion
            )
        }
    }
}

@Composable
private fun BoxScope.AmbientGlow() {
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 150.dp, y = (-140).dp)
            .size(380.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        GreenPrimary.copy(alpha = 0.22f),
                        TealAccent.copy(alpha = 0.10f),
                        Color.Transparent
                    )
                ),
                CircleShape
            )
    )
}
