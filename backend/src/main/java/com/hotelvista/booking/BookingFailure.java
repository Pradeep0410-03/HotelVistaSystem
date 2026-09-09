package com.hotelvista.booking;
import org.springframework.http.HttpStatus;
public class BookingFailure extends RuntimeException {
    private final HttpStatus status;
    public BookingFailure(HttpStatus status,String message) { super(message);this.status=status; }
    public HttpStatus status() { return status; }
}
