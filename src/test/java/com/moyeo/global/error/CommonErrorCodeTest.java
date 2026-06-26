package com.moyeo.global.error;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommonErrorCodeTest {

    @Test
    void commonErrorCodesHaveUniqueCodesAndTypes() {
        assertThat(Arrays.stream(CommonErrorCode.values()).map(CommonErrorCode::code))
                .doesNotHaveDuplicates();
        assertThat(Arrays.stream(CommonErrorCode.values()).map(CommonErrorCode::type))
                .doesNotHaveDuplicates();
    }

    @Test
    void moyeoExceptionRequiresAnErrorCode() {
        assertThatThrownBy(() -> new MoyeoException(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("errorCode must not be null");
    }
}
