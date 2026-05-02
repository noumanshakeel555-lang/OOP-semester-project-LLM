package main.exceptions;

public class AuthenticationException extends Exception {
    public AuthenticationException(String msg) {
        super(msg);
    }
}