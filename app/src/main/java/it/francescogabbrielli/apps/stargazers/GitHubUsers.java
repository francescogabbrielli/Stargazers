package it.francescogabbrielli.apps.stargazers;

import java.util.List;

public class GitHubUsers {

    private int total_count;

    private boolean incomplete_results;

    private List<GitHubUser> items;

    public List<GitHubUser> getItems() {
        return items;
    }

    @Override
    public String toString() {
        return items.toString();
    }
}
