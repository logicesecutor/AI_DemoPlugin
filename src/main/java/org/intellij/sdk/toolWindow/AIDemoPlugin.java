// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.intellij.sdk.toolWindow;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.*;
import com.intellij.ui.content.Content;
import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpClient;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;


public final class AIDemoPlugin implements ToolWindowFactory, DumbAware {
  public static AIDemoPlugin instance;
  private static AIDemoPluginWindowContent toolWindowContent;

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    instance = this;

    toolWindowContent = new AIDemoPluginWindowContent(toolWindow);

    ContentManager contentManager = toolWindow.getContentManager();
    Content codeGenerationTab = contentManager.getFactory().createContent(toolWindowContent.getGenerationContentPanel(), "Code Generation", false);
    contentManager.addContent(codeGenerationTab);

    /*Content dataVisualizerTab = contentManager.getFactory().createContent(toolWindowContent.getDataVisualizerContentPanel(), "Data Visualizer", false);
    contentManager.addContent(dataVisualizerTab);
     */

    Content feedbackPane = contentManager.getFactory().createContent(toolWindowContent.getFeedbackContentPanel(), "Data Visualizer", false);
    contentManager.addContent(feedbackPane);

  }

  public static AIDemoPlugin getInstance(){
    return instance;
  }
  public JBTextArea getGeneratedTextArea(){
    return toolWindowContent.getCodeInsertionTextArea();
  }

  private static class AIDemoPluginWindowContent {
    private final JPanel generationContentPanel;
    private final JPanel feedbackContentPanel;
    //private final JPanel dataVisualizerContentPanel;
    private static JBTextArea codeInsertionTextArea;
    private static JBTextArea generatedTextArea;
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

      // CodeInsertion Text Area
      codeInsertionTextArea = new JBTextArea();
      // Put a placeHolder for the codeInsertionTextArea
      codeInsertionTextArea.getEmptyText().setText("Insert here your code");
      codeInsertionTextArea.setPreferredSize(new Dimension(400, 200));

      // Generated Text Area
      generatedTextArea = new JBTextArea();
      // Put a placeHolder for the generatedTextArea
      generatedTextArea.getEmptyText().setText("Waiting for to Generation input");
      generatedTextArea.setPreferredSize(new Dimension(400, 200));

      generationContentPanel = new JPanel();
      generationContentPanel.setLayout(new BorderLayout(50, 20));
      generationContentPanel.add(createGenerationControlsPanel(toolWindow), BorderLayout.CENTER);

      feedbackContentPanel = new JPanel();
      feedbackContentPanel.setLayout(new BorderLayout(50, 20));
      feedbackContentPanel.add(createFeedbackControlsPanel(toolWindow), BorderLayout.CENTER);





      /*
      dataVisualizerContentPanel = new JPanel();
      dataVisualizerContentPanel.setLayout(new BorderLayout(50, 20));
      dataVisualizerContentPanel.add(createDataVisualizerControlsPanel(toolWindow), BorderLayout.CENTER);
       */
    }



    @NotNull
    private JPanel createGenerationControlsPanel(ToolWindow toolWindow) {

      JPanel controlsPanel = new JPanel( new BorderLayout());

      JPanel labeledTextArea = createLabeledComponent(codeInsertionTextArea, "Insert Here the code:");
      // Add a scroller for the CodeInsertion Area
      JBScrollPane scroller_codeInsertionTextArea = new JBScrollPane(labeledTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

      JButton generateTextButton = getGenerationButton(toolWindow, codeInsertionTextArea, generatedTextArea);
      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      buttonPanel.add(generateTextButton);

      controlsPanel.add(scroller_codeInsertionTextArea,BorderLayout.CENTER);
      controlsPanel.add(buttonPanel, BorderLayout.SOUTH);

      return controlsPanel;
    }

    private JPanel createLabeledComponent(JComponent component, String label){
      JPanel labeledTextArea = new JPanel(new BorderLayout(10,10));
      labeledTextArea.add(new JBLabel(label), BorderLayout.NORTH);
      labeledTextArea.add(component, BorderLayout.CENTER);

      return labeledTextArea;
    }

    @NotNull
    private JButton getGenerationButton(ToolWindow toolWindow, JBTextArea codeInsertionTextArea, JBTextArea generatedTextArea) {

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

          httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                  .thenApply(HttpResponse::body)
                  .thenAccept(string -> {
                    string = string.replace("[", "");
                    string = string.replace("]", "");

                    System.out.println(string);

                    ResponseBodyObject obj = new Gson().fromJson(string, ResponseBodyObject.class);
                    SwingUtilities.invokeLater(() -> {
                      generatedTextArea.setText(obj.generated_text);
                      toolWindow.getContentManager().setSelectedContent(toolWindow.getContentManager().findContent("Data Visualizer"));
                    });

                  });

        }else{
          SwingUtilities.invokeLater(() -> generatedTextArea.setText("No The string was Empty"));
        }
      });

      return generateTextButton;
    }


    private JPanel createFeedbackControlsPanel(ToolWindow toolWindow) {

      JPanel controlsPanel = new JPanel( new BorderLayout() );
      //GridBagConstraints constraints = new GridBagConstraints();
      //constraints.fill = GridBagConstraints.HORIZONTAL;

      JPanel labeledGeneratedTextArea = createLabeledComponent(generatedTextArea, "Model output:");
      JBScrollPane scroller_generatedTextArea = new JBScrollPane(labeledGeneratedTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
      //scroller_generatedTextArea.setPreferredSize(new Dimension(400, 200));


      JButton submitButton = new JButton("Submit");


      controlsPanel.add(scroller_generatedTextArea, BorderLayout.CENTER);
      controlsPanel.add(submitButton, BorderLayout.SOUTH);

      return controlsPanel;

      /*constraints.gridx = 0;
      constraints.gridy = 0;
      constraints.gridwidth = 6;
      controlsPanel.add(scroller_generatedTextArea, constraints);

      ArrayList<JBCheckBox> checkBoxes = new ArrayList<>();
      // Checkbox
      for (int i = 0; i < 5; i++) {
        JBCheckBox checkBox = new JBCheckBox("Checkbox " + (i + 1));
        constraints.gridx = i;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        controlsPanel.add(checkBox, constraints);
        checkBoxes.add(checkBox);
      }

      // TextArea
      JBTextArea feedBackTextArea = new JBTextArea();
      feedBackTextArea.getEmptyText().setText("Give us your Feedback");
      JBScrollPane scrollPane = new JBScrollPane(feedBackTextArea);
      scrollPane.setPreferredSize(new Dimension(200, 100));
      constraints.gridx = 0;
      constraints.gridy = 2;
      constraints.gridwidth = 5;
      constraints.ipady = 80; // set the height of the scroll pane
      controlsPanel.add(scrollPane, constraints);

      // Dropdown List
      String[] options = {"Option 1", "Option 2", "Option 3"};
      JBList<String> dropdownList = new JBList<>(options);


      // ScrollPane for the Dropdown List
      JBScrollPane listScrollPane = new JBScrollPane(dropdownList);
      listScrollPane.setPreferredSize(new Dimension(150, 100));

      constraints.gridx = 0;
      constraints.gridy = 3;
      constraints.gridwidth = 5;
      controlsPanel.add(listScrollPane, constraints);



      constraints.gridx = 2; // Centered horizontally
      constraints.gridy = 4; // Bottom row
      constraints.gridwidth = 1;
      constraints.ipady = 0; // Reset to default
      constraints.insets = JBUI.insetsTop(10); // Add some top margin
       */




    }

    private JPanel createDataVisualizerControlsPanel(ToolWindow toolWindow) {
      return new JPanel();
    }

    public JPanel getGenerationContentPanel() {
      return generationContentPanel;
    }

    public JPanel getFeedbackContentPanel() {
      return feedbackContentPanel;
    }

    public JPanel getDataVisualizerContentPanel() {
      return new JPanel();
    }
  }
}
