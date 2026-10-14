package net.codejava.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import net.codejava.account.dto.LoginRequest;
import net.codejava.account.dto.RegisterRequest;
import net.codejava.account.dto.UserDto;
import net.codejava.account.event.AccountDeletedEvent;
import net.codejava.account.model.AccountStatus;
import net.codejava.account.model.Role;
import net.codejava.account.model.User;
import net.codejava.account.repository.UserRepository;
import net.codejava.account.security.PasswordHasher;
import net.codejava.common.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/** Tests for Account Registration, Login and Deletion. */
@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher events;

    private final PasswordHasher hasher = new PasswordHasher();
    private AccountServiceImpl service;
    private User camari;

    @BeforeEach
    void setUp() {
        service = new AccountServiceImpl(userRepository, hasher, events, 3, 15,
                Clock.fixed(Instant.parse("2026-10-08T00:00:00Z"), ZoneId.of("Pacific/Fiji")));
        camari = new User("Camari K", "camari@example.com", "9123456", hasher.hash("Bula2026x"), Role.CUSTOMER);
        camari.setId(7L);
    }

    @Test
    void register_validDetails_createsCustomer() {
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto user = service.register(new RegisterRequest("Mere Tabua", "New@Example.com", "912 3456",
                "Vinaka123", "Vinaka123"));

        assertEquals("new@example.com", user.email());
        assertEquals(Role.CUSTOMER, user.role());
    }

    @Test
    void register_reportsEveryProblemAtOnce() {
        ValidationException ex = assertThrows(ValidationException.class, () -> service.register(
                new RegisterRequest("", "not-an-email", "12", "short", "different")));
        assertTrue(ex.getErrors().size() >= 4);
    }

    @Test
    void register_existingEmail_isRejected() {
        when(userRepository.existsByEmailIgnoreCase("camari@example.com")).thenReturn(true);
        assertThrows(ValidationException.class, () -> service.register(
                new RegisterRequest("Camari K", "camari@example.com", null, "Bula2026x", "Bula2026x")));
    }

    @Test
    void login_correctPassword_succeeds() {
        when(userRepository.findByEmailIgnoreCase("camari@example.com")).thenReturn(Optional.of(camari));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(7L, service.login(new LoginRequest("camari@example.com", "Bula2026x")).id());
    }

    @Test
    void login_threeWrongPasswords_locksAccount() {
        when(userRepository.findByEmailIgnoreCase("camari@example.com")).thenReturn(Optional.of(camari));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        for (int i = 0; i < 3; i++) {
            assertThrows(ValidationException.class,
                    () -> service.login(new LoginRequest("camari@example.com", "wrong-pass1")));
        }
        ValidationException locked = assertThrows(ValidationException.class,
                () -> service.login(new LoginRequest("camari@example.com", "Bula2026x")));
        assertTrue(locked.getMessage().startsWith("Too many wrong passwords"));
    }

    @Test
    void deleteAccount_rightPassword_removesPersonalDetails() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(camari));

        service.deleteAccount(7L, "Bula2026x");

        assertEquals(AccountStatus.DELETED, camari.getStatus());
        assertEquals("Deleted user", camari.getFullName());
        verify(events).publishEvent(any(AccountDeletedEvent.class));
    }

    @Test
    void deleteAccount_wrongPassword_keepsAccount() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(camari));

        assertThrows(ValidationException.class, () -> service.deleteAccount(7L, "nope12345"));
        assertEquals(AccountStatus.ACTIVE, camari.getStatus());
        verify(events, never()).publishEvent(any(AccountDeletedEvent.class));
    }
}
