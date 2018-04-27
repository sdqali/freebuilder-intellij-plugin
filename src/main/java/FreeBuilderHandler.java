import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.generation.OverrideImplementUtil;
import com.intellij.compiler.server.BuildManager;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.command.undo.UndoUtil;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.source.PsiClassImpl;
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
            addBuilderClass(project, psiClass);
            rebuild(project);
            UndoUtil.markPsiFileForUndo(file);
          });
   }
  }

  private void rebuild(Project project) {
    CompilerManager.getInstance(project).make((aborted, errors, warnings, compileContext) -> {
      System.out.println(aborted);
      System.out.println(errors);
      System.out.println(warnings);
      System.out.println(compileContext);
    });
  }

  private void addBuilderClass(Project project, PsiClass psiClass) {
    boolean builderClassDoesNotExist = Arrays.stream(psiClass.getInnerClasses())
        .noneMatch(innerClass -> innerClass.getSuperClass().getName().equals(builderName(psiClass)));

    if (builderClassDoesNotExist) {
      PsiJavaFile psiFile = (PsiJavaFile) PsiFileFactory.getInstance(project).createFileFromText("Builder.java",
          JavaFileType.INSTANCE,
          getClassName(psiClass));
      PsiClass builderClass = psiFile.getClasses()[0];
      psiClass.add(builderClass);
    }
  }

  private String builderName(PsiClass psiClass) {
    return String.format("%s_Builder", psiClass.getName());
  }

  private String getClassName(PsiClass psiClass) {
    if (psiClass.isInterface()) {
      return String.format("class Builder extends %s {}", builderName(psiClass));
    } else {
      return String.format("public static class Builder extends %s {}", builderName(psiClass));
    }
  }

  private void annotate(Project project, PsiClass psiClass, Class annotationClass) {
    boolean annotationNotPresent = Arrays.stream(psiClass.getAnnotations())
        .noneMatch(annotation -> annotation.getQualifiedName().equals(annotationClass.getCanonicalName()));
    if (annotationNotPresent) {
      PsiModifierList modifierList = psiClass.getModifierList();
      String annotationText = String.format("@%s", annotationClass.getName());
      PsiAnnotation psiAnnotation = JavaPsiFacade.getInstance(project)
          .getElementFactory()
          .createAnnotationFromText(annotationText, psiClass);
      modifierList.addAfter(psiAnnotation, null);
      JavaCodeStyleManager.getInstance(project).shortenClassReferences(psiClass);
    }
  }
}
