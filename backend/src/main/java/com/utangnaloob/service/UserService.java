package com.utangnaloob.service;

import com.utangnaloob.dto.RoleUpdateRequest;
import com.utangnaloob.dto.UserDto;
import com.utangnaloob.dto.UserProfileResponse;
import com.utangnaloob.entity.*;
import com.utangnaloob.repository.CreditLineRepository;
import com.utangnaloob.repository.KycProfileRepository;
import com.utangnaloob.repository.RoleRepository;
import com.utangnaloob.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private KycProfileRepository kycProfileRepository;

    @Autowired
    private CreditLineRepository creditLineRepository;

    @Autowired
    private AuditLogService auditLogService;

    public UserProfileResponse getUserProfileByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setDateOfBirth(user.getDateOfBirth());
        response.setAddress(user.getAddress());
        response.setEmploymentStatus(user.getEmploymentStatus());
        response.setMonthlyIncome(user.getMonthlyIncome());
        response.setAccountStatus(user.getAccountStatus());
        response.setCreatedAt(user.getCreatedAt());
        response.setRoles(user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toList()));

        // Attach KYC details if present
        Optional<KycProfile> kycOpt = kycProfileRepository.findByUserId(user.getId());
        if (kycOpt.isPresent()) {
            KycProfile kyc = kycOpt.get();
            response.setKycStatus(kyc.getKycStatus());
            response.setIdType(kyc.getIdType());
            response.setIdNumber(kyc.getIdNumber());
            response.setIdDocumentUrl(kyc.getIdDocumentUrl());
            response.setProofOfIncomeType(kyc.getProofOfIncomeType());
            response.setProofOfIncomeUrl(kyc.getProofOfIncomeUrl());
            response.setVerifiedMonthlyIncome(kyc.getVerifiedMonthlyIncome());
            response.setNotesOrRejectionReason(kyc.getNotesOrRejectionReason());
        }

        // Attach Credit Line details if present
        Optional<CreditLine> creditOpt = creditLineRepository.findByUserId(user.getId());
        if (creditOpt.isPresent()) {
            CreditLine credit = creditOpt.get();
            response.setApprovedCreditLimit(credit.getApprovedCreditLimit());
            response.setAvailableBalance(credit.getAvailableBalance());
            response.setDrawnBalance(credit.getDrawnBalance());
            response.setCreditLineStatus(credit.getStatus());
        }

        return response;
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserDto(
                        u.getId(),
                        u.getFirstName(),
                        u.getLastName(),
                        u.getEmail(),
                        u.getPhoneNumber(),
                        u.getEmploymentStatus(),
                        u.getMonthlyIncome(),
                        u.getAccountStatus(),
                        u.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toList()),
                        u.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDto updateUserRoles(Long userId, RoleUpdateRequest request, String adminEmail, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Set<Role> roles = new HashSet<>();
        for (String roleStr : request.getRoles()) {
            String formattedRole = roleStr.startsWith("ROLE_") ? roleStr : "ROLE_" + roleStr.toUpperCase();
            try {
                ERole eRole = ERole.valueOf(formattedRole);
                Role role = roleRepository.findByName(eRole)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + formattedRole));
                roles.add(role);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid role specified: " + roleStr);
            }
        }

        user.setRoles(roles);
        User updated = userRepository.save(user);

        auditLogService.log(
                "ROLE_UPDATED",
                adminEmail,
                user.getEmail(),
                "Roles updated to: " + roles.stream().map(r -> r.getName().name()).collect(Collectors.joining(", ")),
                ipAddress
        );

        return new UserDto(
                updated.getId(),
                updated.getFirstName(),
                updated.getLastName(),
                updated.getEmail(),
                updated.getPhoneNumber(),
                updated.getEmploymentStatus(),
                updated.getMonthlyIncome(),
                updated.getAccountStatus(),
                updated.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toList()),
                updated.getCreatedAt()
        );
    }

    @Transactional
    public UserDto toggleUserStatus(Long userId, String adminEmail, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        String newStatus = "ACTIVE".equalsIgnoreCase(user.getAccountStatus()) ? "LOCKED" : "ACTIVE";
        user.setAccountStatus(newStatus);
        User updated = userRepository.save(user);

        auditLogService.log(
                "USER_STATUS_TOGGLED",
                adminEmail,
                user.getEmail(),
                "Account status changed to: " + newStatus,
                ipAddress
        );

        return new UserDto(
                updated.getId(),
                updated.getFirstName(),
                updated.getLastName(),
                updated.getEmail(),
                updated.getPhoneNumber(),
                updated.getEmploymentStatus(),
                updated.getMonthlyIncome(),
                updated.getAccountStatus(),
                updated.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toList()),
                updated.getCreatedAt()
        );
    }
}
