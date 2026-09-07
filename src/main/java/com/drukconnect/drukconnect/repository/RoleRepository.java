package com.drukconnect.drukconnect.repository;

import com.drukconnect.drukconnect.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByCodeAndActiveTrue(String code);
}
