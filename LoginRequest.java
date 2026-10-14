package net.codejava.account.dto;

/** JSON body for Account Login. */
public record LoginRequest(String email, String password) {
}
