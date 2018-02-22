package it.francescogabbrielli.apps.stargazers;

import org.junit.Test;

import java.util.List;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static org.junit.Assert.assertEquals;

/**
 * Created by Francesco Gabbrielli on 20/02/18.
 */
public class MainTests {

    @Test
    public void stargazers_api() throws Exception {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        GitHubService service = retrofit.create(GitHubService.class);

        //check one repo (update expected data before running!)
        Response<List<GitHubUser>> res = service.listStargazers("codeschool", "NoteWrangler",1).execute();
        String pagesLinks =
                "<https://api.github.com/repositories/29988210/stargazers?page=2>; " +
                "rel=\"next\", " +
                "<https://api.github.com/repositories/29988210/stargazers?page=6>; " +
                "rel=\"last\"";

        assertEquals("Links", pagesLinks, res.headers().get("Link"));
    }

    @Test
    public void seerch_users_api() throws Exception {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        GitHubService service = retrofit.create(GitHubService.class);

        Response<GitHubUsers> search = service.searchUsers("codeschool", 5).execute();

        System.out.println(search.body());

    }

    @Test
    public void search_repos_api() throws Exception {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        GitHubService service = retrofit.create(GitHubService.class);

        Response<GitHubRepos> search = service.searchRepos("francescogabbrielli in:login", "stars", 100).execute();

        System.out.println(search.body());

    }
}
