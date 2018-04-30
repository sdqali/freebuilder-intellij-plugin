package in.sdqali.intellij.freebuilder;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import in.sdqali.intellij.freebuilder.internal.OpenApiShim;

public class Action {
  private OpenApiShim openApiShim;

  public Action(OpenApiShim openApiShim) {
    this.openApiShim = openApiShim;
  }

  public void update(AnActionEvent event) {
    final Project project = event.getProject();
    final Editor editor = event.getData(CommonDataKeys.EDITOR);
    PsiFile psiFile = openApiShim.getDocumentManager(project).getPsiFile(editor.getDocument());
    event.getPresentation().setVisible(project != null && editor != null && psiFile instanceof PsiJavaFile);
  }
}
