package com.moyeo.domain.member;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SocialAccountTokenTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void appleCreationRejectsMissingOrBlankToken(String token) {
        assertThatThrownBy(() -> new SocialAccount(User.pendingOnboarding(), AuthProvider.APPLE, "sub", null, token))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void rejectedAppleUpdatesPreserveOriginalTokenAndClient(String token) {
        SocialAccount account = new SocialAccount(User.pendingOnboarding(), AuthProvider.APPLE,
                "sub", null, "encrypted-original", AppleRefreshTokenClient.NATIVE);
        assertThatThrownBy(() -> account.updateProviderRefreshTokenCiphertext(token))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> account.updateAppleRefreshToken(token, AppleRefreshTokenClient.WEB))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(account.getProviderRefreshTokenCiphertext()).isEqualTo("encrypted-original");
        assertThat(account.getAppleRefreshTokenClient()).isEqualTo(AppleRefreshTokenClient.NATIVE);
    }

    @Test
    void appleRotationPreservesIssuingClientAndLegacyWebDefault() {
        SocialAccount account = new SocialAccount(User.pendingOnboarding(), AuthProvider.APPLE,
                "sub", null, "encrypted-original");
        assertThat(account.getAppleRefreshTokenClient()).isEqualTo(AppleRefreshTokenClient.WEB);
        account.updateAppleRefreshToken("encrypted-native", AppleRefreshTokenClient.NATIVE);
        assertThat(account.getProviderRefreshTokenCiphertext()).isEqualTo("encrypted-native");
        assertThat(account.getAppleRefreshTokenClient()).isEqualTo(AppleRefreshTokenClient.NATIVE);
    }

    @Test
    void kakaoStillAllowsNullToken() {
        SocialAccount account = new SocialAccount(User.pendingOnboarding(), AuthProvider.KAKAO, "kakao-id", null);
        account.updateProviderRefreshTokenCiphertext(null);
        assertThat(account.getProviderRefreshTokenCiphertext()).isNull();
    }
}
