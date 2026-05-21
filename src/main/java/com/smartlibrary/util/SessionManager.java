package com.smartlibrary.util;

import com.smartlibrary.model.Role;
import com.smartlibrary.model.User;

import java.util.Optional;

public class SessionManager {
    private User currentUser;

    // Stores the user as the active session.
    // Parameters: user is the user account being checked or updated.
    public void login(User user) {
        this.currentUser = user;
    }

    // Logs out the current user and returns to the login screen.
    public void logout() {
        this.currentUser = null;
    }

    // Returns the user currently logged in, if any.
    public Optional<User> currentUser() {
        return Optional.ofNullable(currentUser);
    }

    // Checks whether the current user has a specific role.
    // Parameters: role is the role being checked or assigned.
    public boolean hasRole(Role role) {
        return currentUser != null && currentUser.getRole() == role;
    }

    // Checks whether the current user may manage user accounts.
    public boolean canManageUsers() {
        return hasRole(Role.ADMIN);
    }

    // Checks whether the current user may manage book records.
    public boolean canManageBooks() {
        return hasRole(Role.ADMIN) || hasRole(Role.LIBRARIAN);
    }

    // Checks whether the current user may issue and return books.
    public boolean canBorrowReturn() {
        return hasRole(Role.ADMIN) || hasRole(Role.LIBRARIAN);
    }
}
