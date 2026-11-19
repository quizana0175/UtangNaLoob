package com.utangnaloob.repository;

import com.utangnaloob.entity.PaymentTransaction;
import com.utangnaloob.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    List<PaymentTransaction> findByUserOrderByPaidAtDesc(User user);
    List<PaymentTransaction> findByUserIdOrderByPaidAtDesc(Long userId);
    List<PaymentTransaction> findByLoanIdOrderByPaidAtDesc(Long loanId);
    List<PaymentTransaction> findTop50ByOrderByPaidAtDesc();
}
