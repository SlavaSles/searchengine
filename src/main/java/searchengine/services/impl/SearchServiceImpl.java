package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
        LemmaSearcher lemmaSearcher = new LemmaSearcherImpl();
        Set<String> searchLemmas = lemmaSearcher.getLemmas(query);
        Map<Lemma, Float> findingLemmas = new TreeMap<>(Comparator.comparing(Lemma::getFrequency));
//        ToDo: Сделать выборку для нескольких сайтов
        for (SiteCfg siteCfg : sites.getSites()) {
            if (site.equals(siteCfg.getUrl()) || site.isEmpty()) {
                Optional<Site> searchingSiteOpt = siteRepository.findSiteByUrl(siteCfg.getUrl());
                if (searchingSiteOpt.isPresent()) {
                    int countPages = pageRepository.countPages(searchingSiteOpt.get().getId());
//                    ToDo: Сгенерировать ошибки по запросу
                    if (searchingSiteOpt.get().getStatus() == Status.INDEXED) {
                        for (String lemmaStr : searchLemmas) {
                            System.out.println(lemmaStr);
                            Optional<Lemma> findingLemmaOpt = lemmaRepository
                                    .findLemmaBySiteIdAndLemma(searchingSiteOpt.get().getId(), lemmaStr);
                            findingLemmaOpt
                                    .ifPresent(lemma -> findingLemmas.put(lemma, (float) lemma.getFrequency() / countPages));
                        }
                    }
                }
            }
        }
        if (searchLemmas.size() != findingLemmas.size()) {
            return createEmptyListResponse();
        }
//        findingLemmas.forEach((key, value) -> System.out.println(key + " - " + value));
        findingLemmas.entrySet().removeIf(entry -> entry.getValue() > 0.1);
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
//        System.out.println("Количество найденных страниц: " + pagesWithRelevance.size());
//        pagesWithRelevance.forEach((k, v) -> System.out.println(k.getId() + " - " + k.getPath() + " - " + k.getSite().getUrl() + " - " + v));
        if (pagesWithRelevance.isEmpty()) {
            return createEmptyListResponse();
        }
        Float maxAbsRelevance = pagesWithRelevance.values().stream().max(Float::compareTo).get();
        pagesWithRelevance.entrySet().forEach(entry -> entry.setValue(entry.getValue() / maxAbsRelevance));
        List<DetailedSearchItem> searchItems = new ArrayList<>();
        for (Map.Entry<Page, Float> pageWithRelevance: pagesWithRelevance.entrySet()) {
            DetailedSearchItem searchItem = DetailedSearchItem.builder()
                    .site(pageWithRelevance.getKey().getSite().getUrl())
                    .siteName(pageWithRelevance.getKey().getSite().getName())
                    .uri(pageWithRelevance.getKey().getPath())
                    .title("")
                    .snippet("")
                    .relevance(pageWithRelevance.getValue())
                    .build();
            searchItems.add(searchItem);
        }
        searchItems.sort(Comparator.comparing(DetailedSearchItem::getRelevance).reversed());
        return SearchResponse.builder()
                .count(pagesWithRelevance.size())
                .data(searchItems.stream().skip(offset).limit(limit).toList())
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
}
