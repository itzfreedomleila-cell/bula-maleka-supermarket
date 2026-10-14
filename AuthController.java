package net.codejava.account.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import net.codejava.account.dto.DeleteAccountRequest;
import net.codejava.account.dto.LoginRequest;
import net.codejava.account.dto.RegisterRequest;
import net.codejava.account.dto.UserDto;
import net.codejava.account.security.CurrentUser;
import net.codejava.account.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for Module 1 - User Accounts. The login is kept in the server-side
 * HTTP session; the browser only holds the session cookie.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AccountService accountService;
    private final CurrentUser currentUser;

    public AuthController(AccountService accountService, CurrentUser currentUser) {
        this.accountService = accountService;
        this.currentUser = currentUser;
    }

    /** Account Registration (also logs the new customer in). */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto register(@RequestBody RegisterRequest body, HttpServletRequest request) {
        UserDto user = accountService.register(body);
        startSession(request, user);
        return user;
    }

    /** Account Login. */
    @PostMapping("/login")
    public UserDto login(@RequestBody LoginRequest body, HttpServletRequest request) {
        UserDto user = accountService.login(body);
        startSession(request, user);
        return user;
    }

    /** Account Logout. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpSession session) {
        session.invalidate();
    }

    /** Who is logged in (401 if nobody). */
    @GetMapping("/me")
    public UserDto me(HttpSession session) {
        return UserDto.from(currentUser.require(session));
    }

    /** Account Deletion. */
    @DeleteMapping("/account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestBody DeleteAccountRequest body, HttpSession session) {
        Long userId = currentUser.require(session).getId();
        accountService.deleteAccount(userId, body.password());
        session.invalidate();
    }

    /** Stores the user id in a fresh session id (stops session fixation attacks). */
    private void startSession(HttpServletRequest request, UserDto user) {
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.setAttribute(CurrentUser.SESSION_KEY, user.id());
    }
}
