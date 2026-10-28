package com.smartlibrary.service;

import com.smartlibrary.dao.DatabaseManager;
import com.smartlibrary.model.Role;
import com.smartlibrary.model.User;
import com.smartlibrary.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceSecurityTest {
    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(new DatabaseManager(), true);
    }

    @Test
    void successfulLegacyLoginUpgradesHashWithoutLosingAccount() {
        User adminBefore = service.findAll().stream().filter(user -> user.getId() == 1).findFirst().orElseThrow();
        assertTrue(PasswordUtil.needsUpgrade(adminBefore.getPasswordHash()));

        User authenticated = service.authenticate("admin@library.edu", "admin123").orElseThrow();

        assertEquals(1, authenticated.getId());
        assertFalse(PasswordUtil.needsUpgrade(authenticated.getPasswordHash()));
        assertTrue(PasswordUtil.matches("admin123", authenticated.getPasswordHash()));
        assertTrue(service.authenticate("admin@library.edu", "admin123").isPresent());
    }

    @Test
    void invalidOrInactiveAccountsCannotAuthenticate() {
        assertTrue(service.authenticate("admin@library.edu", "wrong").isEmpty());
        assertTrue(service.authenticate(null, "admin123").isEmpty());
        User inactive = service.findAll().stream().filter(user -> user.getId() == 1).findFirst().orElseThrow();
        inactive.setActive(false);
        assertTrue(service.authenticate("admin@library.edu", "admin123").isEmpty());
    }

    @Test
    void memberCannotUseAdministrativeUserOperations() {
        User member = new User(99, "Member", "member@test.invalid", "hash", Role.MEMBER, true, LocalDateTime.now());
        User target = service.findAll().get(0);

        assertThrows(SecurityException.class,
                () -> service.createUser(member, "New Admin", "new@test.invalid", "password", Role.ADMIN));
        assertThrows(SecurityException.class, () -> service.toggleActive(member, target));
    }

    @Test
    void librarianCannotCreatePrivilegedAccount() {
        User librarian = new User(98, "Librarian", "lib@test.invalid", "hash", Role.LIBRARIAN, true, LocalDateTime.now());

        assertThrows(SecurityException.class,
                () -> service.createUser(librarian, "New Admin", "new@test.invalid", "password", Role.ADMIN));
    }
}
