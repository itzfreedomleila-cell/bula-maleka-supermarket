package net.codejava.account.security;

import jakarta.servlet.http.HttpSession;
import net.codejava.account.model.User;
import net.codejava.account.repository.UserRepository;
import net.codejava.common.exception.AccessDeniedException;
import net.codejava.common.exception.AuthenticationRequiredException;
import org.springframework.stereotype.Component;

/**
 * Finds the logged-in user from the HTTP session. Controllers call this
 * so every protected action checks who is asking.
 */
@Component
public class CurrentUser {

    /** Session attribute holding the logged-in user's id. */
    public static final String SESSION_KEY = "userId";

    private final UserRepository userRepository;

    public CurrentUser(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** @return the logged-in user, or throws 401 if nobody is logged in */
    public User require(HttpSession session) {
        Object id = session.getAttribute(SESSION_KEY);
        if (!(id instanceof Long userId)) {
            throw new AuthenticationRequiredException();
        }
        return userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(AuthenticationRequiredException::new);
    }

    /** @return the logged-in admin, or throws 401/403 */
    public User requireAdmin(HttpSession session) {
        User user = require(session);
        if (!user.isAdmin()) {
            throw new AccessDeniedException("Only store staff can do this.");
        }
        return user;
    }

    /** @return the id used by the cart, payment, order and delivery modules */
    public String customerId(HttpSession session) {
        return String.valueOf(require(session).getId());
    }

    /** @return true if the logged-in user may see data owned by this customer id */
    public boolean canView(HttpSession session, String ownerCustomerId) {
        User user = require(session);
        return user.isAdmin() || String.valueOf(user.getId()).equals(ownerCustomerId);
    }
}
