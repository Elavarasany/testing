
package nmk_backend.config;

import nmk_backend.entity.Admin;
import nmk_backend.entity.WebsiteImage;
import nmk_backend.repository.AdminRepository;
import nmk_backend.repository.WebsiteImageRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminDataInitializer {

    @Value("${admin.bootstrap.username:}")
    private String username;

    @Value("${admin.bootstrap.password:}")
    private String password;

    @Bean
    public ApplicationRunner createInitialAdmin(
            AdminRepository adminRepository,
            WebsiteImageRepository websiteImageRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {

            // Create initial admin
            if (username.isBlank() || password.isBlank()) {
                System.out.println(
                        "Admin bootstrap skipped. Configure admin environment variables."
                );
            } else if (!adminRepository.existsByUsername(username)) {

                Admin admin = new Admin();
                admin.setUsername(username);
                admin.setPassword(passwordEncoder.encode(password));

                adminRepository.save(admin);

                System.out.println("Initial admin account created successfully.");
            }

            // Insert existing website images only if their keys are missing
            saveImageIfMissing(
                    websiteImageRepository,
                    "LOGO",
                    "Website Logo",
                    "https://placehold.co/300x100?text=NMK+OPTICALS"
            );
            saveImageIfMissing(
                    websiteImageRepository,
                    "HERO",
                    "Homepage Hero",
                    "https://images.unsplash.com/photo-1511499767150-a48a237f0083?w=1200"
            );

            saveImageIfMissing(
                    websiteImageRepository,
                    "CATEGORY_EYEGLASSES",
                    "Eyeglasses",
                    "https://images.unsplash.com/photo-1511499767150-a48a237f0083?w=700"
            );

            saveImageIfMissing(
                    websiteImageRepository,
                    "CATEGORY_SUNGLASSES",
                    "Sunglasses",
                    "https://images.unsplash.com/photo-1508296695146-257a814070b4?w=700"
            );

            saveImageIfMissing(
                    websiteImageRepository,
                    "CATEGORY_CONTACT_LENSES",
                    "Contact Lenses",
                    "https://images.unsplash.com/photo-1574258495973-f010dfbb5371?w=700"
            );

            saveImageIfMissing(
                    websiteImageRepository,
                    "CATEGORY_COMPUTER_GLASSES",
                    "Computer Glasses",
                    "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=700"
            );

            saveImageIfMissing(
                    websiteImageRepository,
                    "STORE",
                    "Store Image",
                    "https://images.unsplash.com/photo-1574258495973-f010dfbb5371?w=1000"
            );

            System.out.println("Website image initialization completed.");
        };
    }

    private void saveImageIfMissing(
            WebsiteImageRepository repository,
            String key,
            String title,
            String url
    ) {
        if (repository.findByImageKey(key).isEmpty()) {

            WebsiteImage image = new WebsiteImage();
            image.setImageKey(key);
            image.setTitle(title);
            image.setImageUrl(url);

            repository.save(image);

            System.out.println("Added website image: " + key);
        }
    }
}
