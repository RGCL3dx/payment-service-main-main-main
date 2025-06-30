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
    private PaymentProcessingService.PaymentRequest solicitudDePagoFallo;


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
                "detalles_tarjeta_exito"
        );

        solicitudDePagoFallo = new PaymentProcessingService.PaymentRequest(
                "PEDIDO-002",
                BigDecimal.valueOf(50.00),
                "detalles_tarjeta_fallo"
        );
    }

    @Test
    void procesarPago_exito() {
        Payment pagoExitosoSimulado = new Payment();
        pagoExitosoSimulado.setOrderId(solicitudDePagoDePrueba.getIdPedido());
        pagoExitosoSimulado.setAmount(solicitudDePagoDePrueba.getMonto());
        pagoExitosoSimulado.setPaymentMethod("Tarjeta de Crédito/Débito");
        pagoExitosoSimulado.setPaymentStatus("COMPLETADO");
        pagoExitosoSimulado.setTransactionId(UUID.randomUUID().toString());
        pagoExitosoSimulado.setTransactionDate(LocalDateTime.now());
        pagoExitosoSimulado.setId(1L); 

        when(paymentRepository.save(any(Payment.class))).thenReturn(pagoExitosoSimulado);

        Payment resultado = paymentProcessingService.processPayment(solicitudDePagoDePrueba);

        assertNotNull(resultado);
        assertEquals("COMPLETADO", resultado.getPaymentStatus());
        assertEquals(solicitudDePagoDePrueba.getIdPedido(), resultado.getOrderId());
        assertEquals(solicitudDePagoDePrueba.getMonto(), resultado.getAmount());
        assertNotNull(resultado.getTransactionId());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void procesarPago_fallo() {
        Payment pagoFallidoSimulado = new Payment();
        pagoFallidoSimulado.setOrderId(solicitudDePagoFallo.getIdPedido());
        pagoFallidoSimulado.setAmount(solicitudDePagoFallo.getMonto());
        pagoFallidoSimulado.setPaymentMethod("Tarjeta de Crédito/Débito");
        pagoFallidoSimulado.setPaymentStatus("FALLIDO");
        pagoFallidoSimulado.setTransactionId(null);
        pagoFallidoSimulado.setTransactionDate(LocalDateTime.now());
        pagoFallidoSimulado.setId(2L);

        when(paymentRepository.save(any(Payment.class))).thenReturn(pagoFallidoSimulado);

        Payment resultado = paymentProcessingService.processPayment(solicitudDePagoFallo);

        assertNotNull(resultado);
        assertEquals("FALLIDO", resultado.getPaymentStatus());
        assertEquals(solicitudDePagoFallo.getIdPedido(), resultado.getOrderId());
        assertEquals(solicitudDePagoFallo.getMonto(), resultado.getAmount());
        assertNull(resultado.getTransactionId());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void obtenerPagoPorId_encontrado() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(pagoDePrueba));

        Optional<Payment> resultado = paymentProcessingService.getPaymentById(1L);

        assertTrue(resultado.isPresent());
        assertEquals(pagoDePrueba.getId(), resultado.get().getId());
        verify(paymentRepository, times(1)).findById(1L);
    }

    @Test
    void obtenerPagoPorId_noEncontrado() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Payment> resultado = paymentProcessingService.getPaymentById(99L);

        assertFalse(resultado.isPresent());
        verify(paymentRepository, times(1)).findById(99L);
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

        assertTrue(excepcionLanzada.getMessage().contains("Pago no encontrado para ID de pedido: PEDIDO_INEXISTENTE"));
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

        assertTrue(excepcionLanzada.getMessage().contains("Pago no encontrado para ID de transacción: TRANS_INEXISTENTE"));
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


        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment savedPayment = invocation.getArgument(0);
            assertEquals(detallesActualizados.getOrderId(), savedPayment.getOrderId());
            assertEquals(detallesActualizados.getAmount(), savedPayment.getAmount());
            assertEquals(detallesActualizados.getPaymentMethod(), savedPayment.getPaymentMethod());
            assertEquals(detallesActualizados.getPaymentStatus(), savedPayment.getPaymentStatus());
            return savedPayment; 
        });

        Payment resultado = paymentProcessingService.updatePayment(1L, detallesActualizados);

        assertNotNull(resultado);
        assertEquals(detallesActualizados.getOrderId(), resultado.getOrderId());
        assertEquals(detallesActualizados.getAmount(), resultado.getAmount());
        assertEquals(detallesActualizados.getPaymentMethod(), resultado.getPaymentMethod());
        assertEquals(detallesActualizados.getPaymentStatus(), resultado.getPaymentStatus());
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

        assertTrue(excepcionLanzada.getMessage().contains("Pago no encontrado para ID: 99"));
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

        assertTrue(excepcionLanzada.getMessage().contains("Pago no encontrado para ID: 99"));
        verify(paymentRepository, times(1)).existsById(99L);
        verify(paymentRepository, never()).deleteById(anyLong());

    }
}