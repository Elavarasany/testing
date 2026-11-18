
package nmk_backend.service;

import nmk_backend.entity.Admin;
import nmk_backend.repository.AdminRepository;
import nmk_backend.security.JwtService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AdminAuthService(
            AdminRepository adminRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public String login(String username, String password) {

        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(password, admin.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        return jwtService.generateToken(admin.getUsername());
    }
    public void verifyPassword(String username, String password) {

        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("Invalid admin"));

        if (!passwordEncoder.matches(password, admin.getPassword())) {
            throw new RuntimeException("Incorrect password");
        }
    }
}
