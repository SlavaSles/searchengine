package searchengine.dto;

import lombok.Builder;
import lombok.Data;
import searchengine.dto.statistics.StatisticsData;

@Builder
@Data
public class StatisticsResponse {
    private boolean result;
    private StatisticsData statistics;
}
