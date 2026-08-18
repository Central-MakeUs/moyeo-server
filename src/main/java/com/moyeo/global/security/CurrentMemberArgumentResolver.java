package com.moyeo.global.security;

import com.moyeo.global.error.MoyeoException;
import com.moyeo.service.member.AuthenticatedMember;
import com.moyeo.service.member.MemberAuthService;
import com.moyeo.service.member.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentMemberArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberAuthService memberAuthService;
    private final RefreshTokenService refreshTokenService;

    public CurrentMemberArgumentResolver(
            JwtTokenProvider jwtTokenProvider,
            MemberAuthService memberAuthService,
            RefreshTokenService refreshTokenService
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.memberAuthService = memberAuthService;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentMember.class)
                && AuthenticatedMember.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw authenticationRequired();
        }

        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        CurrentMember currentMember = parameter.getParameterAnnotation(CurrentMember.class);
        if (authorization == null && currentMember != null && !currentMember.required()) {
            return null;
        }
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw authenticationRequired();
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            throw authenticationRequired();
        }

        try {
            JwtClaims claims = jwtTokenProvider.parse(token);
            if (claims.sessionId() != null && !refreshTokenService.isActiveSession(claims.userId(), claims.sessionId())) {
                throw authenticationRequired();
            }
            AuthenticatedMember member = memberAuthService.findAuthenticatedMember(claims.userId());
            if (currentMember != null && currentMember.onboardingRequired() && !member.onboardingCompleted()) {
                throw new MoyeoException(AuthenticationErrorCode.ONBOARDING_REQUIRED);
            }
            return member;
        } catch (MoyeoException exception) {
            if (exception.getErrorCode() == AuthenticationErrorCode.ONBOARDING_REQUIRED) {
                throw exception;
            }
            throw authenticationRequired();
        } catch (IllegalArgumentException exception) {
            throw authenticationRequired();
        }
    }

    private MoyeoException authenticationRequired() {
        return new MoyeoException(AuthenticationErrorCode.AUTHENTICATION_REQUIRED);
    }
}
