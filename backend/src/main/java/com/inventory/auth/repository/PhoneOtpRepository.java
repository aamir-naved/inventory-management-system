package com.inventory.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inventory.auth.entity.PhoneOtp;

public interface PhoneOtpRepository extends JpaRepository<PhoneOtp, java.util.UUID> {

    Optional<PhoneOtp> findFirstByPhoneOrderByCreatedAtDesc(String phone);
}
