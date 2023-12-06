package searchengine.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "Detailed found item")
@Builder
@Data
public class DetailedSearchItem {
    @Schema(description = "URL of site", example = "http://www.site.com")
    private String site;
    @Schema(description = "Site name", example = "Имя сайта")
    private String siteName;
    @Schema(description = "URL of found page", example = "/path/to/page/6784")
    private String uri;
    @Schema(description = "Title of found page", example = "Заголовок найденной страницы")
    private String title;
    @Schema(description = "Fragment with found searching query", example = "Фрагмент текста, в котором найдены " +
            "совпадения, <b>выделенные жирным</b>, в формате HTML")
    private String snippet;
    @Schema(description = "Relevance of found result", example = "0.93362")
    private float relevance;
}
