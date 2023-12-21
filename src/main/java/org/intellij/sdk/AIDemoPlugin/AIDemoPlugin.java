// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.intellij.sdk.AIDemoPlugin;

import com.google.gson.GsonBuilder;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.*;
import com.intellij.ui.content.Content;

import com.google.gson.stream.JsonReader;
import com.google.gson.Gson;

import java.io.*;
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
    // It is a Singleton pattern. This instance is needed to communicate with the CodeCaptureAction class.
    instance = this;

    toolWindowContent = new AIDemoPluginWindowContent(toolWindow);

    ContentManager contentManager = toolWindow.getContentManager();
    Content codeGenerationTab = contentManager.getFactory().createContent(toolWindowContent.getGenerationContentPanel(), "Code Generation", false);
    contentManager.addContent(codeGenerationTab);

    Content feedbackPane = contentManager.getFactory().createContent(toolWindowContent.getFeedbackContentPanel(), "Data Visualizer", false);
    contentManager.addContent(feedbackPane);

    // TODO: implement Data visualization panel
    /*Content dataVisualizerTab = contentManager.getFactory().createContent(toolWindowContent.getDataVisualizerContentPanel(), "Data Visualizer", false);
    contentManager.addContent(dataVisualizerTab);
     */
  }



  private static class AIDemoPluginWindowContent {
    private final JPanel generationContentPanel;
    private final JPanel feedbackContentPanel;
    //private final JPanel dataVisualizerContentPanel;
    private static JBTextArea codeInsertionTextArea;
    private static JBTextArea generatedTextArea;

    //TODO: Add this API parameter to a settings file or make them a User choice
    public static String API_KEY;
    public static String API_URL;
    public static String userDataPath;

    private final ArrayList<UserFeedBackObject> userData;
    private static HttpClient httpClient;


    public AIDemoPluginWindowContent(ToolWindow toolWindow) {

      // Get Properties
      Properties currConfigProperties = new Properties();
        InputStream fileStream = getClass().getClassLoader().getResourceAsStream("config.properties");
        try {
            currConfigProperties.load(fileStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

      API_KEY = currConfigProperties.getProperty("API_KEY");
      API_URL = currConfigProperties.getProperty("API_URL");
      userDataPath = currConfigProperties.getProperty("PROJECT_DATA_PATH") + "src/main/resources/toolWindow/userData.json";

      httpClient = HttpClient.newHttpClient();


      // Json simple Database Reading at the start of the plugin
      userData = readUserDataJsonFromFile(userDataPath);

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
                  .thenAccept(response_string -> {

                    if(!response_string.contains("error")) {
                      response_string = response_string.replace("[", "");
                      response_string = response_string.replace("]", "");

                      System.out.println(response_string);

                      ResponseBodyObject obj = new Gson().fromJson(response_string, ResponseBodyObject.class);
                      SwingUtilities.invokeLater(() -> {
                        generatedTextArea.setText(obj.generated_text);
                        toolWindow.getContentManager().setSelectedContent(toolWindow.getContentManager().findContent("Data Visualizer"));
                      });
                    }
                    else {
                      SwingUtilities.invokeLater(() -> {
                        generatedTextArea.getEmptyText().setText("The model is loading. It will be ready in a moment.");
                        toolWindow.getContentManager().setSelectedContent(toolWindow.getContentManager().findContent("Data Visualizer"));
                      });
                    }

                  });

        }else{
          SwingUtilities.invokeLater(() -> generatedTextArea.getEmptyText().setText("No code was provided. We can not generate documentation"));
        }
      });

      return generateTextButton;
    }


    private JComponent createFeedbackControlsPanel(ToolWindow toolWindow) {

      JPanel panel = new JPanel(new GridBagLayout());
      GridBagConstraints constraints = new GridBagConstraints();
      constraints.fill = GridBagConstraints.HORIZONTAL;

      // Text area
      JPanel labeledGeneratedTextArea = createLabeledComponent(generatedTextArea, "Model output:");
      JBScrollPane scroller_generatedTextArea = new JBScrollPane(labeledGeneratedTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

      constraints.gridx = 0;
      constraints.gridy = 0;
      constraints.gridwidth = 2; // Span across two columns
      constraints.weightx = 1.0; // Fill the entire width
      constraints.weighty = 1.0; // Fill the entire height
      constraints.insets = JBUI.insets(5); // Add some padding
      panel.add(scroller_generatedTextArea, constraints);


      // Score questions Panel
      String[] scoreQuestions = {
              "How would you rate the clarity of the documentation?                    ",
              "How would you rate the overall writing style of the documentation?      ",
              "How satisfied are you with the generated documentation?                 "
      };
      ArrayList<ComboBox<String>> comboBoxes = new ArrayList<>();
      JPanel scoreChoicePanel = createScoreBoxComponent(comboBoxes, scoreQuestions);

      constraints.gridx = 0;
      constraints.gridy = 1; // Start from the second row
      constraints.gridwidth = 1; // Span only one column
      constraints.weightx = 1.0; // Reset the weight for checkboxes
      constraints.weighty = 0.0; // Reset the weight for checkboxes
      panel.add(scoreChoicePanel, constraints);


      // Check Boxes Panel
      String[] checkQuestions = {
              "The generated text describe the code's functionality and purpose",
              "The generated text is coherent with the code",
      };
      ArrayList<JBCheckBox> checkBoxes = new ArrayList<>();
      JPanel checkboxesPanel = createCheckBoxComponent(checkBoxes, checkQuestions);

      constraints.gridx = 0;
      constraints.gridy = 2; // Start from the second row
      constraints.gridwidth = 1; // Span only one column
      constraints.weightx = 0.5; // Reset the weight for checkboxes
      constraints.weighty = 0.0; // Reset the weight for checkboxes
      panel.add(checkboxesPanel, constraints);


      // FeedBack TextArea
      JBTextArea feedBackTextArea = new JBTextArea();
      feedBackTextArea.getEmptyText().setText("Leave us your Feedback");
      feedBackTextArea.setPreferredSize(new Dimension(400, 100));

      JPanel labeled_feedBackTextArea = createLabeledComponent(feedBackTextArea, "Are there any features or information you would like to see added to enhance the documentation?");
      JBScrollPane scroller_feedBackTextArea = new JBScrollPane(labeled_feedBackTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

      constraints.gridx = 0;
      constraints.gridy = 3;
      constraints.gridwidth = 2;
      constraints.weightx = 1.0; // Fill the entire width
      constraints.weighty = 1.0; // Fill the entire height
      constraints.insets = JBUI.insets(5); // Add some padding
      panel.add(scroller_feedBackTextArea, constraints);


      JButton submitButton = new JButton("Submit");
      submitButton.addActionListener((event)->{
        // Retrieve all the User Inputs
        String input_text = codeInsertionTextArea.getText();
        String generatedTextAreaText_text = generatedTextArea.getText();
        String fed_text = feedBackTextArea.getText();

        System.out.println(!input_text.isEmpty() && !generatedTextAreaText_text.isEmpty());

        // if those fields are not empty we can parse the feedback form
        if(!input_text.isEmpty() && !generatedTextAreaText_text.isEmpty()){
          Map<String, Boolean> questionsCheckBoxResults = new HashMap<>();
          for (int i = 0; i < checkQuestions.length; i++) {
            questionsCheckBoxResults.put(checkQuestions[i], checkBoxes.get(i).isSelected());
          }

          Map<String, Integer> questionsScoreResults = new HashMap<>();
          for (int i = 0; i < scoreQuestions.length; i++) {
            questionsScoreResults.put(scoreQuestions[i], Integer.valueOf(comboBoxes.get(i).getItem()));
          }

          UserFeedBackObject ufo = new UserFeedBackObject(
                  input_text,
                  generatedTextAreaText_text,
                  (fed_text.isEmpty()) ? "" : fed_text,
                  questionsScoreResults,
                  questionsCheckBoxResults
          );

          userData.add(ufo);
          saveUserDataJsonToFile(userData, userDataPath);
        }

      });

      constraints.gridx = 0; // Centered horizontally
      constraints.gridy = 4; // Bottom row
      constraints.gridwidth = 2; // Span across two columns
      constraints.weightx = 0.0; // Reset the weight for the button
      constraints.weighty = 1; // Reset the weight for the button
      panel.add(submitButton, constraints);


      return new JBScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    }



    // Utils =================================================================

    private JPanel createScoreBoxComponent(ArrayList<ComboBox<String>> comboBoxes, String[] scoreQuestions){

      JPanel scoreChoicePanel = new JPanel(new GridLayout(scoreQuestions.length, 1, 5, 5));
      scoreChoicePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

      for (int i = 0; i < scoreQuestions.length; i++) {
        String[] options = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
        ComboBox<String> dropdownList = createDropDownList(options);
        comboBoxes.add(dropdownList);
        //dropdownList.setPreferredSize(new Dimension(50, 25));
        scoreChoicePanel.add(createLabeledComponent(dropdownList, scoreQuestions[i], BorderLayout.WEST));
      }

      return scoreChoicePanel;
    }


    private JPanel createCheckBoxComponent(ArrayList<JBCheckBox> checkBoxes, String[] checkQuestions){

      JPanel checkboxesPanel = new JPanel(new GridLayout(2, 1, 5, 5));
      checkboxesPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

      for (int i = 0; i < checkQuestions.length; i++) {
        JBCheckBox cb = new JBCheckBox(checkQuestions[i]);
        checkBoxes.add(cb);
        checkboxesPanel.add(cb);
      }

      return checkboxesPanel;
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


    public JBTextArea getCodeInsertionTextArea() {
      return codeInsertionTextArea;
    }


    private JPanel createLabeledComponent(JComponent component, String label){
      JPanel labeledTextArea = new JPanel(new BorderLayout(10,10));
      labeledTextArea.add(new JBLabel(label), BorderLayout.NORTH);
      labeledTextArea.add(component, BorderLayout.CENTER);

      return labeledTextArea;
    }

    private JPanel createLabeledComponent(JComponent component, String label, String position){
      JPanel labeledTextArea = new JPanel(new BorderLayout(10,10));
      labeledTextArea.add(new JBLabel(label), position);
      labeledTextArea.add(component, BorderLayout.CENTER);

      return labeledTextArea;
    }

    private ComboBox<String> createDropDownList(String[] options){
      ComboBox<String> dropdownList = new ComboBox<String>(new DefaultComboBoxModel<>(options));
      return dropdownList;
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


    private static void saveUserDataJsonToFile(ArrayList<UserFeedBackObject> ufos, String filePath) {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();

      try (FileWriter fileWriter = new FileWriter(filePath)) {
        gson.toJson(ufos.toArray(), fileWriter);
        fileWriter.close();

        // Write the JSON string to the file
        System.out.println("Object saved to file: " + filePath);

      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    private static ArrayList<UserFeedBackObject> readUserDataJsonFromFile(String filePath) {

      try (BufferedReader fileReader = new BufferedReader(new FileReader(filePath))) {

        JsonReader reader = new JsonReader(fileReader);
        UserFeedBackObject[] ufos = new Gson().fromJson(reader, UserFeedBackObject[].class);
        fileReader.close();

        // Write the JSON string to the file
        System.out.println("Object loaded from: " + filePath);

        return (ufos!=null)? new ArrayList<UserFeedBackObject>(Arrays.asList(ufos)) : new ArrayList<UserFeedBackObject>();


      } catch (Exception e) {
        e.printStackTrace();
      }

      return null;
    }

  }

  public static AIDemoPlugin getInstance(){
    return instance;
  }

  public JBTextArea getGeneratedTextArea(){
    return toolWindowContent.getCodeInsertionTextArea();
  }
}
