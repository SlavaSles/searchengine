package searchengine.logic.indexing;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.auxclass.Connection;
import searchengine.config.SiteCfg;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final Connection connection;
    @Setter
    private SiteCfg siteCfg;
    @Setter
    private ForkJoinPool fjp;
    @Setter
    private String addedUrl;
    private PageIndexer pageIndexer;
    private final ConcurrentSkipListSet<Page> allPages = new ConcurrentSkipListSet<>(
            (Comparator.comparing(Page::getPath)));
    private final ConcurrentHashMap<String, Lemma> allLemmas = new ConcurrentHashMap<>();
    private final Set<Index> allIndices = new ConcurrentHashMap<>().newKeySet();

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
        }
        savePages(allPages);
        saveLemmas(allLemmas);
        saveIndices(allIndices);
        changeSiteStatus(site);
        saveSite(site);
    }

    private void indexingSite(Site site) {
        deleteIndexFromDbForExistSite(site);
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
        pageIndexer = new PageIndexer(connection, site, firstPage, allPages, allLemmas, allIndices);
        fjp.invoke(pageIndexer);
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
    private void saveLemmas(ConcurrentHashMap<String, Lemma> allLemmas) {
        lemmaRepository.flush();
        lemmaRepository.saveAll(allLemmas.values());
    }

    @Transactional
    private void saveIndices(Set<Index> allIndices) {
        indexRepository.flush();
        indexRepository.saveAll(allIndices);
    }

    @Transactional
    private void deleteIndexFromDbForExistSite(Site site) {
        Optional<Site> existSiteOpt = siteRepository.findSiteByUrl(site.getUrl());
        if (existSiteOpt.isPresent()) {
            ArrayList<Page> pages = pageRepository.findPageBySiteId(existSiteOpt.get().getId());
            pages.forEach(page -> deleteExistIndices(page));
            deleteExistLemmas(existSiteOpt.get());
            pageRepository.deleteAll(pages);
            siteRepository.delete(existSiteOpt.get());
        }
    }

    @Transactional
    private void deleteExistIndices(Page page) {
        ArrayList<Index> indices = indexRepository.findIndexByPageId(page.getId());
        indexRepository.deleteAll(indices);
    }

    @Transactional
    private void deleteExistLemmas(Site site) {
        ArrayList<Lemma> lemmas = lemmaRepository.findLemmaBySiteId(site.getId());
        lemmaRepository.deleteAll(lemmas);
    }

    private String findDomain(Site site) {
        List<String> regexes = Arrays.asList(Regexes.DOMAIN, Regexes.DOMAIN_RU);
        return matchingUrlParts(site.getUrl(), regexes);
    }

    private String findSubDomainUrl(Site site) {
        List<String> regexes = Arrays.asList(Regexes.SLASH_TEXT_SLASH, Regexes.SLASH_TEXT_SLASH_RU);
        if (site.getUrl().length() - site.getDomain().length() <= 1) {
            return "";
        }
        return matchingUrlParts(site.getUrl().substring(site.getDomain().length()), regexes);
    }

    private String matchingUrlParts(String text, List<String> regexes) {
        for (String regex : regexes) {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        return "";
    }

//    @Transactional
    private void addIndexingPage(String addedUrl) {
//        ToDo: Перенести код по работе с БД в отдельные Transactional методы
        Site site;
        Page addedPage = null;
        String addedPath = addedUrl.substring(siteCfg.getUrl().length());
        Optional<Site> existSiteOpt = siteRepository.findSiteByUrl(siteCfg.getUrl());
        if (existSiteOpt.isPresent()) {
            site = existSiteOpt.get();
            Optional<Page> addedPageOpt = pageRepository.findPageByPathAndSiteId(addedPath, site.getId());
            if (addedPageOpt.isPresent()) {
                addedPage = addedPageOpt.get();
                changeLemmasAndIndicesForExistPage(addedPage);
            }
        } else {
            site = Site.builder()
                    .status(Status.INDEXING)
                    .statusTime(LocalDateTime.now())
                    .lastError(null)
                    .url(siteCfg.getUrl())
                    .name(siteCfg.getName())
                    .build();
            saveSite(site);
        }
        site.setDomain(findDomain(site));
        site.setSubDomain(findSubDomainUrl(site));
        if (addedPage == null) {
            addedPage = Page.builder()
                    .site(site)
                    .path(addedPath)
                    .code(0)
                    .content("")
                    .build();
        }
        pageIndexer = new PageIndexer(connection, site, addedPage, allPages, allLemmas, allIndices);
        pageIndexer.compute();
        savePages(allPages);
        updateLemmas(allLemmas);
        saveIndices(allIndices);
        changeSiteStatus(site);
        saveSite(site);
    }

    @Transactional
    private void changeLemmasAndIndicesForExistPage(Page existPage) {
//        ToDo: Изменить статус страницы на Indexing
        ArrayList<Index> indices = indexRepository.findIndexByPageId(existPage.getId());
        ArrayList<Lemma> lemmas = new ArrayList<>();
        for (Index index : indices) {
            Optional<Lemma> lemmaOpt = lemmaRepository.findById(index.getLemma().getId());
            if (lemmaOpt.isPresent()) {
                lemmaOpt.get().setFrequency(lemmaOpt.get().getFrequency() - 1);
                lemmas.add(lemmaOpt.get());
            }
        }
        lemmaRepository.flush();
        lemmaRepository.saveAll(lemmas);
        indexRepository.deleteAll(indices);
    }

    @Transactional
    private void updateLemmas(ConcurrentHashMap<String, Lemma> allLemmas) {
        ArrayList<Lemma> updatedLemmas = new ArrayList<>();
        for (Lemma lemma : allLemmas.values()) {
            Optional<Lemma> lemmaOpt = lemmaRepository.findLemmaByLemma(lemma.getLemma());
            if (lemmaOpt.isPresent()) {
                lemmaOpt.get().setFrequency(lemmaOpt.get().getFrequency() + 1);
                updatedLemmas.add(lemmaOpt.get());
            } else {
                updatedLemmas.add(lemma);
            }
        }
        lemmaRepository.flush();
        lemmaRepository.saveAll(updatedLemmas);
    }
}
