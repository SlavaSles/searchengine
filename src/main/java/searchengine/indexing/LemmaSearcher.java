package searchengine.indexing;

import java.util.HashMap;
import java.util.Set;

public interface LemmaSearcher {
    HashMap<String, Integer> searchLemmas(String content);
    Set<String> getLemmas(String query);
    String getSnippet(String content, Set<String> searchLemmas);
}
