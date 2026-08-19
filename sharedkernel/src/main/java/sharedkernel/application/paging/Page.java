package sharedkernel.application.paging;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public record Page<T>(List<T> items, int pageNumber, int pageSize, long totalCount) {

    public Page {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static <T> Page<T> of(List<T> items, PageRequest request, long totalCount) {
        return new Page<>(items, request.pageNumber(), request.pageSize(), totalCount);
    }

    public static <T> Page<T> empty(PageRequest request) {
        return new Page<>(List.of(), request.pageNumber(), request.pageSize(), 0);
    }

    public int totalPages() {
        if (totalCount == 0) {
            return 0;
        }

        return (int) ((totalCount + pageSize - 1) / pageSize);
    }

    public boolean hasPrevious() {
        return pageNumber > 1;
    }

    public boolean hasNext() {
        return pageNumber < totalPages();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public <R> Page<R> map(Function<T, R> mapper) {
        var mapped = new ArrayList<R>(items.size());
        for (var item : items) {
            mapped.add(mapper.apply(item));
        }

        return new Page<>(mapped, pageNumber, pageSize, totalCount);
    }
}
