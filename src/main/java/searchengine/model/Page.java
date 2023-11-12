package searchengine.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "page")
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", columnDefinition = "INT NOT NULL")
    @ToString.Exclude
    private Site site;
    @Column(columnDefinition = "TEXT NOT NULL, INDEX (PATH(255))")
    private String path;
    @Column(columnDefinition = "INT NOT NULL")
    private Integer code;
    @Column(columnDefinition = "MEDIUMTEXT NOT NULL")
    private String content;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Page page)) return false;
        return site.equals(page.site) && path.equals(page.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(site, path);
    }
}
