package searchengine.model;

import jakarta.persistence.*;
import lombok.*;


import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "site")
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "enum('INDEXING','INDEXED', 'FAILED') NOT NULL")
    private Status status;
    @Column(name = "status_time", columnDefinition = "DATETIME NOT NULL")
    private LocalDateTime statusTime;
//    ToDo: Добавить ошибки из исключений
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
    @Column(columnDefinition = "VARCHAR(255) NOT NULL")
    private String url;
    @Column(columnDefinition = "VARCHAR(255) NOT NULL")
    private String name;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "site")
    @ToString.Exclude
    private Set<Page> pages;
    @Transient
    private String domain;
    @Transient
    private String subDomain;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Site site)) return false;
        return url.equals(site.url) && name.equals(site.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, name);
    }
}
