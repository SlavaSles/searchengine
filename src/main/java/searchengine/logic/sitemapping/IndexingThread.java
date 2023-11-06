package searchengine.logic.sitemapping;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.auxclass.Connection;
import searchengine.config.SiteCfg;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class IndexingThread extends Thread {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final Connection connection;
    @Setter
    private SiteCfg siteCfg;
    @Setter
    private ForkJoinPool fjp;
    @Setter
    private String addedUrl;
    private SiteMapper siteMapper;
    private final ConcurrentSkipListSet<Page> allPages = new ConcurrentSkipListSet<>(
            (Comparator.comparing(Page::getPath)));

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
            indexingSite(site);
            if (Thread.currentThread().isInterrupted()) {
                removeUnmappedPages();
            }
            savePages(allPages);
        }
        changeSiteStatus(site);
        saveSite(site);
    }

    private void indexingSite(Site site) {
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
    }

    private void changeSiteStatus(Site site) {
        if (Thread.currentThread().isInterrupted()) {
            site.setStatus(Status.FAILED);
            site.setLastError("Операция прервана пользователем");
        } else {
            site.setStatus(Status.INDEXED);
        }
        site.setStatusTime(LocalDateTime.now());
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
        List<String> regexes = Arrays.asList(Regexes.DOMAIN, Regexes.DOMAIN_RU);
        for (String regex : regexes) {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(site.getUrl());
            if (matcher.find()) {
                domain = matcher.group(1).trim();
                break;
            }
        }
        return domain;
    }

    private String findSubDomainUrl(Site site) {
        String subDomainUrl = "";
        List<String> regexes = Arrays.asList(Regexes.SLASH_TEXT_SLASH, Regexes.SLASH_TEXT_SLASH_RU);
        if (site.getUrl().length() - site.getDomain().length() > 1) {
            String secondPartOfUrl = site.getUrl().substring(site.getDomain().length());
            for (String regex : regexes) {
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(secondPartOfUrl);
                if (matcher.find()) {
                    subDomainUrl = matcher.group(1).trim();
                    break;
                }
            }
        }
        return subDomainUrl;
    }

    @Transactional
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
