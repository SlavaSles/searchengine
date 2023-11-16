package searchengine.logic.indexing;

import java.util.HashMap;

public interface LemmaSearcher {
    HashMap<String, Integer> searchLemmas(String content);
}
