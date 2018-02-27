package it.francescogabbrielli.apps.stargazers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
    private final static String KEY_STATUS          = "status";
    private final static String KEY_ERROR           = "error";

    private final static int STATUS_DEFAULT         = 1;
    private final static int STATUS_MORE            = 2;
    private final static int STATUS_EMPTY           = 3;
    private final static int STATUS_ERROR           = 4;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView usernameView;
        TextView errorView;
        ViewHolder(View v) {
            super(v);
            imageView = v.findViewById(R.id.user_image);
            usernameView = v.findViewById(R.id.user_name);
            errorView = v.findViewById(R.id.error);
        }
    }

    /** Android context */
    private Context context;
    /** Endless scroll listener */
    private EndlessRecyclerViewScrollListener scrollListener;
    /** State */
    private int status;
    /** List of users already loaded */
    private List<GitHubUser> users;
    /** Glide request options */
    private RequestOptions requestOptions;
    /** Error in search result */
    private String error;


    public RecyclerAdapter(
            @NonNull Context context,
            @NonNull EndlessRecyclerViewScrollListener scrollListener) {

        this.context = context;
        this.scrollListener = scrollListener;
        users = new LinkedList<>();
        status = STATUS_DEFAULT;
        requestOptions = new RequestOptions();
        requestOptions.placeholder(R.drawable.ic_github);
        clear();
    }

    public void readFromBundle(Bundle bundle) {
        clear();
        for (Parcelable p : bundle.getParcelableArray(KEY_USERS))
            users.add((GitHubUser) p);
        status = bundle.getInt(KEY_STATUS);
        if (bundle.getString(KEY_ERROR)!=null)
            setError(bundle.getString(KEY_ERROR));
    }

    public void writeToBundle(Bundle bundle) {
        bundle.putParcelableArray(KEY_USERS, users.toArray(new GitHubUser[users.size()]));
        bundle.putInt(KEY_STATUS, status);
        bundle.putString(KEY_ERROR, error);
    }

    public synchronized void resetStatus() {
        clear();
        status = STATUS_DEFAULT;
        notifyDataSetChanged();
    }

    public synchronized void setLoading() {
        clear();
        status = STATUS_MORE;
        scrollListener.resetState();
        notifyDataSetChanged();
    }

    public synchronized void setEmpty() {
        clear();
        status = STATUS_EMPTY;
        notifyDataSetChanged();
    }

    public synchronized void setError(@NonNull String error) {
        clear();
        this.error = error;
        status = STATUS_ERROR;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        int layout = 0;
        // Inflate the custom layout
        switch (viewType) {
            case 0:
                layout = R.layout.item_odd;
                break;
            case 1:
                layout = R.layout.item_even;
                break;
            case STATUS_EMPTY:
                layout = R.layout.item_empty;
                break;
            case STATUS_ERROR:
                layout = R.layout.item_error;
                break;
            case STATUS_MORE:
                layout = R.layout.item_loading;
                break;

        }
        View userView = inflater.inflate(layout, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(userView);
        return viewHolder;
    }

    @Override
    public int getItemViewType(int position) {
        int type = status;
        switch (status) {
            case STATUS_DEFAULT:
            case STATUS_MORE:
                if (position<users.size())
                    type = position%2;
                break;
        }
        return type;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        int type = getItemViewType(position);

        if (type<=STATUS_DEFAULT) {

            GitHubUser user = users.get(position);

            Glide.with(context)
                    .setDefaultRequestOptions(requestOptions)
                    .load(user.getAvatarUrl()+"&size=48")
                    .into(holder.imageView);
            holder.usernameView.setText(user.getLogin());
            holder.itemView.setOnClickListener((v) -> {
                if (user.getHtmlUrl()!=null) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(user.getHtmlUrl()));
                    context.startActivity(browserIntent);
                }
            });

        } else if (type==STATUS_ERROR) {

            holder.errorView.setText(error);

        }
    }

    @Override
    public int getItemCount() {
        if (status==STATUS_EMPTY || status==STATUS_ERROR)
            return 1;
        else
            return users.size() + (status==STATUS_MORE ? 1 : 0);
    }

    /**
     * Add a set of users and notify observers
     *
     * @param data
     *      a whole bunch of user data
     * @param more
     *      if there are more data to load
     */
    public synchronized void addData(@Nullable List<GitHubUser> data, boolean more) {
        if (data!=null && !data.isEmpty()) {
            int prevSize = users.size();
            users.addAll(data);
            if (more) {
                status = STATUS_MORE;
                notifyItemChanged(0);
                prevSize++;
            } else {
                status = STATUS_DEFAULT;
                notifyItemRemoved(0);
            }
            notifyItemRangeInserted(prevSize, data.size());
        } else if(users.isEmpty())
            setEmpty();
    }

    /**
     * Clear model
     */
    private void clear() {
        users.clear();
        error = null;
    }

}
