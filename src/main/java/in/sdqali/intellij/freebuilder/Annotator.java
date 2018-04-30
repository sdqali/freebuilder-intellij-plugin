package in.sdqali.intellij.freebuilder;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import in.sdqali.intellij.freebuilder.internal.OpenApiShim;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.intellij.psi.JavaPsiFacade.getElementFactory;

public class Annotator {
  private Notifier notifier;
  private OpenApiShim openApiShim;

  public Annotator(Notifier notifier, OpenApiShim openApiShim) {
    this.notifier = notifier;
    this.openApiShim = openApiShim;
  }

  public void annotate(Module module, PsiClass targetClass,
                       String annotationClass, Map<String, String> attributes, PsiAnnotation anchor) {
    Project project = module.getProject();
    if (detectClassInPath(module, annotationClass)) {
      if (notAlreadyAnnotated(targetClass, annotationClass)) {
        PsiModifierList modifierList = targetClass.getModifierList();
        PsiAnnotation psiAnnotation = getElementFactory(project)
            .createAnnotationFromText(buildAnnotationText(annotationClass, attributes), targetClass);
        modifierList.addAfter(psiAnnotation, anchor);
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(targetClass);
      } else {
        notifier.info("Skipped", String.format("Class %s is already annotated with %s",
            targetClass.getQualifiedName(), annotationClass));
      }
    } else notifier.warn("Skipped", String.format("Skipping %s annotation as it is not found in classpath",
        annotationClass));
  }

  private boolean detectClassInPath(Module module, String canonicalName) {
    Project project = module.getProject();
    PsiClass psiClass = openApiShim.getFacade(project)
        .findClass(canonicalName, openApiShim.runTimeScope(module));
    return null != psiClass;
  }

  private boolean notAlreadyAnnotated(PsiClass targetClass, String annotationClass) {
    return !possibleExistingAnnotation(targetClass, annotationClass).isPresent();
  }

  private Optional<PsiAnnotation> possibleExistingAnnotation(PsiClass targetClass, String annotationClass) {
    return Arrays.stream(targetClass.getAnnotations())
        .filter(psiAnnotation -> psiAnnotation.getQualifiedName().equals(annotationClass))
        .findFirst();
  }

  private String buildAnnotationText(String annotationClass, Map<String, String> attributes) {
    StringBuilder stringBuilder = new StringBuilder(String.format("@%s", annotationClass));
    if (!attributes.isEmpty()) {
      stringBuilder.append("(");
      stringBuilder.append(attributes.entrySet().stream()
          .map(entry -> String.format("%s = %s", entry.getKey(), entry.getValue()))
          .collect(Collectors.joining(", ")));
      stringBuilder.append(")");
    }
    return stringBuilder.toString();
  }
}
