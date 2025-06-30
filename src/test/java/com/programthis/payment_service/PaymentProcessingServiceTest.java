package com.programthis.payment_service;

import com.programthis.payment_service.entity.Payment;
import com.programthis.payment_service.repository.PaymentRepository;
import com.programthis.payment_service.service.PaymentProcessingService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentProcessingServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentProcessingService paymentProcessingService;

    private Payment pagoDePrueba;
    private PaymentProcessingService.PaymentRequest solicitudDePagoDePrueba;

    @BeforeEach
    void setUp() {
        pagoDePrueba = new Payment();
        pagoDePrueba.setId(1L);
        pagoDePrueba.setOrderId("PEDIDO-001");
        pagoDePrueba.setAmount(BigDecimal.valueOf(100.00));
        pagoDePrueba.setPaymentMethod("Tarjeta de Crédito");
        pagoDePrueba.setPaymentStatus("COMPLETADO");
        pagoDePrueba.setTransactionId("TRANSACCION-XYZ");
        pagoDePrueba.setTransactionDate(LocalDateTime.now());

        solicitudDePagoDePrueba = new PaymentProcessingService.PaymentRequest(
                "PEDIDO-001",
                BigDecimal.valueOf(100.00),
                "Detalles de Tarjeta de Crédito"
        );
    }

    @Test
    void procesarPago_exito() {
        when(paymentRepository.save(any(Payment.class))).thenReturn(pagoDePrueba);

        Payment resultado = paymentProcessingService.processPayment(solicitudDePagoDePrueba);

        assertNotNull(resultado);
        assertEquals("COMPLETADO", resultado.getPaymentStatus());
        assertNotNull(resultado.getTransactionId());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void procesarPago_fallo() {
        PaymentProcessingService.PaymentRequest solicitudFallo = new PaymentProcessingService.PaymentRequest(
                "PEDIDO-002",
                BigDecimal.valueOf(50.00),
                "detalles_tarjeta_fallo"
        );

        Payment pagoFallido = new Payment();
        pagoFallido.setOrderId("PEDIDO-002");
        pagoFallido.setAmount(BigDecimal.valueOf(50.00));
        pagoFallido.setPaymentMethod("METODO_PROCESADO");
        pagoFallido.setPaymentStatus("FALLIDO");
        pagoFallido.setTransactionDate(LocalDateTime.now());
        pagoFallido.setTransactionId(null);

        when(paymentRepository.save(any(Payment.class))).thenReturn(pagoFallido);

        Payment resultado = paymentProcessingService.processPayment(solicitudFallo);

        assertNotNull(resultado);
        assertEquals("FALLIDO", resultado.getPaymentStatus());
        assertNull(resultado.getTransactionId());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void obtenerEstadoPagoPorIdPedido_encontrado() {
        when(paymentRepository.findByOrderId("PEDIDO-001")).thenReturn(Optional.of(pagoDePrueba));

        Payment resultado = paymentProcessingService.getPaymentStatusByOrderId("PEDIDO-001");

        assertNotNull(resultado);
        assertEquals("PEDIDO-001", resultado.getOrderId());
        verify(paymentRepository, times(1)).findByOrderId("PEDIDO-001");
    }

    @Test
    void obtenerEstadoPagoPorIdPedido_noEncontrado() {
        when(paymentRepository.findByOrderId("PEDIDO_INEXISTENTE")).thenReturn(Optional.empty());

        RuntimeException excepcionLanzada = assertThrows(RuntimeException.class, () -> {
            paymentProcessingService.getPaymentStatusByOrderId("PEDIDO_INEXISTENTE");
        });

        assertTrue(excepcionLanzada.getMessage().contains("Payment not found for order ID: PEDIDO_INEXISTENTE"));
        verify(paymentRepository, times(1)).findByOrderId("PEDIDO_INEXISTENTE");
    }

    @Test
    void obtenerPagoPorIdTransaccion_encontrado() {
        when(paymentRepository.findByTransactionId("TRANSACCION-XYZ")).thenReturn(Optional.of(pagoDePrueba));

        Payment resultado = paymentProcessingService.getPaymentByTransactionId("TRANSACCION-XYZ");

        assertNotNull(resultado);
        assertEquals("TRANSACCION-XYZ", resultado.getTransactionId());
        verify(paymentRepository, times(1)).findByTransactionId("TRANSACCION-XYZ");
    }

    @Test
    void obtenerPagoPorIdTransaccion_noEncontrado() {
        when(paymentRepository.findByTransactionId("TRANS_INEXISTENTE")).thenReturn(Optional.empty());

        RuntimeException excepcionLanzada = assertThrows(RuntimeException.class, () -> {
            paymentProcessingService.getPaymentByTransactionId("TRANS_INEXISTENTE");
        });

        assertTrue(excepcionLanzada.getMessage().contains("Payment not found for transaction ID: TRANS_INEXISTENTE"));
        verify(paymentRepository, times(1)).findByTransactionId("TRANS_INEXISTENTE");
    }

    @Test
    void obtenerTodosLosPagos_exito() {
        Payment otroPago = new Payment();
        otroPago.setId(2L);
        otroPago.setOrderId("PEDIDO-002");
        otroPago.setAmount(BigDecimal.valueOf(200.00));
        otroPago.setPaymentMethod("Tarjeta de Débito");
        otroPago.setPaymentStatus("PENDIENTE");
        otroPago.setTransactionDate(LocalDateTime.now());
        otroPago.setTransactionId(UUID.randomUUID().toString());

        List<Payment> pagos = Arrays.asList(pagoDePrueba, otroPago);
        when(paymentRepository.findAll()).thenReturn(pagos);

        List<Payment> resultado = paymentProcessingService.getAllPayments();

        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals("PEDIDO-001", resultado.get(0).getOrderId());
        assertEquals("PEDIDO-002", resultado.get(1).getOrderId());
        verify(paymentRepository, times(1)).findAll();
    }

    @Test
    void actualizarPago_exito() {
        Payment detallesActualizados = new Payment();
        detallesActualizados.setOrderId("PEDIDO-001-ACTUALIZADO");
        detallesActualizados.setAmount(BigDecimal.valueOf(150.00));
        detallesActualizados.setPaymentMethod("PayPal");
        detallesActualizados.setPaymentStatus("REEMBOLSADO");
        detallesActualizados.setTransactionId(pagoDePrueba.getTransactionId());
        detallesActualizados.setTransactionDate(pagoDePrueba.getTransactionDate());

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(pagoDePrueba));
        
        Payment pagoDespuesDeGuardar = new Payment();
        pagoDespuesDeGuardar.setId(pagoDePrueba.getId());
        pagoDespuesDeGuardar.setOrderId(pagoDePrueba.getOrderId()); 
        pagoDespuesDeGuardar.setAmount(detallesActualizados.getAmount());
        pagoDespuesDeGuardar.setPaymentMethod(detallesActualizados.getPaymentMethod());
        pagoDespuesDeGuardar.setPaymentStatus(detallesActualizados.getPaymentStatus());
        pagoDespuesDeGuardar.setTransactionId(detallesActualizados.getTransactionId());
        pagoDespuesDeGuardar.setTransactionDate(detallesActualizados.getTransactionDate());

        when(paymentRepository.save(any(Payment.class))).thenReturn(pagoDespuesDeGuardar);

        Payment resultado = paymentProcessingService.updatePayment(1L, detallesActualizados);

        assertNotNull(resultado);
        assertEquals("PEDIDO-001", resultado.getOrderId()); 
        assertEquals(BigDecimal.valueOf(150.00), resultado.getAmount());
        assertEquals("PayPal", resultado.getPaymentMethod());
        assertEquals("REEMBOLSADO", resultado.getPaymentStatus());
        verify(paymentRepository, times(1)).findById(1L);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void actualizarPago_noEncontrado() {
        Payment detallesActualizados = new Payment();
        detallesActualizados.setOrderId("PEDIDO_INEXISTENTE_ACTUALIZADO");

        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException excepcionLanzada = assertThrows(RuntimeException.class, () -> {
            paymentProcessingService.updatePayment(99L, detallesActualizados);
        });

        assertTrue(excepcionLanzada.getMessage().contains("Payment not found for ID: 99"));
        verify(paymentRepository, times(1)).findById(99L);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void eliminarPago_exito() {
        when(paymentRepository.existsById(1L)).thenReturn(true);
        doNothing().when(paymentRepository).deleteById(1L);

        assertDoesNotThrow(() -> paymentProcessingService.deletePayment(1L));

        verify(paymentRepository, times(1)).existsById(1L);
        verify(paymentRepository, times(1)).deleteById(1L);
    }

    @Test
    void eliminarPago_noEncontrado() {
        when(paymentRepository.existsById(99L)).thenReturn(false);

        RuntimeException excepcionLanzada = assertThrows(RuntimeException.class, () -> {
            paymentProcessingService.deletePayment(99L);
        });

        assertTrue(excepcionLanzada.getMessage().contains("Payment not found for ID: 99"));
        verify(paymentRepository, times(1)).existsById(99L);
        verify(paymentRepository, never()).deleteById(anyLong());
        
    }
//matenme estoy chato de la vida 2 horas para que funcionara    
}