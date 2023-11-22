package searchengine.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import searchengine.dto.indexing.Response;

@NoArgsConstructor
@Getter
public class SuccessResponse extends Response {
    private final boolean result = true;
}
