package in.sdqali.intellij.freebuilder;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.generation.actions.BaseGenerateAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import in.sdqali.intellij.freebuilder.internal.OpenApiShim;

public class FreeBuilderAction extends BaseGenerateAction {
  public FreeBuilderAction() {
    super(new FreeBuilderHandler());
  }

  public FreeBuilderAction(CodeInsightActionHandler handler) {
    super(handler);
  }

  @Override
  public void update(AnActionEvent event) {
    new Action(new OpenApiShim()).update(event);
 }
}
