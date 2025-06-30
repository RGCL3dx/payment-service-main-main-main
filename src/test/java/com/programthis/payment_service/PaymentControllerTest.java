package com.programthis.payment_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.programthis.payment_service.controller.PaymentController;
import com.programthis.payment_service.entity.Payment;
import com.programthis.payment_service.service.PaymentProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
public class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentProcessingService paymentProcessingService;

    @Autowired
    private ObjectMapper objectMapper;

    private Payment payment;
    private PaymentProcessingService.PaymentRequest paymentRequest;


    @BeforeEach
    void setUp() {
        payment = new Payment();
        payment.setId(1L);
        payment.setOrderId("ORD-123");
        payment.setAmount(new BigDecimal("100.00"));
        payment.setPaymentMethod("Credit Card");
        payment.setPaymentStatus("COMPLETED");
        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setTransactionDate(LocalDateTime.now());

        paymentRequest = new PaymentProcessingService.PaymentRequest("ORD-123", new BigDecimal("100.00"), "card-details");
    }

    @Test
    void processPayment_Success() throws Exception {
        given(paymentProcessingService.processPayment(any(PaymentProcessingService.PaymentRequest.class))).willReturn(payment);

        mockMvc.perform(post("/api/v1/payments/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("COMPLETED"));
    }
    
    @Test
    void processPayment_Failed() throws Exception {
        payment.setPaymentStatus("FAILED");
        given(paymentProcessingService.processPayment(any(PaymentProcessingService.PaymentRequest.class))).willReturn(payment);

        mockMvc.perform(post("/api/v1/payments/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.paymentStatus").value("FAILED"));
    }

    @Test
    void getPaymentStatusByOrderId_Found() throws Exception {
        given(paymentProcessingService.getPaymentStatusByOrderId("ORD-123")).willReturn(payment);

        mockMvc.perform(get("/api/v1/payments/status/order/{orderId}", "ORD-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ORD-123"));
    }

    @Test
    void getPaymentStatusByOrderId_NotFound() throws Exception {
        given(paymentProcessingService.getPaymentStatusByOrderId("ORD-456")).willThrow(new RuntimeException("Payment not found"));

        mockMvc.perform(get("/api/v1/payments/status/order/{orderId}", "ORD-456"))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void getPaymentByTransactionId_Found() throws Exception {
        given(paymentProcessingService.getPaymentByTransactionId(payment.getTransactionId())).willReturn(payment);

        mockMvc.perform(get("/api/v1/payments/status/transaction/{transactionId}", payment.getTransactionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(payment.getTransactionId()));
    }

    @Test
    void getPaymentByTransactionId_NotFound() throws Exception {
        String transactionId = UUID.randomUUID().toString();
        given(paymentProcessingService.getPaymentByTransactionId(transactionId)).willThrow(new RuntimeException("Payment not found"));

        mockMvc.perform(get("/api/v1/payments/status/transaction/{transactionId}", transactionId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllPayments_Success() throws Exception {
        List<Payment> payments = Collections.singletonList(payment);
        given(paymentProcessingService.getAllPayments()).willReturn(payments);

        mockMvc.perform(get("/api/v1/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value("ORD-123"));
    }

    @Test
    void updatePayment_Success() throws Exception {
        given(paymentProcessingService.updatePayment(eq(1L), any(Payment.class))).willReturn(payment);

        mockMvc.perform(put("/api/v1/payments/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ORD-123"));
    }

    @Test
    void updatePayment_NotFound() throws Exception {
        given(paymentProcessingService.updatePayment(eq(2L), any(Payment.class))).willThrow(new RuntimeException("Payment not found"));

        mockMvc.perform(put("/api/v1/payments/{id}", 2L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payment)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletePayment_Success() throws Exception {
        doNothing().when(paymentProcessingService).deletePayment(1L);

        mockMvc.perform(delete("/api/v1/payments/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletePayment_NotFound() throws Exception {
        doThrow(new RuntimeException("Payment not found")).when(paymentProcessingService).deletePayment(2L);

        mockMvc.perform(delete("/api/v1/payments/{id}", 2L))
                .andExpect(status().isNotFound());
    }
}