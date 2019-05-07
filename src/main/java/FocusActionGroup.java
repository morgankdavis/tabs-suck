
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;


public class FocusActionGroup extends DefaultActionGroup {

    private static final Logger log = Logger.getInstance("net.morgankdavis.TabsSuck.FocusActionGroup");


    public FocusActionGroup() {
        super("Focus", true);
    }

    @Override
    public void update(AnActionEvent event) {

        Project project = event.getProject();
        TabsSuckProjectComponent projectComponent = project.getComponent(TabsSuckProjectComponent.class);
        TabsSuckApplicationComponent applicationComponent = project.getComponent(TabsSuckApplicationComponent.class);

        if (projectComponent != null) {
            if (applicationComponent != null) {

                // rebuild the menu

                removeAll();

                int numEditors = projectComponent.numEditors();

                for (int i = 0; i < numEditors; ++i) {
                    add(applicationComponent.focusAction(i));
                }
            }
            else {
                log.warn("ApplicationComponent in FocusActionGroup.update() is null.");
            }
        }
        else {
            log.warn("ProductComponent in FocusActionGroup.update() is null.");
        }
    }
}
