package org.jetbrains.kotlin.ui.builder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.utils.KotlinEnvironment;
import org.jetbrains.kotlin.ui.editors.AnalyzerScheduler;
import org.jetbrains.kotlin.ui.editors.AnnotationManager;
import org.jetbrains.kotlin.ui.editors.DiagnosticAnnotation;
import org.jetbrains.kotlin.ui.editors.DiagnosticAnnotationUtil;

public class KotlinBuilder extends IncrementalProjectBuilder {

    @Override
    protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
        KotlinEnvironment.updateKotlinEnvironment(JavaCore.create(getProject()));
        updateLineMarkers(getProject());
        return null;
    }
    
    public static void updateLineMarkers(@NotNull IProject project) {
        IJavaProject javaProject = JavaCore.create(project);
        List<Diagnostic> diagnostics = analyzeProjectInForeground(javaProject);
        
        try {
            updateLineMarkers(diagnostics, project);
        } catch (CoreException e) {
            KotlinLogger.logError(e);
        }
    }
    
    private static void updateLineMarkers(List<Diagnostic> diagnostics, IProject project) throws CoreException {
        Map<IFile, List<DiagnosticAnnotation>> annotations = DiagnosticAnnotationUtil.INSTANCE.handleDiagnostics(diagnostics);
        
        addMarkersToProject(annotations, project);
    }
    
    @NotNull
    public static List<Diagnostic> analyzeProjectInForeground(IJavaProject javaProject) {
        AnalyzerScheduler analyzer = new AnalyzerScheduler(javaProject);
        analyzer.cancel();
        analyzer.schedule();
        
        IJobManager jobManager = Job.getJobManager();
        try {
            jobManager.join(AnalyzerScheduler.FAMILY, null);
        } catch (OperationCanceledException | InterruptedException e) {
            KotlinLogger.logInfo(e.getMessage());
            
            return Collections.emptyList();
        }
        
        return analyzer.getDiagnostics();
    }
    
    private static void addMarkersToProject(Map<IFile, List<DiagnosticAnnotation>> annotations, IProject project) throws CoreException {
        for (IFile file : KotlinPsiManager.INSTANCE.getFilesByProject(project)) {
            if (file.exists()) {
                file.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
            }
        }
        
        for (Map.Entry<IFile, List<DiagnosticAnnotation>> entry : annotations.entrySet()) {
            for (DiagnosticAnnotation annotation : entry.getValue()) {
                AnnotationManager.addProblemMarker(annotation, entry.getKey());
            }
        }
    }
}
