package com.movieflix.movieAPI.auth.repositories;

import com.movieflix.movieAPI.auth.entities.FogotPassword;
import com.movieflix.movieAPI.auth.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface FogotPasswordRepository extends JpaRepository<FogotPassword,Integer> {

    @Query("select fp from FogotPassword fp where fp.otp = ?1 and fp.user = ?2")
    Optional<FogotPassword> findByOtpAndUser(Integer otp, User user);
}
