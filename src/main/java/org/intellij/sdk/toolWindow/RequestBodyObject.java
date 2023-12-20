package org.intellij.sdk.toolWindow;

import com.google.gson.Gson;

public class RequestBodyObject{
    String inputs;
    public RequestBodyObject(String input){
        this.inputs = input;
    }
    public RequestBodyObject(){
        this.inputs = "";
    }

    public void setModelInput(String input) {
        this.inputs = input;
    }
    public String toJson() {
        return new Gson().toJson(this);
    }
}
