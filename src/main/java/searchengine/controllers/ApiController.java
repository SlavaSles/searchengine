package searchengine.controllers;

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
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    @GetMapping("/statistics")
    public StatisticsResponse statistics() {
        return statisticsService.getStatistics();
    }

//    ToDo: 1. Сделать валидацию значений параметров
    @GetMapping("/startIndexing")
    public SuccessResponse startIndexing() {
        indexingService.startIndexing();
        return new SuccessResponse();
    }

    @GetMapping("/stopIndexing")
    public SuccessResponse stopIndexing() {
        indexingService.stopIndexing();
        return new SuccessResponse();
    }

    @PostMapping("/indexPage")
    public SuccessResponse indexPage(@RequestBody String url) {
        indexingService.indexPage(url);
        return new SuccessResponse();
    }

    @GetMapping("/search")
    public SearchResponse search(
            @RequestParam("query") String query,
            @RequestParam(value = "site", required = false) String site,
            @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset,
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit
    )
    {
        return searchService.search(query, (site == null) ? "" : site, offset, limit);
    }
}
