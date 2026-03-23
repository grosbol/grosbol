package com.example.confluence.sso.oidc;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OIDCUserInfoTest {

    @Test
    void toUsernameStripsEmailDomain() {
        OIDCUserInfo info = new OIDCUserInfo("sub123", "alice@corp.com", "Alice", List.of());
        assertEquals("alice", info.toUsername());
    }

    @Test
    void toUsernameFallsBackToSubject() {
        OIDCUserInfo info = new OIDCUserInfo("SubjectABC", null, null, List.of());
        assertEquals("subjectabc", info.toUsername());
    }

    @Test
    void toUsernameReturnsUnknownWhenNoSubjectOrEmail() {
        OIDCUserInfo info = new OIDCUserInfo(null, null, null, List.of());
        assertEquals("unknown", info.toUsername());
    }

    @Test
    void groupsAreImmutable() {
        OIDCUserInfo info = new OIDCUserInfo("s", "e@e.com", "E", List.of("g1", "g2"));
        assertThrows(UnsupportedOperationException.class, () -> info.getGroups().add("x"));
    }
}
