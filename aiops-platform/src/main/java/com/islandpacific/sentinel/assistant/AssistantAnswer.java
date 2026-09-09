package com.islandpacific.sentinel.assistant;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * 2.1 — the assistant's answer to one question, plus the exact tool/query citations that back it.
 * Citations are derived from the tool calls this agent actually executed (server-side bookkeeping),
 * never from the model's self-reported claims, so "the queries shown" are guaranteed accurate.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssistantAnswer {

    private String answer;
    private List<Citation> citations = new ArrayList<>();
    private boolean aiAvailable = true;
    private boolean insufficient;

    public AssistantAnswer() {
    }

    public static AssistantAnswer of(String answer, List<Citation> citations) {
        AssistantAnswer result = new AssistantAnswer();
        result.answer = answer;
        result.citations = citations != null ? citations : new ArrayList<>();
        return result;
    }

    public static AssistantAnswer insufficient(List<Citation> citations) {
        AssistantAnswer result = of(
                "I could not gather enough evidence from the available tools to answer this confidently.",
                citations);
        result.insufficient = true;
        return result;
    }

    public static AssistantAnswer unavailable() {
        AssistantAnswer result = new AssistantAnswer();
        result.answer = "The AI Assistant is currently unavailable. Please try again later.";
        result.aiAvailable = false;
        return result;
    }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public List<Citation> getCitations() { return citations; }
    public void setCitations(List<Citation> citations) { this.citations = citations != null ? citations : new ArrayList<>(); }

    public boolean isAiAvailable() { return aiAvailable; }
    public void setAiAvailable(boolean aiAvailable) { this.aiAvailable = aiAvailable; }

    public boolean isInsufficient() { return insufficient; }
    public void setInsufficient(boolean insufficient) { this.insufficient = insufficient; }

    /** One tool call this agent ran while answering: the tool, the exact query/arguments, and a result summary. */
    public static final class Citation {
        private String tool;
        private String query;
        private String summary;

        public Citation() {
        }

        public Citation(String tool, String query, String summary) {
            this.tool = tool;
            this.query = query;
            this.summary = summary;
        }

        public String getTool() { return tool; }
        public void setTool(String tool) { this.tool = tool; }

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
    }
}
