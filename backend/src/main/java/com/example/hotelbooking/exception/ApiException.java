package com.example.hotelbooking.exception;

import org.springframework.http.HttpStatus;

/**
 * Business-rule failure that maps to a well-defined HTTP status and a stable
 * machine-readable error code (the frontend can switch on {@code code}).
 */
public class ApiException extends RuntimeException {

    public static final String INVALID_DATES = "INVALID_DATES";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String ROOM_UNAVAILABLE = "ROOM_UNAVAILABLE";
    public static final String ALREADY_CANCELLED = "ALREADY_CANCELLED";
    public static final String CANCELLATION_NOT_ALLOWED = "CANCELLATION_NOT_ALLOWED";

    private final HttpStatus status;
    private final String code;

    private ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static ApiException invalidDates(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, INVALID_DATES, message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, NOT_FOUND, message);
    }

    public static ApiException roomUnavailable(String message) {
        return new ApiException(HttpStatus.CONFLICT, ROOM_UNAVAILABLE, message);
    }

    public static ApiException alreadyCancelled(String message) {
        return new ApiException(HttpStatus.CONFLICT, ALREADY_CANCELLED, message);
    }

    public static ApiException cancellationNotAllowed(String message) {
        return new ApiException(HttpStatus.CONFLICT, CANCELLATION_NOT_ALLOWED, message);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
