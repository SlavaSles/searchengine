package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Index;

import java.util.ArrayList;
import java.util.Optional;

public interface IndexRepository extends JpaRepository<Index, Integer> {
    ArrayList<Index> findIndexByPageId(Integer pageId);
    ArrayList<Index> findIndexByLemmaId(Integer lemmaId);
    Optional<Index> findIndexByLemmaIdAndPageId(Integer lemmaId, Integer pageId);
}
