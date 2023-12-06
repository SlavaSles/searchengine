package searchengine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import searchengine.dto.search.DetailedSearchItem;

import java.util.List;

@Schema(description = "Searching response")
@Builder
@Data
public class SearchResponse {
    @Schema(description = "Result of operation")
    private final boolean result = true;
    @Schema(description = "Count of results")
    private int count;
    @Schema(description = "List of found items")
    private List<DetailedSearchItem> data;
}
