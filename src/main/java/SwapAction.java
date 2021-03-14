
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;


public class SwapAction extends AnAction {

    private static final Logger log = Logger.getInstance("net.mkd.TabsSuck.SwapAction");
    private int targetIndex;


    public SwapAction(int targetIndex) {
        super("Editor " + (targetIndex + 1));

        this.targetIndex = targetIndex;
    }

    @Override
    public void update(AnActionEvent event) {

        Project project = event.getProject();
        TabsSuckProject projectComponent = project.getComponent(TabsSuckProject.class);

        if (projectComponent != null) {
            Editor editor = event.getData(CommonDataKeys.EDITOR);
            if (editor != null) {

            int sourceIndex = projectComponent.indexOfEditor(editor);
            Presentation presentation = event.getPresentation();

            if (targetIndex == sourceIndex) {
                presentation.setEnabled(false);
            }
            else {
                presentation.setEnabled(true);
            }
            }
            else {
                log.warn("Editor in SwapAction.update() is null.");
            }
        }
        else {
            log.warn("ProductComponent in SwapAction.update() is null.");
        }
    }

    @Override
    public void actionPerformed(AnActionEvent event) {

        Project project = event.getProject();
        log.debug("SwapAction.actionPerformed(), project: " + project);

        TabsSuckProject projectComponent = project.getComponent(TabsSuckProject.class);
        if (projectComponent != null) {
            Editor editor = event.getData(CommonDataKeys.EDITOR);
            if (editor != null) {

                int sourceIndex = projectComponent.indexOfEditor(editor);
                projectComponent.swapAction(sourceIndex, targetIndex);
            }
            else {
                log.warn("Editor in SwapAction.actionPerformed() is null.");
            }
        }
        else {
            log.warn("ProductComponent in SwapAction.actionPerformed() is null.");
        }
    }
}
