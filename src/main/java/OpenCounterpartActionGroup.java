import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;

public class OpenCounterpartActionGroup extends DefaultActionGroup {

    private static final Logger log = Logger.getInstance("net.morgankdavis.TabsSuck.OpenCounterpartActionGroup");


    public OpenCounterpartActionGroup() {
        super("Open Counterpart In", true);
    }

    @Override
    public void update(AnActionEvent event) {

        Project project = event.getProject();
        TabsSuckProjectComponent projectComponent = project.getComponent(TabsSuckProjectComponent.class);
        TabsSuckApplicationComponent applicationComponent = project.getComponent(TabsSuckApplicationComponent.class);

        if (projectComponent != null) {
            if (applicationComponent != null) {

                Editor editor = event.getData(CommonDataKeys.EDITOR);
                if (editor != null) {

                    Presentation presentation = event.getPresentation();

                    if (projectComponent.isEditorAttachedToProjectWindow(editor)) {

                        // rebuild the menu

                        removeAll();

                        int numEditors = projectComponent.numEditors();

                        for (int i = 0; i < numEditors; ++i) {
                            add(applicationComponent.openCounterpartAction(i));
                        }

                        presentation.setVisible(true);
                    }
                    else {
                        presentation.setVisible(false);
                    }
                }
                else {
                    log.warn("Editor in OpenCounterpartActionGroup.update() is null.");
                }
            }
            else {
                log.warn("ApplicationComponent in OpenCounterpartActionGroup.update() is null.");
            }
        }
        else {
            log.warn("ProductComponent in OpenCounterpartActionGroup.update() is null.");
        }
    }
}
