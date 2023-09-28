package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.indexing.ErrorStartIndexingResponse;
import searchengine.dto.indexing.ErrorStopIndexingResponse;
import searchengine.dto.indexing.StartIndexingResponse;
import searchengine.dto.indexing.StopIndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
    }


    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<?> startIndexing() {
        //TODO: добавить сервис с проверкой запуска индексации
//        boolean result = false;
//        if (true) {
//
//            return ResponseEntity.ok(new StartIndexingResponse());
//        }
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorStartIndexingResponse());
//        result = indexingService.indexing().isResult();
        return ResponseEntity.ok( indexingService.indexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<?> stopIndexing() {
        //TODO: добавить сервис с проверкой запуска индексации
        if (false) {
            return ResponseEntity.ok(new StopIndexingResponse());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorStopIndexingResponse());
    }
}
