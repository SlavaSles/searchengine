package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import searchengine.config.SiteCfg;
import searchengine.config.SitesList;
import searchengine.exceptions.PageIndexingException;
import searchengine.exceptions.StartIndexingException;
import searchengine.exceptions.StopIndexingException;
import searchengine.indexing.IndexingThread;
import searchengine.indexing.PageIndexer;
import searchengine.services.IndexingService;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final ApplicationContext context;
    private final SitesList sites;
    private final List<Thread> currentTasks = new ArrayList<>();
    private ForkJoinPool fjp;

    public void startIndexing() {
        if (currentTasks.isEmpty()) {
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
//            add log
            throw new StartIndexingException();
        }
    }

    public void stopIndexing() {
        if (!currentTasks.isEmpty()) {
            PageIndexer.setIsInterrupted(true);
            for (Thread task : currentTasks) {
                task.interrupt();
            }
            fjp.shutdown();
            currentTasks.clear();
        } else {
            //            add log
            throw new StopIndexingException();
        }
    }

    public void indexPage(String url) {
        boolean correctUrl = false;
        String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8).substring(4);
        PageIndexer.setIsInterrupted(false);
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
            //            add log
            throw new PageIndexingException();
        }
    }

    private IndexingThread getIndexingThread(SiteCfg siteCfg) {
        IndexingThread indexingThread = context.getBean(IndexingThread.class);
        indexingThread.setSiteCfg(siteCfg);
        return indexingThread;
    }
}
