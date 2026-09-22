import sys

with open("app/src/main/java/com/saleh/enezi/MainActivity.java", "r", encoding="utf-8") as f:
    code = f.read()

in_str = False
in_char = False
in_line_comment = False
in_block_comment = False
escape = False
stack = []
line_num = 1
col_num = 0

errors = []
i = 0
n = len(code)
while i < n:
    c = code[i]
    if c == '\n':
        line_num += 1
        col_num = 0
        if in_line_comment:
            in_line_comment = False
        i += 1
        continue
    col_num += 1
    
    if in_line_comment:
        i += 1
        continue
    if in_block_comment:
        if c == '*' and i + 1 < n and code[i+1] == '/':
            in_block_comment = False
            i += 2
            continue
        i += 1
        continue
    if in_str:
        if escape:
            escape = False
        elif c == '\\':
            escape = True
        elif c == '"':
            in_str = False
        i += 1
        continue
    if in_char:
        if escape:
            escape = False
        elif c == '\\':
            escape = True
        elif c == "'":
            in_char = False
        i += 1
        continue

    if c == '/' and i + 1 < n and code[i+1] == '/':
        in_line_comment = True
        i += 2
        continue
    if c == '/' and i + 1 < n and code[i+1] == '*':
        in_block_comment = True
        i += 2
        continue
    if c == '"':
        in_str = True
        i += 1
        continue
    if c == "'":
        in_char = True
        i += 1
        continue

    if c in "{[(":
        stack.append((c, line_num, col_num))
    elif c in "}])":
        if not stack:
            errors.append(f"Unmatched {c} at {line_num}:{col_num}")
        else:
            top, tl, tc = stack.pop()
            matches = {")": "(", "}": "{", "]": "["}
            if matches[c] != top:
                errors.append(f"Mismatched {c} at {line_num}:{col_num}, expected match for {top} from {tl}:{tc}")
    i += 1

if stack:
    for top, tl, tc in stack[-5:]:
        errors.append(f"Unclosed {top} from {tl}:{tc}")

print("Total lines:", line_num)
print("Errors found:", len(errors))
for e in errors[:10]:
    print(" ", e)
