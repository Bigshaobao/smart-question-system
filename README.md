# Smart Question System（智能题库系统）

Smart Question System 是一个基于 **Flask + MySQL + Android** 开发的智能题库练习系统，用于学生进行题目练习与学习记录管理。

本项目为本科毕业设计基础版本（Ver1），实现了完整的 **后端API + 数据库设计 + Android客户端** 架构，后续将扩展 AI 智能分析功能。

GitHub:
https://github.com/Bigshaobao/smart-question-system

---

# 项目简介 Project Introduction

本系统是一个完整的题库练习系统，包括：

- Flask 后端 REST API
- MySQL 数据库存储
- Android 客户端应用
- 简单后台管理页面（Flask Template）

系统支持用户登录、题目练习、收藏题目等功能。

---

# 技术栈 Tech Stack

## Backend

- Python
- Flask
- MySQL
- RESTful API
- Session Authentication

主要负责：

- 用户认证
- 题目管理
- 数据查询
- API服务

---

## Android Client

- Java
- Android Studio
- HTTP API 调用

主要功能：

- 用户登录注册
- 获取题目
- 在线练习
- 收藏题目
- 成绩展示

---

# 系统架构 Architecture

```
Android App
    ↓ HTTP API
Flask Backend
    ↓
MySQL Database
```

后端提供 REST API，Android 客户端通过 HTTP 请求访问服务器。

---

# 核心功能 Features

## 用户系统 User System

- 用户注册 Register
- 用户登录 Login
- Session认证
- 管理员登录页面

主要接口：

```
POST /login
POST /register
```

---

## 题库系统 Question System

已实现：

- 获取题目列表
- 获取题目详情
- 在线答题
- 收藏题目

主要接口：

```
GET /questions
GET /question/<id>
```

---

## 后台页面 Admin Pages

使用 Flask Template 实现：

- 管理员登录页面
- 基础管理功能

路径：

```
backend/templates/
```

---

# 数据库设计 Database Design

主要数据表：

### user 表

存储用户信息：

- id
- username
- password

---

### question 表

存储题目信息：

- id
- question
- answer
- type

---

### favorite 表

存储收藏记录：

- user_id
- question_id

---

# 项目结构 Project Structure

```
smart-question-system
│
├── backend/
│   ├── app.py
│   ├── insert.py
│   ├── templates/
│   └── 123.sql
│
├── Android/
│
└── README.md
```

---

# 运行方法 Run Project

## 1 启动后端

进入 backend 目录：

```
cd backend
```

运行：

```
python app.py
```

---

## 2 导入数据库

导入：

```
backend/123.sql
```

数据库：

```
MySQL
```

---

## 项目状态 Project Status

当前版本：

```
Version 1
```

已实现完整题库系统基础功能。

---

# 后续开发计划 Future Work（毕业设计方向）

计划增加：

### 后端优化

- Redis 登录缓存
- Token认证
- API性能优化

---

### AI功能（毕业设计）

计划实现：

- Transformer题目分类
- 布鲁姆认知分类（Bloom Taxonomy）
- 学生薄弱点分析
- 智能推荐题目

---

# 作者 Shaobao

Backend Developer (Python / Flask)