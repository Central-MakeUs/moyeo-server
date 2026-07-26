package com.moyeo.development;

import com.moyeo.domain.member.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DevTestAccountWithdrawalExemptionTest {

    private final DevTestAccountWithdrawalExemption exemption =
            new DevTestAccountWithdrawalExemption();

    @Test
    void exemptsOnlyRegisteredDevelopmentTestUserIds() {
        User fixedAccount = userWithId(1L);
        User sameNicknameButDifferentAccount = userWithId(2L);

        exemption.register(fixedAccount);

        assertThat(exemption.appliesTo(fixedAccount)).isTrue();
        assertThat(exemption.appliesTo(sameNicknameButDifferentAccount)).isFalse();
    }

    @Test
    void rejectsUnpersistedUserRegistration() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> exemption.register(new User("개발 사용자 1")));
    }

    private User userWithId(Long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }
}
