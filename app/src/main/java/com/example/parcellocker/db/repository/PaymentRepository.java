package com.example.parcellocker.db.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.example.parcellocker.db.MachineDatabase;
import com.example.parcellocker.db.dao.PaymentDao;
import com.example.parcellocker.db.entities.Payment;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for Payment operations.
 * Handles both online payments (API-based) and cash payments (machine-local).
 */
public class PaymentRepository {

    private PaymentDao paymentDao;
    private ExecutorService executor;

    public PaymentRepository(Application application) {
        MachineDatabase database = MachineDatabase.getInstance(application);
        paymentDao = database.paymentDao();
        executor = Executors.newFixedThreadPool(4);
    }

    // Insert operations
    public void insert(Payment payment) {
        executor.execute(() -> paymentDao.insert(payment));
    }

    public void insertAll(List<Payment> payments) {
        executor.execute(() -> paymentDao.insertAll(payments));
    }

    // Update operations
    public void update(Payment payment) {
        executor.execute(() -> paymentDao.update(payment));
    }

    public void updateSyncStatus(UUID id, String syncStatus) {
        executor.execute(() -> paymentDao.updateSyncStatus(id, syncStatus));
    }

    // Payment lifecycle operations
    public void markAsPaid(UUID paymentId) {
        executor.execute(() -> {
            long now = System.currentTimeMillis();
            paymentDao.markAsPaid(paymentId, now, now);
        });
    }

    public void markAsFailed(UUID paymentId) {
        executor.execute(() -> paymentDao.markAsFailed(paymentId, System.currentTimeMillis()));
    }

    public void completeCashPayment(UUID paymentId, Double amountPaid, Double changeGiven,
                                   String cashDenominations, Double machineCashBalance) {
        executor.execute(() -> {
            long now = System.currentTimeMillis();
            paymentDao.completeCashPayment(paymentId, amountPaid, changeGiven,
                                         cashDenominations, machineCashBalance, now, now);
        });
    }

    public void completeOnlinePayment(UUID paymentId, String transactionId) {
        executor.execute(() -> {
            long now = System.currentTimeMillis();
            paymentDao.completeOnlinePayment(paymentId, transactionId, now, now);
        });
    }

    // Delete operations
    public void delete(Payment payment) {
        executor.execute(() -> paymentDao.delete(payment));
    }

    public void deleteById(UUID id) {
        executor.execute(() -> paymentDao.deleteById(id));
    }

    // Query operations (synchronous)
    public Payment getById(UUID id) {
        return paymentDao.getById(id);
    }

    public List<Payment> getByPackageId(UUID packageId) {
        return paymentDao.getByPackageId(packageId);
    }

    public Payment getPaidPaymentForPackage(UUID packageId) {
        return paymentDao.getPaidPaymentForPackage(packageId);
    }

    public Payment getPendingPaymentForPackage(UUID packageId) {
        return paymentDao.getPendingPaymentForPackage(packageId);
    }

    public List<Payment> getByPaymentMethod(String paymentMethod) {
        return paymentDao.getByPaymentMethod(paymentMethod);
    }

    public List<Payment> getByPaymentStatus(String paymentStatus) {
        return paymentDao.getByPaymentStatus(paymentStatus);
    }

    // Cash payment operations
    public List<Payment> getPaidCashPayments() {
        return paymentDao.getPaidCashPayments();
    }

    public List<Payment> getPendingCashPayments() {
        return paymentDao.getPendingCashPayments();
    }

    public Double getTotalCashCollected() {
        return paymentDao.getTotalCashCollected();
    }

    public Double getTotalChangeGiven() {
        return paymentDao.getTotalChangeGiven();
    }

    public int getPaidCashPaymentsCount() {
        return paymentDao.getPaidCashPaymentsCount();
    }

    // Online payment operations
    public List<Payment> getPaidOnlinePayments() {
        return paymentDao.getPaidOnlinePayments();
    }

    public Payment getByTransactionId(String transactionId) {
        return paymentDao.getByTransactionId(transactionId);
    }

    public List<Payment> getByPaymentGateway(String gateway) {
        return paymentDao.getByPaymentGateway(gateway);
    }

    // General queries
    public List<Payment> getAll() {
        return paymentDao.getAll();
    }

    public List<Payment> getBySyncStatus(String syncStatus) {
        return paymentDao.getBySyncStatus(syncStatus);
    }

    public List<Payment> getByTimeRange(Long startTime, Long endTime) {
        return paymentDao.getByTimeRange(startTime, endTime);
    }

    public int getPendingPaymentsCount() {
        return paymentDao.getPendingPaymentsCount();
    }

    // LiveData operations (for UI)
    public LiveData<List<Payment>> getPendingPaymentsLive() {
        return paymentDao.getPendingPaymentsLive();
    }

    public LiveData<List<Payment>> getPaidCashPaymentsLive() {
        return paymentDao.getPaidCashPaymentsLive();
    }

    public LiveData<List<Payment>> getByPackageIdLive(UUID packageId) {
        return paymentDao.getByPackageIdLive(packageId);
    }

    public LiveData<List<Payment>> getAllLive() {
        return paymentDao.getAllLive();
    }

    // Business logic helpers
    public boolean isPackagePaid(UUID packageId) {
        Payment paidPayment = getPaidPaymentForPackage(packageId);
        return paidPayment != null;
    }

    // CORRECTED: Algeria-specific payment creation (DZD currency)
    public Payment createCashPaymentDZD(UUID packageId, Double amount) {
        Payment payment = new Payment(packageId, amount, "DZD");
        insert(payment);
        return payment;
    }

    public Payment createOnlinePaymentDZD(UUID packageId, Double amount, String gateway) {
        Payment payment = new Payment(packageId, amount, "DZD", gateway);
        insert(payment);
        return payment;
    }

    // CORRECTED: Cash payment processing with insufficient payment handling
    public boolean processCashPaymentDZD(UUID paymentId, Double receivedAmount, String denominations, Double machineBalance) {
        Payment payment = getById(paymentId);
        if (payment == null) return false;

        if (payment.isSufficientPayment(receivedAmount)) {
            // Sufficient payment - calculate change and complete
            Double change = payment.calculateChange(receivedAmount);
            completeCashPayment(paymentId, receivedAmount, change, denominations, machineBalance);
            return true;
        } else {
            // Insufficient payment - mark as failed, all money should be returned
            payment.markAsInsufficientAndFailed(receivedAmount);
            update(payment);
            return false;
        }
    }

    // CORRECTED: Internet-dependent payment options
    public boolean isOnlinePaymentAvailable() {
        return checkInternetConnectivity();
    }

    private boolean checkInternetConnectivity() {
        // This would be implemented with actual connectivity check
        // For now, return true as placeholder
        return true;
    }

    public void shutdown() {
        executor.shutdown();
    }
}
