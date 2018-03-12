package it.francescogabbrielli.apps.stargazers.model;

import java.util.List;

public class GitHubSearch<T> {

    private int total_count;

    private boolean incomplete_results;

    private List<T> items;

    public int getTotalCount() {
        return total_count;
    }

    public boolean isIncompleteResults() {
        return incomplete_results;
    }

    public List<T> getItems() {
        return items;
    }

    @Override
    public String toString() {
        return items.toString();
    }
}
