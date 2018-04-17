# Stargazers

A sample GitHub API Android client that shows the stargazers list for a given repository.

## Description
Stargazers are GitHub users that show interest in a particular repository by starring it.

This app UI is pretty straightforward: there is a search bar to find a GitHub repository, with search suggestions on the fly. 
Once a repository is found, you can see the list of stargazers for that repository (with avatar and login name). 
The interaction over a user in the list is as follows:
- by tapping you will be redirected to the user's GitHub page
- by long tapping you will predispose a new search on that user's repositories (if any)

### Youtube sample
[![Open on Youtube](https://img.youtube.com/vi/uqopHsHzD04/2.jpg)](https://youtu.be/uqopHsHzD04)

## Technical notes and Open Source libraries
- The client is implemented with [Retrofit](http://square.github.io/retrofit/).
- The scrollable list is implemented with a RecyclerView using a slightly modified version of [EndlessRecyclerViewScrollListener](https://github.com/codepath/android_guides/wiki/Endless-Scrolling-with-AdapterViews-and-RecyclerView) for the infinite progressive scrolling and [Glide](https://github.com/bumptech/glide) for the thumbnail images asynchrounous management inside the RecyclerView.
- Dependency injection with [Dagger2](http://square.github.io/dagger/)
