import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.generation.OverrideImplementUtil;
import com.intellij.openapi.command.undo.UndoUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import org.inferred.freebuilder.FreeBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class FreeBuilderHandler implements CodeInsightActionHandler {
  @Override
  public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    if(file.isWritable()) {
      PsiJavaFile psiJavaFile = (PsiJavaFile) file;
      String fileNameWithoutExtension = psiJavaFile.getName().replace(".java", "");
      Arrays.stream(psiJavaFile.getClasses())
          .filter(psiClass -> psiClass.getName().equals(fileNameWithoutExtension))
          .forEach(psiClass -> {
            annotate(project, psiClass, FreeBuilder.class);
            UndoUtil.markPsiFileForUndo(file);
          });
   }
  }

  private void annotate(Project project, PsiClass psiClass, Class annotationClass) {
    PsiModifierList modifierList = psiClass.getModifierList();
    String annotationText = String.format("@%s", annotationClass.getName());
    PsiAnnotation psiAnnotation = JavaPsiFacade.getInstance(project)
        .getElementFactory()
        .createAnnotationFromText(annotationText, psiClass);
    modifierList.addAfter(psiAnnotation, null);
    JavaCodeStyleManager.getInstance(project).shortenClassReferences(psiClass);
  }
}
