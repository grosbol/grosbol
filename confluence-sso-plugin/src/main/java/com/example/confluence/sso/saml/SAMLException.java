package com.example.confluence.sso.saml;

/** Checked exception for SAML processing errors. */
public class SAMLException extends Exception {
    public SAMLException(String message)            { super(message); }
    public SAMLException(String message, Throwable cause) { super(message, cause); }
}
