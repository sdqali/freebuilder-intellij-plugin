package in.sdqali.intellij.freebuilder;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.generation.actions.BaseGenerateAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import in.sdqali.intellij.freebuilder.internal.OpenApiShim;
import org.jetbrains.annotations.NotNull;

public class FreeBuilderAction extends BaseGenerateAction {
  private static OpenApiShim openApiShim;
  private static Notifier notifier;
  public FreeBuilderAction() {
    super(new FreeBuilderHandler(getOpenApiShim(),
        new Annotator(getNotifier(), getOpenApiShim()),
        getNotifier()));
  }

  @NotNull
  private static Notifier getNotifier() {
    if (notifier == null) {
      notifier = new Notifier();
    }
    return notifier;
  }

  @NotNull
  private static OpenApiShim getOpenApiShim() {
    if (openApiShim == null) {
      openApiShim = new OpenApiShim();
    }
    return openApiShim;
  }

  public FreeBuilderAction(CodeInsightActionHandler handler) {
    super(handler);
  }

  @Override
  public void update(AnActionEvent event) {
    new Action(getOpenApiShim()).update(event);
 }
}
