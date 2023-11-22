package searchengine.dto.search;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class DetailedSearchItem {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private float relevance;
}
