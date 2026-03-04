package com.vitaliq.vitaliq_platform.model.user;

import com.vitaliq.vitaliq_platform.model.auth.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "body_metrics_logs")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
public class BodyMetricsLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Double weightKg;

    private Double bodyFatPercentage;

    @Column(nullable = false)
    private LocalDate loggedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}