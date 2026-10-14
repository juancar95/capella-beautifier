import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.eclipse.osgi.launch.EquinoxFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;

/** Dependency/API smoke check in a fresh framework; does not open or modify models. */
public final class BundleCompatibilityCheck {
    private static final String BEAUTIFIER = "org.polarsys.capella.core.sirius.analysis.beautify";

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            throw new IllegalArgumentException(
                "Usage: BundleCompatibilityCheck <Capella plugins> <extra plugins> <plugin.jar> <storage> <resolved|unresolved>");
        }
        boolean expectedResolution = switch (args[4]) {
            case "resolved" -> true;
            case "unresolved" -> false;
            default -> throw new IllegalArgumentException("Expected resolved or unresolved");
        };
        Map<String, String> config = new HashMap<>();
        config.put(Constants.FRAMEWORK_STORAGE, args[3]);
        config.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
        Framework framework = new EquinoxFactory().newFramework(config);
        framework.start();
        try {
            List<Bundle> extras = new ArrayList<>();
            installDirectory(framework, Path.of(args[1]), extras);
            installDirectory(framework, Path.of(args[0]), new ArrayList<>());
            FrameworkWiring wiring = framework.adapt(FrameworkWiring.class);
            Bundle capella = null;
            for (Bundle bundle : framework.getBundleContext().getBundles()) {
                if ("org.polarsys.capella.core.sirius.analysis".equals(bundle.getSymbolicName())) capella = bundle;
            }
            if (capella == null || !wiring.resolveBundles(List.of(capella))) {
                throw new AssertionError("The baseline Capella analysis bundle must resolve before checking the plugin");
            }
            System.out.println("Capella analysis " + capella.getVersion() + ": RESOLVED");
            Bundle plugin = framework.getBundleContext().installBundle(Path.of(args[2]).toUri().toString());
            boolean resolved = wiring.resolveBundles(List.of(plugin));
            System.out.println("Beautifier " + plugin.getVersion() + ": " + (resolved ? "RESOLVED" : "UNRESOLVED"));
            if (resolved != expectedResolution) {
                printRequirements(plugin);
                try {
                    plugin.start();
                } catch (BundleException error) {
                    System.out.println(error.getMessage());
                }
                throw new AssertionError("Unexpected plugin resolution result");
            }
            if (!resolved) {
                printRequirements(plugin);
                return;
            }
            for (Bundle extra : extras) {
                if (!wiring.resolveBundles(List.of(extra))) {
                    printRequirements(extra);
                    throw new AssertionError("Unresolved full-package dependency: " + extra.getSymbolicName());
                }
                System.out.println("Full-package dependency: " + extra.getSymbolicName() + " " + extra.getVersion() + " RESOLVED");
            }
            Class<?> injector = plugin.loadClass("org.polarsys.capella.beautify.ElkConfigInjector");
            Object configuration = injector.getMethod("createFullElkConfig").invoke(null);
            Class<?> configurationApi = plugin.loadClass("org.eclipse.sirius.diagram.description.CustomLayoutConfiguration");
            Object algorithm = configurationApi.getMethod("getId").invoke(configuration);
            if (!"org.eclipse.elk.layered".equals(algorithm)) {
                throw new AssertionError("Unexpected layout algorithm: " + algorithm);
            }
            System.out.println("ELK configuration factory: PASS (" + algorithm + ")");
            Class<?> handler = plugin.loadClass("org.polarsys.capella.beautify.handlers.BeautifyDiagramHandler");
            handler.getDeclaredMethods();
            handler.getConstructor().newInstance();
            System.out.println("Beautify handler linkage and construction: PASS");
        } finally {
            framework.stop();
            framework.waitForStop(10000);
        }
    }

    private static void installDirectory(Framework framework, Path directory, List<Bundle> installed) throws Exception {
        try (var paths = Files.list(directory)) {
            for (Path path : paths.sorted().toList()) {
                String symbolicName;
                String version;
                if (Files.isDirectory(path)) {
                    Path manifest = path.resolve("META-INF/MANIFEST.MF");
                    if (!Files.isRegularFile(manifest)) continue;
                    try (var input = Files.newInputStream(manifest)) {
                        var headers = new Manifest(input).getMainAttributes();
                        symbolicName = headers.getValue("Bundle-SymbolicName");
                        version = headers.getValue("Bundle-Version");
                    }
                } else if (path.getFileName().toString().endsWith(".jar")) {
                    try (JarFile jar = new JarFile(path.toFile())) {
                        if (jar.getManifest() == null) continue;
                        symbolicName = jar.getManifest().getMainAttributes().getValue("Bundle-SymbolicName");
                        version = jar.getManifest().getMainAttributes().getValue("Bundle-Version");
                    }
                } else {
                    continue;
                }
                if (symbolicName == null || symbolicName.startsWith("org.eclipse.osgi;")
                        || symbolicName.equals("org.eclipse.osgi") || symbolicName.startsWith(BEAUTIFIER)
                        || symbolicName.contains(".source")) continue;
                String bundleName = symbolicName.split(";")[0];
                boolean alreadyInstalled = false;
                for (Bundle existing : framework.getBundleContext().getBundles()) {
                    if (bundleName.equals(existing.getSymbolicName()) && existing.getVersion().toString().equals(version)) {
                        alreadyInstalled = true;
                        break;
                    }
                }
                if (alreadyInstalled) continue;
                installed.add(framework.getBundleContext().installBundle(path.toUri().toString()));
            }
        }
    }

    private static void printRequirements(Bundle bundle) {
        System.out.println("Requirements for " + bundle.getSymbolicName() + ":");
        bundle.adapt(BundleRevision.class).getDeclaredRequirements(null)
            .forEach(requirement -> System.out.println("  " + requirement));
    }
}
