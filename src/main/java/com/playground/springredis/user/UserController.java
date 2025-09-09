package com.playground.springredis.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final TemporaryPasswordService temporaryPasswordService;

    @PostMapping("/reset-password/{userId}")
    public ResponseEntity<Void> resetPassword(@PathVariable Long userId) {

        String randomPassword = temporaryPasswordService.issueTemporaryPassword(userId, Duration.ofMinutes(3L).getSeconds());
        log.info(" success:userId: {}, temporaryPassword: {}", userId, randomPassword);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-password")
    public ResponseEntity<Void> verifyTemporaryPassword(@RequestParam Long userId, @RequestParam String temporaryPassword) {


        boolean b = temporaryPasswordService.verifyTemporaryPassword(userId, temporaryPassword);
        if (b) {
            log.info(" success: userId: {}, temporaryPassword: {}", userId, temporaryPassword);
            return ResponseEntity.ok().build();
        } else {
            log.info(" fail: userId: {}, temporaryPassword: {}", userId, temporaryPassword);
            return ResponseEntity.badRequest().build();
        }
    }

}
