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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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

    private Payment pagoDePrueba;
    private PaymentProcessingService.PaymentRequest solicitudDePagoDePrueba;
    private PaymentProcessingService.PaymentRequest solicitudDePagoFallo;


    @BeforeEach
    void setUp() {
        pagoDePrueba = new Payment();
        pagoDePrueba.setId(1L);
        pagoDePrueba.setOrderId("PEDIDO-001");
        pagoDePrueba.setAmount(BigDecimal.valueOf(100.00));
        pagoDePrueba.setPaymentMethod("Tarjeta de Crédito");
        pagoDePrueba.setPaymentStatus("COMPLETADO");
        pagoDePrueba.setTransactionId(UUID.randomUUID().toString());
        pagoDePrueba.setTransactionDate(LocalDateTime.now());

        solicitudDePagoDePrueba = new PaymentProcessingService.PaymentRequest(
                "PEDIDO-001",
                BigDecimal.valueOf(100.00),
                "detalles_tarjeta_exito"
        );

        solicitudDePagoFallo = new PaymentProcessingService.PaymentRequest(
                "PEDIDO-002",
                BigDecimal.valueOf(50.00),
                "detalles_tarjeta_fallo"
        );
    }

    @Test
    void procesarPago_exito() throws Exception {
        given(paymentProcessingService.processPayment(any(PaymentProcessingService.PaymentRequest.class))).willReturn(pagoDePrueba);

        mockMvc.perform(post("/api/v1/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudDePagoDePrueba)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("PEDIDO-001")) 
                .andExpect(jsonPath("$.paymentStatus").value("COMPLETADO"))
                .andExpect(jsonPath("$._links.self.href").exists()) 
                .andExpect(jsonPath("$._links.payment-by-order-id.href").exists());
    }

    @Test
    void procesarPago_fallo() throws Exception {
        Payment pagoFallido = new Payment();
        pagoFallido.setId(2L); // Un ID diferente para el pago fallido
        pagoFallido.setOrderId("PEDIDO-002");
        pagoFallido.setAmount(BigDecimal.valueOf(50.00));
        pagoFallido.setPaymentMethod("Tarjeta de Crédito/Débito");
        pagoFallido.setPaymentStatus("FALLIDO");
        pagoFallido.setTransactionId(null);
        pagoFallido.setTransactionDate(LocalDateTime.now());

        // Mock del servicio para devolver el pago fallido
        given(paymentProcessingService.processPayment(any(PaymentProcessingService.PaymentRequest.class))).willReturn(pagoFallido);

        mockMvc.perform(post("/api/v1/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudDePagoFallo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.paymentStatus").value("FALLIDO"))
                .andExpect(jsonPath("$.orderId").value("PEDIDO-002"))
                .andExpect(jsonPath("$._links.self.href").exists()); 
    }

    @Test
    void obtenerPagoPorId_encontrado() throws Exception {
        given(paymentProcessingService.getPaymentById(1L)).willReturn(Optional.of(pagoDePrueba));

        mockMvc.perform(get("/api/v1/payments/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.orderId").value("PEDIDO-001"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.by-order-id.href").exists());
    }

    @Test
    void obtenerPagoPorId_noEncontrado() throws Exception {
        given(paymentProcessingService.getPaymentById(99L)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/payments/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenerEstadoPagoPorIdPedido_encontrado() throws Exception {
        given(paymentProcessingService.getPaymentStatusByOrderId("PEDIDO-001")).willReturn(pagoDePrueba);

        mockMvc.perform(get("/api/v1/payments/status/order/{orderId}", "PEDIDO-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("PEDIDO-001"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.by-id.href").exists());
    }

    @Test
    void obtenerEstadoPagoPorIdPedido_noEncontrado() throws Exception {
        given(paymentProcessingService.getPaymentStatusByOrderId("PEDIDO_INEXISTENTE")).willThrow(new RuntimeException("Pago no encontrado para ID de pedido: PEDIDO_INEXISTENTE"));

        mockMvc.perform(get("/api/v1/payments/status/order/{orderId}", "PEDIDO_INEXISTENTE"))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenerPagoPorIdTransaccion_encontrado() throws Exception {
        given(paymentProcessingService.getPaymentByTransactionId(pagoDePrueba.getTransactionId())).willReturn(pagoDePrueba);

        mockMvc.perform(get("/api/v1/payments/status/transaction/{transactionId}", pagoDePrueba.getTransactionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(pagoDePrueba.getTransactionId()))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.by-id.href").exists());
    }

    @Test
    void obtenerPagoPorIdTransaccion_noEncontrado() throws Exception {
        String idTransaccionInexistente = UUID.randomUUID().toString();
        given(paymentProcessingService.getPaymentByTransactionId(idTransaccionInexistente)).willThrow(new RuntimeException("Pago no encontrado para ID de transacción: " + idTransaccionInexistente));

        mockMvc.perform(get("/api/v1/payments/status/transaction/{transactionId}", idTransaccionInexistente))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenerTodosLosPagos_exito() throws Exception {
        Payment otroPago = new Payment();
        otroPago.setId(2L);
        otroPago.setOrderId("PEDIDO-002");
        otroPago.setAmount(BigDecimal.valueOf(200.00));
        otroPago.setPaymentMethod("Tarjeta de Débito");
        otroPago.setPaymentStatus("PENDIENTE");
        otroPago.setTransactionDate(LocalDateTime.now());
        otroPago.setTransactionId(UUID.randomUUID().toString());

        List<Payment> pagos = Arrays.asList(pagoDePrueba, otroPago);
        given(paymentProcessingService.getAllPayments()).willReturn(pagos);

        mockMvc.perform(get("/api/v1/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.paymentList[0].orderId").value("PEDIDO-001"))
                .andExpect(jsonPath("$._embedded.paymentList[1].orderId").value("PEDIDO-002"))
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    void actualizarPago_exito() throws Exception {
        Payment detallesActualizados = new Payment();
        detallesActualizados.setId(1L);
        detallesActualizados.setOrderId("PEDIDO-001-ACTUALIZADO");
        detallesActualizados.setAmount(BigDecimal.valueOf(150.00));
        detallesActualizados.setPaymentMethod("PayPal");
        detallesActualizados.setPaymentStatus("REEMBOLSADO");
        detallesActualizados.setTransactionId(pagoDePrueba.getTransactionId());
        detallesActualizados.setTransactionDate(pagoDePrueba.getTransactionDate());

        given(paymentProcessingService.updatePayment(eq(1L), any(Payment.class))).willReturn(detallesActualizados);

        mockMvc.perform(put("/api/v1/payments/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(detallesActualizados)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.orderId").value("PEDIDO-001-ACTUALIZADO"))
                .andExpect(jsonPath("$.paymentStatus").value("REEMBOLSADO"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.all-payments.href").exists());
    }

    @Test
    void actualizarPago_noEncontrado() throws Exception {
        Payment detallesActualizados = new Payment();
        detallesActualizados.setOrderId("PEDIDO_INEXISTENTE_ACTUALIZADO");

        given(paymentProcessingService.updatePayment(eq(99L), any(Payment.class))).willThrow(new RuntimeException("Pago no encontrado para ID: 99"));

        mockMvc.perform(put("/api/v1/payments/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(detallesActualizados)))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarPago_exito() throws Exception {
        doNothing().when(paymentProcessingService).deletePayment(1L);

        mockMvc.perform(delete("/api/v1/payments/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void eliminarPago_noEncontrado() throws Exception {
        doThrow(new RuntimeException("Pago no encontrado para ID: 99")).when(paymentProcessingService).deletePayment(99L);

        mockMvc.perform(delete("/api/v1/payments/{id}", 99L))
                .andExpect(status().isNotFound());
    }
}