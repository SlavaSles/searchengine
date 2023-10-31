package searchengine.logic.sitemapping;

public class Regexes {
    public static final String TDT = "%?\\w+([\\-_%\\w])*"; // Text - dash - text regex
    public static final String RUDRU = "[А-Яа-я]*%?\\w*([\\-_%][А-Яа-я]*\\w*)*"; // Russian - dash - russian regex
    public static final String DOMAIN = "(https?://(www\\.)?" + TDT + "(\\." + TDT + ")*\\.\\w+)/?";
    public static final String DOMAIN_RU = "(https?://(www\\.)?" + RUDRU + "(\\." + RUDRU + ")*\\.[А-Яа-я0-9]+)/?";
    public static final String SLASH_TEXT_SLASH = "((/" + TDT + ")+/?)";
    public static final String SLASH_TEXT_SLASH_RU = "((/" + RUDRU + ")+/?)";
    public static final String SEARCH_PARAMS = "(\\?" + TDT + "=" + TDT +
            "(&" + TDT + "=" + TDT + ")*)?";
    public static final String SEARCH_PARAMS_RU = "(\\?" + RUDRU + "=" + RUDRU +
            "(&" + RUDRU + "=" + RUDRU + ")*)?";
    public static final String PHP_URL = TDT + "\\.php";
    public static final String HTML_URL = TDT + "\\.html";
}
