package searchengine.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.LazyInitializationException;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Lazy;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.SiteEntity;
import searchengine.repository.PageRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MapperThread extends RecursiveTask<TreeSet<String>> {

//    ToDo: Нужно ли объявлять в полях класса regex, которые используются только в конструкторе?
    public static final String REGEX_SLASH_TEXT_SLASH = "(/\\w+[[\\-_]?\\w+]*)+/?";
    public static final String REGEX_SEARCH_PARAMS = "(\\?\\w+[_]?\\w+[=]\\w+[_]?\\w+(&\\w+[_]?\\w+[=]\\w+[_]?\\w+)*)?";
    public static final String REGEX_HTML_URL = "\\w+([\\-_]?\\w+)*\\.html";
    public static final String REGEX_PHP_URL = "\\w+([\\-_]?\\w+)*\\.php";
    public final String REGEX_DOMAIN_URL_SEARCH;
    public final String REGEX_DOMAIN_URL_HTML_SEARCH;
    public final String REGEX_DOMAIN_URL_PHP_SEARCH;
    @Autowired
    private final PageRepository pageRepository;
    // ToDo: Сделать regex для русского языка и добавить его обработку.
    private static final Logger LOGGER = LogManager.getLogger(MapperThread.class);
    private SiteAddress siteAddress;
    private String subUrl;
    private TreeSet<String> parentUrls;
    private TreeSet<String> subUrls = new TreeSet<>();

    public MapperThread(PageRepository pageRepository, SiteAddress siteAddress, String subUrl, TreeSet<String> parentUrls) {
        this.pageRepository = pageRepository;
        this.siteAddress = siteAddress;
        this.subUrl = subUrl;
        this.parentUrls = parentUrls;
        this.REGEX_DOMAIN_URL_SEARCH = "href=\"(" + siteAddress.getDomain() + ")?(" +
                siteAddress.getSubDomainUrl() + REGEX_SLASH_TEXT_SLASH + REGEX_SEARCH_PARAMS + ")\"";
        this.REGEX_DOMAIN_URL_HTML_SEARCH = "href=\"(" + siteAddress.getDomain() + ")?(" +
                siteAddress.getSubDomainUrl() + REGEX_SLASH_TEXT_SLASH + REGEX_HTML_URL +
                REGEX_SEARCH_PARAMS + ")\"";
        this.REGEX_DOMAIN_URL_PHP_SEARCH = "href=\"(" + siteAddress.getDomain() + ")?(" +
                siteAddress.getSubDomainUrl() + REGEX_SLASH_TEXT_SLASH + REGEX_PHP_URL +
                REGEX_SEARCH_PARAMS + ")\"";
    }

    @Override
//    @Transactional
    protected TreeSet<String> compute() {
        Document doc = null;
        int statusCode = 0;
        String content = "";

        try {
            Thread.currentThread().sleep(150);
            LOGGER.info("Обращение по адресу: " + siteAddress.getDomain().concat(subUrl));
            String threadName = Thread.currentThread().getName();
            int threadNumber = Integer.parseInt((threadName).substring(threadName.length() - 1, threadName.length() ));
            if (threadNumber % 2 == 0) {
                doc = Jsoup.connect(siteAddress.getDomain().concat(subUrl)).userAgent("Mozilla/5.0 " +
                        "(Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36").referrer("http://google.com")
//                      .timeout(10000)
//                      .ignoreContentType(true)
//                      .ignoreHttpErrors(true)
//                      .followRedirects(false)
//                      .execute();
                        .get();
            } else {
                doc = Jsoup.connect(siteAddress.getDomain().concat(subUrl)).
                        userAgent("ExperimentalSearchBot").referrer("http://google.com").get();
            }
            String head = String.valueOf(doc.head());
            String body = String.valueOf(doc.body());
            content = head.concat("\n").concat(body);
            statusCode = doc.connection().response().statusCode();
        } catch (HttpStatusException e) {
            e.getMessage();
        } catch (IOException e) {
            e.getMessage();
            String message = e.getMessage();
            int position = message.indexOf("Status=");
            if (position == -1) {
                statusCode = 522;
            } else {
                statusCode = Integer.parseInt(message.substring(position + 7, position + 10));
            }
        } catch (InterruptedException e) {
            e.getMessage();
        }

        Page existPage = null;
        try {
//            ToDo: Для осуществления поиска адреса страницы по site_id нужно передавать в метод SiteEntity
            existPage = pageRepository.findPageByPath(subUrl);
//            SiteEntity site = existPage.getSite();
//            System.out.println(site.getId());
        } catch (LazyInitializationException e) {
            e.printStackTrace();
        }
        if (existPage == null) {
            Page page = Page.builder()
                    .site(siteAddress.getSite())
                    .path(subUrl)
                    .code(statusCode)
                    .content(content)
                    .build();
            pageRepository.save(page);
            pageRepository.flush();
        }
        if (doc == null) {
            return subUrls;
        }

        parentUrls.addAll(findUrls(doc));
        List<MapperThread> taskList = new ArrayList<>();
        for (String subUrl : subUrls) {
            MapperThread mapperThreadTask = new MapperThread(pageRepository, siteAddress, subUrl, parentUrls);
            mapperThreadTask.fork();
            taskList.add(mapperThreadTask);
        }
        for (MapperThread mapperThreadTask : taskList) {
            subUrls.addAll(mapperThreadTask.join());
        }
        return subUrls;
    }
//    ToDo: Выяснить нужно ли в явном виде передавать параметр subUrls в метод

    public TreeSet<String> findUrls(Document doc) {
        int subUrlLevel = subUrl.length() - subUrl.replaceAll("/","").length();
        Elements links = doc.select("[^href]");
        links.forEach(link -> {
                    String textLink = link.toString();
                    Pattern pattern1 = Pattern.compile(REGEX_DOMAIN_URL_SEARCH);
                    Matcher matcher1 = pattern1.matcher(textLink);
                    Pattern pattern2 = Pattern.compile(REGEX_DOMAIN_URL_HTML_SEARCH);
                    Matcher matcher2 = pattern2.matcher(textLink);
                    Pattern pattern3 = Pattern.compile(REGEX_DOMAIN_URL_PHP_SEARCH);
                    Matcher matcher3 = pattern3.matcher(textLink);
                    String urlLink = "";
                    if (matcher1.find()) {
                        urlLink = matcher1.group(2).trim();
                    }
                    else if (matcher2.find()) {
                        urlLink = matcher2.group(2).trim();
                    }
                    else if (matcher3.find()) {
                        urlLink = matcher3.group(2).trim();
                    }
                    int urlLevel = urlLink.length() - urlLink.replaceAll("/","").length();
                    if (!subUrl.equals(urlLink) && (urlLevel - subUrlLevel) >= 0 && !parentUrls.contains(urlLink)) {
                        subUrls.add(urlLink);
                    }
                }
        );
        return subUrls;
    }
}