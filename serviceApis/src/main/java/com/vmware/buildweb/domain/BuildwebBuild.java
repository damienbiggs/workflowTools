package com.vmware.buildweb.domain;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.vmware.BuildStatus;

import java.net.URI;
import java.util.Date;

/**
 * Represents information on a buildweb build.
 * Can either be an official build or a sandbox build
 */
public class BuildwebBuild {

    public long id;
    public String changeset;
    public String branch;
    @SerializedName("buildtype")
    public String buildType;

    @Expose(serialize = false)
    @SerializedName("buildstate")
    @JsonAdapter(BuildResultDeserializer.class)
    public BuildStatus buildStatus;

    @SerializedName("_buildtree_url")
    public String buildTreeUrl;

    @SerializedName("_buildmachines_url")
    public String buildMachinesUrl;

    @SerializedName("_elapsed_sec")
    public long elapsedSeconds;

    @SerializedName("_deliverables_url")
    public String deliverablesUrl;

    public String relativeBuildTreePath() {
        URI buildUri = URI.create(buildTreeUrl);
        return buildUri.getPath();
    }
}
