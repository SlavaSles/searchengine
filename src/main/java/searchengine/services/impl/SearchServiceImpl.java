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

//    @Transactional
    @Override
    public SearchResponse search(String query, String site, Integer offset, Integer limit) {
        Set<String> searchingLemmas =  new LemmaSearcherImpl().getLemmas(query);
        List<Map<Lemma, Float>> findingLemmasOnSites = new ArrayList<>();
//        ToDo: Сделать выборку для нескольких сайтов
        Map<Lemma, Float> findingLemmas = new TreeMap<>(Comparator.comparing(Lemma::getFrequency));
        for (SiteCfg siteCfg : sites.getSites()) {
            findingLemmas = findLemmasOnSite(site, siteCfg, searchingLemmas);
            if (searchingLemmas.size() == findingLemmas.size()) {
                findingLemmasOnSites.add(findingLemmas);
            } else {
                continue;
            }

        }
//        if (searchingLemmas.size() != findingLemmas.size()) {
//            return createEmptyListResponse();
//        }
        findingLemmas.forEach((key, value) -> System.out.println(key + " - " + value));
        findingLemmas.entrySet().removeIf(entry -> entry.getValue() > 0.1 && findingLemmas.size() > 2);
        Map<Page, Float> pagesWithRelevance = new HashMap<>();
        boolean firstLemma = true;
        for (Lemma lemma : findingLemmas.keySet()) {
            if (firstLemma) {
                firstLemma = false;
                findPagesWithFirstLemma(pagesWithRelevance, lemma);
                continue;
            }
            if (pagesWithRelevance.isEmpty()) {
                break;
            }
            pagesWithRelevance = findLemmaOnPages(pagesWithRelevance, lemma);
        }
        System.out.println("Количество найденных страниц: " + pagesWithRelevance.size());
//        pagesWithRelevance.forEach((k, v) -> System.out.println(k.getId() + " - " + k.getPath() + " - " + k.getSite().getUrl() + " - " + v));
        if (pagesWithRelevance.isEmpty()) {
            return createEmptyListResponse();
        }
        Float maxAbsRelevance = pagesWithRelevance.values().stream().max(Float::compareTo).get();
//        ToDo: На этом этапе можно сливать мапы с разных сайтов в одну
        pagesWithRelevance.entrySet().forEach(entry -> entry.setValue(entry.getValue() / maxAbsRelevance));
        List<Map.Entry<Page, Float>> sortedPagesWithRelevance = new ArrayList<>(pagesWithRelevance.entrySet().stream().toList());
        sortedPagesWithRelevance.sort((e1, e2) -> -e1.getValue().compareTo(e2.getValue()));
        List<Map.Entry<Page, Float>> reducedPagesWithRelevance = sortedPagesWithRelevance.stream()
                .skip(offset).limit(limit).toList();
        reducedPagesWithRelevance.forEach((entry) -> System.out.println(entry.getKey().getId() + " - " + entry.getKey().getPath() + " - " + entry.getKey().getSite().getUrl() + " - " + entry.getValue()));
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
        searchItems.sort(Comparator.comparing(DetailedSearchItem::getRelevance).reversed());
        return SearchResponse.builder()
                .count(pagesWithRelevance.size())
                .data(searchItems)
                .build();
    }

    private void findPagesWithFirstLemma(Map<Page, Float> pagesWithRelevance, Lemma lemma) {
        List<Index> indices = indexRepository.findIndexByLemmaId(lemma.getId());
        if (!indices.isEmpty()) {
            for (Index index : indices) {
                pagesWithRelevance.put(index.getPage(), index.getRank());
            }
        }
    }

    private Map<Page, Float> findLemmaOnPages(Map<Page, Float> pagesWithRelevance, Lemma lemma) {
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

    private String getSnippet(String content, Set<String> searchingLemmas) {
        LemmaSearcher lemmaSearcher = new LemmaSearcherImpl();
        return lemmaSearcher.getSnippet(content, searchingLemmas);
    }

    private Map<Lemma, Float> findLemmasOnSite(String site, SiteCfg siteCfg, Set<String> searchingLemmas) {
        Map<Lemma, Float> findingLemmas = new TreeMap<>(Comparator.comparing(Lemma::getFrequency));
        if (!site.isEmpty() && !site.equals(siteCfg.getUrl())) {
            return findingLemmas;
        }
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
}
