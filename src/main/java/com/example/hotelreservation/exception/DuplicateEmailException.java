package com.example.hotelreservation.exception;

public class DuplicateEmailException extends BusinessValidationException {
    public DuplicateEmailException(String message) {
        super(message);
    }
}