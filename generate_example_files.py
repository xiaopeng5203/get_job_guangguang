import re
import os

# 定义脱敏规则
SENSITIVE_MAP = {
    '谷鹏': '张三',
    '恒瑞通': '某公司A',
    '四创科技': '某公司B',
    '新大陆': '某公司C',
    # 如有更多敏感词，继续添加
}

# 文件路径
src_config = 'src/main/resources/config.yaml'
dst_config = 'src/main/resources/config.example.yaml'
src_readme = 'README.md'
dst_readme = 'README.example.md'

# 新增：README.md 脱敏覆盖路径
cover_readme = 'README.md'

def desensitize(content):
    for k, v in SENSITIVE_MAP.items():
        content = re.sub(k, v, content)
    return content

def process_file(src, dst):
    if not os.path.exists(src):
        print(f'未找到源文件: {src}')
        return
    with open(src, 'r', encoding='utf-8') as f:
        content = f.read()
    content = desensitize(content)
    with open(dst, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f'已生成脱敏文件: {dst}')

if __name__ == '__main__':
    process_file(src_config, dst_config)
    process_file(src_readme, dst_readme)
    # 新增：用脱敏内容覆盖 README.md
    if os.path.exists(src_readme):
        with open(src_readme, 'r', encoding='utf-8') as f:
            content = f.read()
        content = desensitize(content)
        with open(cover_readme, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f'已用脱敏内容覆盖 README.md') 