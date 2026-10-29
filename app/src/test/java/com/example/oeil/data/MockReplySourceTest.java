package com.example.oeil.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.oeil.model.ReplyMode;

import org.junit.Test;

public class MockReplySourceTest {

    private final MockReplySource source = new MockReplySource();

    @Test
    public void answersGreeting() {
        assertTrue(source.answer("Hello there", ReplyMode.Auto).startsWith("Hey there"));
    }

    @Test
    public void answersKotlinQuestion() {
        String reply = source.answer("Tell me about Kotlin", ReplyMode.Auto);
        assertTrue(reply.contains("Kotlin"));
    }

    @Test
    public void isCaseInsensitive() {
        assertEquals(
                source.answer("How are you?", ReplyMode.Auto),
                source.answer("HOW ARE YOU?", ReplyMode.Auto));
    }

    @Test
    public void fallsBackForUnknownQuery() {
        String reply = source.answer("zzz unrelated question zzz", ReplyMode.Auto);
        assertFalse(reply.isEmpty());
        assertTrue(reply.contains("bit more detail"));
    }

    @Test
    public void preciseModeKeepsOnlyTheFirstSentence() {
        assertEquals(
                "Kotlin is a modern language that makes Android development concise and expressive.",
                source.answer("Tell me about Kotlin", ReplyMode.Precise));
    }

    @Test
    public void creativeModeInvitesAnotherPass() {
        String auto = source.answer("zzz unrelated question zzz", ReplyMode.Auto);
        String creative = source.answer("zzz unrelated question zzz", ReplyMode.Creative);
        assertTrue(creative.startsWith(auto));
        assertTrue(creative.contains("another run"));
    }
}
