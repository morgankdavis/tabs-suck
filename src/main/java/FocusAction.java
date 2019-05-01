import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;

public class FocusAction extends AnAction {

    private static final Logger log = Logger.getInstance("net.morgankdavis.TabsSuck.FocusAction");
    private int targetIndex;


    public FocusAction(int targetIndex) {
        super("Editor " + (targetIndex + 1));

        this.targetIndex = targetIndex;
    }

    @Override
    public void update(AnActionEvent event) {

        Project project = event.getProject();
        TabsSuckProjectComponent projectComponent = project.getComponent(TabsSuckProjectComponent.class);

        if (projectComponent != null) {

            int focusedIndex = projectComponent.focusedEditorIndex();
            Presentation presentation = event.getPresentation();

            if (targetIndex == focusedIndex) {
                presentation.setEnabled(false);
            }
            else {
                presentation.setEnabled(true);
            }
        }
        else {
            log.warn("ProductComponent in FocusAction.update() is null.");
        }
    }

    @Override
    public void actionPerformed(AnActionEvent event) {

        Project project = event.getProject();
        log.debug("FocusAction.actionPerformed(), project: " + project);

        TabsSuckProjectComponent projectComponent = project.getComponent(TabsSuckProjectComponent.class);
        if (projectComponent != null) {
            Editor editor = event.getData(CommonDataKeys.EDITOR);
            if (editor != null) {

                projectComponent.focusAction(targetIndex);
            }
            else {
                log.warn("Editor in FocusAction.actionPerformed() is null.");
            }
        }
        else {
            log.warn("ProductComponent in FocusAction.actionPerformed() is null.");
        }
    }
}
