package com.project.urlshortener.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "click_events", indexes = {
    @Index(name = "idx_click_url_id", columnList = "url_id"),
    @Index(name = "idx_click_clicked_at", columnList = "clicked_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many clicks belong to one URL
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_id", nullable = false)
    private Url url;

    @Column(name = "clicked_at", nullable = false)
    private LocalDateTime clickedAt;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "referer", length = 2048)
    private String referer;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @PrePersist
    public void prePersist() {
        if (this.clickedAt == null) this.clickedAt = LocalDateTime.now();
    }
}
