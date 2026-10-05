package com.portal.repository;

import com.portal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for User entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /**
     * Looks up a user by either username or email (case-insensitive for username/email).
     */
    @Query("SELECT u FROM User u WHERE lower(u.username) = lower(:identifier) OR lower(u.email) = lower(:identifier)")
    Optional<User> findByIdentifier(@Param("identifier") String identifier);
}
