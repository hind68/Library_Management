package com.smartlibrary.model;

// Stores the summary numbers shown on the main dashboard.
// Each value represents one visible dashboard metric.
public record DashboardStats(
        int totalBooks,
        int borrowedBooks,
        int overdueBooks,
        int activeUsers,
        int todayBorrowings,
        int pendingReturns,
        int unreadNotifications,
        int reservations
) {}
