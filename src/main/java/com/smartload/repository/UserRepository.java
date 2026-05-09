package com.smartload.repository;

import com.smartload.entity.User;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserRepository extends CrudRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByVerificationCode(String verificationCode);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
