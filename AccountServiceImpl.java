package net.codejava.account.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.codejava.account.dto.LoginRequest;
import net.codejava.account.dto.RegisterRequest;
import net.codejava.account.dto.UserDto;
import net.codejava.account.event.AccountDeletedEvent;
import net.codejava.account.model.AccountStatus;
import net.codejava.account.model.Role;
import net.codejava.account.model.User;
import net.codejava.account.repository.UserRepository;
import net.codejava.account.security.PasswordHasher;
import net.codejava.common.exception.NotFoundException;
import net.codejava.common.exception.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
/**
 * Business rules for registration, login and account deletion.
 */
@Service
@Transactional
public class AccountServiceImpl implements AccountService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^\\d{7}$");
    private static final Pattern NAME = Pattern.compile("^[A-Za-z][A-Za-z .'-]{1,79}$");
    private static final String LOGIN_FAILED = "Email or password is incorrect.";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final ApplicationEventPublisher events;
    private final int maxFailedLogins;
    private final int lockMinutes;
    private final Clock clock;
    
    @Autowired
    public AccountServiceImpl(UserRepository userRepository,
                              PasswordHasher passwordHasher,
                              ApplicationEventPublisher events,
                              @Value("${account.max-failed-logins:5}") int maxFailedLogins,
                              @Value("${account.lock-minutes:15}") int lockMinutes) {
        this(userRepository, passwordHasher, events, maxFailedLogins, lockMinutes, Clock.systemDefaultZone());
    }

    /** Used by tests to control the time. */
    AccountServiceImpl(UserRepository userRepository, PasswordHasher passwordHasher,
                       ApplicationEventPublisher events, int maxFailedLogins, int lockMinutes, Clock clock) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.events = events;
        this.maxFailedLogins = maxFailedLogins;
        this.lockMinutes = lockMinutes;
        this.clock = clock;
    }

    @Override
    public UserDto register(RegisterRequest request) {
        List<String> errors = new ArrayList<>();
        String name = request.fullName() == null ? "" : request.fullName().trim();
        String email = request.email() == null ? "" : request.email().trim().toLowerCase();
        String phone = request.phone() == null ? "" : request.phone().replaceAll("\\D", "");

        if (!NAME.matcher(name).matches()) {
            errors.add("Enter your full name (letters only).");
        }
        if (!EMAIL.matcher(email).matches()) {
            errors.add("Enter a valid email address.");
        } else if (userRepository.existsByEmailIgnoreCase(email)) {
            errors.add("An account with this email already exists. Try logging in.");
        }
        if (!phone.isEmpty() && !PHONE.matcher(phone).matches()) {
            errors.add("Phone number must be 7 digits.");
        }
        errors.addAll(passwordProblems(request.password()));
        if (request.password() != null && !request.password().equals(request.confirmPassword())) {
            errors.add("Passwords do not match.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        User user = new User(name, email, phone.isEmpty() ? null : phone,
                passwordHasher.hash(request.password()), Role.CUSTOMER);
        return UserDto.from(userRepository.save(user));
    }

    @Override
    public UserDto login(LoginRequest request) {
        String email = request.email() == null ? "" : request.email().trim();
        User user = userRepository.findByEmailIgnoreCase(email)
                .filter(User::isActive)
                .orElseThrow(() -> new ValidationException(LOGIN_FAILED));
        LocalDateTime now = LocalDateTime.now(clock);
        if (user.isLocked(now)) {
            throw new ValidationException("Too many wrong passwords. Try again after "
                    + user.getLockedUntil().toLocalTime().withNano(0) + ".");
        }
        if (!passwordHasher.matches(request.password(), user.getPasswordHash())) {
            user.recordFailedLogin(maxFailedLogins, lockMinutes, now);
            userRepository.save(user);
            throw new ValidationException(LOGIN_FAILED);
        }
        user.recordSuccessfulLogin();
        return UserDto.from(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUser(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isActive)
                .map(UserDto::from)
                .orElseThrow(() -> new NotFoundException("Account not found."));
    }

    @Override
    public void deleteAccount(Long userId, String password) {
        User user = userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new NotFoundException("Account not found."));
        if (!passwordHasher.matches(password, user.getPasswordHash())) {
            throw new ValidationException("Password is incorrect. Your account was not deleted.");
        }
        if (user.getRole() == Role.ADMIN
                && userRepository.countByRoleAndStatus(Role.ADMIN, AccountStatus.ACTIVE) <= 1) {
            throw new ValidationException("The last staff account cannot be deleted.");
        }
        String customerId = String.valueOf(user.getId());
        user.anonymise(LocalDateTime.now(clock));
        userRepository.save(user);
        events.publishEvent(new AccountDeletedEvent(customerId));
    }

    /** Password rules: at least 8 characters with a letter and a number. */
    static List<String> passwordProblems(String password) {
        List<String> errors = new ArrayList<>();
        if (password == null || password.length() < 8) {
            errors.add("Password must be at least 8 characters.");
        } else if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            errors.add("Password must contain at least one letter and one number.");
        }
        return errors;
    }
}
