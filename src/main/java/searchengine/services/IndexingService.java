package searchengine.services;

public interface IndexingService {
    boolean indexing();
    boolean stopIndexing();
    boolean indexPage(String url);
}
