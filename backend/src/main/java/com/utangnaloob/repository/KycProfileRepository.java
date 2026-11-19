package com.utangnaloob.repository;

import com.utangnaloob.entity.KycProfile;
import com.utangnaloob.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KycProfileRepository extends JpaRepository<KycProfile, Long> {
    Optional<KycProfile> findByUser(User user);
    Optional<KycProfile> findByUserId(Long userId);
    List<KycProfile> findByKycStatusOrderBySubmittedAtDesc(String kycStatus);
    long countByKycStatus(String kycStatus);
}
