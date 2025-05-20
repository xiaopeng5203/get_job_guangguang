# AI功能使用说明

本项目支持使用AI服务来优化职位匹配和自动生成打招呼语，以下是配置和使用说明。

## 配置方法

### 方法一：创建.env文件（推荐）

在项目根目录下创建一个名为`.env`的文件，添加以下内容：

```
BASE_URL=https://api.openai.com
API_KEY=your_openai_api_key_here
MODEL=gpt-3.5-turbo
```

请将`your_openai_api_key_here`替换为你的实际OpenAI API密钥。

### 方法二：设置系统环境变量

你也可以直接在系统环境变量中设置以下变量：

- `BASE_URL`: API服务基础URL，默认为`https://api.openai.com`
- `API_KEY`: 你的OpenAI API密钥
- `MODEL`: 使用的模型名称，默认为`gpt-3.5-turbo`

## 功能说明

AI功能主要用于：

1. **职位匹配分析**：分析岗位名称、描述和职责，判断是否与你的期望匹配
2. **智能打招呼语生成**：基于岗位信息自动生成个性化的打招呼语

## 配置选项

在`config.yaml`文件中，可以设置以下AI相关选项：

```yaml
boss:
  enableAI: true # 开启AI检测与自动生成打招呼语

h5boss:
  enableAI: true # 开启AI检测与自动生成打招呼语

ai:
  introduce: "我的个人介绍..." # 你的个人介绍，用于AI生成打招呼语
  prompt: "提示词模板..." # AI提示词模板
```

## 无API密钥时的行为

如果未配置API密钥，AI功能将自动降级：
- 不会进行职位匹配AI分析，所有符合关键词的岗位都将视为匹配
- 使用配置文件中设置的默认打招呼语 