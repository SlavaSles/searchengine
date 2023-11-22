package searchengine.logic.indexing;

import java.util.HashMap;
import java.util.Set;

public interface LemmaSearcher {
    HashMap<String, Integer> searchLemmas(String content);
    Set<String> getLemmas(String query);
}
