package searchengine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "Success response for operations without content")
@NoArgsConstructor
@Getter
public class SuccessResponse {
    @Schema(description = "Result of operation")
    private final boolean result = true;
}
