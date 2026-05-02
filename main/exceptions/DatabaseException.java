package main.exceptions;

public class DatabaseException extends RuntimeException {
    public DatabaseException(String msg, Throwable cause) {
        super(msg, cause);
    }
}