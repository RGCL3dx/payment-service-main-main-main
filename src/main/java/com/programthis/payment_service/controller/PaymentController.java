package com.programthis.payment_service.controller;

import com.programthis.payment_service.entity.Payment;
import com.programthis.payment_service.service.PaymentProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException; // Esta importación es CRÍTICA

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    @Autowired
    private PaymentProcessingService paymentProcessingService;

    @PostMapping("/process")
    public ResponseEntity<EntityModel<Payment>> processPayment(@RequestBody PaymentProcessingService.PaymentRequest paymentRequest) {
        Payment payment = paymentProcessingService.processPayment(paymentRequest);
        if ("COMPLETED".equals(payment.getPaymentStatus())) {
            EntityModel<Payment> resource = EntityModel.of(payment,
                    linkTo(methodOn(PaymentController.class).getPaymentByTransactionId(payment.getTransactionId())).withSelfRel(),
                    linkTo(methodOn(PaymentController.class).getPaymentStatusByOrderId(payment.getOrderId())).withRel("payment-by-order-id"),
                    linkTo(methodOn(PaymentController.class).getAllPayments()).withRel("all-payments"));
            return ResponseEntity.ok(resource);
        } else {
            EntityModel<Payment> resource = EntityModel.of(payment,
                    linkTo(methodOn(PaymentController.class).processPayment(null)).withSelfRel());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);
        }
    }

    @GetMapping("/{id}")
    public EntityModel<Payment> getPaymentById(@PathVariable Long id) {
        Payment payment = paymentProcessingService.getPaymentById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found for ID: " + id));

        return EntityModel.of(payment,
                linkTo(methodOn(PaymentController.class).getPaymentById(id)).withSelfRel(),
                linkTo(methodOn(PaymentController.class).getPaymentStatusByOrderId(payment.getOrderId())).withRel("by-order-id"),
                payment.getTransactionId() != null ?
                    linkTo(methodOn(PaymentController.class).getPaymentByTransactionId(payment.getTransactionId())).withRel("by-transaction-id") : null,
                linkTo(methodOn(PaymentController.class).getAllPayments()).withRel("all-payments"));
    }

    @GetMapping("/status/order/{orderId}")
    public EntityModel<Payment> getPaymentStatusByOrderId(@PathVariable String orderId) {
        try {
            Payment payment = paymentProcessingService.getPaymentStatusByOrderId(orderId);
            return EntityModel.of(payment,
                    linkTo(methodOn(PaymentController.class).getPaymentStatusByOrderId(orderId)).withSelfRel(),
                    linkTo(methodOn(PaymentController.class).getPaymentById(payment.getId())).withRel("by-id"),
                    payment.getTransactionId() != null ?
                        linkTo(methodOn(PaymentController.class).getPaymentByTransactionId(payment.getTransactionId())).withRel("by-transaction-id") : null,
                    linkTo(methodOn(PaymentController.class).getAllPayments()).withRel("all-payments"));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/status/transaction/{transactionId}")
    public EntityModel<Payment> getPaymentByTransactionId(@PathVariable String transactionId) {
        try {
            Payment payment = paymentProcessingService.getPaymentByTransactionId(transactionId);
            return EntityModel.of(payment,
                    linkTo(methodOn(PaymentController.class).getPaymentByTransactionId(transactionId)).withSelfRel(),
                    linkTo(methodOn(PaymentController.class).getPaymentById(payment.getId())).withRel("by-id"),
                    linkTo(methodOn(PaymentController.class).getPaymentStatusByOrderId(payment.getOrderId())).withRel("by-order-id"),
                    linkTo(methodOn(PaymentController.class).getAllPayments()).withRel("all-payments"));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping
    public CollectionModel<EntityModel<Payment>> getAllPayments() {
        List<EntityModel<Payment>> payments = paymentProcessingService.getAllPayments().stream()
                .map(payment -> EntityModel.of(payment,
                        linkTo(methodOn(PaymentController.class).getPaymentById(payment.getId())).withSelfRel(),
                        linkTo(methodOn(PaymentController.class).getPaymentStatusByOrderId(payment.getOrderId())).withRel("by-order-id"),
                        payment.getTransactionId() != null ?
                            linkTo(methodOn(PaymentController.class).getPaymentByTransactionId(payment.getTransactionId())).withRel("by-transaction-id") : null))
                .collect(Collectors.toList());

        return CollectionModel.of(payments,
                linkTo(methodOn(PaymentController.class).getAllPayments()).withSelfRel());
    }

    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<Payment>> updatePayment(@PathVariable Long id, @RequestBody Payment paymentDetails) {
        try {
            Payment updatedPayment = paymentProcessingService.updatePayment(id, paymentDetails);
            EntityModel<Payment> resource = EntityModel.of(updatedPayment,
                    linkTo(methodOn(PaymentController.class).getPaymentById(updatedPayment.getId())).withSelfRel(),
                    linkTo(methodOn(PaymentController.class).getAllPayments()).withRel("all-payments"));
            return ResponseEntity.ok(resource);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable Long id) {
        try {
            paymentProcessingService.deletePayment(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}