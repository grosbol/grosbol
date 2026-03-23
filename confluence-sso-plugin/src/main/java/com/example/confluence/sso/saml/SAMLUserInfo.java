package com.example.confluence.sso.saml;

import java.util.Collections;
import java.util.List;

/** Value object carrying user attributes extracted from a SAML assertion. */
public final class SAMLUserInfo {

    private final String       nameId;
    private final String       email;
    private final String       fullName;
    private final List<String> groups;
    private final String       sessionIndex;

    public SAMLUserInfo(String nameId, String email, String fullName,
                        List<String> groups, String sessionIndex) {
        this.nameId       = nameId;
        this.email        = email;
        this.fullName     = fullName;
        this.groups       = groups != null ? List.copyOf(groups) : Collections.emptyList();
        this.sessionIndex = sessionIndex;
    }

    public String       getNameId()       { return nameId; }
    public String       getEmail()        { return email; }
    public String       getFullName()     { return fullName; }
    public List<String> getGroups()       { return groups; }
    public String       getSessionIndex() { return sessionIndex; }

    /** Derive a Confluence username from the NameID (strip domain part). */
    public String toUsername() {
        if (email != null && !email.isBlank()) {
            return email.contains("@") ? email.split("@")[0].toLowerCase() : email.toLowerCase();
        }
        return nameId.toLowerCase();
    }

    @Override
    public String toString() {
        return "SAMLUserInfo{nameId='" + nameId + "', email='" + email + "'}";
    }
}
