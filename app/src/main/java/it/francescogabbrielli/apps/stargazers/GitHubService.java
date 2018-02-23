package it.francescogabbrielli.apps.stargazers;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * GitHub API access interface for Retrofit
 *
 * Created by Francesco Gabbrielli on 20/02/18.
 */
public interface GitHubService {

    final static int PER_PAGE = 30;

    final static Pattern PATTERN_LAST_PAGE  =
            Pattern.compile("stargazers\\?page=([0-9]+?)>; rel=\"last\"");

    /**
     * Get the list of users stargazing a repository
     *
     * @param user
     *      the owner of the repository
     * @param repo
     *      the repository name
     * @param page
     *      page number (for pagination)
     * @return
     *      the retrofit-callable list of users
     */
    @Headers({"User-Agent: francescogabbrielli", "Authorization: token 4ebc4b13477b7f240f21beb3b5c998c41b96b0c1"})
    @GET("repos/{user}/{repo}/stargazers")
    Call<List<GitHubUser>> listStargazers(
            @Path("user") String user,
            @Path("repo") String repo,
            @Query("page") int page);


    /**
     * Search for users
     *
     * @param query
     *      github query
     * @param perPage
     *      how many results per page
     * @return
     *      search result
     */
    @Headers({"User-Agent: francescogabbrielli", "Authorization: token 4ebc4b13477b7f240f21beb3b5c998c41b96b0c1"})
    @GET("search/users")
    Call<GitHubSearch<GitHubUser>> searchUsers(
            @Query("q") String query,
            @Query("per_page") int perPage);

    /**
     * Search for repositories
     *
     * @param query
     *      github query
     * @param sort
     *      sort by ?
     * @param perPage
     *      how many results per page
     * @return
     *      search result
     */
    @Headers({"User-Agent: francescogabbrielli", "Authorization: token 4ebc4b13477b7f240f21beb3b5c998c41b96b0c1"})
    @GET("search/repositories")
    Call<GitHubSearch<GitHubRepo>> searchRepos(
            @Query("q") String query,
            @Query("sort") String sort,
            @Query("per_page") int perPage);

    static int findLastPage(okhttp3.Headers headers) {
        Matcher m = PATTERN_LAST_PAGE.matcher(headers.get("Link"));
        if (m.find())
            return Integer.parseInt(m.group(1));
        return 0;
    }

    static int countStargazers(int lastPage, List<GitHubUser> lastPageUsers) {
        return (lastPage-1) * PER_PAGE + lastPageUsers.size();
    }

}
