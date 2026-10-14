package net.codejava.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import net.codejava.payment.model.LedgerEntry;
import net.codejava.payment.model.LedgerEntryType;

/** Money transfer records plus the store's current settlement balance. */
public record LedgerDto(String storeAccount, BigDecimal storeBalance, List<EntryDto> entries) {

    public record EntryDto(String transactionReference, LedgerEntryType type, String fromAccount,
                           String toAccount, BigDecimal amount, LocalDateTime createdAt) {

        public static EntryDto from(LedgerEntry e) {
            return new EntryDto(e.getTransactionReference(), e.getType(), e.getFromAccount(),
                    e.getToAccount(), e.getAmount(), e.getCreatedAt());
        }
    }
}
