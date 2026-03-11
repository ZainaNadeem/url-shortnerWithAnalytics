package com.project.urlshortener.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import java.io.Serializable;

@Entity                          // tells Hibernate: "this class = a DB table"
@Table(name = "urls")            // the actual table name in MySQL
@Data                            // Lombok: generates getters, setters, toString, equals
@NoArgsConstructor               // Lombok: generates empty constructor (required by JPA)
@AllArgsConstructor              // Lombok: generates constructor with all fields
@Builder                         // Lombok: lets us do Url.builder().shortCode("abc").build()
public class Url implements Serializable { 

    @Id                                                    // this field is the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)    // MySQL auto-increments it
    private Long id;

    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;                            // the full URL user submitted

    @Column(name = "short_code", unique = true, nullable = false, length = 10)
    private String shortCode;                              // e.g. "aB3xYz"

    @Column(name = "created_at")
    private LocalDateTime createdAt;                       // when was this URL shortened

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;                       // optional expiry (can be null)

    @Column(name = "click_count")
    @Builder.Default
    private Long clickCount = 0L;                     // total number of redirects

    @PrePersist                                            // runs automatically before saving
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.clickCount == null) {
            this.clickCount = 0L;
        }
    }
}