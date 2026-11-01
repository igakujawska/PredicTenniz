package dev.courtvision;

import jakarta.validation.constraints.*;
import java.util.List;

public record DrawRequest(
    @NotBlank @Size(max=100) String title,
    @NotNull Surface surface,
    @NotNull @Size(min=2, max=64) List<@NotBlank String> playerIds,
    @NotNull @Size(min=2, max=7) List<@NotNull @PositiveOrZero @Max(1000000000) Long> prizes,
    @NotBlank @Pattern(regexp="USD|EUR|GBP|PLN") String currency
) {}
