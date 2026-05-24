package com.kaandev.salonexplorer.domain.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PagedResponse<T>(
    List<T> content,
    PageMetadata page
) {
    public static <T> PagedResponse<T> from(Page<T> springPage) {
        return new PagedResponse<>(
            springPage.getContent(),
            new PageMetadata(
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages()
            )
        );
    }

    public record PageMetadata(int number, int size, long totalElements, int totalPages) {}
}
