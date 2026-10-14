package net.codejava.account.dto;

/** JSON body for Account Registration. */
public record RegisterRequest(String fullName, String email, String phone, String password, String confirmPassword) {
}
