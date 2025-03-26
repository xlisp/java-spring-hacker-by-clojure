package com.github.xlisp;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JavaMethodParser {
    private static class MethodVisitor extends VoidVisitorAdapter<List<MethodDeclaration>> {
        @Override
        public void visit(MethodDeclaration method, List<MethodDeclaration> collector) {
            super.visit(method, collector);
            collector.add(method);
        }
    }

    private static List<MethodDeclaration> findMethods(CompilationUnit cu) {
        List<MethodDeclaration> methods = new ArrayList<>();
        MethodVisitor methodVisitor = new MethodVisitor();
        methodVisitor.visit(cu, methods);
        return methods;
    }

    private static String extractMethodCode(MethodDeclaration method) {
        // JavaParser already provides the complete method text
        return method.toString();
    }

    private static void printWithSeparator(String content) {
        System.out.println("-----------split-line-----------------");
        System.out.println(content);
    }

    private static List<String> parseJavaCode(String code) {
        List<String> methodCodes = new ArrayList<>();
        try {
            CompilationUnit cu = new JavaParser().parse(code).getResult().orElseThrow();
            List<MethodDeclaration> methods = findMethods(cu);
            for (MethodDeclaration method : methods) {
                methodCodes.add(extractMethodCode(method));
            }
        } catch (Exception e) {
            System.err.println("Error parsing Java code: " + e.getMessage());
        }
        return methodCodes;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Please provide a Java file path as argument");
            System.exit(1);
        }

        try {
            File file = new File(args[args.length - 1]);
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fileInputStream.read(data);
            fileInputStream.close();

            String javaCode = new String(data, "UTF-8");
            List<String> methods = parseJavaCode(javaCode);

            for (String method : methods) {
                printWithSeparator(method);
            }

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
        }
    }
}
