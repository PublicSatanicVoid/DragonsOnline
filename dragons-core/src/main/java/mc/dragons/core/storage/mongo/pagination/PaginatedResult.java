package mc.dragons.core.storage.mongo.pagination;

import java.util.List;

/**
 * A single page of results returned from MongoDB.
 * 
 * @author Adam
 *
 * @param <E> The type of object returned per result.
 * For example, for a page of user reports, this would
 * be the Report class.
 */
public class PaginatedResult<E> {
	private List<E> results;
	private int total;
	private int pages;
	private int currentPage;
	
	public PaginatedResult(List<E> resultsPage, int total, int currentPage, int pageSize) {
		this.results = resultsPage;
		this.total = total;
		this.pages = (int) Math.ceil((double) total / pageSize);
		this.currentPage = currentPage;
	}
	
	public List<E> getPage() { return results; }
	public int getTotal() { return total; }
	public int getPages() { return pages; }
	public int getPageIndex() { return currentPage; }
	
}
