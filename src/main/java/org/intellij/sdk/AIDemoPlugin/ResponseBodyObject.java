package org.intellij.sdk.AIDemoPlugin;

public class ResponseBodyObject{
    String generated_text;
    public void setModelOutput(String output) {
        this.generated_text = output;
    }
}
