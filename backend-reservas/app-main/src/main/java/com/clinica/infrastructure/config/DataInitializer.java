package com.clinica.infrastructure.config;


import com.clinica.shared.domain.UserRole;
import com.clinica.users.domain.entities.Admin;
import com.clinica.users.domain.entities.User;
import com.clinica.users.infrastructure.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Creates default users when the application starts.
 */
@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UserRepository userRepository,
                                      PasswordEncoder passwordEncoder) {
        return args -> {

            // Crear admin por defecto si no existe
            if (userRepository.findByUsername("admin").isEmpty()) {

                Admin adminPerson = new Admin();
                adminPerson.setIdentification("000000001");
                adminPerson.setFirstName("Super");
                adminPerson.setLastName("Admin");
                adminPerson.setEmail("admin@clinica.com");
                adminPerson.setPhone("3000000000");

                User adminUser = new User();
                adminUser.setUsername("admin");
                adminUser.setPassword(passwordEncoder.encode("admin123"));
                adminUser.setEnabled(true);
                adminUser.setRole(UserRole.ADMIN);
                adminUser.setPerson(adminPerson);

                userRepository.save(adminUser);
                System.out.println("Admin creado: usuario=admin, contraseña=admin123");
            }
        };
    }
}
