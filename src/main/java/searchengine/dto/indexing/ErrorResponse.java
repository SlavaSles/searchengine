package searchengine.dto.indexing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class ErrorResponse extends Response {
    private final boolean result = false;
    private final String error;
}
