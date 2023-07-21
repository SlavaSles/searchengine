package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.indexing.StartIndexingResponse;
import searchengine.model.Page;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final SitesList sites;
    @Autowired
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    public StartIndexingResponse indexing() {
        TreeSet<String> subUrls = new TreeSet<>();
        String originDomainUrl = String.valueOf(sites.getSites().get(0).getUrl());
        SiteEntity siteEntity = SiteEntity.builder()
                .status(Status.INDEXING)
                .statusTime(LocalDateTime.now())
                .lastError(null)
                .name(sites.getSites().get(0).getName())
                .url(sites.getSites().get(0).getUrl())
                .build();
        SiteEntity existSite = siteRepository.findSiteEntityByName(siteEntity.getName());
        if (existSite != null) {
            siteRepository.delete(existSite);
        }
        siteRepository.save(siteEntity);
        SiteAddress siteAddress = SiteAddress.builder()
                .site(siteEntity)
                .build();
        Mapper mapper = new Mapper(originDomainUrl, pageRepository, siteAddress);
        subUrls = mapper.sitesMapping();
//        subUrls.forEach(System.out::println);
        return new StartIndexingResponse();
    }
}
