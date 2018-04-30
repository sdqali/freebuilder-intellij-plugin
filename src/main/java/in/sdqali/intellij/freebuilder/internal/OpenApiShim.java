package in.sdqali.intellij.freebuilder.internal;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;

public class OpenApiShim {
  public PsiDocumentManager getDocumentManager(Project project) {
    return PsiDocumentManager.getInstance(project);
  }
}
