package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.Page;
import searchengine.repository.PageRepository;

import java.util.TreeSet;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Mapper {
    public static final String REGEX_DOMAIN = "(http[s]?://(www\\.)?\\w+[[\\-_]?\\w+]*\\.\\w+)/?";
    public static final String REGEX_SLASH_TEXT_SLASH = "((/\\w+[[\\-_]?\\w+]*)+)/?";
    private SiteAddress siteAddress;
    private TreeSet<String> subUrls = new TreeSet<>();
    @Autowired
    private final PageRepository pageRepository;
    public Mapper(String originDomainUrl, PageRepository pageRepository, SiteAddress siteAddress) {
        this.pageRepository = pageRepository;
        this.siteAddress = siteAddress;
        siteAddress.setDomain(findDomain(originDomainUrl));
        siteAddress.setSubDomainUrl(findSubDomainUrl(originDomainUrl));
    }

    public TreeSet<String> sitesMapping() {
        subUrls.addAll( new ForkJoinPool().invoke(new MapperThread(pageRepository, siteAddress,
                siteAddress.getSubDomainUrl(), subUrls)));
        return subUrls;
    }

    public String findDomain(String originDomainUrl) {
        String domain = "";
        Pattern pattern1 = Pattern.compile(REGEX_DOMAIN);
        Matcher matcher1 = pattern1.matcher(originDomainUrl);
        if (matcher1.find()) {
            domain = matcher1.group(1).trim();
        }
        return domain;
    }

    public String findSubDomainUrl(String originDomainUrl) {
        String subDomainUrl = "";
        if (originDomainUrl.length() - siteAddress.getDomain().length() > 1) {
            String secondPartOfUrl = originDomainUrl.substring(siteAddress.getDomain().length());
            Pattern pattern2 = Pattern.compile(REGEX_SLASH_TEXT_SLASH);
            Matcher matcher2 = pattern2.matcher(secondPartOfUrl);
            if (matcher2.find()) {
                subDomainUrl = matcher2.group(1).trim();
                subUrls.add(subDomainUrl);
            }
        } else {
            subUrls.add("/");
        }
        return subDomainUrl;
    }
}
