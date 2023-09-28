package searchengine.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.SiteEntity;

import java.util.Set;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {

    Page findPageByPath (String path);
    Page findPageByPathAndSiteId (String path);
    Set<Page> findPageBySite (SiteEntity site);
}
