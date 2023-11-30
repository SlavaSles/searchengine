package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import searchengine.config.SiteCfg;
import searchengine.config.SitesList;
import searchengine.exceptions.PageIndexingException;
import searchengine.exceptions.StartIndexingException;
import searchengine.exceptions.StopIndexingException;
import searchengine.exceptions.errorMessage.ErrorMessage;
import searchengine.indexing.IndexingThread;
import searchengine.indexing.PageIndexer;
import searchengine.services.IndexingService;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final ApplicationContext context;
    private final SitesList sites;
    private final List<Thread> currentTasks = new ArrayList<>();
    private ForkJoinPool fjp;

    public void startIndexing() {
        if (currentTasks.isEmpty()) {
            log.info("Запущена индексация сайтов");
            fjp = new ForkJoinPool();
            currentTasks.add(new Thread(System.out::println));
            PageIndexer.setIsInterrupted(false);
            for(SiteCfg siteCfg : sites.getSites()) {
                IndexingThread indexingThread = getIndexingThread(siteCfg);
                indexingThread.setFjp(fjp);
                currentTasks.add(indexingThread);
                indexingThread.start();
            }
        } else if (fjp.getActiveThreadCount() == 0) {
            stopIndexing();
        } else {
            log.info(ErrorMessage.START_INDEXING_ERROR);
            throw new StartIndexingException();
        }
    }

    public void stopIndexing() {
        if (!currentTasks.isEmpty()) {
            log.info("Прервана индексация сайтов");
            PageIndexer.setIsInterrupted(true);
            for (Thread task : currentTasks) {
                task.interrupt();
            }
            fjp.shutdown();
            currentTasks.clear();
        } else {
            log.info(ErrorMessage.STOP_INDEXING_ERROR);
            throw new StopIndexingException();
        }
    }

    public void indexPage(String url) {
        boolean correctUrl = false;
        String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8).substring(4);
        PageIndexer.setIsInterrupted(false);
        log.info("Запущена индексация отдельной страницы: {}", decodedUrl);
        for(SiteCfg siteCfg : sites.getSites()) {
            if (decodedUrl.startsWith(siteCfg.getUrl().concat("/"))) {
                IndexingThread indexingThread = getIndexingThread(siteCfg);
                indexingThread.setAddedUrl(decodedUrl);
                indexingThread.start();
                correctUrl = true;
                break;
            }
        }
        if (!correctUrl) {
            log.info(ErrorMessage.PAGE_INDEXING_ERROR);
            throw new PageIndexingException();
        }
    }

    private IndexingThread getIndexingThread(SiteCfg siteCfg) {
        IndexingThread indexingThread = context.getBean(IndexingThread.class);
        indexingThread.setSiteCfg(siteCfg);
        return indexingThread;
    }
}
