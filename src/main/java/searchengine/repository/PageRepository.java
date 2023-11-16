package searchengine.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;

import java.util.ArrayList;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    Optional<Page> findPageBySiteIdAndPath(Integer siteId, String path);
    ArrayList<Page> findPageBySiteId(Integer siteId);
    @Query(value = "SELECT count(*) from page where site_id = :siteId", nativeQuery = true)
    Integer countPages(Integer siteId);
}
