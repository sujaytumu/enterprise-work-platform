package com.enterprise.core.service;

import com.enterprise.core.model.Account;
import com.enterprise.core.model.AuthorizationRequest;
import com.enterprise.core.model.AuthorizationResponse;
import com.enterprise.core.repository.AccountRepository;
import com.enterprise.core.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthorizationServiceTest {

    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private VelocityRuleService velocityRuleService;
    private TransactionEventPublisher eventPublisher;
    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        velocityRuleService = mock(VelocityRuleService.class);
        eventPublisher = mock(TransactionEventPublisher.class);
        authorizationService = new AuthorizationService(
                accountRepository, transactionRepository, velocityRuleService, eventPublisher);

        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(velocityRuleService.check(any())).thenReturn(
                new VelocityRuleService.VelocityCheckResult(false, null));
    }

    private Account accountWith(BigDecimal balance, BigDecimal dailyLimit, Account.AccountStatus status) throws Exception {
        Account account = new Account("tok_123", balance, dailyLimit);
        account.setStatus(status);
        // set generated id via reflection since it's normally assigned by JPA
        Field idField = Account.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(account, "acct_1");
        return account;
    }

    @Test
    void approvesWhenBalanceAndLimitAreSufficient() throws Exception {
        Account account = accountWith(new BigDecimal("100.00"), new BigDecimal("500.00"), Account.AccountStatus.ACTIVE);
        when(accountRepository.findWithLockByCardToken("tok_123")).thenReturn(Optional.of(account));

        AuthorizationRequest request = new AuthorizationRequest();
        request.setCardToken("tok_123");
        request.setMerchantId("merchant_1");
        request.setAmount(new BigDecimal("25.00"));

        AuthorizationResponse response = authorizationService.authorize(request);

        assertEquals("APPROVED", response.getStatus());
        assertEquals("00", response.getResponseCode());
        assertEquals(new BigDecimal("75.00"), account.getBalance());
        verify(eventPublisher).publish(any());
    }

    @Test
    void declinesWhenInsufficientFunds() throws Exception {
        Account account = accountWith(new BigDecimal("10.00"), new BigDecimal("500.00"), Account.AccountStatus.ACTIVE);
        when(accountRepository.findWithLockByCardToken("tok_123")).thenReturn(Optional.of(account));

        AuthorizationRequest request = new AuthorizationRequest();
        request.setCardToken("tok_123");
        request.setMerchantId("merchant_1");
        request.setAmount(new BigDecimal("25.00"));

        AuthorizationResponse response = authorizationService.authorize(request);

        assertEquals("DECLINED", response.getStatus());
        assertEquals("51", response.getResponseCode());
        assertEquals(new BigDecimal("10.00"), account.getBalance());
    }

    @Test
    void declinesWhenAccountBlocked() throws Exception {
        Account account = accountWith(new BigDecimal("1000.00"), new BigDecimal("500.00"), Account.AccountStatus.BLOCKED);
        when(accountRepository.findWithLockByCardToken("tok_123")).thenReturn(Optional.of(account));

        AuthorizationRequest request = new AuthorizationRequest();
        request.setCardToken("tok_123");
        request.setMerchantId("merchant_1");
        request.setAmount(new BigDecimal("25.00"));

        AuthorizationResponse response = authorizationService.authorize(request);

        assertEquals("DECLINED", response.getStatus());
        assertEquals("62", response.getResponseCode());
    }

    @Test
    void declinesWhenVelocityRuleFlags() throws Exception {
        Account account = accountWith(new BigDecimal("1000.00"), new BigDecimal("5000.00"), Account.AccountStatus.ACTIVE);
        when(accountRepository.findWithLockByCardToken("tok_123")).thenReturn(Optional.of(account));
        when(velocityRuleService.check(any())).thenReturn(
                new VelocityRuleService.VelocityCheckResult(true, "too many transactions"));

        AuthorizationRequest request = new AuthorizationRequest();
        request.setCardToken("tok_123");
        request.setMerchantId("merchant_1");
        request.setAmount(new BigDecimal("25.00"));

        AuthorizationResponse response = authorizationService.authorize(request);

        assertEquals("DECLINED", response.getStatus());
        assertEquals("05", response.getResponseCode());
    }
}
