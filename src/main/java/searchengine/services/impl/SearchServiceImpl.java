package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SiteCfg;
import searchengine.config.SitesList;
import searchengine.dto.SearchResponse;
import searchengine.dto.search.DetailedSearchItem;
import searchengine.logic.indexing.LemmaSearcher;
import searchengine.logic.indexing.impl.LemmaSearcherImpl;
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

    @Override
    public SearchResponse search(String query, String site, Integer offset, Integer limit) {
        Set<String> searchingLemmas =  new LemmaSearcherImpl().getLemmas(query);
        List<Map<Page, Float>> findingPagesWithRelevanceForSites = new ArrayList<>();
        for (SiteCfg siteCfg : sites.getSites()) {
            if (!site.isEmpty() && !site.equals(siteCfg.getUrl())) {
                continue;
            }
            Map<Lemma, Float> findingLemmas = findLemmasOnSite(siteCfg, searchingLemmas);
            if (searchingLemmas.size() != findingLemmas.size()) {
                continue;
            }
            findingLemmas.forEach((key, value) -> System.out.println(key + " - " + value));
//            Исключение часто встречающихся лемм. В поисковой выдаче попадаются результаты без этих лемм.
//            findingLemmas.entrySet().removeIf(entry -> entry.getValue() > 0.1f && findingLemmas.size() > 2);
            Map<Page, Float> pagesWithRelevance = findPagesWithRelevance(findingLemmas);
            System.out.println("Количество найденных страниц: " + pagesWithRelevance.size());
//        pagesWithRelevance.forEach((k, v) -> System.out.println(k.getId() + " - " + k.getPath() + " - " + k.getSite().getUrl() + " - " + v));
            if (!pagesWithRelevance.isEmpty()) {
                findingPagesWithRelevanceForSites.add(pagesWithRelevance);
            }
        }
        if (findingPagesWithRelevanceForSites.isEmpty()) {
            return createEmptyListResponse();
        }
        Map<Page, Float> pagesWithRelevanceForAllSites = joinAllResultPages(findingPagesWithRelevanceForSites);
        calculateRelativelyRelevance(pagesWithRelevanceForAllSites);
        List<Map.Entry<Page, Float>> reducedPagesWithRelevance =
                reduceAllPages(pagesWithRelevanceForAllSites, offset, limit);
        List<DetailedSearchItem> searchItems = createSearchItems(reducedPagesWithRelevance);
        return SearchResponse.builder()
                .count(pagesWithRelevanceForAllSites.size())
                .data(searchItems)
                .build();
    }

    @Transactional
    public Map<Lemma, Float> findLemmasOnSite(SiteCfg siteCfg, Set<String> searchingLemmas) {
        Map<Lemma, Float> findingLemmas = new TreeMap<>(Comparator.comparing(Lemma::getFrequency));
        Optional<Site> searchingSiteOpt = siteRepository.findSiteByUrl(siteCfg.getUrl());
        if (searchingSiteOpt.isEmpty() || searchingSiteOpt.get().getStatus() != Status.INDEXED) {
//                    ToDo: Сгенерировать ошибки по запросу
            return findingLemmas;
        }
        int countPages = pageRepository.countPages(searchingSiteOpt.get().getId());
        for (String lemmaStr : searchingLemmas) {
            Optional<Lemma> findingLemmaOpt = lemmaRepository
                    .findLemmaBySiteIdAndLemma(searchingSiteOpt.get().getId(), lemmaStr);
            findingLemmaOpt
                    .ifPresent(lemma -> findingLemmas.put(lemma, (float) lemma.getFrequency() / countPages));
        }
        return findingLemmas;
    }

    private Map<Page, Float> findPagesWithRelevance(Map<Lemma, Float> findingLemmas) {
        Map<Page, Float> pagesWithRelevance = new HashMap<>();
        boolean firstLemma = true;
        for (Lemma lemma : findingLemmas.keySet()) {
            if (firstLemma) {
                firstLemma = false;
                findPagesWithFirstLemma(pagesWithRelevance, lemma);
            } else {
                pagesWithRelevance = findLemmaOnPages(pagesWithRelevance, lemma);
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

    private SearchResponse createEmptyListResponse() {
        return SearchResponse.builder()
                .count(0)
                .data(new ArrayList<>())
                .build();
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
        List<Map.Entry<Page, Float>> reducedPagesWithRelevance = sortedPagesWithRelevance.stream()
                .skip(offset).limit(limit).toList();
        reducedPagesWithRelevance.forEach((entry) -> System.out.println(entry.getKey().getId() + " - "
                + entry.getKey().getPath() + " - " + entry.getKey().getSite().getUrl() + " - " + entry.getValue()));
    return reducedPagesWithRelevance;
    }

    private List<DetailedSearchItem> createSearchItems(List<Map.Entry<Page, Float>> reducedPagesWithRelevance) {
        List<DetailedSearchItem> searchItems = new ArrayList<>();
        for (Map.Entry<Page, Float> pageWithRelevance : reducedPagesWithRelevance) {
            DetailedSearchItem searchItem = DetailedSearchItem.builder()
                    .site(pageWithRelevance.getKey().getSite().getUrl())
                    .siteName(pageWithRelevance.getKey().getSite().getName())
                    .uri(pageWithRelevance.getKey().getPath())
                    .title(Jsoup.parse(pageWithRelevance.getKey().getContent()).title())
                    .snippet("getSnippet(pageWithRelevance.getKey().getContent(), searchingLemmas)")
                    .relevance(pageWithRelevance.getValue())
                    .build();
            searchItems.add(searchItem);
        }
        return searchItems;
    }

    private String getSnippet(String content, Set<String> searchingLemmas) {
        LemmaSearcher lemmaSearcher = new LemmaSearcherImpl();
        return lemmaSearcher.getSnippet(content, searchingLemmas);
    }
}
