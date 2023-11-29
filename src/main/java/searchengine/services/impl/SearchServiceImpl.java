package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SiteCfg;
import searchengine.config.SitesList;
import searchengine.dto.SearchResponse;
import searchengine.dto.search.DetailedSearchItem;
import searchengine.exceptions.EmptySearchQueryException;
import searchengine.exceptions.PageNotFoundException;
import searchengine.exceptions.SiteNotIndexedException;
import searchengine.indexing.impl.LemmaSearcherImpl;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.SearchService;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SitesList sites;
    private Set<String> searchingLemmas;
    private boolean sitesNotIndexed;

    @Override
    public SearchResponse search(String query, String site, Integer offset, Integer limit) {
        if (query.isEmpty()) {
//            add log
            throw new EmptySearchQueryException();
        }
        sitesNotIndexed = true;
        searchingLemmas =  new LemmaSearcherImpl().getLemmas(query);
        List<Map<Page, Float>> findingPagesWithRelevanceForSites = new ArrayList<>();
        for (SiteCfg siteCfg : sites.getSites()) {
            if (!site.isEmpty() && !site.equals(siteCfg.getUrl())) {
                continue;
            }
            Map<Lemma, Float> findingLemmas = findLemmasOnSite(siteCfg);
            if (searchingLemmas.size() != findingLemmas.size()) {
                continue;
            }
//            Закомментировано исключение часто встречающихся лемм. В поисковой выдаче попадаются результаты без этих лемм.
//            Если исключать леммы из поиска, то нужно также убирать их из Set-а, передаваемого в snippet.
//            findingLemmas.entrySet().removeIf(entry -> entry.getValue() > 0.1f && findingLemmas.size() > 2);
            List<Map.Entry<Lemma, Float>> sortedLemmasByFrequency = sortFindingLemmasByFrequency(findingLemmas);
            Map<Page, Float> pagesWithRelevance = findPagesWithRelevance(sortedLemmasByFrequency);
            if (!pagesWithRelevance.isEmpty()) {
                findingPagesWithRelevanceForSites.add(pagesWithRelevance);
            }
        }
        if (sitesNotIndexed) {
//            add log
            throw new SiteNotIndexedException();
        }
        if (findingPagesWithRelevanceForSites.isEmpty()) {
//            add log
            throw new PageNotFoundException();
        }
        Map<Page, Float> pagesWithRelevanceForAllSites = joinAllResultPages(findingPagesWithRelevanceForSites);
        calculateRelativelyRelevance(pagesWithRelevanceForAllSites);
        List<Map.Entry<Page, Float>> reducedPagesWithRelevance =
                reduceAllPages(pagesWithRelevanceForAllSites, offset, limit);
        List<DetailedSearchItem> searchItems = createSearchItems(reducedPagesWithRelevance);
        return createSearchResponse(pagesWithRelevanceForAllSites.size(), searchItems);
    }

    @Transactional
    public Map<Lemma, Float> findLemmasOnSite(SiteCfg siteCfg) {
        Map<Lemma, Float> findingLemmas = new HashMap<>();
        Optional<Site> searchingSiteOpt = siteRepository.findSiteByUrl(siteCfg.getUrl());
        if (searchingSiteOpt.isEmpty() || searchingSiteOpt.get().getStatus() != Status.INDEXED) {
            return findingLemmas;
        }
        sitesNotIndexed = false;
        int countPages = pageRepository.countPages(searchingSiteOpt.get().getId());
        for (String lemmaStr : searchingLemmas) {
            Optional<Lemma> findingLemmaOpt = lemmaRepository
                    .findLemmaBySiteIdAndLemma(searchingSiteOpt.get().getId(), lemmaStr);
            findingLemmaOpt
                    .ifPresent(lemma -> findingLemmas.put(lemma, (float) lemma.getFrequency() / countPages));
        }
        return findingLemmas;
    }

    private List<Map.Entry<Lemma, Float>> sortFindingLemmasByFrequency(Map<Lemma, Float> findingLemmas) {
        List<Map.Entry<Lemma, Float>> sortedLemmasByFrequency =
                new ArrayList<>(findingLemmas.entrySet().stream().toList());
        sortedLemmasByFrequency.sort(Comparator.comparing(e -> e.getKey().getFrequency()));
        return sortedLemmasByFrequency;
    }

    private Map<Page, Float> findPagesWithRelevance(List<Map.Entry<Lemma, Float>> sortedLemmasByFrequency) {
        Map<Page, Float> pagesWithRelevance = new HashMap<>();
        boolean firstLemma = true;
        for (Map.Entry<Lemma, Float> entry : sortedLemmasByFrequency) {
            if (firstLemma) {
                firstLemma = false;
                findPagesWithFirstLemma(pagesWithRelevance, entry.getKey());
            } else {
                pagesWithRelevance = findLemmaOnPages(pagesWithRelevance, entry.getKey());
            }
            if (pagesWithRelevance.isEmpty()) {
                return pagesWithRelevance;
            }
        }
        return pagesWithRelevance;
    }

    @Transactional
    public void findPagesWithFirstLemma(Map<Page, Float> pagesWithRelevance, Lemma lemma) {
        List<Index> indices = indexRepository.findIndexByLemmaId(lemma.getId());
        if (!indices.isEmpty()) {
            for (Index index : indices) {
                pagesWithRelevance.put(index.getPage(), index.getRank());
            }
        }
    }

    @Transactional
    public Map<Page, Float> findLemmaOnPages(Map<Page, Float> pagesWithRelevance, Lemma lemma) {
        Map<Page, Float> reducedPagesWithRelevance = new HashMap<>();
        for (Page page : pagesWithRelevance.keySet()) {
            Optional<Index> indexOpt = indexRepository
                    .findIndexByLemmaIdAndPageId(lemma.getId(), page.getId());
            indexOpt.ifPresent(index -> reducedPagesWithRelevance
                    .put(page, pagesWithRelevance.get(page) + index.getRank()));
        }
        return reducedPagesWithRelevance;
    }

    private Map<Page, Float> joinAllResultPages(List<Map<Page, Float>> findingPagesWithRelevanceForSites) {
        Map<Page, Float> pagesWithRelevanceForAllSites = new HashMap<>();
        for (Map<Page, Float> pagesWithRelevance : findingPagesWithRelevanceForSites) {
            pagesWithRelevanceForAllSites.putAll(pagesWithRelevance);
        }
        return pagesWithRelevanceForAllSites;
    }

    private void calculateRelativelyRelevance(Map<Page, Float> pagesWithRelevanceForAllSites) {
        Float maxAbsRelevance = pagesWithRelevanceForAllSites.values()
                .stream().max(Float::compareTo).get();
        pagesWithRelevanceForAllSites.entrySet()
                .forEach(entry -> entry.setValue(entry.getValue() / maxAbsRelevance));
    }

    private List<Map.Entry<Page, Float>> reduceAllPages(Map<Page, Float> pagesWithRelevanceForAllSites,
                                                        Integer offset, Integer limit) {
        List<Map.Entry<Page, Float>> sortedPagesWithRelevance = new ArrayList<>(pagesWithRelevanceForAllSites
                .entrySet().stream().toList());
        sortedPagesWithRelevance.sort((e1, e2) -> -e1.getValue().compareTo(e2.getValue()));
    return sortedPagesWithRelevance.stream().skip(offset).limit(limit).toList();
    }

    private List<DetailedSearchItem> createSearchItems(List<Map.Entry<Page, Float>> reducedPagesWithRelevance) {
        List<DetailedSearchItem> searchItems = new ArrayList<>();
        for (Map.Entry<Page, Float> pageWithRelevance : reducedPagesWithRelevance) {
            DetailedSearchItem searchItem = DetailedSearchItem.builder()
                    .site(pageWithRelevance.getKey().getSite().getUrl())
                    .siteName(pageWithRelevance.getKey().getSite().getName())
                    .uri(pageWithRelevance.getKey().getPath())
                    .title(Jsoup.parse(pageWithRelevance.getKey().getContent()).title())
                    .snippet(getSnippet(pageWithRelevance.getKey().getContent()))
                    .relevance(pageWithRelevance.getValue())
                    .build();
            searchItems.add(searchItem);
        }
        return searchItems;
    }

    private String getSnippet(String content) {
        return new LemmaSearcherImpl().getSnippet(content, searchingLemmas);
    }

    private SearchResponse createSearchResponse(Integer size, List<DetailedSearchItem> searchItems) {
        return SearchResponse.builder()
                .count(size)
                .data(searchItems)
                .build();
    }
}
