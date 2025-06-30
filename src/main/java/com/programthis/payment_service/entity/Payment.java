package com.programthis.payment_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor; 
import lombok.Data;             
import lombok.NoArgsConstructor;  

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data               
@NoArgsConstructor  
@AllArgsConstructor 
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String orderId;
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentStatus;
    private String transactionId;
    private LocalDateTime transactionDate;


}