package com.fitcoach.app.backend.service;

import com.fitcoach.app.backend.dto.AuthResponse;
import com.fitcoach.app.backend.dto.LoginRequest;
import com.fitcoach.app.backend.dto.RegisterRequest;
import com.fitcoach.app.backend.entity.Role;
import com.fitcoach.app.backend.entity.User;
import com.fitcoach.app.backend.entity.UserProfile;
import com.fitcoach.app.backend.repository.RoleRepository;
import com.fitcoach.app.backend.repository.UserRepository;
import com.fitcoach.app.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        Role userRole = roleRepository.findByName(Role.RoleName.USER)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(Role.RoleName.USER);
                    return roleRepository.save(role);
                });

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of(userRole));

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setMainGoal("General fitness");
        user.setProfile(profile);

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser.getEmail());

        return AuthResponse.builder()
                .token(token)
                .email(savedUser.getEmail())
                .role(userRole.getName().name())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String token = jwtService.generateToken(user.getEmail());

        String roleName = user.getRoles().stream().findFirst().map(r -> r.getName().name()).orElse("USER");
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(roleName)
                .build();
    }
}
