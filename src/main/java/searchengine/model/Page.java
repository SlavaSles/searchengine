package searchengine.model;

import jakarta.persistence.*;
import lombok.*;

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
}
