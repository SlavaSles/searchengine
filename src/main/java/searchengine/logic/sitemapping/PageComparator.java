package searchengine.logic.sitemapping;

import searchengine.model.Page;

import java.util.Comparator;

public class PageComparator implements Comparator<Page> {

    @Override
    public int compare(Page page1, Page page2) {
        if(page1.getSite().getId().equals(page2.getSite().getId())) {
            return page1.getPath().compareTo(page2.getPath());
        } else {
            return page1.getSite().getId().compareTo(page2.getSite().getId());
        }
    }
}
