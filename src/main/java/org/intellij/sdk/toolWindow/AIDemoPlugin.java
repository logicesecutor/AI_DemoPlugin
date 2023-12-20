// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.intellij.sdk.toolWindow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
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
    return toolWindowContent.getCodeInsertionTextArea();
  }

  private static class AIDemoPluginWindowContent {
    private final JPanel contentPanel;
    private JBTextArea codeInsertionTextArea;
    public static String API_KEY = "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";
    public static String API_URL = "https://api-inference.huggingface.co/models/codellama/CodeLlama-7b-hf";

    private static HttpClient httpClient;

    public JBTextArea getCodeInsertionTextArea() {
      return codeInsertionTextArea;
    }

    public HttpRequest buildNewHttpRequest(String API_KEY, String API_URL, String json){

      return HttpRequest.newBuilder()
              .uri(URI.create(API_URL))
              .timeout(Duration.ofMinutes(2))
              .header("Content-Type", "application/json")
              .header("Authorization", "Bearer "+ API_KEY)
              .POST(HttpRequest.BodyPublishers.ofString(json))
              .build();
    }

    public AIDemoPluginWindowContent(ToolWindow toolWindow) {

      httpClient = HttpClient.newHttpClient();

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
      // Put a placeHolder for the codeInsertionTextArea
      codeInsertionTextArea.getEmptyText().setText("Insert here your code");

      // Add a scroller for the CodeInsertion Area
      JBScrollPane scroller_codeInsertionTextArea = new JBScrollPane(codeInsertionTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

      // Generated Text Area
      JBTextArea generatedTextArea = new JBTextArea();
      // Put a placeHolder for the generatedTextArea
      generatedTextArea.getEmptyText().setText("Waiting for the Generated documentation");


      JBScrollPane scroller_generatedTextArea = new JBScrollPane(generatedTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
      JButton generateTextButton = getGenerationButton(codeInsertionTextArea, generatedTextArea);

      controlsPanel.add(scroller_codeInsertionTextArea,0);
      controlsPanel.add(generateTextButton,1);
      controlsPanel.add(scroller_generatedTextArea,2);

      return controlsPanel;
    }


    @NotNull
    private JButton getGenerationButton(JBTextArea codeInsertionTextArea, JBTextArea generatedTextArea) {

      // Add Generation Button
      JButton generateTextButton = new JButton("Generate Documentation");
      generateTextButton.addActionListener((event) ->
      {
        if (!codeInsertionTextArea.getText().isEmpty()) {
          // Lambda function that call a callback to the LLM model API
          System.out.println("Generation Event: Async POST submitted!\n");

          HttpRequest request = buildNewHttpRequest(
                  API_KEY,
                  API_URL,
                  new RequestBodyObject("Generate documentation for the following code:" +
                          "%s".formatted(codeInsertionTextArea.getText())).toJson()
          );

          /*CompletableFuture<HttpResponse<String>> response = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
          CompletableFuture<Integer> responseCode = response.thenApply(HttpResponse::statusCode);


          response.thenApply(HttpResponse::body)
                  .thenAccept(string -> {
                      try {
                          Integer res = responseCode.get();
                          if(res >= 200 && res<=299) {
                            string = string.replace("[", "");
                            string = string.replace("]", "");
                            System.out.println(string);

                            ResponseBodyObject obj = new Gson().fromJson(string, ResponseBodyObject.class);
                            SwingUtilities.invokeLater(() -> generatedTextArea.setText(obj.generated_text));
                          }
                          else{
                            SwingUtilities.invokeLater(() -> generatedTextArea.setText("Bad request"));
                          }

                      } catch (InterruptedException | ExecutionException e) {
                        SwingUtilities.invokeLater(() -> generatedTextArea.setText(e.toString()));
                      }

                  });
                  */

          httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                  .thenApply(HttpResponse::body)
                  .thenAccept(string -> {
                    string = string.replace("[", "");
                    string = string.replace("]", "");

                    System.out.println(string);

                    ResponseBodyObject obj = new Gson().fromJson(string, ResponseBodyObject.class);
                    SwingUtilities.invokeLater(() -> generatedTextArea.setText(obj.generated_text));

                  });

        }else{
          SwingUtilities.invokeLater(() -> generatedTextArea.setText("No The string was Empty"));
        }
      });

      return generateTextButton;
    }

    public JPanel getContentPanel() {
      return contentPanel;
    }

  }
}
