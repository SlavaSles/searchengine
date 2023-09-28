package searchengine.dto.indexing;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class StartIndexingResponse {

//    ToDo: Объединить ответы и ошибочные ответы, т. к. наполнение у них одинаковое
    private final boolean result = true;
}
