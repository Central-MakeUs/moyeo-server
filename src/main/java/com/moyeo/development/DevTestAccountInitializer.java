package com.moyeo.development;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"local", "dev"})
class DevTestAccountInitializer {

    @Bean
    ApplicationRunner initializeDevTestAccounts(DevTestAccountService devTestAccountService) {
        return arguments -> {
            for (DevTestAccount account : DevTestAccount.values()) {
                devTestAccountService.getOrCreate(account);
            }
        };
    }
}
