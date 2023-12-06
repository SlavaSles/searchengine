package searchengine.dto.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Schema(description = "Compose of total and detailed statistics for all indexed sites")
@Builder
@Data
public class StatisticsData {
    @Schema(description = "Total statistics for all sites")
    private TotalStatistics total;
    @Schema(description = "List of detailed statistic for all items")
    private List<DetailedStatisticsItem> detailed;
}
