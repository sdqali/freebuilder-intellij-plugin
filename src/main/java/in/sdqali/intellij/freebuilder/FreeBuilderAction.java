package in.sdqali.intellij.freebuilder;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.generation.actions.BaseGenerateAction;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;

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
    PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    event.getPresentation().setVisible(project != null && editor != null && psiFile instanceof PsiJavaFile);
  }
}
