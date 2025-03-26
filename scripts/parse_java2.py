import sys
from typing import List
from javalang.parse import parse
from javalang.tree import MethodDeclaration

def get_method(node):
    if isinstance(node, MethodDeclaration):
        return [node]
    return []

def foo(node) -> List[MethodDeclaration]:
    methods = []
    for path, child in node:
        methods.extend(get_method(child))
    return methods

def extract_method_code(method: MethodDeclaration, code: str) -> str:
    lines = code.splitlines()
    start_line = method.position.line - 1  # Convert to 0-based index
    # Attempt to find the method's end by identifying the closing bracket
    brace_count = 0
    method_lines = []
    for line in lines[start_line:]:
        method_lines.append(line)
        brace_count += line.count('{') - line.count('}')
        if brace_count == 0:
            break
    return "\n".join(method_lines)

def parse_java_code(code: str) -> List[str]:
    tree = parse(code)
    methods = foo(tree)
    return [extract_method_code(method, code) for method in methods]

def my_print(s: str):
    print("-----------split-line-----------------")
    print(s)

def main():
    args = sys.argv
    with open(args[-1], 'r') as file:
        java_code = file.read()
        methods = parse_java_code(java_code)
        for method in methods:
            my_print(method)

if __name__ == "__main__":
    main()

