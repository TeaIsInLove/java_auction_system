package com.example.bai_tap_lon;

import com.example.bai_tap_lon.model.entity.user.Admin;
import com.example.bai_tap_lon.model.entity.user.Bidder;
import com.example.bai_tap_lon.model.entity.user.Seller;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserModelTest {

    // ===== Bidder =====

    @Test
    void bidderDefaultBalanceIsZero() {
        Bidder b = new Bidder("alice", "pass", "alice@test.com");
        assertEquals(0.0, b.getBalance());
    }

    @Test
    void bidderRoleIsBidder() {
        Bidder b = new Bidder("alice", "pass", "alice@test.com");
        assertEquals("Bidder", b.getRole());
    }

    @Test
    void bidderDepositIncreasesBalance() {
        Bidder b = new Bidder("alice", "pass", "alice@test.com", 10_000_000);
        b.deposit(5_000_000);
        assertEquals(15_000_000, b.getBalance());
    }

    @Test
    void bidderDepositIgnoresNegativeAmount() {
        Bidder b = new Bidder("alice", "pass", "alice@test.com", 10_000_000);
        b.deposit(-1_000);
        assertEquals(10_000_000, b.getBalance());
    }

    @Test
    void bidderWithdrawSucceedsWhenSufficientBalance() {
        Bidder b = new Bidder("alice", "pass", "alice@test.com", 10_000_000);
        assertTrue(b.withdraw(5_000_000));
        assertEquals(5_000_000, b.getBalance());
    }

    @Test
    void bidderWithdrawFailsWhenInsufficientBalance() {
        Bidder b = new Bidder("alice", "pass", "alice@test.com", 1_000);
        assertFalse(b.withdraw(5_000_000));
        assertEquals(1_000, b.getBalance());
    }

    @Test
    void bidderHasEnoughBalance() {
        Bidder b = new Bidder("alice", "pass", "alice@test.com", 10_000_000);
        assertTrue(b.hasEnoughBalance(10_000_000));
        assertFalse(b.hasEnoughBalance(10_000_001));
    }

    // ===== Seller =====

    @Test
    void sellerRoleIsSeller() {
        Seller s = new Seller("bob", "pass", "bob@test.com", "BobShop");
        assertEquals("Seller", s.getRole());
    }

    @Test
    void sellerDefaultReputationIsFive() {
        Seller s = new Seller("bob", "pass", "bob@test.com", "BobShop");
        assertEquals(5.0, s.getReputationScore());
    }

    @Test
    void sellerCreditIncreasesBalance() {
        Seller s = new Seller("bob", "pass", "bob@test.com", "BobShop", 0);
        s.credit(20_000_000);
        assertEquals(20_000_000, s.getBalance());
    }

    @Test
    void sellerDebitSucceedsWhenSufficientBalance() {
        Seller s = new Seller("bob", "pass", "bob@test.com", "BobShop", 10_000_000);
        assertTrue(s.debit(3_000_000));
        assertEquals(7_000_000, s.getBalance());
    }

    @Test
    void sellerDebitFailsWhenInsufficientBalance() {
        Seller s = new Seller("bob", "pass", "bob@test.com", "BobShop", 1_000);
        assertFalse(s.debit(5_000_000));
        assertEquals(1_000, s.getBalance());
    }

    // ===== Admin =====

    @Test
    void adminRoleIsAdmin() {
        Admin a = new Admin("admin", "pass", "admin@test.com", 1);
        assertEquals("Admin", a.getRole());
    }

    @Test
    void adminClearanceLevelIsStored() {
        Admin a = new Admin("admin", "pass", "admin@test.com", 2);
        assertEquals(2, a.getClearanceLevel());
    }

    @Test
    void userIdIsUniquePerInstance() {
        Bidder b1 = new Bidder("u1", "p", "u1@test.com");
        Bidder b2 = new Bidder("u2", "p", "u2@test.com");
        assertNotEquals(b1.getId(), b2.getId());
    }
}
