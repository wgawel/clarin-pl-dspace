package org.dspace.rest.common.clarinpl;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Created by tnaskret on 13.08.15.
 */
public class ProcessStatusResponse {

    @JsonProperty("itemId")
    private String itemId;

    @JsonProperty("nlpEngineToken")
    private String nlpEngineToken;

    @JsonProperty("status")
    private String status;

    @JsonProperty("error")
    private String error;

    @JsonProperty("progress")
    private String progress = "0";


    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getNlpEngineToken() {
        return nlpEngineToken;
    }

    public void setNlpEngineToken(String nlpEngineToken) {
        this.nlpEngineToken = nlpEngineToken;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getProgress() {
        return progress;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }
}
