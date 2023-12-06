package searchengine.dto.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "Detailed statistic for item")
@Builder
@Data
public class DetailedStatisticsItem {
    @Schema(description = "URL of the site", example = "http://www.site.com")
    private String url;
    @Schema(description = "Site name", example = "Имя сайта")
    private String name;
    @Schema(description = "Status of indexing: indexing, indexed, failed", example = "FAILED")
    private String status;
    @Schema(description = "Status time (timestamp)", example = "1600160357")
    private long statusTime;
    @Schema(description = "Error message", example = "Операция прервана пользователем")
    private String error;
    @Schema(description = "Count of pages for the site", example = "5764")
    private int pages;
    @Schema(description = "Count of lemmas for the site", example = "321115")
    private int lemmas;
}
