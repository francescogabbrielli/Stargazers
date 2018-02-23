package it.francescogabbrielli.apps.stargazers;

import android.content.Intent;

import org.junit.Test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Basic GitHub Tests
 *
 * NB: check one repos and update expected data before running!
 *
 * Created by Francesco Gabbrielli on 20/02/18.
 */
public class GitHubApiTest {

    private final static Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();
    private final static GitHubService service = retrofit.create(GitHubService.class);


    @Test
    public void stargazers() throws Exception {

        Response<List<GitHubUser>> res = service.listStargazers("codeschool", "NoteWrangler", 1).execute();

        assertTrue(res.isSuccessful());

        Pattern PATTERN_LAST_PAGE  =
                Pattern.compile("stargazers\\?page=([0-9]+?)>; rel=\"last\"");

        int last = 0;
        Matcher m = PATTERN_LAST_PAGE.matcher(res.headers().get("Link"));
        if(m.find())
            last = Integer.parseInt(m.group(1));
        assertEquals(6, last);
    }

    @Test
    public void count_stargazers() throws Exception {

        int lastPage = 6;
        Response<List<GitHubUser>> res = service.listStargazers("codeschool", "NoteWrangler", lastPage).execute();

        assertTrue(res.isSuccessful());

        assertEquals(158, (lastPage-1)*30 + res.body().size());
    }

    @Test
    public void search_users() throws Exception {

        Response<GitHubSearch<GitHubUser>> search = service.searchUsers("codeschoo", 5).execute();

        assertTrue(search.isSuccessful());
        assertEquals(53, search.body().getTotalCount());

    }

    @Test
    public void search_repos() throws Exception {

        Response<GitHubSearch<GitHubRepo>> search = service.searchRepos("codeschool/", "score", 5).execute();

        assertTrue(search.isSuccessful());
        assertEquals(2240, search.body().getTotalCount());

    }
}
