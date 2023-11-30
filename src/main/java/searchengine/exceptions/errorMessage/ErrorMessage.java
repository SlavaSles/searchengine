package searchengine.exceptions.errorMessage;

public class ErrorMessage {

    public static final String START_INDEXING_ERROR = "Индексация уже запущена";
    public static final String STOP_INDEXING_ERROR = "Индексация не запущена";
    public static final String PAGE_INDEXING_ERROR = "Данная страница находится за пределами сайтов, " +
            "указанных в конфигурационном файле";
    public static final String EMPTY_SEARCH_QUERY = "Задан пустой поисковый запрос";
    public static final String SITE_NOT_INDEXED = "Сайт(ы), по которому(ым) ведется поиск, не проиндексирован(ы)";
    public static final String LEMMATIZER_NOT_FOUND = "Ошибка подключения библиотек лемматизатора";
    public static final String PAGE_NOT_FOUND = "Искомая страница не найдена";
    public static final String INDEXING_CANCELLED = "Операция прервана пользователем";
}
