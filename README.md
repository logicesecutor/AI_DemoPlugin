# AI Documentation Generator Demo
### Description
This Plugin generates documentation for a provided piece of code.
The application now allows the user to write a piece of code or select an existing one, and generate textual documentation thanks to the AI.

Moreover, the User can give some feedback to improve the quality of generated text!

## Installation
Download or clone the Repository.
Open the project with IntelliJ IDEA. Gradle should automatically build the plugin and install the needed dependencies.
During this step, a .zip file will be generated under the folder build.

To try it run "runPlugin" in the run box.

To permanently install the package follow the steps:
- Open Settings (CTRL + ALT + S);
- Go under "Plugins";
- Click on the gear and select Install manually.

You should select the generated tool_window.zip file. It is done, you have installed your new Plugin.
<img src="https://github.com/logicesecutor/AI_DemoPlugin/blob/master/Images/runPlugin.png" alt="Run plugin" width="200"/>

## How to Use It
The new plugin button is located on the bottom right side of the screen.
https://github.com/logicesecutor/AI_DemoPlugin/tree/master/Images
<img src="https://github.com/logicesecutor/AI_DemoPlugin/tree/master/Images/pluginPosition.png" alt="Position" width="800"/>

Once clicked we are presented with the main panel. Here you can put your code using three methods:
<img src="https://github.com/logicesecutor/AI_DemoPlugin/blob/master/Images/mainPanel.png" alt="Position" width="800"/>

1. Copy/paste;
2. Writing the code;
3. We can capture the code directly from the IDE.

Indeed, it is possible to select a portion of the code and capture it with the newly available option in the Contextual Pop-Up menu.

<img src="https://github.com/logicesecutor/AI_DemoPlugin/tree/master/Images/capturing.png" alt="Capturing the Code" width="800"/>
<img src="https://github.com/logicesecutor/AI_DemoPlugin/tree/master/Images/captured.png" alt="Code Captured" width="800"/>

Once we have the code in the box we can generate the documentation.
When we receive a response from the servers, the plugin captures the answer and automatically switches to the feedback panel.

Here we can provide three kinds of Human feedback:
- A score type feedback from 1 to 10;
- A yes/no type of feedback;
- A textual type of feedback to give more context to the model and further improve the generation.

<img src="https://github.com/logicesecutor/RayCasting/blob/main/src/final_results.gif" alt="FeedBack Panel the Code" width="800"/>
  
All the collected data are then saved to a JSON file ready to be in an API-friendly format. 

## The Model
For the text generation, I have chosen a LLama 7B finetuned for code generation.

**Note!**
Sometimes, often the first one, the model needs to be loaded by the HuggingFace servers, consequently it is not always ready and we need to wait for 1 minute.
<img src="https://github.com/logicesecutor/AI_DemoPlugin/blob/master/Images/modelLoading.png" alt="Model Loading" width="400"/>


## Configuration file (config.properties)
There is a configuration file where is possible to customize some important variables.
- API_KEY: This is the personal token given by HuggingFace to make REST queries. MUST BE CHANGED!
- API_URL: This is the URL of the model that we use for the text generation. This can be changed if needed.
- PROJECT_DATA_PATH: path to the Project to locate the JSON file where to save all the data. MUST BE CHANGED!


## Improvements
Clearly, a lot of improvements need to be made.
Improve the general User Experience




 
