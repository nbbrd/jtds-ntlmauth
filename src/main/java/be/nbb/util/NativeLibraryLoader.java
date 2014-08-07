/*
 * Copyright 2013 National Bank of Belgium
 * 
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved 
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and 
 * limitations under the Licence.
 */
package be.nbb.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Load an os-dependent library from a jar file.
 *
 * @author Philippe Charles
 */
public final class NativeLibraryLoader {

    public static NativeLibraryLoader fromSystemProperties() {
        return new NativeLibraryLoader(
                System.getProperty("os.name"),
                System.getProperty("os.arch"),
                System.getProperty("os.version"),
                System.getProperty("java.io.tmpdir"));
    }

    private final String osName;
    private final String osArch;
    private final String osVersion;
    private final String tmpDir;
    private final List<Route> paths;
    private Class<?> resourceClass;
    private String prefix;

    // @VisibleForTesting
    NativeLibraryLoader(String osName, String osArch, String osVersion, String tmpDir) {
        this.osName = osName;
        this.osArch = osArch;
        this.osVersion = osVersion;
        this.tmpDir = tmpDir;
        this.paths = new ArrayList<>();
        this.resourceClass = NativeLibraryLoader.class;
        this.prefix = generatePrefix(resourceClass.getPackage());
    }

    public NativeLibraryLoader route(String osNamePattern, String osArchPattern, String osVersionPattern, String path) {
        return route(Pattern.compile(osNamePattern), Pattern.compile(osArchPattern), Pattern.compile(osVersionPattern), path);
    }

    public NativeLibraryLoader route(Pattern osNamePattern, Pattern osArchPattern, Pattern osVersionPattern, String path) {
        paths.add(new Route(osNamePattern, osArchPattern, osVersionPattern, path));
        return this;
    }

    public NativeLibraryLoader resourceClass(Class<?> resourceClass) {
        this.resourceClass = Objects.requireNonNull(resourceClass);
        return this;
    }

    public NativeLibraryLoader prefix(String prefix) {
        this.prefix = Objects.requireNonNull(prefix);
        return this;
    }

    public void load(String libName) throws SecurityException, UnsatisfiedLinkError, NullPointerException {
        System.load(getFile(libName).toString());
    }

    private Path getFile(String libName) throws SecurityException, UnsatisfiedLinkError, NullPointerException {
        Objects.requireNonNull(libName);

        String libDir = getLibPathForCurrentOS();
        if (libDir == null) {
            throw new UnsatisfiedLinkError("Cannot retrieve library path for '" + osName + "' and '" + osArch + "' and '" + osVersion + "'");
        }

        String libFileName = System.mapLibraryName(libName);
        String libFile = libDir + "/" + libFileName;

        try (InputStream stream = resourceClass.getResourceAsStream(libFile)) {
            if (stream == null) {
                throw new UnsatisfiedLinkError(libFile);
            }
            Path result = Paths.get(tmpDir, prefix + libFileName);
            writeIfDifferent(toBytes(stream), result);
            return result;
        } catch (IOException ex) {
            throw new UnsatisfiedLinkError("IOException with '" + libFile + "': " + ex.getMessage());
        }
    }

    private String getLibPathForCurrentOS() {
        for (Route o : paths) {
            if (o.osNamePattern.matcher(osName).matches()
                    && o.osArchPattern.matcher(osArch).matches()
                    && o.osVersionPattern.matcher(osVersion).matches()) {
                return o.path;
            }
        }
        return null;
    }

    private static byte[] toBytes(InputStream is) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024 * 8];
            int len;
            while ((len = is.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            return baos.toByteArray();
        }
    }

    private static void writeIfDifferent(byte[] source, Path target) throws IOException {
        if (!Files.exists(target) || source.length != Files.size(target)) {
            Files.write(target, source);
        } else {
            if (!Arrays.equals(source, Files.readAllBytes(target))) {
                Files.write(target, source);
            }
        }
    }

    private static String generatePrefix(Package p) {
        return p.getImplementationTitle() + "-" + p.getImplementationVersion() + "-";
    }

    private static final class Route {

        final Pattern osNamePattern;
        final Pattern osArchPattern;
        final Pattern osVersionPattern;
        final String path;

        public Route(Pattern osNamePattern, Pattern osArchPattern, Pattern osVersionPattern, String path) {
            this.osNamePattern = osNamePattern;
            this.osArchPattern = osArchPattern;
            this.osVersionPattern = osVersionPattern;
            this.path = path;
        }
    }
}
