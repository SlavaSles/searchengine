package searchengine.dto;

import lombok.Builder;
import lombok.Data;
import searchengine.dto.search.DetailedSearchItem;

import java.util.List;

@Builder
@Data
public class SearchResponse {
    private final boolean result = true;
    private int count;
    private List<DetailedSearchItem> data;
}
