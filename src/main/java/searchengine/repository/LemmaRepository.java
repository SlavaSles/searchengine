package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Lemma;

import java.util.ArrayList;
import java.util.Optional;

public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    Optional<Lemma> findLemmaBySiteIdAndLemma(Integer siteId, String lemma);
    ArrayList<Lemma> findLemmaBySiteId(Integer siteId);
}
