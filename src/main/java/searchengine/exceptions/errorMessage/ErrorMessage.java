package searchengine.exceptions.errorMessage;

import lombok.Getter;

@Getter
public enum ErrorMessage {
    START_INDEXING_ERROR ("Индексация уже запущена"),
    STOP_INDEXING_ERROR ("Индексация не запущена"),
    PAGE_INDEXING_ERROR ("Данная страница находится за пределами сайтов, указанных в конфигурационном файле"),
    EMPTY_SEARCH_QUERY ("Задан пустой поисковый запрос"),
    SITE_NOT_INDEXED ("Сайт(ы), по которому(ым) ведется поиск, не проиндексирован(ы)"),
    LEMMATIZER_NOT_FOUND ("Ошибка подключения библиотек лемматизатора"),
    PAGE_NOT_FOUND ("Искомая страница не найдена"),
    INDEXING_CANCELLED ("Операция прервана пользователем");

    private final String message;
    ErrorMessage(String message) {
        this.message = message;
    }
}
