import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import java.util.Optional;
import java.util.stream.Collectors;

public class FreeBuilderHandler implements CodeInsightActionHandler {

  private PsiElementFactory elementFactory;

  @Override
  public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    if(file.isWritable()) {
      PsiJavaFile psiJavaFile = (PsiJavaFile) file;
      String fileNameWithoutExtension = psiJavaFile.getName().replace(".java", "");
      Arrays.stream(psiJavaFile.getClasses())
          .filter(psiClass -> psiClass.getName().equals(fileNameWithoutExtension))
          .forEach(psiClass -> {
            annotate(project, psiClass, FreeBuilder.class, Collections.emptyMap(), null);
            addBuilderClass(project, psiClass);
            addJacksonAnnotation(project, psiClass);
            rebuild(project);
            UndoUtil.markPsiFileForUndo(file);
          });
   }
  }

  private void addJacksonAnnotation(Project project, PsiClass psiClass) {
    Module moduleForPsiElement = ModuleUtil.findModuleForPsiElement(psiClass);
    PsiClass jacksonAnnotationClass = JavaPsiFacade.getInstance(project)
        .findClass(JsonDeserialize.class.getCanonicalName(),
            GlobalSearchScope.moduleRuntimeScope(moduleForPsiElement, false));
    if (jacksonAnnotationClass != null) {
      Optional<PsiAnnotation> annotation = Arrays.stream(psiClass.getAnnotations())
          .filter(psiAnnotation -> psiAnnotation.getQualifiedName().equals(FreeBuilder.class.getCanonicalName()))
          .findFirst();
      annotate(project, psiClass, JsonDeserialize.class,
          Collections.singletonMap("builder", String.format("%s.Builder.class", psiClass.getName())),
          annotation.orElse(null));
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
      annotate(project, builderClass, JsonIgnoreProperties.class, Collections.singletonMap("ignoreUnknown", "true"), null);
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

  private void annotate(Project project, PsiClass psiClass,
                                 Class annotationClass, Map<String, String> attributes, PsiAnnotation anchor) {
    boolean annotationNotPresent = Arrays.stream(psiClass.getAnnotations())
        .noneMatch(annotation -> annotation.getQualifiedName().equals(annotationClass.getCanonicalName()));
    if (annotationNotPresent) {
      PsiModifierList modifierList = psiClass.getModifierList();
      PsiAnnotation psiAnnotation = getElementFactory(project)
          .createAnnotationFromText(buildAnnotationText(annotationClass, attributes), psiClass);
      modifierList.addAfter(psiAnnotation, anchor);
      JavaCodeStyleManager.getInstance(project).shortenClassReferences(psiClass);
    }
  }

  @NotNull
  private String buildAnnotationText(Class annotationClass, Map<String, String> attributes) {
    StringBuilder stringBuilder = new StringBuilder(String.format("@%s", annotationClass.getName()));
    if(!attributes.isEmpty()) {
      stringBuilder.append("(");
      stringBuilder.append(attributes.entrySet().stream()
          .map(entry -> String.format("%s = %s", entry.getKey(), entry.getValue()))
          .collect(Collectors.joining(", ")));
      stringBuilder.append(")");
    }
    return stringBuilder.toString();
  }

  private PsiElementFactory getElementFactory(Project project) {
    if (elementFactory == null) {
      elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
    }
    return elementFactory;
  }
}
