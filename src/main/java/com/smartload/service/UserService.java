package com.smartload.service;

import com.smartload.dto.UserResponse;
import com.smartload.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.StreamSupport;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponse> allUsers() {
        return StreamSupport.stream(userRepository.findAll().spliterator(), false)
            .map(UserResponse::from)
            .toList();
    }
}
