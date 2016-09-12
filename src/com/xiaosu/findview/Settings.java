package com.xiaosu.findview;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.xiaosu.findview.common.Utils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Settings UI for the plugin.
 *
 * @author David Vávra (vavra@avast.com)
 */
public class Settings implements Configurable {

    public static final String PREFIX = "butterknifezelezny_prefix";
    public static final String VIEWHOLDER_CLASS_NAME = "butterknifezelezny_viewholder_class_name";

    private JPanel mPanel;
    private JTextField mHolderName;
    private JTextField mPrefix;
    private JTextField mFindCode;

    @Nls
    @Override
    public String getDisplayName() {
        return "findViewByMe";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        reset();
        return mPanel;
    }

    @Override
    public boolean isModified() {
        return true;
    }

    @Override
    public void apply() throws ConfigurationException {
        PropertiesComponent.getInstance().setValue(PREFIX, mPrefix.getText());
        PropertiesComponent.getInstance().setValue(VIEWHOLDER_CLASS_NAME, mHolderName.getText());
    }

    @Override
    public void reset() {
        mPrefix.setText(Utils.getPrefix());
        mHolderName.setText(Utils.getViewHolderClassName());
    }

    @Override
    public void disposeUIResources() {

    }
}
