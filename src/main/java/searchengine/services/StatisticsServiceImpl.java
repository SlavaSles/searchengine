package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SiteCfg;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Site;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = TotalStatistics.builder()
                .sites(0)
                .pages(0)
                .lemmas(0)
                .indexing(false)
                .build();
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        for (SiteCfg siteCfg : sites.getSites()) {
            Optional<Site> siteOpt = siteRepository.findSiteByUrl(siteCfg.getUrl());
            if (siteOpt.isPresent()) {
                DetailedStatisticsItem siteStatistic = getDetailStatistic(siteOpt.get());
                detailed.add(siteStatistic);
                total.setSites(total.getSites() + 1);
                total.setPages(total.getPages() + siteStatistic.getPages());
                total.setLemmas(total.getLemmas() + siteStatistic.getLemmas());
            }
        }
        total.setIndexing(total.getLemmas() > 0);
        StatisticsData data = StatisticsData.builder()
                .total(total)
                .detailed(detailed)
                .build();
        return StatisticsResponse.builder()
                .result(true)
                .statistics(data)
                .build();
    }

    private DetailedStatisticsItem getDetailStatistic(Site site) {
        return DetailedStatisticsItem.builder()
                .url(site.getUrl())
                .name(site.getName())
                .status(site.getStatus().toString())
                .statusTime(site.getStatusTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .error(site.getLastError())
                .pages(pageRepository.countPages(site.getId()))
                .lemmas(lemmaRepository.countLemmas(site.getId()))
                .build();
    }
}
