package com.finpay.repository;

import com.finpay.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByDocumentNumber(String documentNumber);

    @Query("SELECT u FROM User u JOIN FETCH u.wallet WHERE u.email = :email")
    Optional<User> findByEmailWithWallet(@Param("email") String email);
}