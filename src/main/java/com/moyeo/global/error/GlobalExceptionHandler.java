package com.moyeo.global.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String REQUEST_FIELD = "request";

    @ExceptionHandler(MoyeoException.class)
    public ResponseEntity<Object> handleMoyeoException(MoyeoException exception, WebRequest request) {
        return handleProblem(exception, exception.getErrorCode(), HttpHeaders.EMPTY, request, List.of());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolationException(
            ConstraintViolationException exception,
            WebRequest request
    ) {
        List<FieldViolation> violations = exception.getConstraintViolations().stream()
                .map(this::toFieldViolation)
                .sorted(violationComparator())
                .toList();
        return handleProblem(exception, CommonErrorCode.VALIDATION_FAILED, HttpHeaders.EMPTY, request, violations);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpectedException(Exception exception, WebRequest request) {
        log.error("Unhandled exception for {}", request.getDescription(false), exception);
        return handleProblem(
                exception,
                CommonErrorCode.INTERNAL_SERVER_ERROR,
                HttpHeaders.EMPTY,
                request,
                List.of()
        );
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        List<FieldViolation> violations = Stream.concat(
                        exception.getBindingResult().getFieldErrors().stream().map(this::toFieldViolation),
                        exception.getBindingResult().getGlobalErrors().stream().map(this::toFieldViolation)
                )
                .sorted(violationComparator())
                .toList();
        return handleProblem(exception, CommonErrorCode.VALIDATION_FAILED, headers, request, violations);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        if (exception.isForReturnValue()) {
            return handleInternalServerError(exception, headers, request);
        }

        List<FieldViolation> violations = exception.getParameterValidationResults().stream()
                .flatMap(this::toFieldViolations)
                .sorted(violationComparator())
                .toList();
        return handleProblem(exception, CommonErrorCode.VALIDATION_FAILED, headers, request, violations);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return handleProblem(exception, CommonErrorCode.MALFORMED_REQUEST, headers, request, List.of());
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return handleProblem(exception, CommonErrorCode.MISSING_REQUIRED_VALUE, headers, request, List.of());
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestPart(
            MissingServletRequestPartException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return handleProblem(exception, CommonErrorCode.MISSING_REQUIRED_VALUE, headers, request, List.of());
    }

    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(
            ServletRequestBindingException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        if (exception instanceof MissingRequestHeaderException
                || exception instanceof MissingRequestCookieException) {
            return handleProblem(exception, CommonErrorCode.MISSING_REQUIRED_VALUE, headers, request, List.of());
        }
        return handleProblem(exception, CommonErrorCode.REQUEST_BINDING_FAILED, headers, request, List.of());
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return handleProblem(exception, CommonErrorCode.TYPE_MISMATCH, headers, request, List.of());
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return handleProblem(exception, CommonErrorCode.METHOD_NOT_ALLOWED, headers, request, List.of());
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return handleProblem(exception, CommonErrorCode.UNSUPPORTED_MEDIA_TYPE, headers, request, List.of());
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(
            HttpMediaTypeNotAcceptableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return handleProblem(exception, CommonErrorCode.NOT_ACCEPTABLE, headers, request, List.of());
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(
            NoHandlerFoundException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return handleProblem(exception, CommonErrorCode.ENDPOINT_NOT_FOUND, headers, request, List.of());
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return handleProblem(exception, CommonErrorCode.ENDPOINT_NOT_FOUND, headers, request, List.of());
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return handleProblem(exception, CommonErrorCode.PAYLOAD_TOO_LARGE, headers, request, List.of());
    }

    @Override
    protected ResponseEntity<Object> handleAsyncRequestTimeoutException(
            AsyncRequestTimeoutException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return handleProblem(exception, CommonErrorCode.REQUEST_TIMEOUT, headers, request, List.of());
    }

    @Override
    protected ResponseEntity<Object> handleErrorResponseException(
            ErrorResponseException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        if (status.is5xxServerError()) {
            log.error("Framework error response for {}", request.getDescription(false), exception);
        }
        return handleProblem(exception, mapStatus(status), headers, request, List.of());
    }

    @Override
    protected ResponseEntity<Object> handleMissingPathVariable(
            MissingPathVariableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return handleInternalServerError(exception, headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleConversionNotSupported(
            ConversionNotSupportedException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return handleInternalServerError(exception, headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotWritable(
            HttpMessageNotWritableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return handleInternalServerError(exception, headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodValidationException(
            MethodValidationException exception,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request
    ) {
        return handleInternalServerError(exception, headers, request);
    }

    private ResponseEntity<Object> handleInternalServerError(
            Exception exception,
            HttpHeaders headers,
            WebRequest request
    ) {
        log.error("Server-side request handling failure for {}", request.getDescription(false), exception);
        return handleProblem(exception, CommonErrorCode.INTERNAL_SERVER_ERROR, headers, request, List.of());
    }

    private ResponseEntity<Object> handleProblem(
            Exception exception,
            ErrorCode errorCode,
            HttpHeaders headers,
            WebRequest request,
            List<FieldViolation> violations
    ) {
        ProblemDetail problemDetail = ProblemDetailFactory.create(errorCode, request, violations);
        return handleExceptionInternal(exception, problemDetail, headers, errorCode.status(), request);
    }

    private CommonErrorCode mapStatus(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> CommonErrorCode.ENDPOINT_NOT_FOUND;
            case 405 -> CommonErrorCode.METHOD_NOT_ALLOWED;
            case 406 -> CommonErrorCode.NOT_ACCEPTABLE;
            case 413 -> CommonErrorCode.PAYLOAD_TOO_LARGE;
            case 415 -> CommonErrorCode.UNSUPPORTED_MEDIA_TYPE;
            default -> status.is4xxClientError()
                    ? CommonErrorCode.INVALID_REQUEST
                    : CommonErrorCode.INTERNAL_SERVER_ERROR;
        };
    }

    private FieldViolation toFieldViolation(FieldError error) {
        return new FieldViolation(error.getField(), resolveReason(error));
    }

    private FieldViolation toFieldViolation(ObjectError error) {
        return new FieldViolation(error.getObjectName(), resolveReason(error));
    }

    private FieldViolation toFieldViolation(ConstraintViolation<?> violation) {
        String field = StreamSupport.stream(violation.getPropertyPath().spliterator(), false)
                .reduce((first, second) -> second)
                .map(node -> node.getName())
                .filter(Objects::nonNull)
                .orElse(REQUEST_FIELD);
        return new FieldViolation(field, safeReason(violation.getMessage()));
    }

    private Stream<FieldViolation> toFieldViolations(ParameterValidationResult result) {
        String parameterName = result.getMethodParameter().getParameterName();
        String field = parameterName != null ? parameterName : REQUEST_FIELD;
        return result.getResolvableErrors().stream()
                .map(error -> new FieldViolation(field, resolveReason(error)));
    }

    private String resolveReason(MessageSourceResolvable error) {
        return safeReason(error.getDefaultMessage());
    }

    private String safeReason(String reason) {
        return reason != null && !reason.isBlank() ? reason : "요청 값이 올바르지 않습니다.";
    }

    private Comparator<FieldViolation> violationComparator() {
        return Comparator.comparing(FieldViolation::field).thenComparing(FieldViolation::reason);
    }
}
