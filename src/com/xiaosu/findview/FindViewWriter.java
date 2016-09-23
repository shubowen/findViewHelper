package com.xiaosu.findview;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.EverythingGlobalScope;
import com.xiaosu.findview.common.Definitions;
import com.xiaosu.findview.model.Element;

import java.util.List;


public class FindViewWriter extends WriteCommandAction.Simple {

    private final PsiFile mFile;
    private final Project mProject;
    private final PsiClass mClass;
    private final List<Element> mElements;
    private final PsiElementFactory mFactory;

    protected FindViewWriter(List<Element> elements, PsiFile file, PsiClass clazz, String command) {
        super(clazz.getProject(), command);

        mFile = file;
        mProject = clazz.getProject();
        mClass = clazz;
        mElements = elements;
        mFactory = JavaPsiFacade.getElementFactory(mProject);
    }

    @Override
    protected void run() throws Throwable {

        filterElementHasFind(mClass.findMethodsByName("findView", false)[0],
                mClass.findMethodsByName("onCreate", false)[0]);

        generateFields();
        generateFindView();

        //格式化
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(mProject);
        styleManager.optimizeImports(mFile);
        styleManager.shortenClassReferences(mClass);
        new ReformatCodeProcessor(mProject, mClass.getContainingFile(), null, false).runWithoutProgress();
    }

    private void generateFindView() {
        PsiClass activityClass = JavaPsiFacade.getInstance(mProject).findClass(
                "android.app.Activity", new EverythingGlobalScope(mProject));
        PsiClass fragmentClass = JavaPsiFacade.getInstance(mProject).findClass(
                "android.app.Fragment", new EverythingGlobalScope(mProject));
        PsiClass supportFragmentClass = JavaPsiFacade.getInstance(mProject).findClass(
                "android.support.v4.app.Fragment", new EverythingGlobalScope(mProject));


        if (activityClass != null && mClass.isInheritor(activityClass, true)) {
            generateActivityFindView();
        } else if ((fragmentClass != null && mClass.isInheritor(fragmentClass, true)) || (supportFragmentClass != null && mClass.isInheritor(supportFragmentClass, true))) {
            generateFragmentFindView();
        }
    }

    private void generateFragmentFindView() {
        if (mClass.findMethodsByName("onViewCreated", false).length == 0) {
            addOnViewCreatedMethod();
        }

        PsiMethod onViewCreated = mClass.findMethodsByName("onViewCreated", false)[0];
        PsiParameter psiParameter = onViewCreated.getParameterList().getParameters()[0];
        for (Element element : mElements) {
            if (!element.used) continue;

            onViewCreated.getBody().add(mFactory.createStatementFromText(
                    element.fieldName + " = (" +
                            ((element.nameFull != null && element.nameFull.length() > 0) ? element.nameFull : element.name)
                            + ") " + psiParameter.getName() + ".findViewById(R.id." + element.id + ");\n", mClass));
        }
    }

    /**
     * 给Activity添加findViewById代码
     */
    private void generateActivityFindView() {
        if (mClass.findMethodsByName("onCreate", false).length == 0) {
            //Activity没有重写onCreate方法,自动添加
            addOnCreateMethod();
        }
        if (mClass.findMethodsByName("findView", false).length == 0) {
            //Activity没有重写onCreate方法,自动添加
            addFindViewMethod();
        }

        PsiMethod onCreate = mClass.findMethodsByName("onCreate", false)[0];
        boolean hasFindViewInvoked = false;
        for (PsiStatement statement : onCreate.getBody().getStatements()) {
            //查找setContentView()
            if (statement.getFirstChild() instanceof PsiMethodCallExpression) {
                PsiReferenceExpression methodExpression = ((PsiMethodCallExpression) statement.getFirstChild()).getMethodExpression();
                //查找findView方法调用
                if (methodExpression.getText().equals("findView")) {
                    hasFindViewInvoked = true;
                    break;
                }
            }
        }

        for (PsiStatement statement : onCreate.getBody().getStatements()) {
            //查找setContentView()
            if (statement.getFirstChild() instanceof PsiMethodCallExpression) {
                PsiReferenceExpression methodExpression = ((PsiMethodCallExpression) statement.getFirstChild()).getMethodExpression();
                //在setContentView()后面插入findViewById代码
                if (methodExpression.getText().equals("setContentView")) {
                    //添加findView方法调用
                    if (!hasFindViewInvoked)
                        onCreate.getBody().addAfter(mFactory.createStatementFromText("findView();\n", mClass), statement);
                    break;
                }
            }
        }

        //给findView方法添加findView代码
        PsiMethod findView = mClass.findMethodsByName("findView", false)[0];
        for (Element element : mElements) {
            //没有勾选
            if (!element.used) continue;

            findView.getBody().add(mFactory.createStatementFromText(
                    element.fieldName + " = (" +
                            ((element.nameFull != null && element.nameFull.length() > 0) ? element.nameFull : element.name)
                            + ") findViewById(R.id." + element.id + ");\n", mClass));
        }
    }

    private void filterElementHasFind(PsiMethod... methods) {
        for (PsiMethod method : methods) {
            if (null == method) return;

            for (PsiStatement statement : method.getBody().getStatements()) {
                if (statement.getFirstChild() instanceof PsiAssignmentExpression) {
                    PsiAssignmentExpression pai = (PsiAssignmentExpression) statement.getFirstChild();
                    //获取右边表达式
                    PsiElement rightExp = pai.getRExpression().getLastChild();
                    Element element = findElement(rightExp.getText());
                    if (null != element) element.used = false;
                }
            }
        }
    }

    private Element findElement(String text) {
        for (Element element : mElements) {
            if (text.contains(element.id))
                return element;
        }
        return null;
    }

    /**
     * Activity重写onCreate方法
     */
    private void addOnCreateMethod() {
        String method = "@Override protected void onCreate(android.os.Bundle savedInstanceState) {\n" +
                "super.onCreate(savedInstanceState);\n" +
                "\t// TODO: add setContentView(...) invocation\n" +
                "(this);\n" +
                "}";
        mClass.add(mFactory.createMethodFromText(method, mClass));
    }

    /**
     * Activity添加findView方法
     */
    private void addFindViewMethod() {
        String method = "private void findView() {\n" +
                "}";
        mClass.add(mFactory.createMethodFromText(method, mClass));
    }

    private void addOnViewCreatedMethod() {
        String method = "@Override public void onViewCreated(android.view.View view, @Nullable android.os.Bundle savedInstanceState) {\n" +
                "super.onViewCreated(view, savedInstanceState);\n" +
                "}";
        mClass.add(mFactory.createMethodFromText(method, mClass));
    }

    /**
     * 生成属性
     */
    private void generateFields() {
        for (Element element : mElements) {
            if (!element.used) {
                continue;
            }

            StringBuilder injection = new StringBuilder();
            if (element.nameFull != null && element.nameFull.length() > 0) { // custom package+class
                injection.append(element.nameFull);
            } else if (Definitions.paths.containsKey(element.name)) { // listed class
                injection.append(Definitions.paths.get(element.name));
            } else { // android.widget
                injection.append("android.widget.");
                injection.append(element.name);
            }
            injection.append(" ");
            injection.append(element.fieldName);
            injection.append(";");

            mClass.add(mFactory.createFieldFromText(injection.toString(), mClass));
        }
    }
}
