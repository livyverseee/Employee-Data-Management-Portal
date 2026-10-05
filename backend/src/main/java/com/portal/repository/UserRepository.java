package com.portal.repository;

import com.portal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for User entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by their unique username.
     *
     * @param username user login handle
     * @return Optional containing the User if found
     */
    Optional<User> findByUsername(String username);
}
