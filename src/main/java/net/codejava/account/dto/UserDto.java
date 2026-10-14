package net.codejava.account.dto;

import net.codejava.account.model.Role;
import net.codejava.account.model.User;

/** A user as shown to the browser (never includes the password hash). */
public record UserDto(Long id, String fullName, String email, String phone, Role role) {

    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(), user.getRole());
    }
}
