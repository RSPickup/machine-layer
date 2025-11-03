package com.example.parcellocker.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.example.parcellocker.db.entities.Payment;

import java.util.List;
import java.util.UUID;

@Dao
public interface PaymentDao {

    @Query("SELECT * FROM payments WHERE id = :id LIMIT 1")
    Payment getById(UUID id);

    @Query("SELECT * FROM payments WHERE package_id = :packageId")
    List<Payment> getByPackageId(UUID packageId);

    @Query("SELECT * FROM payments WHERE package_id = :packageId AND payment_status = 'paid' LIMIT 1")
    Payment getPaidPaymentForPackage(UUID packageId);

    @Query("SELECT * FROM payments WHERE package_id = :packageId AND payment_status = 'pending' LIMIT 1")
    Payment getPendingPaymentForPackage(UUID packageId);

    @Query("SELECT * FROM payments WHERE payment_method = :paymentMethod")
    List<Payment> getByPaymentMethod(String paymentMethod);

    @Query("SELECT * FROM payments WHERE payment_status = :paymentStatus")
    List<Payment> getByPaymentStatus(String paymentStatus);

    // Cash payment specific queries
    @Query("SELECT * FROM payments WHERE payment_method = 'cash' AND payment_status = 'paid'")
    List<Payment> getPaidCashPayments();

    @Query("SELECT * FROM payments WHERE payment_method = 'cash' AND payment_status = 'pending'")
    List<Payment> getPendingCashPayments();

    @Query("SELECT SUM(amount_paid) FROM payments WHERE payment_method = 'cash' AND payment_status = 'paid'")
    Double getTotalCashCollected();

    @Query("SELECT SUM(change_given) FROM payments WHERE payment_method = 'cash' AND payment_status = 'paid'")
    Double getTotalChangeGiven();

    // Online payment specific queries
    @Query("SELECT * FROM payments WHERE payment_method = 'online' AND payment_status = 'paid'")
    List<Payment> getPaidOnlinePayments();

    @Query("SELECT * FROM payments WHERE transaction_id = :transactionId LIMIT 1")
    Payment getByTransactionId(String transactionId);

    @Query("SELECT * FROM payments WHERE payment_gateway = :gateway")
    List<Payment> getByPaymentGateway(String gateway);

    // General queries
    @Query("SELECT * FROM payments")
    List<Payment> getAll();

    @Query("SELECT * FROM payments WHERE sync_status = :syncStatus")
    List<Payment> getBySyncStatus(String syncStatus);

    @Query("SELECT * FROM payments WHERE created_at BETWEEN :startTime AND :endTime")
    List<Payment> getByTimeRange(Long startTime, Long endTime);

    @Insert
    void insert(Payment payment);

    @Insert
    void insertAll(List<Payment> payments);

    @Update
    void update(Payment payment);

    @Delete
    void delete(Payment payment);

    @Query("DELETE FROM payments WHERE id = :id")
    void deleteById(UUID id);

    // Payment lifecycle updates
    @Query("UPDATE payments SET payment_status = 'paid', completed_at = :completedAt, updated_at = :updatedAt WHERE id = :id")
    void markAsPaid(UUID id, Long completedAt, Long updatedAt);

    @Query("UPDATE payments SET payment_status = 'failed', updated_at = :updatedAt WHERE id = :id")
    void markAsFailed(UUID id, Long updatedAt);

    @Query("UPDATE payments SET amount_paid = :amountPaid, change_given = :changeGiven, cash_denominations = :cashDenominations, machine_cash_balance = :machineCashBalance, payment_status = 'paid', completed_at = :completedAt, updated_at = :updatedAt WHERE id = :id")
    void completeCashPayment(UUID id, Double amountPaid, Double changeGiven, String cashDenominations, Double machineCashBalance, Long completedAt, Long updatedAt);

    @Query("UPDATE payments SET transaction_id = :transactionId, payment_status = 'paid', completed_at = :completedAt, updated_at = :updatedAt WHERE id = :id")
    void completeOnlinePayment(UUID id, String transactionId, Long completedAt, Long updatedAt);

    // LiveData queries
    @Query("SELECT * FROM payments WHERE payment_status = 'pending'")
    LiveData<List<Payment>> getPendingPaymentsLive();

    @Query("SELECT * FROM payments WHERE payment_method = 'cash' AND payment_status = 'paid'")
    LiveData<List<Payment>> getPaidCashPaymentsLive();

    @Query("SELECT * FROM payments WHERE package_id = :packageId")
    LiveData<List<Payment>> getByPackageIdLive(UUID packageId);

    @Query("SELECT * FROM payments")
    LiveData<List<Payment>> getAllLive();

    @Query("UPDATE payments SET sync_status = :syncStatus WHERE id = :id")
    void updateSyncStatus(UUID id, String syncStatus);

    @Query("SELECT COUNT(*) FROM payments WHERE payment_status = 'pending'")
    int getPendingPaymentsCount();

    @Query("SELECT COUNT(*) FROM payments WHERE payment_method = 'cash' AND payment_status = 'paid'")
    int getPaidCashPaymentsCount();

    // CORRECTED: Algeria-specific payment queries (DZD currency)
    @Query("SELECT * FROM payments WHERE currency = 'DZD' AND payment_status = 'paid'")
    List<Payment> getPaidDZDPayments();

    @Query("SELECT * FROM payments WHERE currency = 'DZD' AND payment_method = 'cash' AND payment_status = 'paid'")
    List<Payment> getPaidCashPaymentsDZD();

    @Query("SELECT SUM(amount_paid) FROM payments WHERE currency = 'DZD' AND payment_method = 'cash' AND payment_status = 'paid'")
    Double getTotalCashCollectedDZD();

    @Query("SELECT SUM(change_given) FROM payments WHERE currency = 'DZD' AND payment_method = 'cash' AND payment_status = 'paid'")
    Double getTotalChangeGivenDZD();

    // CORRECTED: Insufficient payment tracking
    @Query("SELECT * FROM payments WHERE payment_status = 'failed' AND amount_paid < amount_required")
    List<Payment> getInsufficientPayments();

    @Query("SELECT COUNT(*) FROM payments WHERE payment_status = 'failed' AND amount_paid < amount_required")
    int getInsufficientPaymentsCount();

    // CORRECTED: Cash payment completion with insufficient payment handling
    @Query("UPDATE payments SET amount_paid = :amountPaid, change_given = :changeGiven, cash_denominations = :cashDenominations, machine_cash_balance = :machineCashBalance, payment_status = CASE WHEN :amountPaid >= amount_required THEN 'paid' ELSE 'failed' END, completed_at = :completedAt, updated_at = :updatedAt WHERE id = :id")
    void completeCashPaymentWithValidation(UUID id, Double amountPaid, Double changeGiven, String cashDenominations, Double machineCashBalance, Long completedAt, Long updatedAt);

    // LiveData for insufficient payments monitoring
    @Query("SELECT * FROM payments WHERE payment_status = 'failed' AND amount_paid < amount_required")
    LiveData<List<Payment>> getInsufficientPaymentsLive();
}
