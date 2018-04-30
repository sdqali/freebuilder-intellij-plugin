package in.sdqali.intellij.freebuilder.internal;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.command.undo.UndoUtil;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;

public class OpenApiShim {
  public PsiDocumentManager getDocumentManager(Project project) {
    return PsiDocumentManager.getInstance(project);
  }

  public JavaPsiFacade getFacade(Project project) {
    return JavaPsiFacade.getInstance(project);
  }

  public GlobalSearchScope runTimeScope(Module module) {
    return GlobalSearchScope.moduleRuntimeScope(module, false);
  }

  public CompilerManager getProjectManager(Project project) {
    return CompilerManager.getInstance(project);
  }

  public void make(Project project) {
    getProjectManager(project).make((aborted, errors, warnings, compileContext) -> {});
  }

  public void markForUndo(PsiFile file) {
    UndoUtil.markPsiFileForUndo(file);
  }

  public PsiFileFactory getFileFactory(Project project) {
    return PsiFileFactory.getInstance(project);
  }

  public Module getModuleOf(PsiClass targetClass) {
    return ModuleUtil.findModuleForPsiElement(targetClass);
  }
}
