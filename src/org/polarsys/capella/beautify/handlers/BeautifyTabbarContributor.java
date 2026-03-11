package org.polarsys.capella.beautify.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.gmf.runtime.diagram.ui.parts.IDiagramWorkbenchPart;
import org.eclipse.sirius.diagram.ui.tools.api.editor.tabbar.ITabbarContributor;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.PlatformUI;

import java.util.HashMap;
import java.util.Map;

public class BeautifyTabbarContributor implements ITabbarContributor {

    private static final String COMMAND_ID = "org.polarsys.capella.core.sirius.analysis.beautifyDiagram";
    private static final String DIRECTION_PARAM = "org.polarsys.capella.core.sirius.analysis.beautifyDiagram.direction";

    private List<IContributionItem> items;

    @Override
    public boolean accept(ISelection selection) {
        return true;
    }

    @Override
    public List<IContributionItem> getContributionItems(ISelection selection, IDiagramWorkbenchPart workbenchPart, ToolBarManager manager) {
        return getItems(workbenchPart);
    }

    @Override
    public List<IContributionItem> getContributionItems(IDiagramWorkbenchPart workbenchPart, ToolBarManager manager) {
        return getItems(workbenchPart);
    }

    private List<IContributionItem> getItems(IDiagramWorkbenchPart workbenchPart) {
        if (items == null) {
            items = new ArrayList<>();

            // Horizontal button
            Map<String, String> paramsH = new HashMap<>();
            paramsH.put(DIRECTION_PARAM, "horizontal");

            CommandContributionItemParameter paramH = new CommandContributionItemParameter(
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow(),
                    "org.polarsys.capella.beautify.horizontal",
                    COMMAND_ID,
                    paramsH,
                    null, null, null,
                    "Beautify H",
                    null,
                    "Arrange horizontal (left to right)",
                    CommandContributionItem.STYLE_PUSH,
                    null, false);
            items.add(new CommandContributionItem(paramH));

            // Vertical button
            Map<String, String> paramsV = new HashMap<>();
            paramsV.put(DIRECTION_PARAM, "vertical");

            CommandContributionItemParameter paramV = new CommandContributionItemParameter(
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow(),
                    "org.polarsys.capella.beautify.vertical",
                    COMMAND_ID,
                    paramsV,
                    null, null, null,
                    "Beautify V",
                    null,
                    "Arrange vertical (top to bottom)",
                    CommandContributionItem.STYLE_PUSH,
                    null, false);
            items.add(new CommandContributionItem(paramV));
        }
        return items;
    }
}
