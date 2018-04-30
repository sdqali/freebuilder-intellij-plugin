package in.sdqali.intellij.freebuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import in.sdqali.intellij.freebuilder.internal.OpenApiShim;
import org.inferred.freebuilder.FreeBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

public class FreeBuilderHandler implements CodeInsightActionHandler {
  private OpenApiShim openApiShim;
  private Annotator annotator;
  private Notifier notifier;

  public FreeBuilderHandler(OpenApiShim openApiShim, Annotator annotator, Notifier notifier) {
    this.openApiShim = openApiShim;
    this.annotator = annotator;
    this.notifier = notifier;
  }

  @Override
  public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    if (file.isWritable()) {
      PsiJavaFile psiJavaFile = (PsiJavaFile) file;
      String fileNameWithoutExtension = psiJavaFile.getName().replace(".java", "");
      Arrays.stream(psiJavaFile.getClasses())
          .filter(psiClass -> psiClass.getName().equals(fileNameWithoutExtension))
          .forEach(targetClass -> {
            addFreeBuilderAnnotation(targetClass);
            addBuilderClass(project, targetClass);
            addJacksonAnnotation(targetClass);
            openApiShim.make(project);
            openApiShim.markForUndo(file);
          });
    }
  }

  private void addFreeBuilderAnnotation(PsiClass targetClass) {
    annotator.annotate(moduleOf(targetClass), targetClass, FreeBuilder.class, Collections.emptyMap(), null);
  }

  private void addJacksonAnnotation(PsiClass targetClass) {
    annotator.annotate(moduleOf(targetClass), targetClass, JsonDeserialize.class,
        Collections.singletonMap("builder", String.format("%s.Builder.class", targetClass.getName())),
        possibleExistingAnnotation(targetClass, FreeBuilder.class).orElse(null));
  }

  private Optional<PsiAnnotation> possibleExistingAnnotation(PsiClass targetClass, Class annotationClass) {
    return Arrays.stream(targetClass.getAnnotations())
        .filter(psiAnnotation -> psiAnnotation.getQualifiedName().equals(annotationClass.getCanonicalName()))
        .findFirst();
  }

  private void addBuilderClass(Project project, PsiClass targetClass) {
    boolean builderClassDoesNotExist = Arrays.stream(targetClass.getInnerClasses())
        .noneMatch(innerClass -> innerClass.getName().equals("Builder"));

    if (builderClassDoesNotExist) {
      PsiJavaFile psiFile = (PsiJavaFile) openApiShim.getFileFactory(project)
          .createFileFromText("Builder.java", JavaFileType.INSTANCE, getClassName(targetClass));
      PsiClass builderClass = psiFile.getClasses()[0];
      annotateBuilderClass(targetClass, builderClass);
      targetClass.add(builderClass);
    } else {
      Optional<PsiClass> builderClass = Arrays.stream(targetClass.getInnerClasses())
          .filter(innerClass -> innerClass.getName().equals("Builder"))
          .findFirst();
      annotateBuilderClass(targetClass, builderClass.get());
      notifier.info("Skipped", String.format("Did not generate Builder class %s.Builder as it already exists.",
          targetClass.getQualifiedName()));
    }
  }

  private void annotateBuilderClass(PsiClass targetClass, PsiClass builderClass) {
    annotator.annotate(moduleOf(targetClass),
        builderClass,
        JsonIgnoreProperties.class,
        Collections.singletonMap("ignoreUnknown", "true"),
        null);
  }

  private Module moduleOf(PsiClass targetClass) {
    return openApiShim.getModuleOf(targetClass);
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
}
