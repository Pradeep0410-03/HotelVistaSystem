package com.hotelvista.movie;
import org.springframework.http.HttpStatus;
public class MovieFailure extends RuntimeException {
    private final HttpStatus status;
    public MovieFailure(HttpStatus status,String message){super(message);this.status=status;}
    public HttpStatus status(){return status;}
}
