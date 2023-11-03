package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import searchengine.config.SiteCfg;
import searchengine.config.SitesList;
import searchengine.logic.sitemapping.IndexingThread;
import searchengine.logic.sitemapping.SiteMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    @Autowired
    private final ApplicationContext context;
    @Autowired
    private final SitesList sites;
    private final List<IndexingThread> currentTasks = new ArrayList<>();
    private ForkJoinPool fjp;

    public boolean indexing() {
        fjp = new ForkJoinPool();
        if (currentTasks.isEmpty()) {
            SiteMapper.setIsInterrupted(false);
            for(SiteCfg siteCfg : sites.getSites()) {
                IndexingThread indexingThread = context.getBean(IndexingThread.class);
                indexingThread.setSiteCfg(siteCfg);
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
            SiteMapper.setIsInterrupted(true);
            for (IndexingThread task : currentTasks) {
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
        for(SiteCfg siteCfg : sites.getSites()) {
            if (url.startsWith(siteCfg.getUrl().concat("/"))) {
                SiteMapper.setIsInterrupted(false);
                IndexingThread indexingThread = context.getBean(IndexingThread.class);
                indexingThread.setSiteCfg(siteCfg);
                indexingThread.setAddedUrl(url);
                indexingThread.start();
                correctUrl = true;
                break;
            }
        }
        return correctUrl;
    }
}
