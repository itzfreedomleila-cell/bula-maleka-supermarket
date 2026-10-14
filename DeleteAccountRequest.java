package net.codejava.account.dto;

/** JSON body for Account Deletion: the password confirms it is really the owner. */
public record DeleteAccountRequest(String password) {
}
