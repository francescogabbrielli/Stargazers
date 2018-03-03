package it.francescogabbrielli.apps.stargazers;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;

/**
 *
 *
 * REFERENCES
 * ==========
 * https://gist.github.com/nesquena/d09dc68ff07e845cc622
 */
public abstract class EndlessRecyclerViewScrollListener extends RecyclerView.OnScrollListener {

    private final static String TAG = "Stargazers-Scrolling";

    private final static String KEY_PAGE        = "page";
    private final static String KEY_LOADING     = "loading";
    private final static String KEY_COUNT       = "count";

    // The minimum amount of items to have below your current scroll position
    // before item_loading more.
    private int visibleThreshold = 5;
    // The current offset index of data you have loaded
    private int currentPage = 0;
    // The total number of items in the dataset after the last load
    private int previousTotalItemCount = 0;
    // True if we are still waiting for the last set of data to load.
    private boolean loading = true;
    // Sets the starting page index
    private int startingPageIndex = 1;

    RecyclerView.LayoutManager mLayoutManager;

    public EndlessRecyclerViewScrollListener(LinearLayoutManager layoutManager) {
        this.mLayoutManager = layoutManager;
    }

    public EndlessRecyclerViewScrollListener(GridLayoutManager layoutManager) {
        this.mLayoutManager = layoutManager;
        visibleThreshold = visibleThreshold * layoutManager.getSpanCount();
    }

    public EndlessRecyclerViewScrollListener(StaggeredGridLayoutManager layoutManager) {
        this.mLayoutManager = layoutManager;
        visibleThreshold = visibleThreshold * layoutManager.getSpanCount();
    }

    public int getLastVisibleItem(int[] lastVisibleItemPositions) {
        int maxSize = 0;
        for (int i = 0; i < lastVisibleItemPositions.length; i++) {
            if (i == 0) {
                maxSize = lastVisibleItemPositions[i];
            }
            else if (lastVisibleItemPositions[i] > maxSize) {
                maxSize = lastVisibleItemPositions[i];
            }
        }
        return maxSize;
    }

    // This happens many times a second during a scroll, so be wary of the code you place here.
    // We are given a few useful parameters to help us work out if we need to load some more data,
    // but first we check if we are waiting for the previous load to finish.
    @Override
    public synchronized void onScrolled(RecyclerView view, int dx, int dy) {

        // don't react soon after a failure
        if (SystemClock.elapsedRealtime()-failureTime<1000)
            return;

        int lastVisibleItemPosition = 0;
        int totalItemCount = mLayoutManager.getItemCount();

        if (mLayoutManager instanceof StaggeredGridLayoutManager) {
            int[] lastVisibleItemPositions = ((StaggeredGridLayoutManager) mLayoutManager).findLastVisibleItemPositions(null);
            // get maximum element within the list
            lastVisibleItemPosition = getLastVisibleItem(lastVisibleItemPositions);
        } else if (mLayoutManager instanceof GridLayoutManager) {
            lastVisibleItemPosition = ((GridLayoutManager) mLayoutManager).findLastVisibleItemPosition();
        } else if (mLayoutManager instanceof LinearLayoutManager) {
            lastVisibleItemPosition = ((LinearLayoutManager) mLayoutManager).findLastVisibleItemPosition();
        }

        // If the total item count is zero and the previous isn't, assume the
        // list is invalidated and should be reset back to initial state
        if (totalItemCount < previousTotalItemCount) {
            this.currentPage = this.startingPageIndex;
            this.previousTotalItemCount = totalItemCount;
            if (totalItemCount == 0) {
                this.loading = true;
            }
            Log.v(TAG, "INVALIDATED: "+currentPage+","+loading);
        }
        // If it’s still item_loading, we check to see if the dataset count has
        // changed, if so we conclude it has finished item_loading and update the current page
        // number and total item count.
        if (loading && (totalItemCount > previousTotalItemCount)) {
            loading = false;
            previousTotalItemCount = totalItemCount;
            Log.v(TAG, "LOADED: "+currentPage+","+loading);
        }

        // If it isn’t currently item_loading, we check to see if we have breached
        // the visibleThreshold and need to reload more data.
        // If we do need to reload some more data, we execute onLoadMore to fetch the data.
        // threshold should reflect how many total columns there are too
        if (!loading && (lastVisibleItemPosition + visibleThreshold) > totalItemCount) {
            currentPage++;
            onLoadMore(currentPage, totalItemCount, view);
            loading = true;
            Log.v(TAG, "LOAD: "+currentPage+","+loading);
        }
    }

    // Call this method whenever performing new searches
    public synchronized void resetState() {
        this.currentPage = this.startingPageIndex;
        this.previousTotalItemCount = 0;
        this.loading = true;
        Log.v(TAG, "RESET: "+currentPage+","+loading);
    }

    /**
     * Retrieve state from bundle
     *
     * @param bundle
     */
    public synchronized void retrieveState(Bundle bundle) {
        currentPage = bundle.getInt(KEY_PAGE);
        loading = bundle.getBoolean(KEY_LOADING);
        previousTotalItemCount = bundle.getInt(KEY_COUNT);
        Log.v(TAG, "RETRIEVE: "+currentPage+","+loading);
    }

    /**
     * Save state to bundle
     *
     * @param bundle
     */
    public synchronized void saveState(Bundle bundle) {
        bundle.putInt(KEY_PAGE, currentPage);
        bundle.putBoolean(KEY_LOADING, loading);
        bundle.putInt(KEY_COUNT, previousTotalItemCount);
    }

    // Defines the process for actually item_loading more data based on page
    public abstract void onLoadMore(int page, int totalItemsCount, RecyclerView view);

    public synchronized int getCurrentPage() {
        return currentPage;
    }

    private long failureTime;

    public synchronized void fail() {
        failureTime = SystemClock.elapsedRealtime();
        currentPage--;
        loading = false;
        Log.v(TAG, "FAIL: "+currentPage+","+loading);

    }

}