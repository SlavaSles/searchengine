package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import searchengine.config.SiteCfg;
import searchengine.config.SitesList;
import searchengine.logic.indexing.IndexingThread;
import searchengine.logic.indexing.PageIndexer;

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

    public boolean startIndexing() {
        fjp = new ForkJoinPool();
        if (currentTasks.isEmpty()) {
            currentTasks.add(new Thread(System.out::println));
            PageIndexer.setIsInterrupted(false);
            for(SiteCfg siteCfg : sites.getSites()) {
                IndexingThread indexingThread = getIndexingThread(siteCfg);
                indexingThread.setFjp(fjp);
                currentTasks.add(indexingThread);
                indexingThread.start();
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean stopIndexing() {
        if (!currentTasks.isEmpty()) {
            PageIndexer.setIsInterrupted(true);
            for (Thread task : currentTasks) {
                task.interrupt();
            }
            fjp.shutdown();
            currentTasks.clear();
            return true;
        } else {
            return false;
        }
    }

    public boolean indexPage(String url) {
        boolean correctUrl = false;
        PageIndexer.setIsInterrupted(false);
        for(SiteCfg siteCfg : sites.getSites()) {
            if (url.startsWith(siteCfg.getUrl().concat("/"))) {
                IndexingThread indexingThread = getIndexingThread(siteCfg);
                indexingThread.setAddedUrl(url);
                indexingThread.start();
                correctUrl = true;
                break;
            }
        }
        return correctUrl;
    }

    private IndexingThread getIndexingThread(SiteCfg siteCfg) {
        IndexingThread indexingThread = context.getBean(IndexingThread.class);
        indexingThread.setSiteCfg(siteCfg);
        return indexingThread;
    }
}
