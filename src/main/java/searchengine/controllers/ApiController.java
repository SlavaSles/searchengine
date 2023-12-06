package searchengine.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.SearchResponse;
import searchengine.dto.SuccessResponse;
import searchengine.dto.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Searching engine API controller", description = "Allows indexing sites, a certain page of a site, " +
        "stop indexing, get statistics about indexed sites, perform a search on indexed sites")
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    @Operation(summary = "Getting site statistics")
    @GetMapping("/statistics")
    public StatisticsResponse statistics() {
        return statisticsService.getStatistics();
    }

    @Operation(summary = "Starting site indexing")
    @GetMapping("/startIndexing")
    public SuccessResponse startIndexing() {
        indexingService.startIndexing();
        return new SuccessResponse();
    }

    @Operation(summary = "Stopping site indexing")
    @GetMapping("/stopIndexing")
    public SuccessResponse stopIndexing() {
        indexingService.stopIndexing();
        return new SuccessResponse();
    }

    @Operation(summary = "Certain page indexing")
    @PostMapping("/indexPage")
    public SuccessResponse indexPage(@RequestBody
                                         @Parameter(description = "URL of certain page for indexing")
                                         String url) {
        indexingService.indexPage(url);
        return new SuccessResponse();
    }

    @Operation(summary = "Searching on indexed sites")
    @GetMapping("/search")
    public SearchResponse search(
            @RequestParam(value = "query")
                @Parameter(description = "Search query", required = true) String query,
            @RequestParam(value = "site", required = false, defaultValue = "")
                @Parameter(description = "Site for searching") String site,
            @RequestParam(value = "offset", required = false, defaultValue = "0")
                @Parameter(description = "Offset for paging") Integer offset,
            @RequestParam(value = "limit", required = false, defaultValue = "20")
                @Parameter(description = "Limit of searching items") Integer limit
    )
    {
        return searchService.search(query, site, offset, limit);
    }
}
