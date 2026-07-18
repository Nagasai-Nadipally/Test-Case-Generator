package com.testgenai.dto;

import jakarta.validation.constraints.NotBlank;

public class GenerateRequest {

    @NotBlank(message = "Input text must not be blank")
    private String input;

    /** Optional second field, e.g. a free-form question for the chat endpoint. */
    private String question;

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
