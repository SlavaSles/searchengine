package searchengine.dto.message;

public class ErrorMessage {

    public final static String START_INDEXING_ERROR = "Индексация уже запущена";
    public final static String STOP_INDEXING_ERROR = "Индексация не запущена";
    public final static String PAGE_INDEXING_ERROR = "Данная страница находится за пределами сайтов, " +
            "указанных в конфигурационном файле";
    public final static String PAGE_NOT_FOUND = "Указанная страница не найдена";
    public final static String EMPTY_SEARCH_QUERY = "Задан пустой поисковый запрос";
}
