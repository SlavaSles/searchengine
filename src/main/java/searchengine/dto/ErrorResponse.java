package searchengine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Schema(description = "Error response")
@RequiredArgsConstructor
@Getter
public class ErrorResponse {
    @Schema(description = "Result of operation")
    private final boolean result = false;
    @Schema(description = "Error message", example = "Сообщение об ошибке")
    private final String error;
}
