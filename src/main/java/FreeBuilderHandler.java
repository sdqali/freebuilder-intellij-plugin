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
          .forEach(targetClass -> {
            annotate(project, moduleOf(targetClass), targetClass, FreeBuilder.class, Collections.emptyMap(), null);
            addBuilderClass(project, targetClass);
            addJacksonAnnotation(project, targetClass);
            rebuild(project);
            UndoUtil.markPsiFileForUndo(file);
          });
   }
  }

  private void addJacksonAnnotation(Project project, PsiClass targetClass) {
    Optional<PsiAnnotation> anchor = possibleExistingAnnotation(targetClass, FreeBuilder.class);
    annotate(project, moduleOf(targetClass), targetClass, JsonDeserialize.class,
        Collections.singletonMap("builder", String.format("%s.Builder.class", targetClass.getName())),
        anchor.orElse(null));
  }

  private Optional<PsiAnnotation> possibleExistingAnnotation(PsiClass targetClass, Class annotationClass) {
    return Arrays.stream(targetClass.getAnnotations())
        .filter(psiAnnotation -> psiAnnotation.getQualifiedName().equals(annotationClass.getCanonicalName()))
        .findFirst();
  }

  private void rebuild(Project project) {
    CompilerManager.getInstance(project).make((aborted, errors, warnings, compileContext) -> {
      System.out.println(aborted);
      System.out.println(errors);
      System.out.println(warnings);
      System.out.println(compileContext);
    });
  }

  private void addBuilderClass(Project project, PsiClass targetClass) {
    boolean builderClassDoesNotExist = Arrays.stream(targetClass.getInnerClasses())
        .noneMatch(innerClass -> innerClass.getName().equals("Builder"));

    if (builderClassDoesNotExist) {
      PsiJavaFile psiFile = (PsiJavaFile) PsiFileFactory.getInstance(project).createFileFromText("Builder.java",
          JavaFileType.INSTANCE,
          getClassName(targetClass));
      PsiClass builderClass = psiFile.getClasses()[0];
      annotate(project, moduleOf(targetClass), builderClass, JsonIgnoreProperties.class, Collections.singletonMap("ignoreUnknown", "true"), null);
      targetClass.add(builderClass);
    }
  }

  private Module moduleOf(PsiClass targetClass) {
    return ModuleUtil.findModuleForPsiElement(targetClass);
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

  private void annotate(Project project, Module module, PsiClass psiClass,
                        Class annotationClass, Map<String, String> attributes, PsiAnnotation anchor) {
    if (detectClassInPath(project, module, annotationClass)) {
      if (notAlreadyAnnotated(psiClass, annotationClass)) {
        PsiModifierList modifierList = psiClass.getModifierList();
        PsiAnnotation psiAnnotation = getElementFactory(project)
            .createAnnotationFromText(buildAnnotationText(annotationClass, attributes), psiClass);
        modifierList.addAfter(psiAnnotation, anchor);
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(psiClass);
      }
    }
 }

  private boolean notAlreadyAnnotated(PsiClass targetClass, Class annotationClass) {
    return !possibleExistingAnnotation(targetClass, annotationClass).isPresent();
  }

  private boolean detectClassInPath(Project project, Module module, Class annotationClass) {
    PsiClass jacksonAnnotationClass = JavaPsiFacade.getInstance(project)
        .findClass(annotationClass.getCanonicalName(),
            GlobalSearchScope.moduleRuntimeScope(module, false));
    return null != jacksonAnnotationClass;
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
