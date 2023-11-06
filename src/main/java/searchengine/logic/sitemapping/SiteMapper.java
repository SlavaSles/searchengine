package searchengine.logic.sitemapping;

import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.auxclass.Connection;
import searchengine.model.Page;
import searchengine.model.Site;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Thread.sleep;

public class SiteMapper extends RecursiveAction {
    private final String REGEX_SUBDOMAIN_URL_SEARCH;
    private final String REGEX_SUBDOMAIN_URL_RU_SEARCH;
    private final String REGEX_SUBDOMAIN_URL_HTML_SEARCH;
    private final String REGEX_SUBDOMAIN_URL_PHP_SEARCH;
    private final Connection connection;
    private final Site site;
    private final Page page;
    private final ConcurrentSkipListSet<Page> pages;
    @Setter
    private static volatile Boolean isInterrupted;
    private static final Logger LOGGER = LogManager.getLogger(SiteMapper.class);

    public SiteMapper(Connection connection, Site site, Page page, ConcurrentSkipListSet<Page> pages) {
        this.connection = connection;
        this.site = site;
        this.page = page;
        this.pages = pages;
        this.REGEX_SUBDOMAIN_URL_SEARCH = "^(" + site.getSubDomain() +
                Regexes.SLASH_TEXT_SLASH + Regexes.SEARCH_PARAMS + ")$";
        this.REGEX_SUBDOMAIN_URL_RU_SEARCH = "^(" + site.getSubDomain() +
                Regexes.SLASH_TEXT_SLASH_RU + Regexes.SEARCH_PARAMS_RU + ")$";
        this.REGEX_SUBDOMAIN_URL_HTML_SEARCH = "^(" + site.getSubDomain() +
                Regexes.SLASH_TEXT_SLASH + Regexes.HTML_URL + Regexes.SEARCH_PARAMS + ")$";
        this.REGEX_SUBDOMAIN_URL_PHP_SEARCH = "^(" + site.getSubDomain() +
                Regexes.SLASH_TEXT_SLASH + Regexes.PHP_URL + Regexes.SEARCH_PARAMS + ")$";
    }

    @Override
    protected void compute() {
        Document doc;
        if (isInterrupted) {
            return;
        }
        doc = getPage();
        if (pages.isEmpty()) {
            pages.add(page);
            return;
        }
        if (doc == null) {
            return;
        }
        if (isInterrupted) {
            return;
        }
        ConcurrentSkipListSet<Page> newPages = new ConcurrentSkipListSet<>(
                Comparator.comparing(Page::getPath));
        findUrls(doc, newPages);
        List<SiteMapper> taskList = new ArrayList<>();
        for (Page newPage : newPages) {
            if (!isInterrupted) {
                SiteMapper siteMapperTask = new SiteMapper(connection, site, newPage, pages);
                siteMapperTask.fork();
                taskList.add(siteMapperTask);
            }
        }
        for (SiteMapper siteMapperTask : taskList) {
            siteMapperTask.join();
        }
    }

    private Document getPage() {
        Document doc = null;
        int statusCode = 0;
        String content = "";
        try {
            sleep(300);
            LOGGER.info("Обращение по адресу: " + site.getDomain().concat(page.getPath()));
            doc = Jsoup.connect(site.getDomain().concat(page.getPath()))
                    .userAgent(connection.getUserAgents().get(selectAgent()).getAgent())
                    .referrer(connection.getReferrer())
                    .timeout(10000)
                    .followRedirects(false)
                    .get();
            content = doc.outerHtml();
            statusCode = doc.connection().response().statusCode();
        } catch (HttpStatusException e) {
            statusCode = getStatus(e.getMessage());
        } catch (IOException e) {
            statusCode = getStatus(e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        page.setCode(statusCode);
        page.setContent(content);
        return doc;
    }

    private int selectAgent() {
        String threadName = Thread.currentThread().getName();
        int threadNumber = Integer.parseInt(threadName.substring(threadName.length() - 1));
        return threadNumber % 2;
    }

    private int getStatus(String message) {
        int position = message.indexOf("Status=");
        if (position == -1) {
            return 522;
        } else {
            return Integer.parseInt(message.substring(position + 7, position + 10));
        }
    }

    private void findUrls(Document doc, ConcurrentSkipListSet<Page> newPages) {
        int subUrlLevel = calculateUrlLevel(page.getPath());
        Elements links = doc.select("a");
        links.forEach(link -> {
                    if (!isInterrupted) {
                        String urlLink = urlMatching(link);
                        int urlLevel = calculateUrlLevel(urlLink);
                        if (!page.getPath().equals(urlLink) && ((urlLevel - subUrlLevel) >= 0)) {
                            Page findPage = Page.builder()
                                    .site(site)
                                    .path(urlLink)
                                    .code(0)
                                    .content("")
                                    .build();
                            if (!pages.contains(findPage)) {
                                pages.add(findPage);
                                newPages.add(findPage);
                            }
                        }
                    }
                }
        );
    }

    private int calculateUrlLevel(String url) {
        return url.length() - url.replaceAll("/","").length();
    }

    private String urlMatching(Element link) {
        String textLink = link.attr("href");
        String urlLink = "";
        List<String> regexes = Arrays.asList(REGEX_SUBDOMAIN_URL_SEARCH, REGEX_SUBDOMAIN_URL_HTML_SEARCH,
                REGEX_SUBDOMAIN_URL_PHP_SEARCH, REGEX_SUBDOMAIN_URL_RU_SEARCH);
        if (textLink.startsWith("http")) {
            List<String> httpRegexes = new ArrayList<>();
            for (String regex : regexes) {
                httpRegexes.add(site.getDomain().concat(regex.substring(1)));
            }
            regexes = httpRegexes;
        }
        for (String regex : regexes) {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(textLink);
            if (matcher.find()) {
                urlLink = matcher.group(1).trim();
                break;
            }
        }
        return urlLink;
    }
}
