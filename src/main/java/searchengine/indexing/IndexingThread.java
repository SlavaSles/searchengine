package searchengine.indexing;

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
        Site site = createNewSite();
        deleteIndexFromDbForExistSite(site);
        saveSite(site);
        if (!Thread.currentThread().isInterrupted()) {
            indexSite(site);
            removeUnmappedPages();
        }
        savePages();
        saveLemmas();
        saveIndices();
        changeSiteStatus(site);
        saveSite(site);
    }

    private Site createNewSite() {
        Site site = Site.builder()
                .status(Status.INDEXING)
                .statusTime(LocalDateTime.now())
                .lastError(null)
                .url(siteCfg.getUrl())
                .name(siteCfg.getName())
                .build();
        site.setDomain(findDomain(site));
        site.setSubDomain(findSubDomainUrl(site));
        return site;
    }

    private Page createNewPage(Site site, String path) {
        return Page.builder()
                .site(site)
                .path(path)
                .code(0)
                .content("")
                .build();
    }

    private void indexSite(Site site) {
        Page firstPage = createNewPage(site, site.getSubDomain().concat("/"));
        allPages.add(firstPage);
        fjp.invoke(new PageIndexer(connection, firstPage, allPages, allLemmas, allIndices));
    }

    private void changeSiteStatus(Site site) {
        if (Thread.currentThread().isInterrupted()) {
            site.setStatus(Status.FAILED);
            site.setLastError("Операция прервана пользователем");
        } else {
            site.setStatus(Status.INDEXED);
            site.setLastError(null);
        }
        site.setStatusTime(LocalDateTime.now());
    }

    private void removeUnmappedPages() {
        if (Thread.currentThread().isInterrupted()) {
            allPages.removeIf(page -> page.getCode() == 0);
        }
    }

    @Transactional
    public void saveSite(Site site) {
        siteRepository.flush();
        siteRepository.save(site);
    }

    @Transactional
    public void savePages() {
        pageRepository.flush();
        pageRepository.saveAll(allPages);
    }

    @Transactional
    public void saveLemmas() {
        lemmaRepository.flush();
        lemmaRepository.saveAll(allLemmas.values());
    }

    @Transactional
    public void saveIndices() {
        indexRepository.flush();
        indexRepository.saveAll(allIndices);
    }

    @Transactional
    public void deleteIndexFromDbForExistSite(Site site) {
        Optional<Site> existSiteOpt = siteRepository.findSiteByUrl(site.getUrl());
        if (existSiteOpt.isPresent()) {
            ArrayList<Page> pages = pageRepository.findPageBySiteId(existSiteOpt.get().getId());
            pages.forEach(this::deleteExistIndices);
            deleteExistLemmas(existSiteOpt.get());
            pageRepository.deleteAll(pages);
            siteRepository.delete(existSiteOpt.get());
        }
    }

    @Transactional
    public void deleteExistIndices(Page page) {
        ArrayList<Index> indices = indexRepository.findIndexByPageId(page.getId());
        indexRepository.deleteAll(indices);
    }

    @Transactional
    public void deleteExistLemmas(Site site) {
        ArrayList<Lemma> lemmas = lemmaRepository.findLemmaBySiteId(site.getId());
        lemmaRepository.deleteAll(lemmas);
    }

    private String findDomain(Site site) {
        List<String> regexes = Arrays.asList(Regexes.DOMAIN, Regexes.DOMAIN_RU);
        return matchUrlParts(site.getUrl(), regexes);
    }

    private String findSubDomainUrl(Site site) {
        List<String> regexes = Arrays.asList(Regexes.SLASH_TEXT_SLASH, Regexes.SLASH_TEXT_SLASH_RU);
        if (site.getUrl().length() - site.getDomain().length() <= 1) {
            return "";
        }
        return matchUrlParts(site.getUrl().substring(site.getDomain().length()), regexes);
    }

    private String matchUrlParts(String text, List<String> regexes) {
        for (String regex : regexes) {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        return "";
    }

    private void addIndexingPage(String addedUrl) {
        String addedPath = addedUrl.substring(siteCfg.getUrl().length());
        Site site = findSiteByUrl();
        saveSite(site);
        Page addedPage = findPageByPathAndSiteId(site, addedPath);
        if (addedPage.getId() != null) {
            changeLemmasAndIndicesForExistPage(addedPage);
        }
        new PageIndexer(connection, addedPage, allPages, allLemmas, allIndices).compute();
        savePages();
        updateLemmas(site);
        saveIndices();
        changeSiteStatus(site);
        saveSite(site);
    }

    private Site findSiteByUrl() {
        Optional<Site> existSiteOpt = siteRepository.findSiteByUrl(siteCfg.getUrl());
        if (existSiteOpt.isPresent()) {
            Site site = existSiteOpt.get();
            site.setStatus(Status.INDEXING);
            site.setDomain(findDomain(site));
            site.setSubDomain(findSubDomainUrl(site));
            return site;
        } else {
            return createNewSite();
        }
    }

    private Page findPageByPathAndSiteId(Site site, String path) {
        Optional<Page> addedPageOpt = pageRepository.findPageBySiteIdAndPath(site.getId(), path);
        if (addedPageOpt.isPresent()) {
            Page addedPage = addedPageOpt.get();
            addedPage.setSite(site);
            return addedPage;
        } else {
            return createNewPage(site, path);
        }
    }

    @Transactional
    public void changeLemmasAndIndicesForExistPage(Page existPage) {
        ArrayList<Index> indices = indexRepository.findIndexByPageId(existPage.getId());
        lemmaRepository.flush();
        for (Index index : indices) {
            Optional<Lemma> lemmaOpt = lemmaRepository.findById(index.getLemma().getId());
            if (lemmaOpt.isPresent()) {
                Lemma updatedLemma = lemmaOpt.get();
                updatedLemma.setFrequency(updatedLemma.getFrequency() - 1);
                lemmaRepository.save(updatedLemma);
            }
        }
        indexRepository.deleteAll(indices);
    }

    @Transactional
    public void updateLemmas(Site site) {
        lemmaRepository.flush();
        for (Lemma lemma : allLemmas.values()) {
            Optional<Lemma> lemmaOpt = lemmaRepository.findLemmaBySiteIdAndLemma(site.getId(), lemma.getLemma());
            if (lemmaOpt.isPresent()) {
                lemma.setId(lemmaOpt.get().getId());
                lemma.setFrequency(lemmaOpt.get().getFrequency() + 1);
            }
            lemmaRepository.save(lemma);
        }
    }
}
