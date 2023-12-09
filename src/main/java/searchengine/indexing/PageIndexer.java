package searchengine.indexing;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.context.ApplicationContext;
import searchengine.config.auxclass.Connection;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class PageIndexer extends RecursiveAction {
    private final ApplicationContext context;
    private final String REGEX_SUBDOMAIN_URL_SEARCH;
    private final String REGEX_SUBDOMAIN_URL_RU_SEARCH;
    private final String REGEX_SUBDOMAIN_URL_HTML_SEARCH;
    private final String REGEX_SUBDOMAIN_URL_PHP_SEARCH;
    private final Connection connection;
    private final Site site;
    private final Page page;
    private final ConcurrentSkipListSet<Page> pages;
    private final ConcurrentHashMap<String, Lemma> lemmas;
    private final Set<Index> indices;
    @Setter
    private static volatile Boolean isInterrupted;

    public PageIndexer(Page page, ConcurrentSkipListSet<Page> pages,
                       ConcurrentHashMap<String, Lemma> lemmas, Set<Index> indices) {
        this.context = ApplicationContextHolder.getApplicationContext();
        this.connection = context.getBean(Connection.class);
        this.site = page.getSite();
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
        Document doc = getPage();
        if (!page.getContent().isEmpty() && (page.getCode() != 404)) {
            getLemmasAndIndicesForPage();
        }
        if (pages.isEmpty()) {
            pages.add(page);
            return;
        }
        if (doc == null || isInterrupted) {
            return;
        }
        ConcurrentSkipListSet<Page> newPages = new ConcurrentSkipListSet<>(
                Comparator.comparing(Page::getPath));
        findUrls(doc, newPages);
        List<PageIndexer> taskList = new ArrayList<>();
        for (Page newPage : newPages) {
            if (!isInterrupted) {
                PageIndexer pageIndexerTask = new PageIndexer(newPage, pages, lemmas, indices);
                pageIndexerTask.fork();
                taskList.add(pageIndexerTask);
            }
        }
        taskList.forEach(ForkJoinTask::join);
    }

    private void getLemmasAndIndicesForPage() {
        LemmaSearcher lemmaSearcher = context.getBean(LemmaSearcher.class);
        HashMap<String, Integer> lemmasCounter = lemmaSearcher.searchLemmas(page.getContent());
        for (String pageLemma : lemmasCounter.keySet()) {
            if (lemmas.containsKey(pageLemma)) {
                lemmas.get(pageLemma).setFrequency(lemmas.get(pageLemma).getFrequency() + 1);
            } else {
                Lemma newLemma = createNewLemma(pageLemma);
                lemmas.put(pageLemma, newLemma);
            }
            indices.add(createNewIndex(lemmas.get(pageLemma), lemmasCounter.get(pageLemma)));
        }
    }

    private Index createNewIndex(Lemma lemma, Integer count) {
        return Index.builder()
                .page(page)
                .lemma(lemma)
                .rank(count)
                .build();
    }

    private Lemma createNewLemma(String pageLemma) {
        return Lemma.builder()
                .site(site)
                .lemma(pageLemma)
                .frequency(1)
                .build();
    }

    private Page createNewPage(String path) {
        return Page.builder()
                .site(site)
                .path(path)
                .code(0)
                .content("")
                .build();
    }

    private Document getPage() {
        Document doc = null;
        int statusCode = 0;
        String content = "";
        try {
            Thread.sleep(450);
            log.debug("Обращение по адресу: {}", site.getDomain().concat(page.getPath()));
//            log.debug("Количество най страниц на сайте {} равно: {}", site.getName(), pages.size());
            doc = Jsoup.connect(site.getDomain().concat(page.getPath()))
                    .userAgent(selectAgent())
                    .referrer(connection.getReferrer())
                    .timeout(10000)
                    .followRedirects(false)
                    .get();
            content = doc.outerHtml();
            statusCode = doc.connection().response().statusCode();
        } catch (IOException ex) {
            log.error("Ошибка при работе Jsoup", ex);
            statusCode = getStatus(ex.getMessage());
        } catch (InterruptedException ex) {
            log.error("Ошибка при работе Jsoup", ex);
        }
        page.setCode(statusCode);
        page.setContent(content);
        return doc;
    }

    private String selectAgent() {
        String threadName = Thread.currentThread().getName();
        int threadNumber = Integer.parseInt(threadName.substring(threadName.length() - 1));
        return connection.getUserAgents().get(threadNumber % 2).getAgent();
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
        links.forEach(link -> checkLink(link, subUrlLevel, newPages));
    }

    private int calculateUrlLevel(String url) {
        return url.length() - url.replaceAll("/","").length();
    }

    private void checkLink(Element link, int subUrlLevel, ConcurrentSkipListSet<Page> newPages) {
        if (isInterrupted) {
            return;
        }
        String urlLink = matchUrls(link);
        int urlLevel = calculateUrlLevel(urlLink);
        if (!page.getPath().equals(urlLink) && ((urlLevel - subUrlLevel) >= 0)) {
            Page foundPage = createNewPage(urlLink);
            if (!pages.contains(foundPage)) {
                pages.add(foundPage);
                newPages.add(foundPage);
            }
        }
    }

    private String matchUrls(Element link) {
        String textLink = link.attr("href");
        if (textLink.contains("%")) {
            try {
                textLink = URLDecoder.decode(textLink, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ex) {
                log.error("Ошибка декодирования URL-ссылки", ex);
            }
        }
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
