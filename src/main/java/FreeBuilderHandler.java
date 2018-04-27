import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.command.undo.UndoUtil;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import org.inferred.freebuilder.FreeBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class FreeBuilderHandler implements CodeInsightActionHandler {
  @Override
  public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    if(file.isWritable()) {
      PsiJavaFile psiJavaFile = (PsiJavaFile) file;
      String fileNameWithoutExtension = psiJavaFile.getName().replace(".java", "");
      Arrays.stream(psiJavaFile.getClasses())
          .filter(psiClass -> psiClass.getName().equals(fileNameWithoutExtension))
          .forEach(psiClass -> {
            PsiAnnotation freeBuilderAnnotation = annotate(project, psiClass, FreeBuilder.class, Collections.emptyMap(), null);
            addBuilderClass(project, psiClass);
            addJacksonAnnotation(project, psiClass, freeBuilderAnnotation);
            rebuild(project);
            UndoUtil.markPsiFileForUndo(file);
          });
   }
  }

  private void addJacksonAnnotation(Project project, PsiClass psiClass, PsiAnnotation freeBuilderAnnotation) {
    Module moduleForPsiElement = ModuleUtil.findModuleForPsiElement(psiClass);
    PsiClass jacksonAnnotationClass = JavaPsiFacade.getInstance(project)
        .findClass(JsonDeserialize.class.getCanonicalName(),
            GlobalSearchScope.moduleRuntimeScope(moduleForPsiElement, false));
    if (jacksonAnnotationClass != null) {
      annotate(project, psiClass, JsonDeserialize.class,
          Collections.singletonMap("builder", String.format("%s.Builder.class", psiClass.getName())), freeBuilderAnnotation);
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
        .noneMatch(innerClass -> innerClass.getName().equals("Builder"));

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

  private PsiAnnotation annotate(Project project, PsiClass psiClass,
                                 Class annotationClass, Map<String, String> attributes, PsiAnnotation anchor) {
    boolean annotationNotPresent = Arrays.stream(psiClass.getAnnotations())
        .noneMatch(annotation -> annotation.getQualifiedName().equals(annotationClass.getCanonicalName()));
    if (annotationNotPresent) {
      PsiModifierList modifierList = psiClass.getModifierList();
      StringBuilder stringBuilder = new StringBuilder(String.format("@%s(", annotationClass.getName()));
      stringBuilder.append(attributes.entrySet().stream()
          .map(entry -> String.format("%s = %s", entry.getKey(), entry.getValue()))
          .collect(Collectors.joining(", ")));
      stringBuilder.append(")");
      PsiElementFactory elementFactory = JavaPsiFacade.getInstance(project)
          .getElementFactory();
      PsiAnnotation psiAnnotation = elementFactory
          .createAnnotationFromText(stringBuilder.toString(), psiClass);
      modifierList.addAfter(psiAnnotation, anchor);
      JavaCodeStyleManager.getInstance(project).shortenClassReferences(psiClass);
      return psiAnnotation;
    }
    return null;
  }
}
