package com.utangnaloob.repository;

import com.utangnaloob.entity.LoanDisbursement;
import com.utangnaloob.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface LoanDisbursementRepository extends JpaRepository<LoanDisbursement, Long> {
    List<LoanDisbursement> findByUserOrderByDisbursedAtDesc(User user);
    List<LoanDisbursement> findByUserIdOrderByDisbursedAtDesc(Long userId);
    List<LoanDisbursement> findByStatusOrderByDisbursedAtDesc(String status);

    @Query("SELECT COALESCE(SUM(l.principalAmount), 0) FROM LoanDisbursement l")
    BigDecimal sumTotalDisbursedPrincipal();

    @Query("SELECT COALESCE(SUM(l.outstandingBalance), 0) FROM LoanDisbursement l WHERE l.status = 'ACTIVE'")
    BigDecimal sumTotalOutstandingBalance();
}
