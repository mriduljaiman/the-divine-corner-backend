package com.divinecorner.service;

import com.divinecorner.dto.*;
import com.divinecorner.dto.UpdateProfileRequest;
import com.divinecorner.dto.response.PageResponse;
import com.divinecorner.dto.response.UserResponse;
import com.divinecorner.entity.User;
import com.divinecorner.enums.UserRole;
import com.divinecorner.exception.NotFoundException;
import com.divinecorner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        User user = findUserById(userId);
        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setCity(request.getCity());
        user.setState(request.getState());
        user.setZipCode(request.getZipCode());
        user.setCountry(request.getCountry());

        user = userRepository.save(user);
        return mapToUserResponse(user);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);
        return mapToPageResponse(page);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = findUserById(userId);
        return mapToUserResponse(user);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = findUserById(userId);
        if (user.getRole() == UserRole.ADMIN) {
            throw new RuntimeException("Cannot delete admin user");
        }
        userRepository.delete(user);
    }

    @Transactional
    public UserResponse toggleUserStatus(UUID userId) {
        User user = findUserById(userId);
        user.setActive(!user.getActive());
        user = userRepository.save(user);
        return mapToUserResponse(user);
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .city(user.getCity())
                .state(user.getState())
                .zipCode(user.getZipCode())
                .country(user.getCountry())
                .role(user.getRole().name())
                .active(user.getActive())
                .build();
    }

    private PageResponse<UserResponse> mapToPageResponse(Page<User> page) {
        return PageResponse.<UserResponse>builder()
                .content(page.getContent().stream().map(this::mapToUserResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}