package org.intellij.sdk.AIDemoPlugin;

import java.util.Map;

public class UserFeedBackObject {
    String model_query;
    String model_response;
    String user_feedBacks;

    Map<String, Integer> questionsScoreResults;
    Map<String, Boolean> questionsCheckBoxResults;
    public UserFeedBackObject(){
        this.model_query = "";
        this.model_response = "";
        this.user_feedBacks = "";
    }
    public UserFeedBackObject(String model_query, String model_response, String user_feedBacks, Map<String, Integer> questionsScoreResults, Map<String, Boolean> questionsCheckBoxResults){
        this.model_query = model_query;
        this.model_response = model_response;
        this.user_feedBacks = user_feedBacks;
        this.questionsScoreResults = questionsScoreResults;
        this.questionsCheckBoxResults = questionsCheckBoxResults;
    }

}
