package net.codejava.account.event;

/**
 * Published when a customer deletes their account, so other modules can
 * clean up (empty the cart, remove saved payment methods, cancel bookings)
 * without the account module depending on them.
 *
 * @param customerId the deleted user's id as a string
 */
public record AccountDeletedEvent(String customerId) {
}
