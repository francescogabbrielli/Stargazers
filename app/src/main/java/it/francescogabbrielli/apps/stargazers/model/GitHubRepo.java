package it.francescogabbrielli.apps.stargazers.model;


public class GitHubRepo {

    private String name;

    private String full_name;

    public String getName() {
        return name;
    }

    public String getFullName() {
        return full_name;
    }

    @Override
    public String toString() {
        return name;
    }
}


