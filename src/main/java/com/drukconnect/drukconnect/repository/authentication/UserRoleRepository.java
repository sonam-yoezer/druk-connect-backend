package com.drukconnect.drukconnect.repository.authentication;

import com.drukconnect.drukconnect.entity.authentication.User;
import com.drukconnect.drukconnect.entity.authentication.UserRole;
import com.drukconnect.drukconnect.enums.authentication.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    @Query("select ur.role.code from UserRole ur where ur.user.id = :userId and ur.role.active = true")
    List<String> findRoleCodesByUserId(@Param("userId") UUID userId);

    @Query("""
            SELECT DISTINCT ur.user
            FROM UserRole ur
            JOIN ur.role r
            WHERE UPPER(r.code) = UPPER(:roleCode)
              AND r.active = true
              AND ur.user.status = :userStatus
              AND ur.user.email IS NOT NULL
            """)
    List<User> findUsersByRoleCodeAndStatus(
            @Param("roleCode") String roleCode,
            @Param("userStatus") UserStatus userStatus
    );
}
