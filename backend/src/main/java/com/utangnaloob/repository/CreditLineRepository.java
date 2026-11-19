package com.utangnaloob.repository;

import com.utangnaloob.entity.CreditLine;
import com.utangnaloob.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface CreditLineRepository extends JpaRepository<CreditLine, Long> {
    Optional<CreditLine> findByUser(User user);
    Optional<CreditLine> findByUserId(Long userId);

    @Query("SELECT COALESCE(SUM(c.approvedCreditLimit), 0) FROM CreditLine c WHERE c.status = 'ACTIVE'")
    BigDecimal sumTotalApprovedCredit();

    @Query("SELECT COALESCE(SUM(c.drawnBalance), 0) FROM CreditLine c WHERE c.status = 'ACTIVE'")
    BigDecimal sumTotalDrawnBalance();

    long countByStatus(String status);
}
