package com.moyeo.development;

import com.moyeo.domain.member.User;
import com.moyeo.repository.member.UserRepository;
import com.moyeo.service.member.AuthenticatedMember;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile({"local", "dev"})
@Transactional(readOnly = true)
class DevTestAccountService {

    private final UserRepository userRepository;
    private final DevTestAccountWithdrawalExemption withdrawalExemption;

    DevTestAccountService(
            UserRepository userRepository,
            DevTestAccountWithdrawalExemption withdrawalExemption
    ) {
        this.userRepository = userRepository;
        this.withdrawalExemption = withdrawalExemption;
    }

    @Transactional
    AuthenticatedMember getOrCreate(DevTestAccount account) {
        User user = userRepository
                .findFirstByNicknameAndDeletedAtIsNullOrderByIdAsc(account.nickname())
                .orElseGet(() -> userRepository.save(new User(account.nickname())));
        withdrawalExemption.register(user);
        return AuthenticatedMember.from(user, false);
    }
}
