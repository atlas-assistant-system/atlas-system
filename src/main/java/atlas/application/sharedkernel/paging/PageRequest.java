package atlas.application.sharedkernel.paging;

public record PageRequest(int pageNumber, int pageSize, String sortBy, boolean sortDescending) {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    public PageRequest {
        pageNumber = pageNumber < 1 ? 1 : pageNumber;
        pageSize = pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
    }

    public static PageRequest first() {
        return new PageRequest(1, DEFAULT_PAGE_SIZE, null, false);
    }

    public static PageRequest of(int pageNumber, int pageSize) {
        return new PageRequest(pageNumber, pageSize, null, false);
    }

    public static PageRequest sortedBy(int pageNumber, int pageSize, String sortBy, boolean descending) {
        return new PageRequest(pageNumber, pageSize, sortBy, descending);
    }

    public int offset() {
        return (pageNumber - 1) * pageSize;
    }

    public PageRequest next() {
        return new PageRequest(pageNumber + 1, pageSize, sortBy, sortDescending);
    }
}
