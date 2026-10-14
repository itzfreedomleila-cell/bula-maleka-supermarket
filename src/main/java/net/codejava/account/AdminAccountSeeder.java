package net.codejava.account;

import java.util.logging.Logger;
import net.codejava.account.model.AccountStatus;
import net.codejava.account.model.Role;
import net.codejava.account.model.User;
import net.codejava.account.repository.UserRepository;
import net.codejava.account.security.PasswordHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Creates the first staff (admin) account on start-up if there is none.
 * The email and password come from application.properties.
 */
@Component
public class AdminAccountSeeder implements CommandLineRunner {

    private static final Logger LOG = Logger.getLogger(AdminAccountSeeder.class.getName());

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final String email;
    private final String password;

    public AdminAccountSeeder(UserRepository userRepository, PasswordHasher passwordHasher,
                              @Value("${app.admin.email}") String email,
                              @Value("${app.admin.password}") String password) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (userRepository.countByRoleAndStatus(Role.ADMIN, AccountStatus.ACTIVE) == 0
                && !userRepository.existsByEmailIgnoreCase(email)) {
            userRepository.save(new User("Store Admin", email, null, passwordHasher.hash(password), Role.ADMIN));
            LOG.info("Created staff account " + email + ". Change the password in application.properties.");
        }
    }
}
