package com.phegon.phegonbank.auth_users.repo;

import com.phegon.phegonbank.auth_users.entity.PassWordResetCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PassWordResetCodeRepo extends JpaRepository<PassWordResetCode, Long> {
    Optional<PassWordResetCode> findByCode(String code);
    void deleteByUserId(Long userId);//frequently give wrong password reset code, delete his account

}
