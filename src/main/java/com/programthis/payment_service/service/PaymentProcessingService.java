package com.programthis.payment_service.service;

import com.programthis.payment_service.entity.Payment;
import com.programthis.payment_service.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentProcessingService {

    @Autowired
    private PaymentRepository paymentRepository;

    public static record PaymentRequest(String orderId, BigDecimal amount, String paymentMethodDetails) {}

    public Payment processPayment(PaymentRequest paymentRequest) {

        Payment payment = new Payment();
        payment.setOrderId(paymentRequest.orderId());
        payment.setAmount(paymentRequest.amount());
        // A more robust implementation would parse paymentMethodDetails to set the actual payment method
        payment.setPaymentMethod("PROCESSED_METHOD");
        payment.setTransactionDate(LocalDateTime.now());

        boolean paymentSuccessful = simulatePaymentGatewayInteraction(paymentRequest.paymentMethodDetails());

        if (paymentSuccessful) {
            payment.setPaymentStatus("COMPLETED");
            payment.setTransactionId(UUID.randomUUID().toString()); // Simulate actual transaction ID from gateway
        } else {
            payment.setPaymentStatus("FAILED");
            // If the payment fails before getting a transaction ID, don't assign one.
            // Or, you might store an error code from the gateway here.
        }
        return paymentRepository.save(payment);
    }

    private boolean simulatePaymentGatewayInteraction(String paymentMethodDetails) {
        System.out.println("Simulating payment gateway interaction for: " + paymentMethodDetails);
        // This is a simple simulation; in a real app, you'd integrate with a payment gateway API.
        return !paymentMethodDetails.contains("fail");
    }

    public Payment getPaymentStatusByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order ID: " + orderId));
    }

    public Payment getPaymentByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Payment not found for transaction ID: " + transactionId));
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Payment updatePayment(Long id, Payment paymentDetails) {
        return paymentRepository.findById(id)
                .map(payment -> {
                    payment.setOrderId(paymentDetails.getOrderId());
                    payment.setAmount(paymentDetails.getAmount());
                    payment.setPaymentMethod(paymentDetails.getPaymentMethod());
                    payment.setPaymentStatus(paymentDetails.getPaymentStatus());
                    payment.setTransactionId(paymentDetails.getTransactionId());
                    payment.setTransactionDate(paymentDetails.getTransactionDate()); // Allow updating transaction date if needed
                    return paymentRepository.save(payment);
                })
                .orElseThrow(() -> new RuntimeException("Payment not found for ID: " + id));
    }

    public void deletePayment(Long id) {
        if (!paymentRepository.existsById(id)) {
            throw new RuntimeException("Payment not found for ID: " + id);
        }
        paymentRepository.deleteById(id);
    }
}