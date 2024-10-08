package com.movieflix.movieAPI.auth.services;

import com.movieflix.movieAPI.auth.entities.RefreshToken;
import com.movieflix.movieAPI.auth.entities.User;
import com.movieflix.movieAPI.auth.repositories.RefreshTokenRepository;
import com.movieflix.movieAPI.auth.repositories.UserRepository;
import com.movieflix.movieAPI.exceptions.RefreshTokenExpiredException;
import com.movieflix.movieAPI.exceptions.RefreshTokenNotFoundException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public RefreshToken createRefreshToken(String username){
        User user = userRepository.findByEmail(username).orElseThrow(()-> new UsernameNotFoundException("User not found with email : " + username));
        RefreshToken refreshToken = user.getRefreshToken();
        if(refreshToken == null){
            long refreshTokenValidity = 30*1000;
            refreshToken = RefreshToken.builder()
                    .refreshToken(UUID.randomUUID().toString())
                    .expirationTime(Instant.now().plusMillis(refreshTokenValidity))
                    .user(user)
                    .build();
            refreshTokenRepository.save(refreshToken);
        }
        return refreshToken;

    }

    public RefreshToken verifyRefreshToken(String refreshToken){
        RefreshToken refToken = refreshTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(()-> new RefreshTokenNotFoundException("Refresh token not found!"));

        if(refToken.getExpirationTime().compareTo(Instant.now()) < 0){
            refreshTokenRepository.delete(refToken);
            throw new RefreshTokenExpiredException("Refresh Token expired");
        }

        return refToken;
    }
}
