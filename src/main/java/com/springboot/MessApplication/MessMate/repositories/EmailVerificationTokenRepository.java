package com.springboot.MessApplication.MessMate.repositories;

import com.springboot.MessApplication.MessMate.entities.EmailVerificationToken;
import com.springboot.MessApplication.MessMate.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    void deleteByUser(User user);

    Optional<EmailVerificationToken> findByUser(User user);

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
}
