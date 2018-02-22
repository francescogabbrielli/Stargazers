package it.francescogabbrielli.apps.stargazers;

import java.util.List;

public class GitHubRepos {

    private int total_count;

    private boolean incomplete_results;

    private List<GitHubRepo> items;

    public List<GitHubRepo> getItems() {
        return items;
    }

    @Override
    public String toString() {
        return items.toString();
    }
}
