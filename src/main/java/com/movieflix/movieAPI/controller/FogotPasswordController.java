package com.movieflix.movieAPI.controller;

import com.movieflix.movieAPI.auth.entities.FogotPassword;
import com.movieflix.movieAPI.auth.entities.User;
import com.movieflix.movieAPI.auth.repositories.FogotPasswordRepository;
import com.movieflix.movieAPI.auth.repositories.UserRepository;
import com.movieflix.movieAPI.dto.MailBody;
import com.movieflix.movieAPI.service.EmailService;
import com.movieflix.movieAPI.utils.ChangePassword;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Random;

@RestController
@RequestMapping("/forgotPassword")
public class FogotPasswordController {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final FogotPasswordRepository fogotPasswordRepository;
    private final PasswordEncoder passwordEncoder;

    public FogotPasswordController(UserRepository userRepository, EmailService emailService, FogotPasswordRepository fogotPasswordRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.fogotPasswordRepository = fogotPasswordRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/verifyMail/{email}")
    public ResponseEntity<String> verifyEmail(@PathVariable String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Please provide an valid Email!"));

        int otp = otpGenerator();
        MailBody mailBody = MailBody.builder()
                .to(email)
                .text("This is OTP for your Forgot Password : " + otp)
                .subject("OTP for Forgot Password Request!")
                .build();

        FogotPassword fp = FogotPassword.builder()
                .otp(otp)
                .expirationTime(new Date(System.currentTimeMillis() + 70 * 1000))
                .user(user)
                .build();

        emailService.sendSimpleMessage(mailBody);
        fogotPasswordRepository.save(fp);

        return ResponseEntity.ok("Email send for verification!");
    }

    @PostMapping("/verifyOtp/{otp}/{email}")
    public ResponseEntity<String> verifyOtp(@PathVariable Integer otp, @PathVariable String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Please provide an valid email!"));

        FogotPassword fp = fogotPasswordRepository.findByOtpAndUser(otp,user)
                .orElseThrow(() -> new RuntimeException("Invalid OTP for Email!" + email));

        if (fp.getExpirationTime().before(Date.from(Instant.now()))){
            fogotPasswordRepository.deleteById(fp.getFpid());
            return new ResponseEntity<>("OTP has expired!", HttpStatus.EXPECTATION_FAILED);
        }

        return ResponseEntity.ok("OTP verified!");
    }

    @PostMapping("/changePassword/{email}")
    public ResponseEntity<String> changePasswordHandler(@RequestBody ChangePassword changePassword,
                                                        @PathVariable String email){
        if(!Objects.equals(changePassword.password(), changePassword.repeatPassword())){
            return new ResponseEntity<>("Please enter the password again!", HttpStatus.EXPECTATION_FAILED);
        }

        String encodedPassword = passwordEncoder.encode(changePassword.password());
        userRepository.updatePassword(email,encodedPassword);
        return ResponseEntity.ok("Password has been changed!");
    }


    private Integer otpGenerator(){
        Random random = new Random();
        return random.nextInt(100_000,999_999);
    }
}
