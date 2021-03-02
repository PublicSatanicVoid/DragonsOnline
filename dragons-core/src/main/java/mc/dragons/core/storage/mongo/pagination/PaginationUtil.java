package mc.dragons.core.storage.mongo.pagination;

import java.util.List;

import org.bson.Document;

import com.mongodb.client.FindIterable;

import mc.dragons.core.Dragons;

/**
 * Utilities to help with multi-page results parsing.
 * Some of these are specific to MongoDB implementation.
 * 
 * @author Adam
 *
 */
public class PaginationUtil {
	public static int pageSkip(int page, int pageSize) {
		return pageSize * (page - 1);
	}
	
	public static <E> List<E> paginateList(List<E> results, int page, int pageSize) {
		int skip = pageSkip(page, pageSize);
		Dragons.getInstance().getLogger().fine("PAGINATION: N="+results.size()+",page="+page+",pageSize="+pageSize+",skip="+skip+",skip+pageSize="+(skip+pageSize));
		return results.subList(Math.min(skip, results.size() - 1), Math.min(skip + pageSize, results.size()));
	}
	
	public static FindIterable<Document> sortAndPaginate(FindIterable<Document> results, int page, int pageSize) {
		return sortAndPaginate(results, page, pageSize, "_id", false);
	}
	
	public static FindIterable<Document> sortAndPaginate(FindIterable<Document> results, int page, int pageSize, String field, boolean asc) {
		return results.sort(new Document(field, asc ? 1 : -1)).skip(pageSkip(page, pageSize)).limit(pageSize);
	}
}
