package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.indexing.ErrorStartIndexingResponse;
import searchengine.dto.indexing.StartIndexingResponse;
import searchengine.model.Page;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    @Autowired
    private final SitesList sites;
    private boolean indexing = false;
    public static final String REGEX_DOMAIN = "(http[s]?://(www\\.)?\\w+[[\\-_]?\\w+]*\\.\\w+)/?";
//    ToDo: Объединить REGEX в Service и Mapper
    public static final String REGEX_SLASH_TEXT_SLASH = "((/\\w+[[\\-_]?\\w+]*)+)/?";
    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final PageRepository pageRepository;

//    ToDo: SiteAddress можно сделать Singleton для каждого из сайтов и обращаться из потоков к нему, не передавая его
//     через параметры метода.

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
            Set<Page> pages = pageRepository.findPageBySite(existSite);
////            List<Page> pages = pageRepository.findPageBySiteId(siteEntity.getId());
            pageRepository.deleteAll(pages);
            siteRepository.delete(existSite);
        }
        siteRepository.save(siteEntity);
        SiteAddress siteAddress = SiteAddress.builder()
                .site(siteEntity)
                .domain(findDomain(originDomainUrl))
                .build();
        siteAddress.setSubDomainUrl(findSubDomainUrl(originDomainUrl, siteAddress.getDomain()));
        subUrls.add(siteAddress.getSubDomainUrl());
//        Mapper mapper = new Mapper(originDomainUrl, pageRepository, siteAddress);
//        subUrls.addAll( new ForkJoinPool().invoke(new MapperThread(pageRepository, siteAddress,
//                siteAddress.getSubDomainUrl(), subUrls)));
//        ToDo: Убрать повторение из списка аргументов в методе
//        ToDo: Как использовать один FJP для нескольких сайтов?
        new ForkJoinPool().invoke(new MapperThread(pageRepository, siteAddress,
                siteAddress.getSubDomainUrl(), subUrls));
//        indexing = true;
////        subUrls.forEach(System.out::println);
//        if (indexing) {
        System.out.println("Выход из сервиса!");
        return new StartIndexingResponse();
//        } else {
//            return new ErrorStartIndexingResponse();
//        }
    }

//    public TreeSet<String> sitesMapping() {
//
//        return subUrls;
//    }

//    ToDo: Можно перенести эти поля и логику в Site и обращаться напрямую к нему
    public String findDomain(String originDomainUrl) {
        String domain = "";
        Pattern pattern1 = Pattern.compile(REGEX_DOMAIN);
        Matcher matcher1 = pattern1.matcher(originDomainUrl);
        if (matcher1.find()) {
            domain = matcher1.group(1).trim();
        }
        return domain;
    }

    public String findSubDomainUrl(String originDomainUrl, String domain) {
        String subDomainUrl = "/";
        if (originDomainUrl.length() - domain.length() > 1) {
            String secondPartOfUrl = originDomainUrl.substring(domain.length());
            Pattern pattern2 = Pattern.compile(REGEX_SLASH_TEXT_SLASH);
            Matcher matcher2 = pattern2.matcher(secondPartOfUrl);
            if (matcher2.find()) {
                subDomainUrl = matcher2.group(1).trim();
//                subUrls.add(subDomainUrl);
            }
//        } else {
//            subUrls.add("/");
        }
        return subDomainUrl;
    }

}
