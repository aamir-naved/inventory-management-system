package com.inventory.support;

import com.inventory.auth.entity.BusinessMembership;
import com.inventory.auth.entity.UserAccount;
import com.inventory.auth.repository.BusinessMembershipRepository;
import com.inventory.auth.repository.UserAccountRepository;
import com.inventory.auth.security.JwtService;
import com.inventory.business.entity.Business;
import com.inventory.business.repository.BusinessRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

public abstract class AuthenticatedControllerTestSupport {

    @Autowired
    protected UserAccountRepository userAccountRepository;

    @Autowired
    protected BusinessRepository businessRepository;

    @Autowired
    protected BusinessMembershipRepository businessMembershipRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtService jwtService;

    protected UserAccount createUserAccount() {
        UserAccount userAccount = new UserAccount();
        userAccount.setFullName("Business Owner");
        userAccount.setEmail("owner-" + System.nanoTime() + "@example.com");
        userAccount.setPasswordHash(passwordEncoder.encode("password123"));
        userAccount.setEmailVerified(true);
        userAccount.setActive(true);
        return userAccountRepository.save(userAccount);
    }

    protected Business createBusinessFor(UserAccount userAccount) {
        Business business = new Business();
        business.setName("North Star Traders");
        business.setBusinessType("Hardware Store");
        business.setAddressLine("42 Market Road");
        business.setMobileNumber("+91 9876543210");
        business.setCurrencyCode("INR");
        business.setTimeZone("Asia/Kolkata");
        business.setAllowNegativeStock(false);
        business.setDefaultLowStockThreshold(java.math.BigDecimal.ZERO);
        business.setDateFormat("dd/MM/yyyy");
        business.setActive(true);
        Business savedBusiness = businessRepository.save(business);

        BusinessMembership membership = new BusinessMembership();
        membership.setBusiness(savedBusiness);
        membership.setUser(userAccount);
        membership.setRole("OWNER");
        membership.setActive(true);
        businessMembershipRepository.save(membership);

        return savedBusiness;
    }

    protected String authorizationHeader(UserAccount userAccount) {
        return "Bearer " + jwtService.issueToken(userAccount.getId(), userAccount.getEmail()).value();
    }
}
