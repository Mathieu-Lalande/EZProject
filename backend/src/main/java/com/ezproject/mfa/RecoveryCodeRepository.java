package com.ezproject.mfa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RecoveryCodeRepository extends JpaRepository<RecoveryCode, Long> {

    List<RecoveryCode> findByUserId(Long userId);
}
