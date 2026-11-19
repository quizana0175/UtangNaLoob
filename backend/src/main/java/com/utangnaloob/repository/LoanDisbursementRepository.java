package com.utangnaloob.repository;

import com.utangnaloob.entity.LoanDisbursement;
import com.utangnaloob.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanDisbursementRepository extends JpaRepository<LoanDisbursement, Long> {
    List<LoanDisbursement> findByUserOrderByDisbursedAtDesc(User user);
    List<LoanDisbursement> findByUserIdOrderByDisbursedAtDesc(Long userId);
    List<LoanDisbursement> findByStatusOrderByDisbursedAtDesc(String status);
}
