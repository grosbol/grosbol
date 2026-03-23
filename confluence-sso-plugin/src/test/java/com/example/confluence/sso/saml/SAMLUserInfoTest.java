package com.example.confluence.sso.saml;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SAMLUserInfoTest {

    @Test
    void toUsernameStripsEmailDomain() {
        SAMLUserInfo info = new SAMLUserInfo("nameId", "john.doe@example.com", "John Doe",
            List.of("devs"), null);
        assertEquals("john.doe", info.toUsername());
    }

    @Test
    void toUsernameUsesNameIdWhenNoEmail() {
        SAMLUserInfo info = new SAMLUserInfo("U12345", null, null, List.of(), null);
        assertEquals("u12345", info.toUsername());
    }

    @Test
    void groupsAreImmutable() {
        SAMLUserInfo info = new SAMLUserInfo("n", "e@e.com", "E", List.of("g1"), null);
        assertThrows(UnsupportedOperationException.class, () -> info.getGroups().add("x"));
    }

    @Test
    void nullGroupsBecomesEmptyList() {
        SAMLUserInfo info = new SAMLUserInfo("n", "e@e.com", "E", null, null);
        assertNotNull(info.getGroups());
        assertTrue(info.getGroups().isEmpty());
    }
}
