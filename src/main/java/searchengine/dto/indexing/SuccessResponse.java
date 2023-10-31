package searchengine.dto.indexing;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class SuccessResponse extends Response{
    private final boolean result = true;
}
