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
            @Query(value = "q", encoded = true) String query,
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
            @Query(value = "q", encoded = true) String query,
            @Query("sort") String sort,
            @Query("per_page") int perPage);




    //------------------------------------- UTILITY METHODS ----------------------------------------
    //

    static String HEADER_LINK         = "Link";
    static int PER_PAGE               = 30;
    static Pattern PATTERN_LAST_PAGE  =
            Pattern.compile("stargazers\\?page=([0-9]+?)>; rel=\"last\"");

    static int findLastPage(okhttp3.Headers headers) {
        String links = headers.get(HEADER_LINK);
        if (links==null)
            return 1;
        Matcher m = PATTERN_LAST_PAGE.matcher(links);
        if (m.find())
            return Integer.parseInt(m.group(1));
        return 1;
    }

    static int countUsers(int lastPage, List<GitHubUser> lastPageUsers) {
        return (lastPage-1) * PER_PAGE + lastPageUsers.size();
    }

    //
    //----------------------------------------------------------------------------------------------

}
