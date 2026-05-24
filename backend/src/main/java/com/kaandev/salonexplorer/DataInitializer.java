package com.kaandev.salonexplorer;

import com.kaandev.salonexplorer.domain.entity.User;
import com.kaandev.salonexplorer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail("admin@salonexplorer.local")) return;

        var admin = User.builder()
            .email("admin@salonexplorer.local")
            .passwordHash(passwordEncoder.encode("Admin123!"))
            .role("ADMIN")
            .isEnabled(true)
            .build();
        userRepository.save(admin);
        log.info("Default admin user created: admin@salonexplorer.local / Admin123!");
    }
}
