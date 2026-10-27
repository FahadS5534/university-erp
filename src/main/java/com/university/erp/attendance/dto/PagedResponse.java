package com.university.erp.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;
import java.util.List;

@Schema(description = "Generic paginated response wrapper")
public record PagedResponse<T>(@Schema(description = "List of items on current page") List<T> content,

        @Schema(description = "Current page index (0-based)", example = "0") int pageNumber,

        @Schema(description = "Requested page size", example = "20") int pageSize,

        @Schema(description = "Total number of elements across all pages", example = "100") long totalElements,

        @Schema(description = "Total number of pages", example = "5") int totalPages,

        @Schema(description = "Indicates if this is the first page", example = "true") boolean isFirst,

        @Schema(description = "Indicates if this is the last page", example = "false") boolean isLast) {
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isFirst(), page.isLast());
    }
}
