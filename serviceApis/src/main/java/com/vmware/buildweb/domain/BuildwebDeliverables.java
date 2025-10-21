package com.vmware.buildweb.domain;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;

public class BuildwebDeliverables {
    @SerializedName("_next_url")
    public String nextUrl;

    @SerializedName("_list")
    public BuildwebDeliverable[] list;

    public BuildwebDeliverable deliverableForPackage(String name) {
        return Arrays.stream(list).filter(deliverable -> deliverable.path.contains(name)).findFirst().orElse(null);
    }
}
