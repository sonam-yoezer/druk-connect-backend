package com.drukconnect.drukconnect.repository.authentication;

import com.drukconnect.drukconnect.entity.authentication.User;
import com.drukconnect.drukconnect.enums.authentication.AccessTypeEnum;
import com.drukconnect.drukconnect.enums.authentication.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);

    @Query("""
            SELECT u
            FROM User u
            WHERE u.id <> :requesterId
            AND u.status = :status
            AND u.accessType = :accessType
            AND (
                LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(CONCAT(u.firstName, ' ', u.lastName))
                    LIKE LOWER(CONCAT('%', :query, '%'))
            )
            """)
    List<User> searchVouchCandidates(
            @Param("requesterId") UUID requesterId,
            @Param("query") String query,
            @Param("status") UserStatus status,
            @Param("accessType") AccessTypeEnum accessType,
            Pageable pageable
    );

    @Query("""
        SELECT DISTINCT u
        FROM User u
        JOIN UserRole ur
            ON ur.user.id = u.id
        JOIN Role r
            ON ur.role.id = r.id
        WHERE r.code = 'ADMIN'
        AND r.active = true
        AND u.status =
            com.drukconnect.drukconnect.enums.authentication.UserStatus.ACTIVE
        """)
    List<User> findActiveAdmins();

    @Query("""
        SELECT u
        FROM User u
        WHERE u.accessType = :accessType
          AND u.status = :status
          AND (
                :query = ''
                OR LOWER(u.firstName)
                    LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(u.lastName)
                    LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(CONCAT(u.firstName, ' ', u.lastName))
                    LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(u.email)
                    LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(u.phoneNumber)
                    LIKE LOWER(CONCAT('%', :query, '%'))
          )
        ORDER BY u.firstName ASC, u.lastName ASC
        """)
    Page<User> searchActiveBuyers(

            @Param("accessType")
            AccessTypeEnum accessType,

            @Param("status")
            UserStatus status,

            @Param("query")
            String query,

            Pageable pageable
    );
}
