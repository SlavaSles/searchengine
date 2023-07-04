package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;


import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "site")
public class SiteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "enum('INDEXING','INDEXED', 'FAILED')", nullable = false)
    private Status status;
//    @Column(name = "status_time", nullable = false)
    @Column(name = "status_time", columnDefinition = "DATETIME NOT NULL")
    private LocalDateTime statusTime;
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
    @Column(columnDefinition = "VARCHAR(255) NOT NULL")//, nullable = false)
    private String url;
    @Column(columnDefinition = "VARCHAR(255) NOT NULL")
    private String name;

}
