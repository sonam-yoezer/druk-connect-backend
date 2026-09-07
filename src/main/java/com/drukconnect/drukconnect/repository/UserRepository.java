package com.drukconnect.drukconnect.repository;

import com.drukconnect.drukconnect.entity.User;
import com.drukconnect.drukconnect.enums.AccessTypeEnum;
import com.drukconnect.drukconnect.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.util.Arrays;
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

}
