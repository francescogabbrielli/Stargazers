# Stargazers

A sample GitHub API Android client

## Description
The UI is pretty simple: just a search bar, used to find a GitHub repository, with search suggestions on the fly. 
Once found, It shows the list of stargazers for that repository: image and login. Clicking on a stargazer you will be redirected to his GitHub page. 

### Youtube sample
[![Open on Youtube](https://img.youtube.com/vi/qh-C3q0vrl4/2.jpg)](https://youtu.be/qh-C3q0vrl4)

## Technical notes and OpenSource libraries
The client is implemented with [Retrofit](http://square.github.io/retrofit/).
The list is implemented with an [endless](https://github.com/codepath/android_guides/wiki/Endless-Scrolling-with-AdapterViews-and-RecyclerView) scroll list, implemented with a RecyclerView. Thumbnail images are managed with [Glide](https://github.com/bumptech/glide)

