package com.campusride.util;

/**
 * Thrown when ride booking or reservation transaction fails.
 */
public class BookingException extends Exception {
    public BookingException(String message) {
        super(message);
    }

    public BookingException(String message, Throwable cause) {
        super(message, cause);
    }
}
