// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.intellij.sdk.toolWindow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.content.Content;
import com.google.gson.Gson;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpClient;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.AnimatedIcon;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public final class AIDemoPlugin implements ToolWindowFactory, DumbAware {
  public static AIDemoPlugin instance;
  private static AIDemoPluginWindowContent toolWindowContent;

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    instance = this;

    toolWindowContent = new AIDemoPluginWindowContent(toolWindow);

    ContentManager contentManager = toolWindow.getContentManager();
    Content codeGenerationTab = contentManager.getFactory().createContent(toolWindowContent.getContentPanel(), "Code Generation", false);
    contentManager.addContent(codeGenerationTab);

    //Content dataVisualizerTab = contentManager.getFactory().createContent(toolWindowContent.getContentPanel(), "Data Visualizer", false);
    //contentManager.addContent(dataVisualizerTab);

  }

  public static AIDemoPlugin getInstance(){
    return instance;
  }
  public JBTextArea getGeneratedTextArea(){
    return toolWindowContent.codeInsertionTextArea;
  }

  private static class AIDemoPluginWindowContent {
    private final JPanel contentPanel;

    public JBTextArea codeInsertionTextArea;

    HttpRequest httpRequest;
    HttpClient httpClient;
    RequestBodyObject rbo;



    public AIDemoPluginWindowContent(ToolWindow toolWindow) {

      String API_KEY = "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";
      String API_URL = "https://api-inference.huggingface.co/models/codellama/CodeLlama-7b-hf";

      this.rbo = new RequestBodyObject();
      this.rbo.setModelInput("Generate documentation for the following code: \"public JPanel getContentPanel() {return contentPanel;}\"");
      Gson json = new Gson();

      String str = json.toJson(rbo);

      this.httpClient = HttpClient.newHttpClient();
      this.httpRequest = HttpRequest.newBuilder()
              .uri(URI.create(API_URL))
              .timeout(Duration.ofMinutes(2))
              .header("Content-Type", "application/json")
              .header("Authorization", "Bearer "+ API_KEY)
              //.POST(HttpRequest.BodyPublishers.ofString("{\"inputs\":\"Generate documentation for the following code public JPanel getContentPanel() {return contentPanel;}\"}"))
              .POST(HttpRequest.BodyPublishers.ofString(str))
              .build();


      this.contentPanel = new JPanel();
      contentPanel.setLayout(new BorderLayout(50, 20));

      contentPanel.add(createControlsPanel(toolWindow), BorderLayout.CENTER);

    }

    @NotNull
    private JPanel createControlsPanel(ToolWindow toolWindow) {

      JPanel controlsPanel = new JPanel( new GridLayout(3, 1) );


      // CodeInsertion Text Area
      //EditorTextField codeInsertionTextArea = new EditorTextField();
      codeInsertionTextArea = new JBTextArea();
      // Put a placeHolder
      codeInsertionTextArea.getEmptyText().setText("Insert here your code");
      // Set some default dimensions
      //codeInsertionTextArea.setPreferredSize( new Dimension( 200, 300 ) );

      // Add a scroller for the CodeInsertion Area
      JScrollPane scroller_codeInsertionTextArea = new JScrollPane(codeInsertionTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

      JBTextArea generatedTextArea = new JBTextArea();
      generatedTextArea.getEmptyText().setText("Waiting for the Generated documentation");
      //generatedTextArea.setPreferredSize( new Dimension( 200, 300 ) );

      JScrollPane scroller_generatedTextArea = new JScrollPane(generatedTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
      JButton generateTextButton = getGenerationButton(controlsPanel, generatedTextArea);

      controlsPanel.add(scroller_codeInsertionTextArea,0);
      //controlsPanel.add(codeInsertionTextArea);
      controlsPanel.add(generateTextButton,1);
      controlsPanel.add(scroller_generatedTextArea,2);


      return controlsPanel;

    }


    @NotNull
    private JButton getGenerationButton(JPanel controlsPanel, JBTextArea generatedTextArea) {

      // Add Generation Button
      JButton generateTextButton = new JButton("Generate Documentation");
      generateTextButton.addActionListener((event) ->
      {
        // Lambda function that call a callback to the LLM model API
        System.out.println("Generation Event: Async POST submitted!\n");

        this.httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(string -> {
                  string = string.replace("[", "");
                  string = string.replace("]", "");

                  ResponseBodyObject obj = new Gson().fromJson(string, ResponseBodyObject.class);
                  System.out.println(string);
                });
      });

      return generateTextButton;
    }

    public void updateGeneratedTextArea(String newText){

    }

    public JPanel getContentPanel() {
      return contentPanel;
    }

  }
}
