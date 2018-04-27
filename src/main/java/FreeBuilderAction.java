import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.generation.actions.BaseGenerateAction;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

public class FreeBuilderAction extends BaseGenerateAction {
  public FreeBuilderAction() {
    super(new FreeBuilderHandler());
  }

  public FreeBuilderAction(CodeInsightActionHandler handler) {
    super(handler);
  }

  @Override
  public void update(AnActionEvent event) {
    final Project project = event.getProject();
    final Editor editor = event.getData(CommonDataKeys.EDITOR);
    event.getPresentation().setVisible(project != null && editor != null);
  }
}
