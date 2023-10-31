package searchengine.logic.sitemapping;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Connection;
import searchengine.config.SiteCfg;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class IndexingThread extends Thread {
    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final PageRepository pageRepository;
    @Autowired
    private final Connection connection;
    @Setter
    private SiteCfg siteCfg;
    @Setter
    private ForkJoinPool fjp;
    @Setter
    private String addedUrl;
    private final ConcurrentSkipListSet<Page> allPages = new ConcurrentSkipListSet<>(new PageComparator());
    private SiteMapper siteMapper;

    @Override
    public void run() {
        if (addedUrl != null) {
            addIndexingPage(addedUrl);
            return;
        }
        Site site = Site.builder()
                .status(Status.INDEXING)
                .statusTime(LocalDateTime.now())
                .lastError(null)
                .url(siteCfg.getUrl())
                .name(siteCfg.getName())
                .build();
        if (!Thread.currentThread().isInterrupted()) {
            deleteExistSiteAndPagesFromDB(site);
            site.setDomain(findDomain(site));
            site.setSubDomain(findSubDomainUrl(site));
            saveSite(site);
            Page firstPage = Page.builder()
                    .site(site)
                    .path(site.getSubDomain().concat("/"))
                    .code(0)
                    .content("")
                    .build();
            allPages.add(firstPage);
            siteMapper = new SiteMapper(connection, site, firstPage, allPages);
            fjp.invoke(siteMapper);
            if (Thread.currentThread().isInterrupted()) {
                removeUnmappedPages();
            }
//            System.out.println("Размер списка: " + allPages.size());
            savePages(allPages);
        }
        changeSiteStatus(site);
        saveSite(site);
    }

    private void changeSiteStatus(Site site) {
        if (Thread.currentThread().isInterrupted()) {
            site.setStatus(Status.FAILED);
            site.setStatusTime(LocalDateTime.now());
            site.setLastError("Операция прервана пользователем");
        } else {
            site.setStatus(Status.INDEXED);
            site.setStatusTime(LocalDateTime.now());
        }
    }

    private void removeUnmappedPages() {
        allPages.removeIf(page -> page.getCode() == 0);
    }

    @Transactional
    private void saveSite(Site site) {
        siteRepository.flush();
        siteRepository.save(site);
    }

    @Transactional
    private void savePages(ConcurrentSkipListSet<Page> allPages) {
        pageRepository.flush();
        pageRepository.saveAll(allPages);
    }

    @Transactional
    private void deleteExistSiteAndPagesFromDB(Site site) {
        Site existSite = siteRepository.findSiteByUrl(site.getUrl());
        if (existSite != null) {
            ArrayList<Page> pages = pageRepository.findPageBySiteId(existSite.getId());
            pageRepository.deleteAll(pages);
            siteRepository.delete(existSite);
        }
    }

    private String findDomain(Site site) {
        String domain = "";
        Pattern pattern1 = Pattern.compile(Regexes.DOMAIN);
        Matcher matcher1 = pattern1.matcher(site.getUrl());
        Pattern pattern2 = Pattern.compile(Regexes.DOMAIN_RU);
        Matcher matcher2 = pattern2.matcher(site.getUrl());
        if (matcher1.find()) {
            domain = matcher1.group(1).trim();
        } else if (matcher2.find()) {
            domain = matcher2.group(1).trim();
        }
        return domain;
    }

    private String findSubDomainUrl(Site site) {
        String subDomainUrl = "";
        if (site.getUrl().length() - site.getDomain().length() > 1) {
            String secondPartOfUrl = site.getUrl().substring(site.getDomain().length());
            Pattern pattern1 = Pattern.compile(Regexes.SLASH_TEXT_SLASH);
            Matcher matcher1 = pattern1.matcher(secondPartOfUrl);
            Pattern pattern2 = Pattern.compile(Regexes.SLASH_TEXT_SLASH_RU);
            Matcher matcher2 = pattern2.matcher(secondPartOfUrl);
            if (matcher1.find()) {
                subDomainUrl = matcher1.group(1).trim();
            } else if (matcher2.find()) {
                subDomainUrl = matcher2.group(1).trim();
            }
        }
        return subDomainUrl;
    }

//    @Transactional
    private void addIndexingPage(String addedUrl) {
        Site existSite = siteRepository.findSiteByUrl(siteCfg.getUrl());
        if (existSite != null) {
            existSite.setDomain(findDomain(existSite));
            existSite.setSubDomain(findSubDomainUrl(existSite));
            String addedPath = addedUrl.substring(siteCfg.getUrl().length());
            Page addedPage = pageRepository.findPageByPathAndSiteId(addedPath, existSite.getId());
            if (addedPage == null) {
                addedPage = Page.builder()
                        .site(existSite)
                        .path(addedPath)
                        .code(0)
                        .content("")
                        .build();
            }
            siteMapper = new SiteMapper(connection, existSite, addedPage, allPages);
            siteMapper.compute();
            savePages(allPages);
            changeSiteStatus(existSite);
            saveSite(existSite);
        }
    }
}
