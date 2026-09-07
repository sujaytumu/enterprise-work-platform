package com.enterprise.core.exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String cardToken) {
        super("No account found for card token: " + cardToken);
    }
}
