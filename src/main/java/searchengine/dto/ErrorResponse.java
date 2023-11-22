package searchengine.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import searchengine.dto.indexing.Response;

@RequiredArgsConstructor
@Getter
public class ErrorResponse extends Response {
    private final boolean result = false;
    private final String error;
}
