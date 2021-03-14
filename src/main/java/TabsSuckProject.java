
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileEditor.impl.EditorWithProviderComposite;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.impl.IdeFrameImpl;
import com.intellij.psi.*;
import com.intellij.util.ui.update.Activatable;
import com.intellij.util.ui.update.UiNotifyConnector;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.reflect.Field;
import java.util.*;
import java.util.Timer;
import java.util.function.Predicate;


public class TabsSuckProject implements ProjectComponent {

    /*********************************************************************************************
        Types, Properties and iVars
     *********************************************************************************************/

    private enum UPDATE_SOURCE {
        STARTUP,
        PROJECT_TREE, // user selected a file by clicking in the project tree view
        MANUAL, // user selected a file from the navigation bar, Navigate -> File... or elsewhere
        PLUGIN_INTERNAL, // this plugin updated the editor internally
        PLUGIN_OPEN,
        PLUGIN_SWAP,
        PLUGIN_SWITCH_TO_COUNTERPART,
        UNKNOWN
    }

    private static final Logger log = Logger.getInstance("net.mkd.TabsSuck.TabsSuckProject");

    private static final int WAIT_TO_FOCUS_DELAY = 250; // ms

    private Project project;
    private HashSet<Editor> visibleEditors = new HashSet<Editor>();
    private ArrayList<Editor> orderedEditors = new ArrayList<Editor>();
    private ArrayList<EditorWindow> orderedEditorWindows = new ArrayList<EditorWindow>();
    private int focusedEditorIndex = -1;

    private HashSet<Integer> followingEditorIndexes = new HashSet<>();

    private ArrayDeque<Triplet<VirtualFile, Integer, Integer>> fileOpenQueue = new ArrayDeque<>(); // file, target, focus (can be -1)
    private HashSet<Editor> waitingForVisibleEditors = new HashSet<>();
    private int nextVisibleEditorFocusIndex = -1;

    private PropertiesComponent propertiesComponent = null;

    /*********************************************************************************************
        Lifecycle
     *********************************************************************************************/

    public TabsSuckProject(Project project) {

        this.log.setLevel(Level.DEBUG);

        log.debug("TabsSuckProject(), project: " + project);

        this.project = project;
        this.propertiesComponent = PropertiesComponent.getInstance(this.project);

        addEditorFactoryListener();
    }

    /*********************************************************************************************
        ProjectComponent
     *********************************************************************************************/

    @Override
    public void initComponent() {
        log.debug("TabsSuckProject.initComponent()");
    }

    @Override
    public  void disposeComponent() {
        log.debug("TabsSuckProject.disposeComponent()");
    }

    @Override
    public void projectOpened() {
        log.debug("projectOpened()");

//        followingEditorsBitmask = propertiesComponent.getInt("net.mkdTabsSuck.followingEditorsBitmask", 0);
//        log.debug("Restored followingEditorsBitmask: " + Integer.toBinaryString(followingEditorsBitmask));
    }

    @Override
    public void projectClosed() {
        log.debug("projectClosed()");

//        log.debug("Saving followingEditorsBitmask: " + Integer.toBinaryString(followingEditorsBitmask));
//        propertiesComponent.setValue("net.mkdTabsSuck.followingEditorsBitmask", followingEditorsBitmask, 0);
    }

    /*********************************************************************************************
        Package-Private
     *********************************************************************************************/

    void switchToCounterpartAction(int index) {

        log.debug("switchToCounterpartAction(), targetIndex: " + index);

        Editor targetEditor = orderedEditors.get(index);
        if (targetEditor != null) {

            VirtualFile currentFile = fileFromEditor(targetEditor);
            VirtualFile counterpartFile = counterpartForFile(currentFile);

            if (counterpartFile != null) {

                queueFileOpenAtIndex(counterpartFile, index, index);
            }
            else {
                log.info("Can't find counterpart for file: " + currentFile);
            }
        }
        else {
            log.error("No editor at index " + index + " to switch to counterpart in!");
        }
    }

    void openAction(int originIndex, int targetIndex) {

        log.debug("openAction(), originIndex: " + originIndex + ", targetIndex: " + targetIndex);

        Editor originEditor = orderedEditors.get(originIndex);
        if (originEditor != null) {

            VirtualFile originFile = fileFromEditor(originEditor);
            queueFileOpenAtIndex(originFile, targetIndex, originIndex);
        }
        else {
            log.error("No editor at index " + originIndex + "!");
        }
    }

    void openCounterpartAction(int originIndex, int targetIndex) {

        log.debug("openCounterpartAction(), originIndex: " + originIndex + ", targetIndex: " + targetIndex);

        Editor originEditor = orderedEditors.get(originIndex);
        if (originEditor != null) {

            VirtualFile currentFile = fileFromEditor(originEditor);
            VirtualFile counterpartFile = counterpartForFile(currentFile);

            if (counterpartFile != null) {

                queueFileOpenAtIndex(counterpartFile, targetIndex, originIndex);
            }
            else {
                log.info("Can't find counterpart for file: " + currentFile);
            }
        }
        else {
            log.error("No editor at index " + originIndex + "!");
        }
    }

    void swapAction(int originIndex, int targetIndex) {

        log.debug("swapAction(), originIndex: " + originIndex + ", targetIndex: " + targetIndex);

        Editor originEditor = orderedEditors.get(originIndex);
        if (originEditor != null) {

            Editor targetEditor = orderedEditors.get(targetIndex);
            if (targetEditor != null) {

                VirtualFile originFile = fileFromEditor(originEditor);
                VirtualFile targetFile = fileFromEditor(targetEditor);

                queueFileOpenAtIndex(originFile, targetIndex, -1);
                queueFileOpenAtIndex(targetFile, originIndex, originIndex);
            }
            else {
                log.error("No editor at index " + targetIndex + "!");
            }
        }
        else {
            log.error("No editor at index " + originIndex + "!");
        }
    }

    void focusAction(int targetIndex) {

        log.debug("focusAction(), targetIndex: " + targetIndex);

        Editor targetEditor = orderedEditors.get(targetIndex);
        if (targetEditor != null) {

            setFocusedEditorIndex(targetIndex);
        }
        else {
            log.error("No editor at index " + targetIndex + " to focus!");
        }
    }

    void debugAction() {

        log.debug("focusedEditorIndex: " + focusedEditorIndex());
    }

    int numEditors() {

        return visibleEditors.size();
    }

    int focusedEditorIndex() {

        // TODO: can we use FileEditorManagerEx.getCurrentWindow() + orderedEditors instead?

        return focusedEditorIndex;
    }

    void setFocusedEditorIndex(int index) {

        log.debug("setFocusedEditorIndex(), index: " + index);

        FileEditorManagerEx fileEditorManager = (FileEditorManagerEx) FileEditorManagerEx.getInstance(project);
        EditorWindow window = orderedEditorWindows.get(index);
        fileEditorManager.setCurrentWindow(window);
    }

    int indexOfEditor(Editor editor) {

        return orderedEditors.indexOf(editor);
    }

    boolean isFollowingFirst(int index) {

        log.debug("isFollowingFirst(), index: " + index);

        return followingEditorIndexes.contains(index);
    }

    void setIsFollowingFirst(int index, boolean following) {

        log.debug("setIsFollowingFirst(), index: " + index + ", " + (following ? "true" : "false"));

        if (following) {
            followingEditorIndexes.add(index);

            openCounterpartAction(0, index);
        }
        else {
            followingEditorIndexes.remove(index);
        }
    }

    boolean isEditorAttachedToProjectWindow(Editor editor) {
        //log.debug("editorIsInProjectWindow()");

        Component parentComponent = editor.getComponent().getParent();
        Component root = null;
        while ( parentComponent != null ) {
            root = parentComponent;
            parentComponent = parentComponent.getParent();
        }

        if (root.getClass() == IdeFrameImpl.class) {
            return true;
        }

        return false;
    }

    /*********************************************************************************************
        Private
     *********************************************************************************************/

    private void addEditorFactoryListener() {

        log.debug("addEditorFactoryListener()");

        EditorFactoryListener editorFactoryListener = new EditorFactoryListener() {
            @Override
            public void editorCreated(@NotNull EditorFactoryEvent event) {

                Editor editor = event.getEditor();
                VirtualFile file = fileFromEditor(editor);
                log.debug("editorCreated(), file: " + (file != null ? file : "null"));

                // null file happens at startup, and no showNotify() event will fire for it
                // also happens for some settings panels (ex. font preview) -- not sure if showNotify() fires.
                if (file != null) {
                    waitingForVisibleEditors.add(editor);
                }

                UPDATE_SOURCE source = currentUpdateSource();


                // register for focus changes
                final JComponent contentComponent = editor.getContentComponent();
                final FocusListener focusListener = new FocusListener() {
                    @Override
                    public void focusGained(@NotNull FocusEvent e) {
                        focusedEditorIndex = orderedEditors.indexOf(editor);
                    }
                    @Override
                    public void focusLost(@NotNull FocusEvent e) {
                    }
                };
                contentComponent.addFocusListener(focusListener);


                // register for visibility changes
                Activatable activatable = new Activatable.Adapter() {
                    public void showNotify() {
                        VirtualFile file = fileFromEditor(editor);
                        log.debug("*** showNotify(): " + file + " ***");
                        log.debug("*** showNotify, component: " +  editor.getComponent().hashCode() + " ***");
                        log.debug("*** showNotify, content component: " +  editor.getContentComponent().hashCode() + " ***");
                        log.debug("*** showNotify, component parent: " +  editor.getComponent().getParent().hashCode() + " ***");
                        log.debug("*** showNotify, component parent parent: " +  editor.getComponent().getParent().getParent().hashCode() + " ***");

                        if (file != null) {

                            if (isEditorAttachedToProjectWindow(editor)) {
                                visibleEditors.add(editor);
                            }
                            updateOrderedEditors(file);
                            updateOrderedWindows();

                            int editorIndex = indexOfEditor(editor);
                            log.debug("editorIndex: " + editorIndex);

                            debugPrintWaitingForVisibleEditors();
                            debugPrintOrderedEditors();

                            if (source == UPDATE_SOURCE.PLUGIN_OPEN) {
                                log.debug("PLUGIN_OPEN");

                                setIsFollowingFirst(editorIndex, false);

                                if (editorIndex == 0) {
                                    updateFollowingEditors(file, -1);
                                }
                            }
                            else if (source == UPDATE_SOURCE.PLUGIN_SWAP) {
                                log.debug("PLUGIN_SWAP");

                                setIsFollowingFirst(editorIndex, false);

                                if (editorIndex == 0) {
                                    updateFollowingEditors(file, -1);
                                }
                            }
                            else if (source == UPDATE_SOURCE.PLUGIN_SWITCH_TO_COUNTERPART) {
                                log.debug("PLUGIN_SWITCH_TO_COUNTERPART");

                                if (editorIndex == 0) {
                                    updateFollowingEditors(file, -1);
                                }
                            }
                            else if (source == UPDATE_SOURCE.PLUGIN_INTERNAL) {
                                log.debug("PLUGIN_INTERNAL");
                                // ignore
                            }
                            else if (source == UPDATE_SOURCE.STARTUP) {
                                log.debug("STARTUP");
                                // ignore
                            }
                            else if (editorIndex == 0) {
                                log.debug("INDEX 0");

                                updateFollowingEditors(file, 0);
                            }
                            else if (source == UPDATE_SOURCE.PROJECT_TREE) {
                                log.debug("PROJECT_TREE");
                                // ignore
                            }
                            else if (source == UPDATE_SOURCE.MANUAL) {
                                log.debug("MANUAL");

                                setIsFollowingFirst(editorIndex, false);
                            }

                            if (nextVisibleEditorFocusIndex != -1) {
                                log.debug("*** FOCUSING INDEX: " + nextVisibleEditorFocusIndex + " ***");

                                // it seems that sometimes even after this showNotify(), the new editor
                                // is hidden, then re-shown, and the focus gets switched away from the target
                                // i *think* this may be a bug with IntelliJ
                                // the "solution" is to delay and re-set the focus in a moment...
                                // this seems to work well (as long as the user doesn't try to change the first
                                // editor again before the timer fires.)

                                Timer timer = new Timer();
                                timer.schedule( new TimerTask(){
                                    public void run() {
                                        //log.debug("************* FOCUS TIMER  *****************");

                                        ApplicationManager.getApplication().invokeLater(new Runnable() {

                                            public void run() {
                                                if (nextVisibleEditorFocusIndex != -1) {
                                                    setFocusedEditorIndex(nextVisibleEditorFocusIndex);
                                                    nextVisibleEditorFocusIndex = -1;
                                                }
                                            }
                                        });
                                    }
                                }, WAIT_TO_FOCUS_DELAY);
                            }

                            // check to see if there are more files to open
                            Predicate<Editor> removePredicate = e-> e.getDocument() == editor.getDocument();
                            waitingForVisibleEditors.removeIf(removePredicate);
                            debugPrintWaitingForVisibleEditors();
                            checkFileOpenQueue();
                        }
                        else {
                            log.debug("Skipping null file.");
                        }
                    }

                    public void hideNotify() {
                        VirtualFile file = fileFromEditor(editor);
                        if (file != null) {
                            log.debug("hideNotify(): " + file);
                        }
                        visibleEditors.remove(editor);
                        updateOrderedEditors(null);
                        updateOrderedWindows();
                    }
                };

                // stupid but needed
                UiNotifyConnector connector = new UiNotifyConnector(editor.getContentComponent(), activatable) {
                    @Override
                    protected void showNotify() {
                        super.showNotify();
                    }

                    @Override
                    protected void hideNotify() {
                        super.hideNotify();
                    }
                };
            }

            @Override
            public void editorReleased(@NotNull EditorFactoryEvent event) {
                Editor editor = event.getEditor();
                VirtualFile file = fileFromEditor(editor);
                if (file != null) {
                    log.debug("editorReleased(): " + file.getPath());
                }
                visibleEditors.remove(editor);
                updateOrderedEditors(null);
                updateOrderedWindows();
            }
        };

        EditorFactory editorFactory = EditorFactory.getInstance();
        editorFactory.addEditorFactoryListener(editorFactoryListener);
    }

    // used in EditorFactoryListener.editorCreated()
    // looks at the stack to see what initiated the creation of the editor
    // this is asking to break in future versions, but it's about all we can do...
    private UPDATE_SOURCE currentUpdateSource() {

        UPDATE_SOURCE source = UPDATE_SOURCE.UNKNOWN;

        StackTraceElement[] stackElements = getStackTrace();

        //debugPrintStackTrace(stackElements);

        if (stackTraceContainsSymbol(stackElements,"openAction")) {
            source = UPDATE_SOURCE.PLUGIN_OPEN;
            log.debug("UPDATE_SOURCE.PLUGIN_OPEN");
        }
        else if (stackTraceContainsSymbol(stackElements,"swapAction")) {
            source = UPDATE_SOURCE.PLUGIN_SWAP;
            log.debug("UPDATE_SOURCE.PLUGIN_SWAP");
        }
        else if (stackTraceContainsSymbol(stackElements,"switchToCounterpartAction")) {
            source = UPDATE_SOURCE.PLUGIN_SWITCH_TO_COUNTERPART;
            log.debug("UPDATE_SOURCE.PLUGIN_SWITCH_TO_COUNTERPART");
        }
        else if (stackTraceContainsSymbol(stackElements, "openFileAtIndex")) { // MUST CHECK FIRST
            source = UPDATE_SOURCE.PLUGIN_INTERNAL;
            log.debug("UPDATE_SOURCE.PLUGIN_INTERNAL");
        }
        else if (stackTraceContainsSymbol(stackElements, "IdeStarter")) {
            source = UPDATE_SOURCE.STARTUP;
            log.debug("UPDATE_SOURCE.STARTUP");
        }
        else if (stackTraceContainsSymbol(stackElements,"SearchEverywhereUI")
                || stackTraceContainsSymbol(stackElements,"NavBarUpdateQueue")
                || stackTraceContainsSymbol(stackElements,"ChooseByNamePopup")) {
            source = UPDATE_SOURCE.MANUAL;
            log.debug("UPDATE_SOURCE.MANUAL");
        }
        else if (stackTraceContainsSymbol(stackElements,"projectView")) {
            source = UPDATE_SOURCE.PROJECT_TREE;
            log.debug("UPDATE_SOURCE.PROJECT_TREE");
        }
        else {
            source = UPDATE_SOURCE.UNKNOWN;
            log.debug("UPDATE_SOURCE.UNKNOWN");
        }

        return source;
    }

    private void updateFollowingEditors(VirtualFile sourceFile, int focus) {

        VirtualFile counterpartFile = counterpartForFile(sourceFile);
        if (counterpartFile != null) {

            for (int i=0; i<orderedEditors.size(); ++i) {

                final int index = i;

                if (isFollowingFirst(i)) {
                    queueFileOpenAtIndex(counterpartFile, index, focus);
                }
            }
        }
        else {
            log.info("Can't find counterpart for file: " + sourceFile);
        }
    }

    // looks at this.visibleEditors, orders them, and updates this.orderedEditors
    private void updateOrderedEditors(VirtualFile newlyVisibleFile) {

        log.debug("updateOrderedEditors()");

        ArrayList<Editor> oldOrderedEditors = (ArrayList<Editor>)orderedEditors.clone();

        HashSet<Editor> unordered = this.visibleEditors;

        ArrayList<Editor> ordered = new ArrayList<Editor>();
        ordered.addAll(unordered);
        Collections.sort(ordered, new Comparator<Editor>() {
            @Override
            public int compare(Editor lhs, Editor rhs) {
                // -1 = less than, 1 = greater than, 0 = equal, all inversed for descending

                int result = 0;

                // area INSIDE gutter
                // gutter width varies.

                try {
                    Point leftPos = lhs.getContentComponent().getLocationOnScreen();
                    Point rightPos = rhs.getContentComponent().getLocationOnScreen();

                    if (leftPos.x < rightPos.x) {
                        result = -1;
                    }
                    else if (leftPos.x > rightPos.x) {
                        result = 1;
                    }
                    // TODO: THIS IS FLAWED
                    //  because depending on how many lines a file has its gutter changed width
                    // the editor's component's location is determined by its content WITHOUT gutter
                    // this means an editor with less lines and a narrower gutter positioned logically under an editor
                    // with more lines and wider gutter will get ordered first. not what we want.
                    // can we get the gutter's left edge?
                    //      -> edit: probably not easily
                    else {
                        // x positions are equal, compare y

                        if (leftPos.y < rightPos.y) {
                            result = -1;
                        }
                        else if (leftPos.y > rightPos.y) {
                            result = 1;
                        }
                    }

                    return result;
                }
                catch (IllegalComponentStateException e) {
                    return -1;
                }



                // works
//                Point leftPos = lhs.getContentComponent().getLocationOnScreen();
//                Point rightPos = rhs.getContentComponent().getLocationOnScreen();
//
//                return ((leftPos.x < rightPos.x) ? -1 : (leftPos.x > rightPos.x) ? 1 : 0);
            }
        });

        this.orderedEditors = (ArrayList<Editor>)ordered.clone();

        //updateFollowingEditorIndexes(oldOrderedEditors, orderedEditors);
        if (oldOrderedEditors.size() != orderedEditors.size()) {
            updateNumVisibleEditorsChanged(oldOrderedEditors.size(), orderedEditors.size(), newlyVisibleFile);
        }

        //debugPrintOrderedEditors();
    }

    private void updateOrderedWindows() {

        log.debug("updateOrderedWindows()");

        FileEditorManagerEx fileEditorManager = (FileEditorManagerEx) FileEditorManagerEx.getInstance(project);
        EditorWindow[] windows = fileEditorManager.getWindows();
        HashSet<EditorWindow> unordered = new HashSet<EditorWindow>();
        for (EditorWindow window : windows) {
            unordered.add(window);
        }

        ArrayList<EditorWindow> ordered = new ArrayList<EditorWindow>();
        ordered.addAll(unordered);
        Collections.sort(ordered, new Comparator<EditorWindow>() {
            @Override
            public int compare(EditorWindow lhs, EditorWindow rhs) {
                // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending

                try {
                    Field fieldLHF = lhs.getClass().getDeclaredField("myPanel"); //NoSuchFieldException
                    fieldLHF.setAccessible(true);
                    JPanel panelLHS = (JPanel) fieldLHF.get(lhs); //IllegalAccessException
                    //log.debug("PANEL: " + panelLHS);
                    Point leftPos = panelLHS.getLocationOnScreen();

                    Field fieldRHF = rhs.getClass().getDeclaredField("myPanel"); //NoSuchFieldException
                    fieldRHF.setAccessible(true);
                    JPanel panelRHS = (JPanel) fieldRHF.get(rhs); //IllegalAccessException
                    //log.debug("PANEL: " + panelRHS);
                    Point rightPos = panelRHS.getLocationOnScreen();


                    int result = 0;

                    if (leftPos.x < rightPos.x) {
                        result = -1;
                    }
                    else if (leftPos.x > rightPos.x) {
                        result = 1;
                    }
                    // TODO: THIS IS FLAWED
                    //  because depending on how many lines a file has its gutter changed width
                    // the editor's component's location is determined by its content WITHOUT gutter
                    // this means an editor with less lines and a narrower gutter positioned logically under an editor
                    // with more lines and wider gutter will get ordered first. not what we want.
                    // can we get the gutter's left edge?
                    //      -> edit: probably not easily
                    else {
                        // x positions are equal, compare y

                        if (leftPos.y < rightPos.y) {
                            result = -1;
                        }
                        else if (leftPos.y > rightPos.y) {
                            result = 1;
                        }
                    }

                    return result;

                    // og
                    //return (pointLHS.x < pointRHS.x ? -1 : (pointLHS.x > pointRHS.x) ? 1 : 0);
                }
                catch (Exception e) {
                    //log.error("EXCEPTION: " + e);
                    return -1;
                }
            }
        });

        this.orderedEditorWindows = ordered;
    }

    // remove all following editor indexes that exceed newNum
    // update following editors if new editor added
    private void updateNumVisibleEditorsChanged(int oldNum, int newNum, VirtualFile newlyVisibleFile) {
        log.debug("updateNumVisibleEditorsChanged(), oldNum: " + oldNum + ", newNum: " + newNum);

        if (newNum < oldNum) {
            Predicate<Integer> removePredicate = i-> i > newNum;
            followingEditorIndexes.removeIf(removePredicate);
        }
        else if (newNum > oldNum) {
            if (orderedEditors.size() > 0) {
                updateFollowingEditors(fileFromEditor(orderedEditors.get(0)), -1);
            }
        }
    }

    private VirtualFile fileFromEditor(Editor editor) {

        return FileDocumentManager.getInstance().getFile(editor.getDocument());
    }

    private void queueFileOpenAtIndex(VirtualFile file, int index, int focus) {
        log.debug("queueFileOpen(), file: " + file.getPath() + ", index: " + index + ", focus: " + focus);

        fileOpenQueue.push(new Triplet<>(file, index, focus));
        checkFileOpenQueue();
    }

    private void checkFileOpenQueue() {
        log.debug("checkFileOpenQueue()");

        if (fileOpenQueue.size() > 0 && waitingForVisibleEditors.size() == 0) {

            Triplet<VirtualFile, Integer, Integer> fileIndexPair = fileOpenQueue.pop();
            VirtualFile file = fileIndexPair.x;
            int index = fileIndexPair.y;
            int focus = fileIndexPair.z;
            openFileAtIndex(file, index, focus);
        }
    }

    // should only be called by checkFileOpenQueue()
    private void openFileAtIndex(VirtualFile file, int index, int focus) {
        log.debug("openFileAtIndex(), file: " + file.getPath() + ", index: " + index);

        if (file != null) {
            FileEditorManagerEx fileEditorManager = (FileEditorManagerEx) FileEditorManagerEx.getInstance(project);
            EditorWindow[] windows = fileEditorManager.getWindows();

            if (index <= windows.length-1) {

                EditorWindow window = orderedEditorWindows.get(index);
                log.debug("Window at index " + index + ": " + window);

                if (window != null) {

                    EditorWithProviderComposite[] editors = window.getEditors();


                     // WORKS
//                                VirtualFile blankFile = new LightVirtualFile("blank.txt", "\n");
//                                fileEditorManager.openFileWithProviders(blankFile, false, window);
//
//                                for (EditorWithProviderComposite e : editors) {
//                                    VirtualFile editorFile = e.getFile();
//                                    if (editorFile != blankFile) {
//                                        window.closeFile(file, false, false);
//                                    }
//                                }
//
//                                fileEditorManager.openFileWithProviders(file, false, window);
//
//                                window.closeFile(blankFile, false, false);


                    boolean alreadyContainsFile = false;
                    for (EditorWithProviderComposite e : editors) {
                        if (e.getFile().getPath().contentEquals(file.getPath())) {
                            log.debug("Already contains file.");
                            alreadyContainsFile = true;
                            break;
                        }
                    }

                    for (EditorWithProviderComposite e : editors) {
                        if (e.getFile() != file) {
                            window.closeFile(file, false, false);
                        }
                    }

                    if (!alreadyContainsFile) {
                        log.debug("OPENING FILE: " + file + " AT INDEX: " + index + ", FOCUS: " + ( index == focus ? "true" : "false" ));
                        //nextCreatedEditorTargetIndex = index;
                        nextVisibleEditorFocusIndex = focus;
                        fileEditorManager.openFileWithProviders(file, (index == focus ? true : false), window);
                    }

//                    log.debug("OPENING FILE: " + file + " AT INDEX: " + index);
//                    fileEditorManager.openFileWithProviders(file, true, window);

                    log.debug("New editors:");
                    for (EditorWithProviderComposite e : editors) {
                        log.debug("\t" + e.getFile());
                    }
                }
                else {
                    log.debug("Window for index is null.");
                }
            }
            else {
                log.debug("Index " + index + " exceeds window count.");
            }
            }
        else {
            log.warn("File is null.");
        }
    }

    private VirtualFile counterpartForFile(VirtualFile file) {

        String basename = file.getNameWithoutExtension();
        String extension = file.getExtension();

        ArrayList<String> counterpartExtensions = new ArrayList<>();

        // note: .C, .inl, .tcc

        if (extension.equals("h")) {

            counterpartExtensions.add("c");
            counterpartExtensions.add("cc");
            counterpartExtensions.add("cpp");
            counterpartExtensions.add("cxx");
        }
        else if (extension.equals("hh")) {

            counterpartExtensions.add("cc");
            counterpartExtensions.add("cpp");
            counterpartExtensions.add("cxx");
        }
        else if (extension.equals("hpp")) {

            counterpartExtensions.add("cpp");
            counterpartExtensions.add("cc");
            counterpartExtensions.add("cxx");
        }
        else if (extension.equals("hxx")) {

            counterpartExtensions.add("cxx");
            counterpartExtensions.add("cpp");
            counterpartExtensions.add("cc");
        }
        else if (extension.equals("cc")) {

            counterpartExtensions.add("hh");
            counterpartExtensions.add("h");
            counterpartExtensions.add("hpp");
            counterpartExtensions.add("hxx");
        }
        else if (extension.equals("cpp")) {

            counterpartExtensions.add("hpp");
            counterpartExtensions.add("h");
            counterpartExtensions.add("hxx");
            counterpartExtensions.add("hh");
        }
        else if (extension.equals("cxx")) {

            counterpartExtensions.add("hxx");
            counterpartExtensions.add("hpp");
            counterpartExtensions.add("hh");
            counterpartExtensions.add("h");
        }
        else if (extension.equals("c")) {

            counterpartExtensions.add("h");
        }

        return mostLikelyFileMatchWithExtensions(basename, counterpartExtensions, file.getPath());
    }

    private VirtualFile mostLikelyFileMatchWithExtensions(String basename, ArrayList<String> extensions, String originalPath) {

        // searches for files with name 'basename' and extensions in 'extensions'
        // computes the hamming distance between the paths of each found file and 'originalPath'
        // returns closest result.

        FilenameIndex index = new FilenameIndex();

        int lowestDistance = Integer.MAX_VALUE;
        VirtualFile lowestDistanceFile = null;

        for (String extension : extensions) {

            PsiFileSystemItem[] psiNames = index.getFilesByName(
                    project,
                    basename + "." + extension,
                    GlobalSearchScope.allScope(project),
                    //GlobalSearchScope.projectScope(project), // seems to prioritize files in immediate project
                    false);

            for (int i=0; i<psiNames.length; ++i) {

                PsiFileSystemItem item = psiNames[i];
                VirtualFile file = item.getVirtualFile();
                String path = file.getPath();

                int distance = hammingDistance(originalPath, path);
                //log.debug("file: " + path + ", distance: " + distance);
                if (distance < lowestDistance) {
                    lowestDistance = distance;
                    lowestDistanceFile = file;
                }
            }
        }
        return lowestDistanceFile;
    }

//    private static int SetBit(int mask, int index) {
//
//        mask |= (1 << index);
//        return mask;
//    }
//
//    private static int ClearBit(int mask, int index) {
//
//        mask &= ~(1 << index);
//        return mask;
//    }
//
//    private static boolean IsBitSet(int mask, int index) {
//
//        mask &= (1 << index);
//        return mask > 0;
//    }

    // not sure if this is a true Hamming Distance since the strings may be of different lengths,
    // but it should work great for our purposes.
    private static int hammingDistance(String str1, String str2)
    {
        int i = 0, count = 0;
        while (i < str1.length())
        {
            if (str1.length()-1 < i || str2.length()-1 < i) {
                ++count;
            }
            else if (str1.charAt(i) != str2.charAt(i)) {
                ++count;
            }
            ++i;
        }
        return count;
    }

    private static StackTraceElement[] getStackTrace() {

        return Thread.currentThread().getStackTrace();
    }

    private static boolean stackTraceContainsSymbol(StackTraceElement[] elements, String symbol) {

        for (StackTraceElement e : elements) {

            if ((e.getClassName() + e.getMethodName()).contains(symbol)) {
                return true;
            }
        }

        return false;
    }

    private void debugPrintOrderedEditors() {

        log.debug("Ordered editors:");
        for (Editor e : orderedEditors) {
            VirtualFile file = fileFromEditor(e);
            log.debug("\t" + file.getPath());
        }
    }

    private void debugPrintVisibleEditors() {

        log.debug("Visible editors:");
        for (Editor e : visibleEditors) {
            VirtualFile file = fileFromEditor(e);
            log.debug("\t" + file.getPath());
        }
    }

    private void debugPrintWaitingForVisibleEditors() {

        log.debug("Waiting for visible editors:");
        for (Editor e : waitingForVisibleEditors) {
            log.debug("\t" + e);
        }
    }

    private  void debugPrintFollowingEditorIndexes() {

        log.debug("Following editor indexes:");
        for (Integer i : followingEditorIndexes) {
            log.debug("\t" + i);
        }
    }

    private static void debugPrintStackTrace(StackTraceElement[] elements) {

        log.debug("Stack trace:");
        for (StackTraceElement e : elements) {
            log.debug("\t" + e.getClassName() + "#" + e.getMethodName());
        }
    }
}
