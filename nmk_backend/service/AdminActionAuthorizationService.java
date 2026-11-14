
package nmk_backend.service;

import nmk_backend.entity.Admin;
import nmk_backend.entity.AdminActionAuthorization;
import nmk_backend.repository.AdminActionAuthorizationRepository;
import nmk_backend.repository.AdminRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Set;

@Service
public class AdminActionAuthorizationService {

    private static final Set<String> ALLOWED_ACTIONS = Set.of(
            "CLEAR_ORDERS",
            "CLEAR_APPOINTMENTS"
    );

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AdminRepository adminRepository;
    private final AdminActionAuthorizationRepository authorizationRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminActionAuthorizationService(
            AdminRepository adminRepository,
            AdminActionAuthorizationRepository authorizationRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.adminRepository = adminRepository;
        this.authorizationRepository = authorizationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public String authorizeAction(
            String username,
            String password,
            String action
    ) {
        if (action == null || !ALLOWED_ACTIONS.contains(action)) {
            throw new IllegalArgumentException("Invalid destructive action");
        }

        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid admin"));

        if (!passwordEncoder.matches(password, admin.getPassword())) {
            throw new IllegalArgumentException("Incorrect admin password");
        }

        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        AdminActionAuthorization authorization =
                new AdminActionAuthorization();

        authorization.setTokenHash(hashToken(rawToken));
        authorization.setAdminUsername(username);
        authorization.setAction(action);
        authorization.setCreatedAt(LocalDateTime.now());
        authorization.setExpiresAt(LocalDateTime.now().plusMinutes(2));

        authorizationRepository.save(authorization);

        return rawToken;
    }

    @Transactional
    public void consumeAuthorization(
            String username,
            String rawToken,
            String expectedAction
    ) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Fresh password authorization is required"
            );
        }

        if (!ALLOWED_ACTIONS.contains(expectedAction)) {
            throw new IllegalArgumentException("Invalid destructive action");
        }

        String tokenHash = hashToken(rawToken);

        AdminActionAuthorization authorization =
                authorizationRepository.findByTokenHashForUpdate(tokenHash)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid or expired authorization"
                                ));

        LocalDateTime now = LocalDateTime.now();

        if (!authorization.getAdminUsername().equals(username)
                || !authorization.getAction().equals(expectedAction)
                || authorization.getConsumedAt() != null
                || !authorization.getExpiresAt().isAfter(now)) {

            throw new IllegalArgumentException(
                    "Authorization is invalid, expired, or already used"
            );
        }

        authorization.setConsumedAt(now);
        authorizationRepository.save(authorization);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to secure authorization token",
                    exception
            );
        }
    }
}
