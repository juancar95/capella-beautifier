/*
 * Copyright (c) 2025-2026 Juan Carlos López Calvo.
 * Based on Eclipse Capella (EPL 2.0).
 */
package org.polarsys.capella.beautify.handlers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eclipse.gef.EditPart;
import org.eclipse.gmf.runtime.diagram.ui.editparts.DiagramEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editparts.GraphicalEditPart;
import org.eclipse.gmf.runtime.diagram.ui.parts.DiagramEditor;
import org.eclipse.gmf.runtime.diagram.ui.requests.ArrangeRequest;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gmf.runtime.notation.Diagram;
import org.eclipse.gmf.runtime.notation.Node;
import org.eclipse.gmf.runtime.notation.View;
import org.eclipse.sirius.diagram.DDiagram;
import org.eclipse.sirius.diagram.DDiagramElement;
import org.eclipse.sirius.diagram.description.CustomLayoutConfiguration;
import org.eclipse.sirius.diagram.description.DescriptionFactory;
import org.eclipse.sirius.diagram.description.DiagramDescription;
import org.eclipse.sirius.diagram.description.EnumLayoutOption;
import org.eclipse.sirius.diagram.description.EnumLayoutValue;
import org.eclipse.sirius.diagram.description.Layout;
import org.eclipse.sirius.diagram.description.LayoutOption;
import org.eclipse.sirius.diagram.description.LayoutOptionTarget;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import org.polarsys.capella.beautify.ElkConfigInjector;
import org.polarsys.capella.core.sirius.analysis.FunctionalChainServices;
import org.polarsys.capella.core.sirius.analysis.PhysicalServices;
import org.polarsys.capella.core.sirius.analysis.ShapeUtil;

public class BeautifyDiagramHandler extends AbstractHandler {

    private static final Logger LOGGER = Logger.getLogger("Diagrams Management");

    private static final String DIRECTION_PARAM = "org.polarsys.capella.core.sirius.analysis.beautifyDiagram.direction";
    private static final String ELK_DIRECTION_ID = "org.eclipse.elk.direction";
    private static final String DIRECTION_HORIZONTAL = "horizontal";
    private static final String ELK_RIGHT = "RIGHT";
    private static final String ELK_DOWN = "DOWN";
    private static final Set<String> PHYSICAL_PATH_SUPPORTING_DIAGRAMS = new HashSet<>(Arrays.asList(
            "Physical Architecture Blank", "System Architecture Blank",
            "Logical Architecture Blank", "Physical Path Description"
    ));

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        LOGGER.info("Beautify: handler execute() called");

        IEditorPart editor = HandlerUtil.getActiveEditor(event);
        if (!(editor instanceof DiagramEditor)) return null;

        DiagramEditor diagramEditor = (DiagramEditor) editor;
        DiagramEditPart dep = diagramEditor.getDiagramEditPart();
        if (dep == null) return null;

        String direction = event.getParameter(DIRECTION_PARAM);
        if (direction == null) direction = DIRECTION_HORIZONTAL;

        DDiagram dDiagram = getDDiagram(diagramEditor);
        if (dDiagram == null) return null;

        DiagramDescription desc = dDiagram.getDescription();
        if (desc == null) return null;

        String elkDir = DIRECTION_HORIZONTAL.equals(direction) ? ELK_RIGHT : ELK_DOWN;

        TransactionalEditingDomain domain = TransactionUtil.getEditingDomain(desc);
        if (domain == null) return null;

        // 1. Set ELK config (inside RecordingCommand for proper undo)
        CustomLayoutConfiguration elkConfig = ElkConfigInjector.createFullElkConfig();
        setDirectionOnConfig(elkConfig, elkDir);

        // Save original layout so we can restore it after arrange (config is transient, not user-visible)
        final Layout originalLayout = desc.getLayout();

        try {
            domain.getCommandStack().execute(new RecordingCommand(domain, "Beautify: set ELK") {
                @Override protected void doExecute() { desc.setLayout(elkConfig); }
            });
        } catch (Throwable t) {
            LOGGER.error("Beautify: failed to set ELK config", t);
            return null;
        }

        // 2. Arrange (creates its own undo entry via performRequest)
        ArrangeRequest req = new ArrangeRequest("arrangeAllAction");
        dep.performRequest(req);

        // 3. Post-processing inside a RecordingCommand (so Ctrl+Z undoes it consistently)
        final DiagramEditPart finalDep = dep;
        final DDiagram finalDDiagram = dDiagram;
        List<GraphicalEditPart> toRefresh = new ArrayList<>();
        try {
            domain.getCommandStack().execute(new RecordingCommand(domain, "Beautify: post-process") {
                @Override protected void doExecute() {
                    // Restore original layout config (the arrange already used the ELK config)
                    desc.setLayout(originalLayout);
                    // Refresh styles
                    refreshSpecificStyles(finalDDiagram);
                    // Normalize node sizes
                    collectNodeSizeChanges(finalDep, toRefresh);
                }
            });
        } catch (Throwable t) {
            LOGGER.error("Beautify: post-process failed", t);
        }

        // UI refresh (not an EMF change, doesn't need to be in transaction)
        for (GraphicalEditPart gep : toRefresh) gep.refresh();
        dep.refresh();
        LOGGER.info("Beautify: done (" + desc.getName() + ", " + elkDir + ")");

        return null;
    }

    private void setDirectionOnConfig(CustomLayoutConfiguration config, String direction) {
        for (LayoutOption opt : config.getLayoutOptions()) {
            if (opt instanceof EnumLayoutOption && ELK_DIRECTION_ID.equals(opt.getId())) {
                EnumLayoutOption enumOpt = (EnumLayoutOption) opt;
                EnumLayoutValue val = enumOpt.getValue();
                if (val != null) {
                    val.setName(direction);
                } else {
                    val = DescriptionFactory.eINSTANCE.createEnumLayoutValue();
                    val.setName(direction);
                    enumOpt.getChoices().clear();
                    enumOpt.getChoices().add(val);
                    enumOpt.setValue(val);
                }
                return;
            }
        }
    }

    private DDiagram getDDiagram(DiagramEditor editor) {
        Diagram d = editor.getDiagram();
        if (d != null && d.getElement() instanceof DDiagram) return (DDiagram) d.getElement();
        return null;
    }

    private void refreshSpecificStyles(DDiagram diagram) {
        try {
            FunctionalChainServices svc = FunctionalChainServices.getFunctionalChainServices();
            if (svc != null) svc.updateFunctionalChainStyles(diagram);
        } catch (Exception e) { /* FC styles not available */ }

        String n = diagram.getDescription() != null ? diagram.getDescription().getName() : null;
        if (n != null && PHYSICAL_PATH_SUPPORTING_DIAGRAMS.contains(n)) {
            try {
                PhysicalServices ps = PhysicalServices.getService();
                if (ps != null) {
                    ps.updateInternalPhysicalPaths(diagram);
                    ps.updatePhysicalPathStyles(diagram);
                }
            } catch (Exception e) { /* PP styles not available */ }
        }
    }

    private static final int MIN_NODE_WIDTH = 120;
    private static final int MIN_NODE_HEIGHT = 35;
    private static final int CHAR_WIDTH_ESTIMATE = 7;  // approx pixels per character
    private static final int LABEL_H_PADDING = 30;     // horizontal padding around label text

    /**
     * Applies node size corrections inside a transaction. Modified edit parts are added to toRefresh
     * for UI refresh after the transaction commits.
     */
    private void collectNodeSizeChanges(DiagramEditPart editPart, List<GraphicalEditPart> toRefresh) {
        Deque<EditPart> queue = new ArrayDeque<>();
        queue.add(editPart);
        while (!queue.isEmpty()) {
            EditPart current = queue.removeFirst();
            for (Object child : current.getChildren())
                if (child instanceof EditPart) queue.addLast((EditPart) child);
            if (!(current instanceof GraphicalEditPart)) continue;
            GraphicalEditPart gep = (GraphicalEditPart) current;
            Object a = gep.getAdapter(View.class);
            if (!(a instanceof Node)) continue;
            Node node = (Node) a;
            if (!isResizableNode(gep, node)) continue;
            try {
                int w = ShapeUtil.getNodeWidth(node);
                int h = ShapeUtil.getNodeHeight(node);

                int requiredWidth = MIN_NODE_WIDTH;
                EObject element = node.getElement();
                if (element instanceof DDiagramElement) {
                    String label = ((DDiagramElement) element).getName();
                    if (label != null && !label.isEmpty()) {
                        int textWidth = label.length() * CHAR_WIDTH_ESTIMATE + LABEL_H_PADDING;
                        requiredWidth = Math.max(requiredWidth, textWidth);
                    }
                }

                boolean changed = false;
                if (w < requiredWidth) { ShapeUtil.setNodeWidth(node, requiredWidth); changed = true; }
                if (h < MIN_NODE_HEIGHT) { ShapeUtil.setNodeHeight(node, MIN_NODE_HEIGHT); changed = true; }
                if (changed) toRefresh.add(gep);
            } catch (RuntimeException e) { /* skip */ }
        }
    }

    private boolean isResizableNode(GraphicalEditPart ep, Node node) {
        String cn = ep.getClass().getSimpleName();
        if (cn.contains("Compartment") || cn.contains("Label") || cn.contains("Name")) return false;
        String t = node.getType();
        return t == null || !t.toLowerCase().contains("compartment");
    }
}
