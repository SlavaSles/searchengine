package searchengine.logic.indexing;

import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.auxclass.Connection;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Thread.sleep;

public class PageIndexer extends RecursiveAction {
    private final String REGEX_SUBDOMAIN_URL_SEARCH;
    private final String REGEX_SUBDOMAIN_URL_RU_SEARCH;
    private final String REGEX_SUBDOMAIN_URL_HTML_SEARCH;
    private final String REGEX_SUBDOMAIN_URL_PHP_SEARCH;
    private final Connection connection;
    private final Site site;
    private final Page page;
    private final ConcurrentSkipListSet<Page> pages;
    private final ConcurrentHashMap<String, Lemma> lemmas;
    private final Set<Index> indices;// = new ConcurrentHashMap<>().newKeySet();
    @Setter
    private static volatile Boolean isInterrupted;
    private static final Logger LOGGER = LogManager.getLogger(PageIndexer.class);

//    ToDo: Попробовать исключить Site из конструктора
    public PageIndexer(Connection connection, Site site, Page page, ConcurrentSkipListSet<Page> pages,
                       ConcurrentHashMap<String, Lemma> lemmas, Set<Index> indices) {
        this.connection = connection;
        this.site = site;
        this.page = page;
        this.pages = pages;
        this.lemmas = lemmas;
        this.indices = indices;
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
//        ToDo: Тут слишком много if идут подряд
        if (isInterrupted) {
            return;
        }
        doc = getPage();
        if (!page.getContent().equals("")) {
            getLemmasAndIndicesForPage();
        }
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
        List<PageIndexer> taskList = new ArrayList<>();
        for (Page newPage : newPages) {
            if (!isInterrupted) {
                PageIndexer pageIndexerTask = new PageIndexer(connection, site, newPage, pages, lemmas, indices);
                pageIndexerTask.fork();
                taskList.add(pageIndexerTask);
            }
        }
        for (PageIndexer pageIndexerTask : taskList) {
            pageIndexerTask.join();
        }
    }

    private void getLemmasAndIndicesForPage() {
        LemmaSearcher lemmaSearcher = new LemmaSearcher();
        HashMap<String, Integer> lemmasCounter = lemmaSearcher.searchLemmas(page.getContent());
        for (String pageLemma : lemmasCounter.keySet()) {
//            ToDo: Тут можно и по другому сделать проверку
            if (lemmas.containsKey(pageLemma)) {
                lemmas.get(pageLemma).setFrequency(lemmas.get(pageLemma).getFrequency() + 1);
            } else {
                Lemma newLemma = Lemma.builder()
                        .site(site)
                        .lemma(pageLemma)
                        .frequency(1)
                        .build();
                lemmas.put(pageLemma, newLemma);
            }
            Index newIndex = Index.builder()
                    .page(page)
                    .lemma(lemmas.get(pageLemma))
                    .rank(lemmasCounter.get(pageLemma))
                    .build();
            indices.add(newIndex);
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
