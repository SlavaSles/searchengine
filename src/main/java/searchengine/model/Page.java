package searchengine.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "page")
//, indexes = @Index(name = "path_index", columnList = "path"))
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    //(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", columnDefinition = "INT NOT NULL")//, nullable = false)
    private SiteEntity site;
    @Column(columnDefinition = "TEXT NOT NULL, Index (path(255))")//, nullable = false)
    private String path;
    @Column(columnDefinition = "INT NOT NULL")//nullable = false)
    private Integer code;
    @Column(columnDefinition = "MEDIUMTEXT NOT NULL")//, nullable = false)
    private String content;
}
