
package nmk_backend.controller;
import org.springframework.security.core.Authentication;
import nmk_backend.service.AdminAuthService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {

        String username = request.get("username");
        String password = request.get("password");

        if (username == null || password == null ||
                username.isBlank() || password.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Username and password are required"));
        }

        try {
            String token = adminAuthService.login(username, password);

            return ResponseEntity.ok(
                    Map.of(
                            "message", "Login successful",
                            "token", token,
                            "username", username
                    )
            );

        } catch (RuntimeException exception) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Invalid username or password"));
        }
    }
    @PostMapping("/verify-password")
    public ResponseEntity<?> verifyPassword(
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {

        String password = request.get("password");

        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Password is required"));
        }

        try {
            adminAuthService.verifyPassword(
                    authentication.getName(),
                    password
            );

            return ResponseEntity.ok(
                    Map.of("verified", true)
            );

        } catch (RuntimeException exception) {
            return ResponseEntity.status(400)
                    .body(Map.of("message", "Incorrect admin password"));
        }
    }
}
