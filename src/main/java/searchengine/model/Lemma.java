package searchengine.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "lemma")
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", columnDefinition = "INT NOT NULL")
    @ToString.Exclude
    private Site site;
    @Column(columnDefinition = "VARCHAR(255) NOT NULL")
    private String lemma;
    @Column(columnDefinition = "INT NOT NULL")
    private Integer frequency;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Lemma compareLemma)) return false;
        return site.equals(compareLemma.site) && lemma.equals(compareLemma.lemma);
    }

    @Override
    public int hashCode() {
        return Objects.hash(site, lemma);
    }
}
