package versioncreator.popup.actions;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import versioncreator.popup.dialog.VersionCreatorDialog;

public class CreateVersionAction implements IObjectActionDelegate{

	private Shell shell;
	private StructuredSelection currentSelection;
	/**
	 * Constructor for Action1.
	 */
	public CreateVersionAction() {
		super();
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		if (!(currentSelection instanceof ITreeSelection)) {
			return;
		}
		TreePath[] paths = ((ITreeSelection) currentSelection).getPaths();
		IResource resource = null;
		for (int i = 0; i < paths.length; i++) {
			TreePath path = paths[i];
			Object segment = path.getLastSegment();
			//project Explorer
			if ((segment instanceof IResource)) {
				resource = (IResource) segment;
			}
			//Package Explorer
			else if ((segment instanceof IJavaElement)) {
				resource = ((IJavaElement) segment).getResource();
			}
		}
		//将右键的项目resource传过去以重新编译项目文件
		new VersionCreatorDialog(shell, resource).open();
	}
	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		this.currentSelection=(StructuredSelection)selection;
	}
}
