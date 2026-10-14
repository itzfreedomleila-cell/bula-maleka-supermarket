package net.codejava.account.service;

import net.codejava.account.dto.LoginRequest;
import net.codejava.account.dto.RegisterRequest;
import net.codejava.account.dto.UserDto;

/** Module 1 - User Accounts. */
public interface AccountService {

    /** Account Registration: creates a customer account. */
    UserDto register(RegisterRequest request);

    /** Account Login: checks the email and password. */
    UserDto login(LoginRequest request);

    /** @return the user's details */
    UserDto getUser(Long userId);

    /** Account Deletion: removes personal details after checking the password. */
    void deleteAccount(Long userId, String password);
}
