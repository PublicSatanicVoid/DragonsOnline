package mc.dragons.core.storage.mongo.pagination;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.FindIterable;

/**
 * Utilities to help with multi-page results parsing.
 * Some of these are specific to MongoDB implementation.
 * 
 * @author Adam
 *
 */
public class PaginationUtil {
	
	/**
	 * 
	 * @param page
	 * @param pageSize
	 * @return How many entries to skip to begin at the given page
	 */
	public static int pageSkip(int page, int pageSize) {
		return pageSize * (page - 1);
	}
	
	/**
	 * 
	 * @param <E>
	 * @param results
	 * @param page
	 * @param pageSize
	 * @return The sublist of results beginning at the given page
	 */
	public static <E> List<E> paginateList(List<E> results, int page, int pageSize) {
		if(results.isEmpty()) return new ArrayList<>();
 		int skip = pageSkip(page, pageSize);
		return results.subList(Math.min(skip, results.size() - 1), Math.min(skip + pageSize, results.size()));
	}
	
	/**
	 * 
	 * @param results
	 * @param page
	 * @param pageSize
	 * @return The sub-iterator of results beginning at the given page,
	 * sorted by ID.
	 * 
	 * @apiNote MongoDB-specific method
	 */
	public static FindIterable<Document> sortAndPaginate(FindIterable<Document> results, int page, int pageSize) {
		return sortAndPaginate(results, page, pageSize, "_id", false);
	}
	
	/**
	 * 
	 * @param results
	 * @param page
	 * @param pageSize
	 * @param field
	 * @param asc
	 * @return The sub-iterator of results beginning at the given page,
	 * sorted bv the given field, either ascending or descending.
	 * 
	 * @apiNote MongoDB-specific method
	 */
	public static FindIterable<Document> sortAndPaginate(FindIterable<Document> results, int page, int pageSize, String field, boolean asc) {
		return results.sort(new Document(field, asc ? 1 : -1)).skip(pageSkip(page, pageSize)).limit(pageSize);
	}
}
