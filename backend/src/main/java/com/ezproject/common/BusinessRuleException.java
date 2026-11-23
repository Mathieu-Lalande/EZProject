package com.ezproject.common;

/** Violation d'une règle métier : traduite en HTTP 409 avec un message destiné à l'utilisateur. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
