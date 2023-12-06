package searchengine.dto.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "Total statistics for all sites")
@Builder
@Data
public class TotalStatistics {
    @Schema(description = "Count of indexed sites", example = "10")
    private int sites;
    @Schema(description = "Total count of indexed pages", example = "3267")
    private int pages;
    @Schema(description = "Total count of indexed lemmas", example = "15673")
    private int lemmas;
    @Schema(description = "Is indexing")
    private boolean indexing;
}
