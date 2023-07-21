package searchengine.dto.indexing;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class ErrorStartIndexingResponse {
    private final boolean result = false;
    private final String error = "Индексация уже запущена";
}
