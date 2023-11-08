package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Index;

import java.util.ArrayList;

public interface IndexRepository extends JpaRepository<Index, Integer> {
    ArrayList<Index> findIndexByPageId(Integer pageId);
}
