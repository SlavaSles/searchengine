package searchengine.dto.indexing;

import lombok.Getter;
import lombok.NoArgsConstructor;
// Не ясно в каких случаях этот ответ может понадобиться: кнопка "Stop indexing" становится видима только после запуска
// индексации, соответственно условий для появления этого сообщения быть не может...
@NoArgsConstructor
@Getter
public class ErrorStopIndexingResponse {
    private final boolean result = false;
    private final String error = "Индексация не запущена";
}
