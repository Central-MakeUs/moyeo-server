package com.moyeo.global.error;

import org.springframework.http.HttpStatus;

import java.net.URI;

public interface ErrorCode {

    HttpStatus status();

    String code();

    URI type();

    String title();

    String detail();
}
