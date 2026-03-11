/*
 * Copyright (c) 2025-2026 Juan Carlos López Calvo.
 * Based on Eclipse Capella (EPL 2.0).
 */
package org.polarsys.capella.beautify;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.sirius.business.api.session.Session;
import org.eclipse.sirius.business.api.session.SessionListener;
import org.eclipse.sirius.business.api.session.SessionManagerListener;
import org.eclipse.sirius.diagram.description.CustomLayoutConfiguration;
import org.eclipse.sirius.diagram.description.DescriptionFactory;
import org.eclipse.sirius.diagram.description.DiagramDescription;
import org.eclipse.sirius.diagram.description.EnumLayoutOption;
import org.eclipse.sirius.diagram.description.EnumLayoutValue;
import org.eclipse.sirius.diagram.description.EnumSetLayoutOption;
import org.eclipse.sirius.diagram.description.BooleanLayoutOption;
import org.eclipse.sirius.diagram.description.DoubleLayoutOption;
import org.eclipse.sirius.diagram.description.Layout;
import org.eclipse.sirius.diagram.description.LayoutOptionTarget;
import org.eclipse.sirius.viewpoint.description.RepresentationDescription;
import org.eclipse.sirius.viewpoint.description.Viewpoint;

/**
 * Injects ELK Layered layout configuration into all DiagramDescriptions
 * when a Sirius session opens. This replicates the effect of CapellaLayoutPatch
 * (.odesign modification) but without replacing any JARs.
 *
 * Registered via extension point org.eclipse.sirius.sessionManagerListener.
 */
public class ElkConfigInjector extends SessionManagerListener.Stub {

    private static final Logger LOGGER = Logger.getLogger("Diagrams Management");

    private static final String ELK_ALGORITHM_ID = "org.eclipse.elk.layered";
    private static final String BEAUTIFY_LABEL = "ELK Layered (Beautify)";

    @Override
    public void notify(Session session, int changeKind) {
        if (changeKind == SessionListener.OPENED) {
            injectElkConfig(session);
        }
    }

    private void injectElkConfig(Session session) {
        TransactionalEditingDomain domain = session.getTransactionalEditingDomain();
        if (domain == null) return;

        Collection<Viewpoint> viewpoints = session.getSelectedViewpoints(false);
        int count = 0;

        for (Viewpoint vp : viewpoints) {
            for (RepresentationDescription rd : vp.getOwnedRepresentations()) {
                if (!(rd instanceof DiagramDescription)) continue;
                DiagramDescription dd = (DiagramDescription) rd;

                // Skip if already has our ELK config
                Layout existing = dd.getLayout();
                if (existing instanceof CustomLayoutConfiguration) {
                    CustomLayoutConfiguration clc = (CustomLayoutConfiguration) existing;
                    if (BEAUTIFY_LABEL.equals(clc.getLabel())) continue;
                }

                CustomLayoutConfiguration elkConfig = createFullElkConfig();
                try {
                    domain.getCommandStack().execute(new RecordingCommand(domain, "Beautify: inject ELK") {
                        @Override
                        protected void doExecute() {
                            dd.setLayout(elkConfig);
                        }
                    });
                    count++;
                } catch (Exception e) {
                    LOGGER.error("Beautify: failed to inject ELK config on " + dd.getName(), e);
                }
            }
        }

        if (count > 0) {
            LOGGER.info("Beautify: injected ELK Layered config into " + count + " diagram descriptions");
        }
    }

    /**
     * Creates a full ELK Layered configuration matching CapellaLayoutPatch options.
     * Default direction is RIGHT (horizontal). BeautifyDiagramHandler changes it per click.
     */
    public static CustomLayoutConfiguration createFullElkConfig() {
        CustomLayoutConfiguration c = DescriptionFactory.eINSTANCE.createCustomLayoutConfiguration();
        c.setId(ELK_ALGORITHM_ID);
        c.setLabel(BEAUTIFY_LABEL);

        // Direction: RIGHT (default horizontal, handler overrides per click)
        addEnumOption(c, "org.eclipse.elk.direction", "RIGHT", LayoutOptionTarget.PARENT);

        // Edge routing: orthogonal (right-angle bends, not diagonal)
        addEnumOption(c, "org.eclipse.elk.edgeRouting", "ORTHOGONAL", LayoutOptionTarget.PARENT);

        // Layering strategy: network simplex (minimizes total edge length)
        addEnumOption(c, "org.eclipse.elk.layered.layering.strategy", "NETWORK_SIMPLEX", LayoutOptionTarget.PARENT);

        // Node placement: Brandes-Koepf (compact, good for Capella)
        addEnumOption(c, "org.eclipse.elk.layered.nodePlacement.strategy", "BRANDES_KOEPF", LayoutOptionTarget.PARENT);

        // Hierarchy handling: layout inside containers too
        addEnumOption(c, "org.eclipse.elk.hierarchyHandling", "INCLUDE_CHILDREN",
                LayoutOptionTarget.NODE, LayoutOptionTarget.PARENT);

        // Crossing minimization: thorough
        addEnumOption(c, "org.eclipse.elk.layered.crossingMinimization.strategy", "LAYER_SWEEP", LayoutOptionTarget.PARENT);
        addBooleanOption(c, "org.eclipse.elk.layered.crossingMinimization.semiInteractive", true, LayoutOptionTarget.PARENT);

        // Feedback edges: true (handles cycles gracefully)
        addBooleanOption(c, "org.eclipse.elk.layered.feedbackEdges", true, LayoutOptionTarget.PARENT);

        // Node size: ensure nodes are big enough for their labels
        addEnumSetOption(c, "org.eclipse.elk.nodeSize.constraints",
                new String[]{"NODE_LABELS", "MINIMUM_SIZE"}, LayoutOptionTarget.NODE);

        // Label placement inside nodes
        addEnumSetOption(c, "org.eclipse.elk.nodeLabels.placement",
                new String[]{"INSIDE", "H_CENTER", "V_CENTER"}, LayoutOptionTarget.NODE);

        // Spacing
        addDoubleOption(c, "org.eclipse.elk.spacing.nodeNode", 8.0, LayoutOptionTarget.PARENT);
        addDoubleOption(c, "org.eclipse.elk.layered.spacing.nodeNodeBetweenLayers", 12.0, LayoutOptionTarget.PARENT);
        addDoubleOption(c, "org.eclipse.elk.spacing.edgeNode", 5.0, LayoutOptionTarget.PARENT);
        addDoubleOption(c, "org.eclipse.elk.spacing.edgeEdge", 5.0, LayoutOptionTarget.PARENT);

        // Container padding
        addDoubleOption(c, "org.eclipse.elk.padding.top", 3.0, LayoutOptionTarget.PARENT);
        addDoubleOption(c, "org.eclipse.elk.padding.bottom", 3.0, LayoutOptionTarget.PARENT);
        addDoubleOption(c, "org.eclipse.elk.padding.left", 3.0, LayoutOptionTarget.PARENT);
        addDoubleOption(c, "org.eclipse.elk.padding.right", 3.0, LayoutOptionTarget.PARENT);

        return c;
    }

    private static void addEnumOption(CustomLayoutConfiguration config, String id, String value, LayoutOptionTarget... targets) {
        EnumLayoutOption opt = DescriptionFactory.eINSTANCE.createEnumLayoutOption();
        opt.setId(id);
        EnumLayoutValue val = DescriptionFactory.eINSTANCE.createEnumLayoutValue();
        val.setName(value);
        opt.getChoices().add(val);
        opt.setValue(val);
        for (LayoutOptionTarget t : targets) opt.getTargets().add(t);
        config.getLayoutOptions().add(opt);
    }

    private static void addEnumSetOption(CustomLayoutConfiguration config, String id, String[] values, LayoutOptionTarget... targets) {
        EnumSetLayoutOption opt = DescriptionFactory.eINSTANCE.createEnumSetLayoutOption();
        opt.setId(id);
        for (String v : values) {
            EnumLayoutValue val = DescriptionFactory.eINSTANCE.createEnumLayoutValue();
            val.setName(v);
            opt.getValues().add(val);
        }
        for (LayoutOptionTarget t : targets) opt.getTargets().add(t);
        config.getLayoutOptions().add(opt);
    }

    private static void addBooleanOption(CustomLayoutConfiguration config, String id, boolean value, LayoutOptionTarget... targets) {
        BooleanLayoutOption opt = DescriptionFactory.eINSTANCE.createBooleanLayoutOption();
        opt.setId(id);
        opt.setValue(value);
        for (LayoutOptionTarget t : targets) opt.getTargets().add(t);
        config.getLayoutOptions().add(opt);
    }

    private static void addDoubleOption(CustomLayoutConfiguration config, String id, double value, LayoutOptionTarget... targets) {
        DoubleLayoutOption opt = DescriptionFactory.eINSTANCE.createDoubleLayoutOption();
        opt.setId(id);
        opt.setValue(value);
        for (LayoutOptionTarget t : targets) opt.getTargets().add(t);
        config.getLayoutOptions().add(opt);
    }
}
