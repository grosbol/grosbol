package com.example.confluence.sso.oidc;

import java.util.Collections;
import java.util.List;

/** Value object carrying user attributes extracted from an OIDC UserInfo response. */
public final class OIDCUserInfo {

    private final String       subject;
    private final String       email;
    private final String       fullName;
    private final List<String> groups;

    public OIDCUserInfo(String subject, String email, String fullName, List<String> groups) {
        this.subject  = subject;
        this.email    = email;
        this.fullName = fullName;
        this.groups   = groups != null ? List.copyOf(groups) : Collections.emptyList();
    }

    public String       getSubject()  { return subject; }
    public String       getEmail()    { return email; }
    public String       getFullName() { return fullName; }
    public List<String> getGroups()   { return groups; }

    /** Derive a Confluence username from the email or subject. */
    public String toUsername() {
        if (email != null && !email.isBlank()) {
            return email.contains("@") ? email.split("@")[0].toLowerCase() : email.toLowerCase();
        }
        return subject != null ? subject.toLowerCase() : "unknown";
    }

    @Override
    public String toString() {
        return "OIDCUserInfo{subject='" + subject + "', email='" + email + "'}";
    }
}
