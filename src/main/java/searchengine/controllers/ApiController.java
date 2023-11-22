package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.Response;
import searchengine.dto.message.ErrorMessage;
import searchengine.dto.ErrorResponse;
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

//    ToDo: 1. Сделать обработчик ошибок вместо ResponseEntity
//    ToDo: 2. Сделать валидацию значений параметров
    @GetMapping("/startIndexing")
    public ResponseEntity<Response> startIndexing() {
        if (indexingService.startIndexing()) {
            return ResponseEntity.ok( new SuccessResponse());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ErrorMessage.START_INDEXING_ERROR));
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Response> stopIndexing() {
        if (indexingService.stopIndexing()) {
            return ResponseEntity.ok(new SuccessResponse());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ErrorMessage.STOP_INDEXING_ERROR));
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Response> indexPage(@RequestBody String url) {
        if (indexingService.indexPage(url)) {
            return ResponseEntity.ok(new SuccessResponse());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ErrorMessage.PAGE_INDEXING_ERROR));
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam("query") String query,
            @RequestParam(value = "site", required = false) String site,
            @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset,
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit
    ) {
//        System.out.println("query: " + query + "\nsite: " + site + "\noffset: " + offset + "\nlimit: " + limit);
        if (site == null) {
            site = "";
        }
        return ResponseEntity.ok( searchService.search(query, site, offset, limit));
//        return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                .body(new ErrorResponse(ErrorMessage.PAGE_NOT_FOUND));
    }
}
