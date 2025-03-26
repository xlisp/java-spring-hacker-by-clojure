import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.core.runtime.NullProgressMonitor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FunctionCallAnalyzer {
    // Store function calls: caller -> list of callees
    private static final Map<String, List<String>> functionCalls = new HashMap<>();

    // Parse method declarations and their function calls
    private static class MethodVisitor extends ASTVisitor {
        private String currentMethod;

        @Override
        public boolean visit(MethodDeclaration node) {
            currentMethod = node.getName().getIdentifier();
            return true;
        }

        @Override
        public boolean visit(MethodInvocation node) {
            if (currentMethod != null) {
                String calledMethod = node.getName().getIdentifier();
                functionCalls.computeIfAbsent(currentMethod, k -> new ArrayList<>())
                           .add(calledMethod);
            }
            return true;
        }
    }

    // Generate the Graphviz DOT format output
    private static void generateDot() {
        System.out.println("digraph G {");
        for (Map.Entry<String, List<String>> entry : functionCalls.entrySet()) {
            String caller = entry.getKey();
            for (String callee : entry.getValue()) {
                System.out.printf("    \"%s\" -> \"%s\";\n", caller, callee);
            }
        }
        System.out.println("}");
    }

    // Parse Java files in the given directory
    private static void parseJavaFilesInDir(String dirPath) throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(dirPath))) {
            List<Path> javaFiles = paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .collect(Collectors.toList());

            for (Path javaFile : javaFiles) {
                parseJavaFile(javaFile.toFile());
            }
        }
    }

    // Parse a single Java file
    private static void parseJavaFile(File file) throws IOException {
        String source = new String(Files.readAllBytes(file.toPath()));
        
        ASTParser parser = ASTParser.newParser(AST.JLS16);
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        
        // Set compiler options
        Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_16, options);
        parser.setCompilerOptions(options);

        CompilationUnit cu = (CompilationUnit) parser.createAST(new NullProgressMonitor());
        
        // Visit the AST
        MethodVisitor visitor = new MethodVisitor();
        cu.accept(visitor);
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java FunctionCallAnalyzer <path_to_directory>");
            return;
        }

        try {
            parseJavaFilesInDir(args[0]);
            generateDot();
        } catch (IOException e) {
            System.err.println("Error processing files: " + e.getMessage());
            System.exit(1);
        }
    }
}
