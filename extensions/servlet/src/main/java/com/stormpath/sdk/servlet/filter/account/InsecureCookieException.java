package com.stormpath.sdk.servlet.filter.account;

/**
 * @since 1.1.0
 */
public class InsecureCookieException extends RuntimeException {
    public InsecureCookieException(String s) {
        super(s);
    }

    @Override
    public String getLocalizedMessage() {
        return "stormpath.cookie.insecure.error";
    }
}
