package searchengine.dto.statistics;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class StatisticsResponse {
    private boolean result;
    private StatisticsData statistics;
}
