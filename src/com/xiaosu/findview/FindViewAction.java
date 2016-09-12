package com.xiaosu.findview;


import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.generation.actions.BaseGenerateAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtilBase;
import com.xiaosu.findview.common.Definitions;
import com.xiaosu.findview.common.Utils;
import com.xiaosu.findview.form.EntryList;
import com.xiaosu.findview.iface.ICancelListener;
import com.xiaosu.findview.iface.IConfirmListener;
import com.xiaosu.findview.model.Element;

import javax.swing.*;
import java.util.ArrayList;

public class FindViewAction extends BaseGenerateAction implements IConfirmListener, ICancelListener {

    protected JFrame mDialog;
    protected static final Logger log = Logger.getInstance(FindViewAction.class);

    @SuppressWarnings("unused")
    public FindViewAction() {
        super(null);
    }

    @SuppressWarnings("unused")
    public FindViewAction(CodeInsightActionHandler handler) {
        super(handler);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);
        Editor editor = event.getData(PlatformDataKeys.EDITOR);

        actionPerformedImpl(project, editor);
    }

    @Override
    public void actionPerformedImpl(Project project, Editor editor) {
        PsiFile file = PsiUtilBase.getPsiFileInEditor(editor, project);
        PsiFile layout = Utils.getLayoutFileFromCaret(editor, file);

        if (layout == null) {
            Utils.showErrorNotification(project, "No layout found");
            return; // no layout found
        }

        log.info("Layout file: " + layout.getVirtualFile());

        ArrayList<Element> elements = Utils.getIDsFromLayout(layout);
        if (!elements.isEmpty()) {
            showDialog(project, editor, elements);
        } else {
            Utils.showErrorNotification(project, "No IDs found in layout");
        }
    }

    public void onConfirm(Project project, Editor editor, ArrayList<Element> elements, String fieldNamePrefix, boolean createHolder) {
        PsiFile file = PsiUtilBase.getPsiFileInEditor(editor, project);
        if (file == null) {
            return;
        }
        PsiFile layout = Utils.getLayoutFileFromCaret(editor, file);

        closeDialog();


        if (Utils.getInjectCount(elements) > 0 || Utils.getClickCount(elements) > 0) { // generate injections
            new FindViewWriter(elements, file, getTargetClass(editor, file), "Generate Injections").execute();
        } else { //提示没有找到id
            Utils.showInfoNotification(project, " 布局文件里面没有设置id");
        }

    }

    public void onCancel() {
        closeDialog();
    }

    protected void showDialog(Project project, Editor editor, ArrayList<Element> elements) {
        PsiFile file = PsiUtilBase.getPsiFileInEditor(editor, project);
        if (file == null) {
            return;
        }
        PsiClass clazz = getTargetClass(editor, file);

        //检查是否有ButterKnife依赖
        /*final IButterKnife butterKnife = ButterKnifeFactory.findButterKnifeForPsiElement(project, file);
        if (clazz == null || butterKnife == null) {
            return;
        }*/

        // get parent classes and check if it's an adapter
        boolean createHolder = false;
        PsiReferenceList list = clazz.getExtendsList();
        if (list != null) {
            for (PsiJavaCodeReferenceElement element : list.getReferenceElements()) {
                if (Definitions.adapters.contains(element.getQualifiedName())) {
                    createHolder = true;
                }
            }
        }

        //根据id显示的内容
        EntryList panel = new EntryList(project, editor, elements, createHolder, this, this);

        mDialog = new JFrame();
        mDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        mDialog.getRootPane().setDefaultButton(panel.getConfirmButton());
        mDialog.getContentPane().add(panel);
        mDialog.pack();
        mDialog.setLocationRelativeTo(null);
        mDialog.setVisible(true);
    }

    protected void closeDialog() {
        if (mDialog == null) {
            return;
        }

        mDialog.setVisible(false);
        mDialog.dispose();
    }
}
