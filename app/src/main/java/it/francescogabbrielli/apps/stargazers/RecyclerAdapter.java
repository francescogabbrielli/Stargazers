package it.francescogabbrielli.apps.stargazers;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * Created by Francesco Gabbrielli on 20/02/18.
 *
 * REFERENCES
 * ==========
 * https://github.com/codepath/android_guides/wiki/Using-the-RecyclerView
 * https://stackoverflow.com/questions/2471935/how-to-load-an-imageview-by-url-in-android
 * https://stackoverflow.com/questions/45187661/placeholder-error-fallback-in-glide-v4
 */
public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

    private final static String KEY_USERS           = "users";
    private final static String KEY_IMAGES          = "images";
    private final static String KEY_NO_MORE         = "no_more";
    private final static String KEY_EMPTY           = "empty";
    private final static String KEY_ERROR           = "error";


    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;
        public TextView usernameView;
        public ViewHolder(View v) {
            super(v);
            imageView = v.findViewById(R.id.user_image);
            usernameView = v.findViewById(R.id.user_name);
        }
    }

    /** Android context */
    private Context context;
    /** Status */
    private int status;
    /** List of users already loaded */
    private List<GitHubUser> users;
    /** Glide request options */
    private RequestOptions requestOptions;

    /** Signal no more data to load */
    private boolean noMoreData;

    /** Empty result */
    private boolean empty;
    /** Error in search result */
    private String error;
    /** Dummy element to represent empty and error states */
    private final static GitHubUser dummyUser = new GitHubUser();


    public RecyclerAdapter(Context context) {
        this.context = context;
        users = new LinkedList<>();
        requestOptions = new RequestOptions();
    }

    public void readFromBundle(Bundle bundle) {
        reset();
        for (Parcelable p : bundle.getParcelableArray(KEY_USERS))
            users.add((GitHubUser) p);
        noMoreData = bundle.getBoolean(KEY_NO_MORE);
        if (bundle.getString(KEY_ERROR)!=null)
            setError(bundle.getString(KEY_ERROR));
        else if (bundle.getBoolean(KEY_EMPTY))
            setEmpty();
    }

    public void writeToBundle(Bundle bundle) {
        bundle.putParcelableArray(KEY_USERS, users.toArray(new GitHubUser[users.size()]));
        bundle.putBoolean(KEY_EMPTY, empty);
        bundle.putString(KEY_ERROR, error);
        bundle.putBoolean(KEY_NO_MORE, noMoreData);
    }

    public void reset() {
        clear();
        empty = false;
        error = null;
        requestOptions.placeholder(R.drawable.ic_github);
        notifyDataSetChanged();
    }

    public void setEmpty() {
        clear();
        empty = true;
        noMoreData = true;
        users.add(dummyUser);
        dummyUser.setLogin(context.getString(R.string.search_empty));
        requestOptions.placeholder(R.drawable.ic_empty);
        notifyDataSetChanged();
    }

    public void setError(String error) {
        this.error = error;
        clear();
        empty = true;
        noMoreData = true;
        users.add(dummyUser);
        dummyUser.setLogin(error);
        requestOptions.placeholder(R.drawable.ic_error);
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        int layout = 0;
        // Inflate the custom layout
        switch (viewType) {
            case 1:
                layout = R.layout.item_odd;
                break;
            case 2:
                layout = R.layout.item_even;
                break;
            case 3:
                layout = R.layout.loading;
                break;
        }
        View userView = inflater.inflate(layout, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(userView);
        return viewHolder;
    }

    @Override
    public int getItemViewType(int position) {
        return position<users.size() ? 1+position%2 : 3;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        if (getItemViewType(position)<3) {

            GitHubUser user = users.get(position);

            Glide.with(context)
                    .setDefaultRequestOptions(requestOptions)
                    .load(user.getAvatarUrl())
                    .into(holder.imageView);
            holder.usernameView.setText(user.getLogin());
            holder.itemView.setOnClickListener((v) -> {
                if (user.getHtmlUrl()!=null) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(user.getHtmlUrl()));
                    context.startActivity(browserIntent);
                }
            });

        }
    }

    @Override
    public int getItemCount() {
        if(empty)
            return 1;
        else if (users.isEmpty())
            return 0;
        else
            return users.size() + (noMoreData ? 0 : 1);
    }

    /**
     * Add a set of users and notify observers
     *
     * @param data
     *      a whole bunch of user data
     */
    public void addData(List<GitHubUser> data) {
        if (data!=null && !data.isEmpty()) {
            int prevSize = users.size();
            users.addAll(data);
            notifyItemRangeInserted(prevSize, data.size());
        } else if(users.isEmpty())
            setEmpty();
    }

    /**
     * Clear model
     */
    private void clear() {
        users.clear();
        noMoreData = false;
    }

    /**
     * Check if there are more data to load
     *
     * @return
     *      if there is more data to load
     */
    public boolean hasMoreData() {
        return !noMoreData;
    }

    /**
     * Control if more loading needed
     *
     * @param noMoreData
     *         if true there is no need to load more data
     */
    public void setNoMoreData(boolean noMoreData) {
        this.noMoreData = noMoreData;
    }

}
