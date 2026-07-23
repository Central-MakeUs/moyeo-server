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

    DevTestAccountService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    AuthenticatedMember getOrCreate(DevTestAccount account) {
        User user = userRepository
                .findFirstByNicknameAndDeletedAtIsNullOrderByIdAsc(account.nickname())
                .orElseGet(() -> userRepository.save(new User(account.nickname())));
        return AuthenticatedMember.from(user, false);
    }
}
