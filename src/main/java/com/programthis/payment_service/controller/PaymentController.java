package com.programthis.payment_service.controller;

import com.programthis.payment_service.dto.PaymentDto;
import com.programthis.payment_service.entity.Payment;
import com.programthis.payment_service.service.PaymentProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Gestión de Pagos", description = "Endpoints para procesar y consultar pagos")
public class PaymentController {

    @Autowired
    private PaymentProcessingService paymentProcessingService;


    private PaymentDto convertToDto(Payment entity) {
        PaymentDto dto = new PaymentDto();
        BeanUtils.copyProperties(entity, dto);
        addLinksToDto(dto);
        return dto;
    }

    private void addLinksToDto(PaymentDto dto) {
        try {
            dto.add(linkTo(methodOn(PaymentController.class).getPaymentById(dto.getId())).withSelfRel());
            dto.add(linkTo(methodOn(PaymentController.class).getPaymentStatusByOrderId(dto.getOrderId())).withRel("by-order-id"));
            if (dto.getTransactionId() != null) {
                dto.add(linkTo(methodOn(PaymentController.class).getPaymentByTransactionId(dto.getTransactionId())).withRel("by-transaction-id"));
            }
            dto.add(linkTo(methodOn(PaymentController.class).getAllPayments()).withRel("all-payments"));
        } catch (Exception e) {
            // Manejar excepciones si la creación de enlaces falla
        }
    }

    // --- Endpoints de la API ---

    @Operation(summary = "Procesar un nuevo pago", description = "Recibe los detalles de una solicitud de pago y la procesa. Devuelve el estado del pago.")
    @ApiResponse(responseCode = "200", description = "Pago procesado exitosamente (COMPLETADO)", content = @Content(schema = @Schema(implementation = PaymentDto.class)))
    @ApiResponse(responseCode = "400", description = "El pago fue RECHAZADO o hubo un error en la solicitud")
    @PostMapping("/process")
    public ResponseEntity<PaymentDto> processPayment(@RequestBody PaymentProcessingService.PaymentRequest paymentRequest) {
        Payment payment = paymentProcessingService.processPayment(paymentRequest);
        PaymentDto paymentDto = convertToDto(payment);

        if ("COMPLETADO".equals(payment.getPaymentStatus())) {
            return ResponseEntity.ok(paymentDto);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(paymentDto);
        }
    }

    @Operation(summary = "Obtener un pago por su ID único", description = "Busca un pago utilizando su ID de base de datos.")
    @ApiResponse(responseCode = "200", description = "Pago encontrado")
    @ApiResponse(responseCode = "404", description = "No se encontró ningún pago con ese ID")
    @GetMapping("/{id}")
    public PaymentDto getPaymentById(@Parameter(description = "ID del pago", required = true) @PathVariable Long id) {
        return paymentProcessingService.getPaymentById(id)
                .map(this::convertToDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no encontrado para ID: " + id));
    }

    @Operation(summary = "Obtener un pago por ID de Pedido", description = "Busca el estado de un pago asociado a un ID de pedido específico.")
    @GetMapping("/status/order/{orderId}")
    public PaymentDto getPaymentStatusByOrderId(@Parameter(description = "ID del pedido (ej. PEDIDO-001)", required = true) @PathVariable String orderId) {
        try {
            Payment payment = paymentProcessingService.getPaymentStatusByOrderId(orderId);
            return convertToDto(payment);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @Operation(summary = "Obtener un pago por ID de Transacción", description = "Busca un pago utilizando el ID de transacción único generado.")
    @GetMapping("/status/transaction/{transactionId}")
    public PaymentDto getPaymentByTransactionId(@Parameter(description = "ID de la transacción (UUID)", required = true) @PathVariable String transactionId) {
        try {
            Payment payment = paymentProcessingService.getPaymentByTransactionId(transactionId);
            return convertToDto(payment);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @Operation(summary = "Listar todos los pagos", description = "Devuelve una colección de todos los pagos registrados en el sistema.")
    @GetMapping
    public CollectionModel<PaymentDto> getAllPayments() {
        List<PaymentDto> payments = paymentProcessingService.getAllPayments().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return CollectionModel.of(payments,
                linkTo(methodOn(PaymentController.class).getAllPayments()).withSelfRel());
    }

    @Operation(summary = "Actualizar un pago existente", description = "Permite modificar los detalles de un pago ya existente a través de su ID.")
    @PutMapping("/{id}")
    public ResponseEntity<PaymentDto> updatePayment(@PathVariable Long id, @RequestBody Payment paymentDetails) {
        try {
            Payment updatedPayment = paymentProcessingService.updatePayment(id, paymentDetails);
            return ResponseEntity.ok(convertToDto(updatedPayment));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @Operation(summary = "Eliminar un pago", description = "Elimina un registro de pago del sistema basado en su ID. Esta acción no puede deshacerse.")
    @ApiResponse(responseCode = "204", description = "Pago eliminado exitosamente")
    @ApiResponse(responseCode = "404", description = "No se encontró el pago a eliminar")
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