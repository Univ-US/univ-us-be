package com.univus.app.member.exception;

public class InvalidLoginException extends RuntimeException{

    public InvalidLoginException(String message) {
        super(message);
    }

}
