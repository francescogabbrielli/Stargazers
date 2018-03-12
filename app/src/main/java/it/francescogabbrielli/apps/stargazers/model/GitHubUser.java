package it.francescogabbrielli.apps.stargazers.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * A GitHub user data (short form)
 *
 * Created by Francesco Gabbrielli on 20/02/18.
 *
 * REFERENCES
 * ==========
 * https://stackoverflow.com/questions/4778834/purpose-of-describecontents-of-parcelable-interface
 */
public class GitHubUser implements Parcelable, Serializable {

    private static final long serialVersionUID = 7526472295622776147L;

    /** Avatar URL */
    private String avatar_url;

    /** Username */
    private String login;

    /** User homepage */
    private String html_url;


    public String getAvatarUrl() {
        return avatar_url;
    }

    public void setAvatarUrl(String avatar_url) {
        this.avatar_url = avatar_url;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getHtmlUrl() {
        return html_url;
    }

    public void setHtmlUrl(String html_url) {
        this.html_url = html_url;
    }

    @Override
    public String toString() {
        return login;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(describeContents());
        dest.writeSerializable(this);
    }

    public transient Parcelable.Creator<GitHubUser> CREATOR
            = new Parcelable.Creator<GitHubUser>()
    {
        public GitHubUser createFromParcel(Parcel in)
        {
            int description=in.readInt();
            Serializable s=in.readSerializable();
            switch(description)
            {
                default:
                    return (GitHubUser) s;
            }
        }

        public GitHubUser[] newArray(int size)
        {
            return new GitHubUser[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

}
