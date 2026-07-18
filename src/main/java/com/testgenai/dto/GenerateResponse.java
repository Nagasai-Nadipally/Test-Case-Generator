package com.testgenai.dto;

public class GenerateResponse {

    private boolean success;
    private String output;
    private String suggestedFileName;
    private String error;

    public static GenerateResponse ok(String output, String suggestedFileName) {
        GenerateResponse r = new GenerateResponse();
        r.success = true;
        r.output = output;
        r.suggestedFileName = suggestedFileName;
        return r;
    }

    public static GenerateResponse fail(String error) {
        GenerateResponse r = new GenerateResponse();
        r.success = false;
        r.error = error;
        return r;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getSuggestedFileName() {
        return suggestedFileName;
    }

    public void setSuggestedFileName(String suggestedFileName) {
        this.suggestedFileName = suggestedFileName;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
