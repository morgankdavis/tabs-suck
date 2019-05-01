import com.intellij.ide.actions.EditSourceInNewWindowAction;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.diagnostic.Logger;
import org.apache.log4j.Level;

import java.util.ArrayList;
import java.util.List;


//
// Editor Context Menu:
//
//    ...
//    -----------------
//    Open In               -> Editor 1
//                          -> Editor 2
//                          -> ...
//    Open Counterpart In   -> Editor 1
//                          -> Editor 2
//                          -> ...
//    Swap With             -> Editor 1
//                          -> Editor 2
//                          -> ...
//    -----------------
//    Switch to Counterpart
//
//
// Navigate Menu:
//
//    ...
//    Line/Column...
//    -----------------
//    Focus                 -> Editor 1
//                          -> Editor 2
//                          -> ...
//    -----------------
//    ...
//

public class TabsSuckApplicationComponent implements ApplicationComponent {

    /*********************************************************************************************
        Types, Properties and iVars
     *********************************************************************************************/

    private static final Logger log = Logger.getInstance("net.morgankdavis.TabsSuck.TabsSuckApplicationComponent");

    private static final int MAX_EDITORS = 12;

    private OpenActionGroup openActionGroup;
    private ArrayList<OpenAction> openActions;

    private OpenCounterpartActionGroup openCounterpartActionGroup;
    private ArrayList<OpenCounterpartAction> openCounterpartActions;

    private SwapActionGroup swapActionGroup;
    private ArrayList<SwapAction> swapActions;

    private FocusActionGroup focusActionGroup;
    private ArrayList<FocusAction> focusActions;

    private SwitchToCounterpartAction switchToCounterpartAction;
    private FollowFirstAction followFirstAction;

    private NewWindowAction newWindowAction;

//    private DefaultActionGroup debugActionGroup;
//    private DebugAction debugAction;

    /*********************************************************************************************
        ApplicationComponent
     *********************************************************************************************/

    @Override
    public void initComponent() {

        this.log.setLevel(Level.DEBUG);

        log.debug("TabsSuckApplicationComponent.initComponent()");

        UISettings.getInstance().setEditorTabPlacement(UISettings.TABS_NONE);

        initActions();

        //debugPrintAllActionIDs();
    }

    @Override
    public  void disposeComponent() {
        log.debug("TabsSuckApplicationComponent.disposeComponent()");
    }

    /*********************************************************************************************
        Public
     *********************************************************************************************/

    public OpenAction openAction(int index) {

        if (index < openActions.size()) {
            return openActions.get(index);
        }
        return null;
    }

    public OpenCounterpartAction openCounterpartAction(int index) {

        if (index < openCounterpartActions.size()) {
            return openCounterpartActions.get(index);
        }
        return null;
    }

    public SwapAction swapAction(int index) {

        if (index < swapActions.size()) {
            return swapActions.get(index);
        }
        return null;
    }

    public FocusAction focusAction(int index) {

        if (index < focusActions.size()) {
            return focusActions.get(index);
        }
        return null;
    }

    public NewWindowAction newWindowAction() {

        return newWindowAction;
    }

    /*********************************************************************************************
        Private
     *********************************************************************************************/

    private void initActions() {
        log.info("initActions()");

        // apparently actions need to be registered in the Application Component.
        // we'll just create all the actions we think we will need here, and grab them from the DefaultActionGroup
        // subclasses to rebuilt the child actions on update()

        ActionManager actionManager = ActionManager.getInstance();

        openActionGroup = new OpenActionGroup();
        openCounterpartActionGroup = new OpenCounterpartActionGroup();
        swapActionGroup = new SwapActionGroup();
        focusActionGroup = new FocusActionGroup();

        String openGroupID = "TabsSuck.action.group.Open";
        actionManager.registerAction(openGroupID, openActionGroup);

        String openCounterpartGroupID = "TabsSuck.action.group.OpenCounterpart";
        actionManager.registerAction(openCounterpartGroupID, openCounterpartActionGroup);

        String swapGroupID = "TabsSuck.action.group.Swap";
        actionManager.registerAction(swapGroupID, swapActionGroup);

        String focusGroupID = "TabsSuck.action.group.Focus";
        actionManager.registerAction(focusGroupID, focusActionGroup);

        openActions = new ArrayList<>();
        openCounterpartActions = new ArrayList<>();
        swapActions = new ArrayList<>();
        focusActions = new ArrayList<>();

        for (int i=0; i<MAX_EDITORS; ++i) {

            OpenAction openAction = new OpenAction(i);
            OpenCounterpartAction openCounterpartAction = new OpenCounterpartAction(i);
            SwapAction swapAction = new SwapAction(i);
            FocusAction focusAction = new FocusAction(i);

            String openID = "TabsSuck.action.Open" + i;
            String openCounterpartID = "TabsSuck.action.OpenCounterpart" + i;
            String swapID = "TabsSuck.action.Swap" + i;
            String focusID = "TabsSuck.action.Focus" + i;

            actionManager.registerAction(openID, openAction);
            actionManager.registerAction(openCounterpartID, openCounterpartAction);
            actionManager.registerAction(swapID, swapAction);
            actionManager.registerAction(focusID, focusAction);

            openActions.add(openAction);
            openCounterpartActions.add(openCounterpartAction);
            swapActions.add(swapAction);
            focusActions.add(focusAction);

            openActionGroup.add(openAction);
            openCounterpartActionGroup.add(openCounterpartAction);
            swapActionGroup.add(swapAction);
            focusActionGroup.add(focusAction);
        }

        switchToCounterpartAction = new SwitchToCounterpartAction();
        String switchToCounterpartID = "TabsSuck.action.SwitchToCounterpart";
        actionManager.registerAction(switchToCounterpartID, switchToCounterpartAction);

        followFirstAction = new FollowFirstAction();
        String followFirstID = "TabsSuck.action.FollowFirst";
        actionManager.registerAction(followFirstID, followFirstAction);

        newWindowAction = new NewWindowAction();
        String newWindowID = "TabsSuck.action.OpenInNewWindow";
        actionManager.registerAction(newWindowID, newWindowAction);

        DefaultActionGroup editorPopupMenu = (DefaultActionGroup)actionManager.getAction("EditorPopupMenu");
        editorPopupMenu.addSeparator();
        editorPopupMenu.add(openActionGroup);
        editorPopupMenu.add(openCounterpartActionGroup);
        editorPopupMenu.add(swapActionGroup);
        editorPopupMenu.addSeparator();
        editorPopupMenu.add(switchToCounterpartAction);
        editorPopupMenu.addSeparator();
        editorPopupMenu.add(followFirstAction);

        //editorPopupMenu.addSeparator();

//        EditSourceInNewWindowAction openInNewWindowAction = actionManager.getAction("EditSourceInNewWindow"); // "Open source in new window"
//        String className = openInNewWindowAction.getClass().getName();
//        log.debug("openInNewWindowAction class name: " + className);
//
        //editorPopupMenu.add(newWindowAction);


        //
        // ...
        //
        // "Line/Column..."
        // --------------------------
        // "Focus ->"
        // --------------------------
        //
        // ...
        //

        DefaultActionGroup navigateActionGroup = (DefaultActionGroup)actionManager.getAction("GoToMenu");

        // add "Focus" under "Line/Column..."
        Constraints afterLineColumnActionConstraint = new Constraints(Anchor.AFTER, "GotoLine");
        navigateActionGroup.add(focusActionGroup, afterLineColumnActionConstraint);

        // add separator above "Focus"
        Constraints beforeFollowMainConstraint = new Constraints(Anchor.BEFORE, focusGroupID);
        Separator separatorAction = Separator.create();
        navigateActionGroup.add(separatorAction, beforeFollowMainConstraint);


        // ************* "Debug" in main menu *************

//        DefaultActionGroup mainMenuActionGroup = (DefaultActionGroup)actionManager.getAction("MainMenu");
//        debugActionGroup = new DefaultActionGroup("Debug", true);
//        debugAction = new DebugAction();
//        String debugID = "TabsSuck.action.Debug";
//        actionManager.registerAction(debugID, debugAction);
//        debugActionGroup.add(debugAction);
//        mainMenuActionGroup.add(debugActionGroup);
    }

    private void debugPrintAllActionIDs() {

        ActionManager actionManager = ActionManager.getInstance();

        String[] allActionIDs = actionManager.getActionIds("");
        for (String actionID : allActionIDs) {
            log.info("*** ACTION ID: '" + actionID + "' ***");
        }
    }
}
