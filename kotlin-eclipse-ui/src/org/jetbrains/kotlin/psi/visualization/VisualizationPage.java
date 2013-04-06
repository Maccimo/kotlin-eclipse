package org.jetbrains.kotlin.psi.visualization;

import java.io.File;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jetbrains.kotlin.parser.KotlinParser;
import org.jetbrains.kotlin.utils.LineEndUtil;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;

public final class VisualizationPage extends Dialog {

    private final Point pageSize = new Point(750, 650);
    private final String sourceCode;
    private final File file;
    
    private final String title = "Psi Viewer";
    
    public VisualizationPage(Shell parentShell, String sourceCode, File file) {
        super(parentShell);
        
        if (sourceCode == null || file == null || !file.exists()) {
            throw new IllegalArgumentException();
        }
        
        this.sourceCode = StringUtil.convertLineSeparators(sourceCode);
        this.file = file;
    }
    
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        
        createControls(composite);
        
        return composite;
    }
    
    private void createControls(Composite composite) {
        setDescriptionLabel(composite);
        Text programText = setTextProgram(composite);
        setTreeViewer(composite, programText);
    }
    
    private void setTreeViewer(Composite composite, final Text programText) {
        TreeViewer psiTreeViewer = new TreeViewer(composite);
        psiTreeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
        psiTreeViewer.setContentProvider(new PsiContentProvider());
        psiTreeViewer.setLabelProvider(new LabelProvider());
        
        KotlinParser parser = new KotlinParser(file);
        psiTreeViewer.setInput(parser.parse());
        
        psiTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
            
            @Override
            public void doubleClick(DoubleClickEvent event) {
                IStructuredSelection thisSelection = (IStructuredSelection) event.getSelection();
                ASTNode selectedNode = (ASTNode) thisSelection.getFirstElement();
                TextRange selectedNodeRange = selectedNode.getTextRange();
                int start = LineEndUtil.convertLfToCrLfOffset(sourceCode, selectedNodeRange.getStartOffset());
                int end = LineEndUtil.convertLfToCrLfOffset(sourceCode, selectedNodeRange.getEndOffset());
                
                programText.setSelection(start, end);
                programText.showSelection();
            }
        });
    }
    
    private void setDescriptionLabel(Composite composite) {
        Label descriptionLabel = new Label(composite, SWT.LEFT | SWT.FILL);
        descriptionLabel.setText("Shows PSI structure for Kotlin file: " + file.getName());
        
        Label separator = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
        GridData sgd = new GridData(GridData.FILL_HORIZONTAL);
        separator.setLayoutData(sgd);
        
    }
    
    private Text setTextProgram(Composite composite) {
        final Text programText = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        programText.setEditable(false);
        programText.setText(sourceCode);
        GridData pgd = new GridData(GridData.FILL_HORIZONTAL);
        pgd.heightHint = pageSize.y / 3;
        programText.setLayoutData(pgd);
        
        return programText;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        Button closeButton = createButton(parent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL, false);
        closeButton.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                setReturnCode(OK);
                close();
            }
        });
    }
    
    @Override
    protected Point getInitialSize() {
        return pageSize;
    }
    
    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(title);
    }
} 