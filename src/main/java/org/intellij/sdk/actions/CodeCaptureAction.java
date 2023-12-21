package org.intellij.sdk.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAwareAction;
import org.intellij.sdk.AIDemoPlugin.AIDemoPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class CodeCaptureAction extends DumbAwareAction {


    @Override
    public void update(@NotNull AnActionEvent event) {
        // Using the event, evaluate the context,
        // and enable or disable the action.

        Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
        CaretModel caretModel = editor.getCaretModel();
        String selectedText = caretModel.getCurrentCaret().getSelectedText();
        //assert selectedText != null;
        if(selectedText!=null) {
            event.getPresentation().setVisible(!selectedText.isEmpty());
        }

    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        // Using the event, implement an action.
        // For example, create and show a dialog.
        AIDemoPlugin aiDemoPlugin = AIDemoPlugin.getInstance();
        if(aiDemoPlugin == null) return;


        Editor ediTorRequiredData = event.getRequiredData(CommonDataKeys.EDITOR);
        CaretModel caretModel = ediTorRequiredData.getCaretModel();
        String selectedText = caretModel.getCurrentCaret().getSelectedText();

        System.out.println(Objects.requireNonNullElse(selectedText, "No text Selected"));

        aiDemoPlugin.getGeneratedTextArea().setText(Objects.requireNonNullElse(selectedText, "No text Selected"));

        /*SwingUtilities.invokeLater(() -> {
            aiDemoPlugin.getGeneratedTextArea().setText(Objects.requireNonNullElse(selectedText, "No text Selected"));
        });

         */

    }

    /*@Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        aiDemoPlugin.getGeneratedTextArea().setText(Objects.requireNonNullElse(selectedText, "No text Selected"));

    }
    */

}
