package com.programthis.payment_service.service;

import com.programthis.payment_service.entity.Payment;
import com.programthis.payment_service.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentProcessingService {

    @Autowired
    private PaymentRepository paymentRepository;

    public static class PaymentRequest {
        private String idPedido;
        private BigDecimal monto;
        private String detallesTarjeta;

        public PaymentRequest(String idPedido, BigDecimal monto, String detallesTarjeta) {
            this.idPedido = idPedido;
            this.monto = monto;
            this.detallesTarjeta = detallesTarjeta;
        }

        public String getIdPedido() {
            return idPedido;
        }

        public BigDecimal getMonto() {
            return monto;
        }

        public String getDetallesTarjeta() {
            return detallesTarjeta;
        }
    }

    public Payment processPayment(PaymentRequest solicitudDePago) {
        Payment pago = new Payment();
        pago.setOrderId(solicitudDePago.getIdPedido());
        pago.setAmount(solicitudDePago.getMonto());
        pago.setPaymentMethod("Tarjeta de Crédito/Débito"); 

        if (solicitudDePago.getDetallesTarjeta().contains("fallo")) { 
            pago.setPaymentStatus("FALLIDO");
            pago.setTransactionId(null);
        } else {
            pago.setPaymentStatus("COMPLETADO");
            pago.setTransactionId(UUID.randomUUID().toString()); 
        }
        pago.setTransactionDate(LocalDateTime.now());

        return paymentRepository.save(pago);
    }

    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }

    public Payment getPaymentStatusByOrderId(String idPedido) {
        Optional<Payment> pago = paymentRepository.findByOrderId(idPedido);
        return pago.orElseThrow(() -> new RuntimeException("Pago no encontrado para ID de pedido: " + idPedido));
    }

    public Payment getPaymentByTransactionId(String idTransaccion) {
        Optional<Payment> pago = paymentRepository.findByTransactionId(idTransaccion);
        return pago.orElseThrow(() -> new RuntimeException("Pago no encontrado para ID de transacción: " + idTransaccion));
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Payment updatePayment(Long id, Payment detallesPago) {
        return paymentRepository.findById(id).map(pagoExistente -> {
            pagoExistente.setOrderId(detallesPago.getOrderId());
            pagoExistente.setAmount(detallesPago.getAmount());
            pagoExistente.setPaymentMethod(detallesPago.getPaymentMethod());
            pagoExistente.setPaymentStatus(detallesPago.getPaymentStatus());
            pagoExistente.setTransactionId(detallesPago.getTransactionId());
            pagoExistente.setTransactionDate(detallesPago.getTransactionDate());
            return paymentRepository.save(pagoExistente);
        }).orElseThrow(() -> new RuntimeException("Pago no encontrado para ID: " + id));
    }

    public void deletePayment(Long id) {
        if (!paymentRepository.existsById(id)) {
            throw new RuntimeException("Pago no encontrado para ID: " + id);
        }
        paymentRepository.deleteById(id);
    }
}