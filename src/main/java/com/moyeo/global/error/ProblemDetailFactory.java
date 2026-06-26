package com.moyeo.global.error;

import org.springframework.http.ProblemDetail;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.util.List;

public final class ProblemDetailFactory {

    private ProblemDetailFactory() {
    }

    public static ProblemDetail create(ErrorCode errorCode, WebRequest request) {
        return create(errorCode, request, List.of());
    }

    public static ProblemDetail create(ErrorCode errorCode, WebRequest request, List<FieldViolation> violations) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(errorCode.status(), errorCode.detail());
        problemDetail.setType(errorCode.type());
        problemDetail.setTitle(errorCode.title());
        problemDetail.setInstance(resolveInstance(request));
        problemDetail.setProperty("code", errorCode.code());

        if (!violations.isEmpty()) {
            problemDetail.setProperty("errors", List.copyOf(violations));
        }

        return problemDetail;
    }

    private static URI resolveInstance(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return URI.create(servletWebRequest.getRequest().getRequestURI());
        }
        return null;
    }
}
