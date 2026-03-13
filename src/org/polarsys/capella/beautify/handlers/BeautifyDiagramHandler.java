/*
 * Copyright (c) 2025-2026 Juan Carlos López Calvo.
 * Plugin for Eclipse Capella.
 */
package org.polarsys.capella.beautify.handlers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eclipse.gmf.runtime.diagram.ui.editparts.DiagramEditPart;
import org.eclipse.gmf.runtime.diagram.ui.parts.DiagramEditor;
import org.eclipse.gmf.runtime.diagram.ui.requests.ArrangeRequest;
import org.eclipse.gmf.runtime.notation.Diagram;
import org.eclipse.sirius.diagram.DDiagram;
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

        // 1. Set ELK config (with INCLUDE_CHILDREN for hierarchical layout)
        CustomLayoutConfiguration elkConfig = ElkConfigInjector.createFullElkConfig();
        setDirectionOnConfig(elkConfig, elkDir);

        // Save original layout so we can restore it after arrange
        final Layout originalLayout = desc.getLayout();

        try {
            domain.getCommandStack().execute(new RecordingCommand(domain, "Beautify: set ELK") {
                @Override protected void doExecute() { desc.setLayout(elkConfig); }
            });
        } catch (Throwable t) {
            LOGGER.error("Beautify: failed to set ELK config", t);
            return null;
        }

        // 2. Arrange
        ArrangeRequest req = new ArrangeRequest("arrangeAllAction");
        dep.performRequest(req);

        // 3. Post-processing inside a RecordingCommand (so Ctrl+Z undoes it consistently)
        final DDiagram finalDDiagram = dDiagram;
        try {
            domain.getCommandStack().execute(new RecordingCommand(domain, "Beautify: post-process") {
                @Override protected void doExecute() {
                    desc.setLayout(originalLayout);
                    refreshSpecificStyles(finalDDiagram);
                }
            });
        } catch (Throwable t) {
            LOGGER.error("Beautify: post-process failed", t);
        }

        // UI refresh
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
}
