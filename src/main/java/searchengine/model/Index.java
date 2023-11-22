package searchengine.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "search_index")
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", columnDefinition = "INT NOT NULL")
    @ToString.Exclude
    private Page page;
    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", columnDefinition = "INT NOT NULL")
    @ToString.Exclude
    private Lemma lemma;
//    ToDo: Поменять тип на Integer
    @Column(name = "lemmas_rank", columnDefinition = "FLOAT NOT NULL")
    private float rank;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Index index)) return false;
        return page.equals(index.page) && lemma.equals(index.lemma);
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, lemma);
    }
}
