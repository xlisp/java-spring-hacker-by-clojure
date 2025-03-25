package com.github.xlisp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

public class ClassRelationshipParser {
    private Set<String> classes = new HashSet<>();
    private Set<Relationship> relationships = new HashSet<>();

    static class Relationship {
        String from;
        String to;
        String type; // "extends", "implements", "associates"

        public Relationship(String from, String to, String type) {
            this.from = from;
            this.to = to;
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Relationship)) return false;
            Relationship that = (Relationship) o;
            return from.equals(that.from) &&
                   to.equals(that.to) &&
                   type.equals(that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to, type);
        }
    }

    public void parseDirectory(String directoryPath) throws IOException {
        File directory = new File(directoryPath);
        parseFiles(directory);
        generateDotFile("class_diagram.dot");
    }

    private void parseFiles(File file) throws IOException {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    parseFiles(child);
                }
            }
        } else if (file.getName().endsWith(".java")) {
            parseJavaFile(file);
        }
    }

    private void parseJavaFile(File file) throws IOException {
        CompilationUnit cu = StaticJavaParser.parse(file);
        
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            String className = classDecl.getNameAsString();
            classes.add(className);

            // Handle extends relationships
            classDecl.getExtendedTypes().forEach(extendedType -> {
                relationships.add(new Relationship(
                    className,
                    extendedType.getNameAsString(),
                    "extends"
                ));
            });

            // Handle implements relationships
            classDecl.getImplementedTypes().forEach(implementedType -> {
                relationships.add(new Relationship(
                    className,
                    implementedType.getNameAsString(),
                    "implements"
                ));
            });

            // Handle field-based associations
            classDecl.getFields().forEach(field -> {
                field.getElementType().ifClassOrInterfaceType(type -> {
                    relationships.add(new Relationship(
                        className,
                        type.getNameAsString(),
                        "associates"
                    ));
                });
            });
        });
    }

    private void generateDotFile(String outputPath) throws IOException {
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write("digraph ClassDiagram {\n");
            writer.write("  rankdir=BT;\n");
            writer.write("  node [shape=box, style=filled, fillcolor=white];\n\n");

            // Write all classes
            for (String className : classes) {
                writer.write(String.format("  %s [label=\"%s\"];\n", 
                    className, className));
            }

            writer.write("\n");

            // Write relationships
            for (Relationship rel : relationships) {
                String arrow = switch (rel.type) {
                    case "extends" -> "empty";
                    case "implements" -> "empty,dashed";
                    default -> "open"; // associations
                };
                writer.write(String.format("  %s -> %s [arrowhead=\"%s\"];\n",
                    rel.from, rel.to, arrow));
            }

            writer.write("}\n");
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java ClassRelationshipParser <directory>");
            return;
        }

        try {
            ClassRelationshipParser parser = new ClassRelationshipParser();
            parser.parseDirectory(args[0]);
            System.out.println("Generated class diagram in class_diagram.dot");
            System.out.println("To generate PNG, run: dot -Tpng class_diagram.dot -o class_diagram.png");
        } catch (IOException e) {
            System.err.println("Error processing files: " + e.getMessage());
        }
    }
}
