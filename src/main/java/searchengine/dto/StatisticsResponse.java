package searchengine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import searchengine.dto.statistics.StatisticsData;

@Schema(description = "Statistic response")
@Builder
@Data
public class StatisticsResponse {
    @Schema(description = "Result of operation")
    private boolean result;
    @Schema(description = "Total and detailed statistics for all indexed sites")
    private StatisticsData statistics;
}
