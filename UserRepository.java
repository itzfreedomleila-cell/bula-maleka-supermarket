package net.codejava.account.repository;

import java.util.Optional;
import net.codejava.account.model.AccountStatus;
import net.codejava.account.model.Role;
import net.codejava.account.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

/** Database access for users. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByRoleAndStatus(Role role, AccountStatus status);
}
