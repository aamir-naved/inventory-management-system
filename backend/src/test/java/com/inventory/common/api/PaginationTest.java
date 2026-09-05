package com.inventory.common.api;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;

class PaginationTest {

    @Test
    void usesDefaultPageAndSizeAndCapsRequestedSize() {
        Pageable defaults = Pagination.pageable(null, null);
        assertThat(defaults.getPageNumber()).isEqualTo(0);
        assertThat(defaults.getPageSize()).isEqualTo(25);

        Pageable capped = Pagination.pageable(-3, 500);
        assertThat(capped.getPageNumber()).isEqualTo(0);
        assertThat(capped.getPageSize()).isEqualTo(100);
    }
}
