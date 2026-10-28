package com.example.oeil.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MockReplySourceTest {

    private final MockReplySource source = new MockReplySource();

    @Test
    public void answersGreeting() {
        assertTrue(source.answer("Hello there").startsWith("Hey there"));
    }

    @Test
    public void answersKotlinQuestion() {
        String reply = source.answer("Tell me about Kotlin");
        assertTrue(reply.contains("Kotlin"));
    }

    @Test
    public void isCaseInsensitive() {
        assertEquals(source.answer("How are you?"), source.answer("HOW ARE YOU?"));
    }

    @Test
    public void fallsBackForUnknownQuery() {
        String reply = source.answer("zzz unrelated question zzz");
        assertFalse(reply.isEmpty());
        assertTrue(reply.contains("bit more detail"));
    }
}
