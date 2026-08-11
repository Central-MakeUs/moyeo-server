package com.moyeo.global;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    void assignsServerOwnedTraceIdToResponseAndMdcThenCleansUp() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/apple");
        request.addHeader(TraceIdFilter.HEADER_NAME, "client-supplied-value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> traceIdInsideChain = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                traceIdInsideChain.set(MDC.get(TraceIdFilter.MDC_KEY))
        );

        String responseTraceId = response.getHeader(TraceIdFilter.HEADER_NAME);
        assertThat(responseTraceId).isNotEqualTo("client-supplied-value");
        assertThat(UUID.fromString(responseTraceId)).isEqualTo(UUID.fromString(traceIdInsideChain.get()));
        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
    }
}
