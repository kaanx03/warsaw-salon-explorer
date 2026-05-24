package com.kaandev.salonexplorer.config;

import com.kaandev.salonexplorer.domain.entity.User;
import com.kaandev.salonexplorer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdminSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner seedAdmin() {
        return args -> {
            String email = "admin@salonexplorer.dev";
            if (!userRepository.existsByEmail(email)) {
                userRepository.save(User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .role("ADMIN")
                    .isEnabled(true)
                    .build());
                log.info("Admin user created — email: {}, password: admin123", email);
            }
        };
    }
}
