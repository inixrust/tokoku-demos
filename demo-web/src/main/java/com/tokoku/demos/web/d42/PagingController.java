package com.tokoku.demos.web.d42;

import java.util.List;
import java.util.stream.IntStream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * D4.2 (slide 34): Page langsung vs PagedModel.
 * <pre>
 * curl.exe -s "localhost:8090/d42/page?size=2"         # bentuk JSON internal PageImpl (pageable, sort, ...) + WARN di log
 * curl.exe -s "localhost:8090/d42/paged-model?size=2"  # {"content":[...],"page":{size,number,totalElements,totalPages}}
 * </pre>
 */
@RestController
@RequestMapping("/d42")
class PagingController {

	private static final List<Item> ITEMS = IntStream.rangeClosed(1, 50).mapToObj(i -> new Item(i, "Produk " + i)).toList();

	@GetMapping("/page")
	Page<Item> page(@PageableDefault(size = 20) Pageable pageable) {
		return slice(pageable);
	}

	@GetMapping("/paged-model")
	PagedModel<Item> pagedModel(@PageableDefault(size = 20) Pageable pageable) {
		return new PagedModel<>(slice(pageable));
	}

	private static Page<Item> slice(Pageable pageable) {
		int from = (int) Math.min(pageable.getOffset(), ITEMS.size());
		int to = Math.min(from + pageable.getPageSize(), ITEMS.size());
		return new PageImpl<>(ITEMS.subList(from, to), pageable, ITEMS.size());
	}

	record Item(long id, String name) {
	}

}
