import com.google.inject.Guice;
import com.google.inject.Inject;
import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.Direction;
import org.eclipse.elk.core.options.EdgeRouting;
import org.eclipse.elk.core.options.HierarchyHandling;
import org.eclipse.elk.core.util.BasicProgressMonitor;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;
import org.eclipse.elk.graph.util.ElkGraphUtil;

/** Executes the actual packaged ELK engine and Guice/Guava integration, without SWT. */
public final class ElkLayoutCheck {
    public static final class Dependency {}
    public static final class Consumer {
        final Dependency dependency;
        @Inject public Consumer(Dependency dependency) { this.dependency = dependency; }
    }

    public static void main(String[] args) {
        if (Guice.createInjector().getInstance(Consumer.class).dependency == null) {
            throw new AssertionError("Guice injection failed");
        }
        System.out.println("Guice/Guava runtime injection: PASS");
        for (Direction direction : new Direction[] { Direction.RIGHT, Direction.DOWN }) {
            for (boolean nested : new boolean[] { false, true }) {
                for (boolean ports : new boolean[] { false, true }) {
                    ElkNode graph = ElkGraphUtil.createGraph();
                    graph.setProperty(CoreOptions.ALGORITHM, "org.eclipse.elk.layered");
                    graph.setProperty(CoreOptions.DIRECTION, direction);
                    graph.setProperty(CoreOptions.EDGE_ROUTING, EdgeRouting.ORTHOGONAL);
                    graph.setProperty(CoreOptions.HIERARCHY_HANDLING, HierarchyHandling.INCLUDE_CHILDREN);
                    ElkNode parent = nested ? ElkGraphUtil.createNode(graph) : graph;
                    ElkNode first = node(parent);
                    ElkNode second = node(parent);
                    ElkNode third = node(parent);
                    ElkEdge a = edge(first, second, ports);
                    ElkEdge b = edge(second, third, ports);
                    new RecursiveGraphLayoutEngine().layout(graph, new BasicProgressMonitor());
                    checkNode(graph);
                    checkNode(first);
                    checkNode(second);
                    checkNode(third);
                    boolean ordered = direction == Direction.RIGHT
                        ? first.getX() + first.getWidth() <= second.getX()
                            && second.getX() + second.getWidth() <= third.getX()
                        : first.getY() + first.getHeight() <= second.getY()
                            && second.getY() + second.getHeight() <= third.getY();
                    if (!ordered || a.getSections().isEmpty() || b.getSections().isEmpty()) {
                        throw new AssertionError("Layout did not arrange and route the connected graph");
                    }
                    for (ElkEdge e : new ElkEdge[] { a, b }) {
                        e.getSections().forEach(section -> {
                            double x = section.getStartX(), y = section.getStartY();
                            for (var bend : section.getBendPoints()) {
                                checkSegment(x, y, bend.getX(), bend.getY());
                                x = bend.getX(); y = bend.getY();
                            }
                            checkSegment(x, y, section.getEndX(), section.getEndY());
                        });
                    }
                    System.out.println("ELK " + direction + " nested=" + nested + " ports=" + ports + ": PASS");
                }
            }
        }
    }

    private static ElkNode node(ElkNode parent) {
        ElkNode node = ElkGraphUtil.createNode(parent);
        node.setDimensions(120, 60);
        return node;
    }

    private static ElkEdge edge(ElkNode source, ElkNode target, boolean ports) {
        if (!ports) return ElkGraphUtil.createSimpleEdge(source, target);
        ElkPort out = ElkGraphUtil.createPort(source);
        ElkPort in = ElkGraphUtil.createPort(target);
        out.setDimensions(8, 8); in.setDimensions(8, 8);
        return ElkGraphUtil.createSimpleEdge(out, in);
    }

    private static void checkNode(ElkNode node) {
        if (!Double.isFinite(node.getX()) || !Double.isFinite(node.getY())
                || !Double.isFinite(node.getWidth()) || !Double.isFinite(node.getHeight())
                || node.getWidth() <= 0 || node.getHeight() <= 0) {
            throw new AssertionError("Invalid node geometry");
        }
    }

    private static void checkSegment(double x, double y, double endX, double endY) {
        if (!Double.isFinite(endX) || !Double.isFinite(endY)
                || Math.abs(x - endX) > 0.001 && Math.abs(y - endY) > 0.001) {
            throw new AssertionError("Non-orthogonal or non-finite edge segment");
        }
    }
}
