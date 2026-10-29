package com.example.oeil.data;

import androidx.annotation.NonNull;

import com.example.oeil.model.ReplyMode;

/**
 * Seam between the chat UI and whatever produces assistant answers.
 *
 * <p>Kept in Java on purpose: a real implementation will most likely wrap a
 * Java/Kotlin SDK or HTTP client, and an interface with explicit nullability
 * annotations is the friendliest contract for both sides of the interop.</p>
 */
public interface ReplySource {

    /**
     * @param query raw user text, never {@code null}
     * @param mode  answer style picked in the header, never {@code null}
     * @return the assistant reply, never {@code null}
     */
    @NonNull
    String answer(@NonNull String query, @NonNull ReplyMode mode);
}
