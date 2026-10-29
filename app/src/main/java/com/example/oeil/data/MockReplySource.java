package com.example.oeil.data;

import androidx.annotation.NonNull;

import com.example.oeil.model.ReplyMode;

import java.util.Locale;

/**
 * Offline {@link ReplySource} used until a real backend is wired in.
 *
 * <p>Deliberately free of Android runtime APIs so it can be unit tested on the
 * JVM with plain JUnit.</p>
 */
public final class MockReplySource implements ReplySource {

    private static final String GREETING_REPLY = "Hey there! How can I help you today?";
    private static final String STATUS_REPLY =
            "I'm running smoothly and ready to help. What's on your mind?";
    private static final String NAME_REPLY =
            "I'm Oeil — your assistant for understanding images, writing code, and exploring ideas.";
    private static final String KOTLIN_REPLY =
            "Kotlin is a modern language that makes Android development concise and expressive. "
                    + "It pairs beautifully with Jetpack Compose.";
    private static final String COMPOSE_REPLY =
            "Jetpack Compose is Android's declarative UI toolkit. "
                    + "You describe the UI based on state, and Compose handles the rest.";
    private static final String IMAGE_REPLY =
            "Upload or describe an image and I'll help you analyze its contents, text, and context.";
    private static final String CODE_REPLY =
            "I can help you design, write, and debug code. "
                    + "Tell me what you're building and I'll get started.";
    private static final String THANKS_REPLY = "You're very welcome! Anything else I can help with?";
    private static final String BYE_REPLY = "See you later! Come back anytime you need a hand.";
    private static final String FALLBACK_REPLY =
            "That's a great question. Could you share a bit more detail so I can give you a focused answer?";

    private static final String CREATIVE_TAIL =
            " Push me with a constraint or a weirder angle and I'll take another run at it.";

    @NonNull
    @Override
    public String answer(@NonNull String query, @NonNull ReplyMode mode) {
        return styled(matcher(query.toLowerCase(Locale.ROOT)), mode);
    }

    private static String matcher(String normalized) {
        if (containsAny(normalized, "hello", "hi")) {
            return GREETING_REPLY;
        }
        if (normalized.contains("how are you")) {
            return STATUS_REPLY;
        }
        if (normalized.contains("name")) {
            return NAME_REPLY;
        }
        if (normalized.contains("kotlin")) {
            return KOTLIN_REPLY;
        }
        if (normalized.contains("compose")) {
            return COMPOSE_REPLY;
        }
        if (normalized.contains("image")) {
            return IMAGE_REPLY;
        }
        if (normalized.contains("code")) {
            return CODE_REPLY;
        }
        if (normalized.contains("thank")) {
            return THANKS_REPLY;
        }
        if (normalized.contains("bye")) {
            return BYE_REPLY;
        }
        return FALLBACK_REPLY;
    }

    /**
     * Precise keeps only the leading sentence, Creative invites another pass,
     * Auto answers as written.
     */
    private static String styled(String reply, ReplyMode mode) {
        if (mode == ReplyMode.Precise) {
            return firstSentence(reply);
        }
        if (mode == ReplyMode.Creative) {
            return reply + CREATIVE_TAIL;
        }
        return reply;
    }

    private static String firstSentence(String value) {
        int cut = -1;
        String[] marks = {". ", "? ", "! "};
        for (String mark : marks) {
            int index = value.indexOf(mark);
            if (index >= 0 && (cut < 0 || index < cut)) {
                cut = index;
            }
        }
        return cut < 0 ? value : value.substring(0, cut + 1);
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
