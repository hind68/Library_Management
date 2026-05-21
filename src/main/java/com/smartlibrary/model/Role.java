package com.smartlibrary.model;

public enum Role {
    ADMIN,
    LIBRARIAN,
    MEMBER;

    // Returns a readable label for the role.
    public String displayName() {
        return switch (this) {
            case ADMIN -> "Administrator";
            case LIBRARIAN -> "Librarian";
            case MEMBER -> "Member / Student";
        };
    }
}
