package com.mdau.momentspackagingbackendjavafirstclient.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private Instant timestamp;
    private int status;
    private String error;
    private String code;
    private String message;
    private Map<String, String> fields;
    private String traceId;

    public static ApiError of(int status, String error, String code, String message, String traceId) {
        return ApiError.builder()
                .timestamp(Instant.now())
                .status(status)
                .error(error)
                .code(code)
                .message(message)
                .traceId(traceId)
                .build();
    }

    public static ApiError of(int status, String error, String code, String message,
                              Map<String, String> fields, String traceId) {
        return ApiError.builder()
                .timestamp(Instant.now())
                .status(status)
                .error(error)
                .code(code)
                .message(message)
                .fields(fields)
                .traceId(traceId)
                .build();
    }
}