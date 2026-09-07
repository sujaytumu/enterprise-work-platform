package com.enterprise.core.repository;

import com.enterprise.core.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {

    Optional<Account> findByCardToken(String cardToken);

    /**
     * Pessimistic lock to prevent race conditions on balance updates when
     * concurrent authorization requests hit the same account. Real systems
     * typically favor optimistic locking + retry at this scale, but a
     * pessimistic lock is the clearest way to demonstrate the concern here.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Account> findWithLockByCardToken(String cardToken);
}
