package com.ezproject.common;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/** Page de résultats au format JSON stable (évite de sérialiser PageImpl, dont le format n'est pas garanti). */
public record PageDto<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageDto<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageDto<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
