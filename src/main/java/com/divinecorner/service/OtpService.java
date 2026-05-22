package com.divinecorner.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final StringRedisTemplate stringRedisTemplate;
    private static final String OTP_PREFIX = "otp:reg:";
    private static final int OTP_TTL_MINUTES = 5;
    private final SecureRandom random = new SecureRandom();

    public String generateAndStore(String email) {
        String otp = String.format("%06d", random.nextInt(1_000_000));
        stringRedisTemplate.opsForValue().set(
            OTP_PREFIX + email.toLowerCase(),
            otp,
            Duration.ofMinutes(OTP_TTL_MINUTES)
        );
        return otp;
    }

    public boolean verifyAndConsume(String email, String otp) {
        String key = OTP_PREFIX + email.toLowerCase();
        String stored = stringRedisTemplate.opsForValue().get(key);
        if (stored != null && stored.equals(otp)) {
            stringRedisTemplate.delete(key);
            return true;
        }
        return false;
    }
}
