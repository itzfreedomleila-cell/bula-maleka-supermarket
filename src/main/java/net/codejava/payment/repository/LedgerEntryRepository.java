package net.codejava.payment.repository;

import java.util.List;
import net.codejava.payment.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

/** Database access for money transfer records. */
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findAllByOrderByCreatedAtDesc();
}
