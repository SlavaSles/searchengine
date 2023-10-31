package searchengine.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;

import java.util.ArrayList;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    Page findPageByPathAndSiteId(String path, Integer siteId);
    ArrayList<Page> findPageBySiteId(Integer siteId);
}
