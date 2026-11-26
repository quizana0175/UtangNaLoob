package com.utangnaloob.repository;

import com.utangnaloob.entity.AmortizationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AmortizationScheduleRepository extends JpaRepository<AmortizationSchedule, Long> {
    List<AmortizationSchedule> findByLoanIdOrderByInstallmentNumberAsc(Long loanId);
    List<AmortizationSchedule> findByStatusAndDueDateBefore(String status, LocalDate date);
    List<AmortizationSchedule> findByStatusInAndDueDateBefore(List<String> statuses, LocalDate date);
    List<AmortizationSchedule> findByStatusOrderByDueDateAsc(String status);
    List<AmortizationSchedule> findByLoanUserIdAndStatusInOrderByDueDateAsc(Long userId, List<String> statuses);
    List<AmortizationSchedule> findByStatusAndDueDateIn(String status, List<LocalDate> dates);
    boolean existsByLoanUserIdAndStatus(Long userId, String status);
}
