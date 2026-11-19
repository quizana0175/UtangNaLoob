package com.utangnaloob.config;

import com.utangnaloob.entity.*;
import com.utangnaloob.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KycProfileRepository kycProfileRepository;

    @Autowired
    private CreditLineRepository creditLineRepository;

    @Autowired
    private SystemParameterRepository systemParameterRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Initializing Utang na Loob seed data and role-based records...");

        // 1. Initialize Roles
        Role borrowerRole = roleRepository.findByName(ERole.ROLE_BORROWER).orElseGet(() ->
                roleRepository.save(new Role(ERole.ROLE_BORROWER)));

        Role underwriterRole = roleRepository.findByName(ERole.ROLE_UNDERWRITER).orElseGet(() ->
                roleRepository.save(new Role(ERole.ROLE_UNDERWRITER)));

        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN).orElseGet(() ->
                roleRepository.save(new Role(ERole.ROLE_ADMIN)));

        // 2. Initialize System Financial Parameters
        initSystemParameter("ANNUAL_INTEREST_RATE", "0.24", "Platform annual nominal interest rate (24% p.a.)", "SYSTEM");
        initSystemParameter("MONTHLY_INTEREST_RATE", "0.02", "Platform standard monthly amortized interest rate (2.0% / mo)", "SYSTEM");
        initSystemParameter("GRACE_PERIOD_DAYS", "3", "Standard grace period before overdue delinquency penalty kicks in", "SYSTEM");
        initSystemParameter("DAILY_LATE_PENALTY_RATE", "0.005", "Daily overdue delinquency penalty surcharge (0.5% daily)", "SYSTEM");
        initSystemParameter("MIN_LOAN_AMOUNT", "1000.00", "Minimum installment loan drawdown disbursement amount (PHP)", "SYSTEM");
        initSystemParameter("MAX_INITIAL_CREDIT_LIMIT", "50000.00", "Maximum algorithmic revolving credit limit ceiling for first-time applicants (PHP)", "SYSTEM");

        // 3. Seed System Administrator
        if (!userRepository.existsByEmail("admin@utangnaloob.ph")) {
            User admin = new User(
                    "System",
                    "Administrator",
                    "admin@utangnaloob.ph",
                    passwordEncoder.encode("Admin@123"),
                    "09170000001",
                    "1990-01-15",
                    "Fintech Tower, BGC, Taguig City, Metro Manila",
                    "EMPLOYED",
                    120000.0
            );
            Set<Role> roles = new HashSet<>();
            roles.add(adminRole);
            admin.setRoles(roles);
            userRepository.save(admin);
            logger.info("Created default System Administrator: admin@utangnaloob.ph / Admin@123");

            auditLogRepository.save(new AuditLog(
                    "SYSTEM_INIT",
                    "SYSTEM",
                    "admin@utangnaloob.ph",
                    "Seeded System Administrator account",
                    "127.0.0.1"
            ));
        }

        // 4. Seed Credit Underwriter / Loan Officer
        if (!userRepository.existsByEmail("underwriter@utangnaloob.ph")) {
            User underwriter = new User(
                    "Clara",
                    "Santos",
                    "underwriter@utangnaloob.ph",
                    passwordEncoder.encode("Underwriter@123"),
                    "09170000002",
                    "1992-06-20",
                    "Ortigas Center, Pasig City, Metro Manila",
                    "EMPLOYED",
                    65000.0
            );
            Set<Role> roles = new HashSet<>();
            roles.add(underwriterRole);
            underwriter.setRoles(roles);
            userRepository.save(underwriter);
            logger.info("Created default Credit Underwriter: underwriter@utangnaloob.ph / Underwriter@123");

            auditLogRepository.save(new AuditLog(
                    "SYSTEM_INIT",
                    "SYSTEM",
                    "underwriter@utangnaloob.ph",
                    "Seeded Credit Underwriter account",
                    "127.0.0.1"
            ));
        }

        // 5. Seed Active Borrower with Approved Revolving Credit Line
        if (!userRepository.existsByEmail("borrower@utangnaloob.ph")) {
            User borrower = new User(
                    "Juan",
                    "Dela Cruz",
                    "borrower@utangnaloob.ph",
                    passwordEncoder.encode("Borrower@123"),
                    "09170000003",
                    "1996-11-28",
                    "123 Sampaguita Street, Barangay San Antonio, Makati City",
                    "EMPLOYED",
                    38000.0
            );
            Set<Role> roles = new HashSet<>();
            roles.add(borrowerRole);
            borrower.setRoles(roles);
            User savedBorrower = userRepository.save(borrower);

            // Active KYC Profile
            KycProfile kyc = new KycProfile(savedBorrower);
            kyc.setIdType("UMID");
            kyc.setIdNumber("CRN-0111-2345678-9");
            kyc.setIdDocumentUrl("https://images.unsplash.com/photo-1589829545856-d10d557cf95f?auto=format&fit=crop&w=600&q=80");
            kyc.setProofOfIncomeType("PAYSLIP");
            kyc.setProofOfIncomeUrl("https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?auto=format&fit=crop&w=600&q=80");
            kyc.setKycStatus("VERIFIED");
            kyc.setVerifiedMonthlyIncome(38000.0);
            kyc.setRecommendedCreditLimit(15000.0);
            kyc.setReviewedBy("underwriter@utangnaloob.ph");
            kyc.setReviewedAt(LocalDateTime.now().minusDays(5));
            kycProfileRepository.save(kyc);

            // Active Revolving Credit Line: PHP 30,000 limit, PHP 22,500 available, PHP 7,500 drawn
            CreditLine creditLine = new CreditLine();
            creditLine.setUser(savedBorrower);
            creditLine.setApprovedCreditLimit(new BigDecimal("30000.00"));
            creditLine.setAvailableBalance(new BigDecimal("22500.00"));
            creditLine.setDrawnBalance(new BigDecimal("7500.00"));
            creditLine.setStatus("ACTIVE");
            creditLine.setUpdatedAt(LocalDateTime.now().minusDays(5));
            creditLineRepository.save(creditLine);

            logger.info("Created default Active Borrower: borrower@utangnaloob.ph / Borrower@123");
        }

        // 6. Seed Applicant Borrower with PENDING KYC (Ready for Underwriter Review demo!)
        if (!userRepository.existsByEmail("applicant@utangnaloob.ph")) {
            User applicant = new User(
                    "Maria",
                    "Clara",
                    "applicant@utangnaloob.ph",
                    passwordEncoder.encode("Borrower@123"),
                    "09180000004",
                    "1998-04-12",
                    "45 Rizal Avenue, Sta. Cruz, Manila",
                    "SELF_EMPLOYED",
                    45000.0
            );
            Set<Role> roles = new HashSet<>();
            roles.add(borrowerRole);
            applicant.setRoles(roles);
            User savedApplicant = userRepository.save(applicant);

            // Pending KYC submission
            KycProfile pendingKyc = new KycProfile(savedApplicant);
            pendingKyc.setIdType("DRIVERS_LICENSE");
            pendingKyc.setIdNumber("N02-18-999888");
            pendingKyc.setIdDocumentUrl("https://images.unsplash.com/photo-1557804506-669a67965ba0?auto=format&fit=crop&w=600&q=80");
            pendingKyc.setProofOfIncomeType("CERTIFICATE_OF_EMPLOYMENT");
            pendingKyc.setProofOfIncomeUrl("https://images.unsplash.com/photo-1450133064473-71024230f91b?auto=format&fit=crop&w=600&q=80");
            pendingKyc.setKycStatus("PENDING");
            pendingKyc.setVerifiedMonthlyIncome(null);
            pendingKyc.setRecommendedCreditLimit(18000.0); // 40% of 45,000
            pendingKyc.setSubmittedAt(LocalDateTime.now().minusHours(3));
            kycProfileRepository.save(pendingKyc);

            // Pending Credit Line
            CreditLine pendingCredit = new CreditLine();
            pendingCredit.setUser(savedApplicant);
            pendingCredit.setApprovedCreditLimit(BigDecimal.ZERO);
            pendingCredit.setAvailableBalance(BigDecimal.ZERO);
            pendingCredit.setDrawnBalance(BigDecimal.ZERO);
            pendingCredit.setStatus("PENDING");
            creditLineRepository.save(pendingCredit);

            logger.info("Created default Pending Applicant: applicant@utangnaloob.ph / Borrower@123");
        }

        logger.info("Utang na Loob seed data initialization complete.");
    }

    private void initSystemParameter(String key, String value, String description, String updatedBy) {
        if (!systemParameterRepository.existsByParamKey(key)) {
            systemParameterRepository.save(new SystemParameter(key, value, description, updatedBy));
        }
    }
}
