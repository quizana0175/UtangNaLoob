package com.utangnaloob.service;

import com.utangnaloob.config.JwtUtils;
import com.utangnaloob.config.UserDetailsImpl;
import com.utangnaloob.dto.AuthResponse;
import com.utangnaloob.dto.LoginRequest;
import com.utangnaloob.dto.MessageResponse;
import com.utangnaloob.dto.RegisterRequest;
import com.utangnaloob.entity.*;
import com.utangnaloob.repository.CreditLineRepository;
import com.utangnaloob.repository.KycProfileRepository;
import com.utangnaloob.repository.RoleRepository;
import com.utangnaloob.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private KycProfileRepository kycProfileRepository;

    @Autowired
    private CreditLineRepository creditLineRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private AuditLogService auditLogService;

    @Transactional
    public ResponseEntity<?> registerUser(RegisterRequest registerRequest, String ipAddress) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: An account with email '" + registerRequest.getEmail() + "' already exists! Please log in or use a different email.", false));
        }

        // Create new user entity with securely hashed password
        User user = new User(
                registerRequest.getFirstName().trim(),
                registerRequest.getLastName().trim(),
                registerRequest.getEmail().trim().toLowerCase(),
                encoder.encode(registerRequest.getPassword()),
                registerRequest.getPhoneNumber(),
                registerRequest.getDateOfBirth(),
                registerRequest.getAddress(),
                registerRequest.getEmploymentStatus(),
                registerRequest.getMonthlyIncome()
        );

        // All public registrations strictly create standard consumer accounts (ROLE_BORROWER)
        Role borrowerRole = roleRepository.findByName(ERole.ROLE_BORROWER)
                .orElseThrow(() -> new RuntimeException("Error: Default ROLE_BORROWER is not initialized in the database."));
        Set<Role> roles = new HashSet<>();
        roles.add(borrowerRole);

        user.setRoles(roles);
        User savedUser = userRepository.save(user);

        // Initialize empty KYC Profile and Credit Line for borrower
        boolean isBorrower = roles.stream().anyMatch(r -> r.getName() == ERole.ROLE_BORROWER);
        if (isBorrower) {
            KycProfile kycProfile = new KycProfile(savedUser);
            kycProfileRepository.save(kycProfile);

            CreditLine creditLine = new CreditLine(savedUser, BigDecimal.ZERO);
            creditLineRepository.save(creditLine);
        }

        auditLogService.log(
                "USER_REGISTERED",
                savedUser.getEmail(),
                savedUser.getEmail(),
                "New user registered with roles: " + roles.stream().map(r -> r.getName().name()).collect(Collectors.joining(", ")),
                ipAddress
        );

        return ResponseEntity.ok(new MessageResponse("User account created successfully! You can now log in.", true));
    }

    public ResponseEntity<?> authenticateUser(LoginRequest loginRequest, String ipAddress) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail().trim().toLowerCase(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        auditLogService.log(
                "USER_LOGIN",
                userDetails.getUsername(),
                userDetails.getUsername(),
                "Successful login authentication",
                ipAddress
        );

        return ResponseEntity.ok(new AuthResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getFirstName(),
                userDetails.getLastName(),
                userDetails.getAccountStatus(),
                roles
        ));
    }

    public ResponseEntity<?> logoutUser(String email, String ipAddress) {
        SecurityContextHolder.clearContext();
        auditLogService.log(
                "USER_LOGOUT",
                email != null ? email : "Anonymous",
                email,
                "User logged out successfully and session terminated",
                ipAddress
        );
        return ResponseEntity.ok(new MessageResponse("User session terminated successfully.", true));
    }
}
