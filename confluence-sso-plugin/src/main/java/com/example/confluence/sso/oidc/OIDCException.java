package com.example.confluence.sso.oidc;

/** Checked exception for OIDC processing errors. */
public class OIDCException extends Exception {
    public OIDCException(String message)                   { super(message); }
    public OIDCException(String message, Throwable cause)  { super(message, cause); }
}
