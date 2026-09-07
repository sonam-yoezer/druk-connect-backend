package com.drukconnect.drukconnect.repository;

import com.drukconnect.drukconnect.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    @Query("select ur.role.code from UserRole ur where ur.user.id = :userId and ur.role.active = true")
    List<String> findRoleCodesByUserId(@Param("userId") UUID userId);
}
