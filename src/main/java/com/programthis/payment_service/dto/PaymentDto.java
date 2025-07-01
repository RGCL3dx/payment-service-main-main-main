package com.programthis.payment_service.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Relation(collectionRelation = "payments", itemRelation = "payment")
public class PaymentDto extends RepresentationModel<PaymentDto> {
    private Long id;
    private String orderId;
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentStatus;
    private String transactionId;
    private LocalDateTime transactionDate;
}