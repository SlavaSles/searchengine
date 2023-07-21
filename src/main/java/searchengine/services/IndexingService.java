package searchengine.services;

import searchengine.dto.indexing.StartIndexingResponse;

public interface IndexingService {
    StartIndexingResponse indexing();
}
