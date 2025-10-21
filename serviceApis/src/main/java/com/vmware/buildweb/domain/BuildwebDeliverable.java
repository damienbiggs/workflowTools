package com.vmware.buildweb.domain;

import com.google.gson.annotations.SerializedName;

public class BuildwebDeliverable {
    public String id;

    public String path;

    @SerializedName("_this_url")
    public String url;

    @SerializedName("_download_url")
    public String downloadUrl;
}
