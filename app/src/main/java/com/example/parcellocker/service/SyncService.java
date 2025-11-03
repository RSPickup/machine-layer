package com.example.parcellocker.service;

import android.app.Application;
import android.content.Context;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.parcellocker.db.repository.*;
import com.example.parcellocker.db.entities.*;

public class SyncService {

    private static final long OFFLINE_SYNC_INTERVAL = 15 * 60 * 1000; // 15 minutes
    private boolean isOnlineMode = false;
    private Timer syncTimer;
    private ExecutorService executor;

    private PackageRepository packageRepository;
    private PaymentRepository paymentRepository;
    private AuditLogRepository auditLogRepository;
    private Context context;

    public SyncService(Context context) {
        this.context = context;
        this.executor = Executors.newFixedThreadPool(2);
        initializeRepositories();
        initializeSyncService();
    }

    private void initializeRepositories() {
        if (context instanceof Application) {
            Application app = (Application) context;
            this.packageRepository = new PackageRepository(app);
            this.paymentRepository = new PaymentRepository(app);
            this.auditLogRepository = new AuditLogRepository(app);
        }
    }

    // CORRECTED: Initialize sync service with internet-dependent strategy
    public void initializeSyncService() {
        this.isOnlineMode = checkInternetConnectivity();

        if (isOnlineMode) {
            enableRealTimeSyncMode();
        } else {
            schedulePeriodicSync();
        }
    }

    // CORRECTED: Real-time sync when internet is available
    private void enableRealTimeSyncMode() {
        System.out.println("Online mode: Real-time sync enabled");

        // Cancel any existing periodic sync
        if (syncTimer != null) {
            syncTimer.cancel();
            syncTimer = null;
        }

        // Note: In real implementation, repositories would have callbacks
        // for immediate sync after each operation
    }

    // CORRECTED: 15-minute periodic sync when offline
    private void schedulePeriodicSync() {
        System.out.println("Offline mode: 15-minute periodic sync enabled");

        if (syncTimer != null) {
            syncTimer.cancel();
        }

        syncTimer = new Timer("SyncService");
        syncTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (checkInternetConnectivity()) {
                    // Internet is back - switch to online mode
                    isOnlineMode = true;
                    enableRealTimeSyncMode();

                    // Sync all pending data immediately
                    performFullSync();
                } else {
                    // Still offline - attempt periodic sync
                    performOfflineSync();
                }
            }
        }, OFFLINE_SYNC_INTERVAL, OFFLINE_SYNC_INTERVAL);
    }

    // CORRECTED: Immediate sync after operations when online
    public void syncPackageImmediately(Package pkg) {
        if (isOnlineMode) {
            executor.execute(() -> {
                try {
                    // Sync to cloud API immediately
                    boolean success = syncPackageToCloud(pkg);
                    if (success && packageRepository != null) {
                        packageRepository.updateSyncStatus(pkg.getId(), "synced");
                    }
                } catch (Exception e) {
                    // Internet lost - switch to offline mode
                    isOnlineMode = false;
                    schedulePeriodicSync();
                }
            });
        }
    }

    public void syncPaymentImmediately(Payment payment) {
        if (isOnlineMode) {
            executor.execute(() -> {
                try {
                    // Critical: Cash payments must sync immediately for accounting
                    if (payment.isCashPayment() && payment.isPaid()) {
                        boolean success = syncCashPaymentToCloud(payment);
                        if (success && paymentRepository != null) {
                            paymentRepository.updateSyncStatus(payment.getId(), "synced");
                        }
                    }
                } catch (Exception e) {
                    isOnlineMode = false;
                    schedulePeriodicSync();
                }
            });
        }
    }

    // CORRECTED: Offline sync operations (15-minute intervals)
    private void performOfflineSync() {
        executor.execute(() -> {
            try {
                syncPendingPackages();
                syncPendingPayments();
                syncPendingAuditLogs();
                receiveCloudUpdates();
            } catch (Exception e) {
                System.err.println("Offline sync failed, will retry in 15 minutes: " + e.getMessage());
            }
        });
    }

    private void performFullSync() {
        executor.execute(() -> {
            try {
                System.out.println("Performing full sync after reconnection...");
                syncPendingPackages();
                syncPendingPayments();
                syncPendingAuditLogs();
                receiveCloudUpdates();
                System.out.println("Full sync completed");
            } catch (Exception e) {
                System.err.println("Full sync failed: " + e.getMessage());
            }
        });
    }

    private void syncPendingPackages() {
        // Implementation would sync all local_only and pending_sync packages
        System.out.println("Syncing pending packages...");
    }

    private void syncPendingPayments() {
        // Priority: Cash payments for accounting accuracy
        System.out.println("Syncing pending payments (priority: cash)...");
    }

    private void syncPendingAuditLogs() {
        System.out.println("Syncing pending audit logs...");
    }

    private void receiveCloudUpdates() {
        // Get new packages assigned to this machine with pre-assigned doors
        System.out.println("Receiving cloud updates...");
    }

    // Placeholder methods - would be implemented with actual cloud API
    private boolean syncPackageToCloud(Package pkg) {
        // Simulate API call
        return true;
    }

    private boolean syncCashPaymentToCloud(Payment payment) {
        // Simulate API call for cash payment sync
        return true;
    }

    private boolean checkInternetConnectivity() {
        // Placeholder - would check actual internet connectivity
        // For testing, simulate connectivity changes
        return Math.random() > 0.3; // 70% chance of being online
    }

    public void shutdown() {
        if (syncTimer != null) {
            syncTimer.cancel();
        }
        if (executor != null) {
            executor.shutdown();
        }
    }

    // Getters for status monitoring
    public boolean isOnlineMode() {
        return isOnlineMode;
    }

    public String getSyncModeStatus() {
        return isOnlineMode ? "Real-time sync (Online)" : "15-minute periodic sync (Offline)";
    }
}
