package com.moyeo.auth.kakao;

import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;

final class KakaoOAuthException {

    private KakaoOAuthException() {
    }

    static MoyeoException failed() {
        return new MoyeoException(AuthenticationErrorCode.SOCIAL_LOGIN_FAILED);
    }

    static MoyeoException unavailable() {
        return new MoyeoException(AuthenticationErrorCode.SOCIAL_LOGIN_UNAVAILABLE);
    }
}
