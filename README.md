# Smart Question System（智能题库系统）

Smart Question System 是一个基于 **Flask + MySQL + Android** 开发的智能题库系统，用于学生练习和测试。

本项目是本科毕业设计的基础版本（Ver1），后续将逐步增加 AI 推荐与学习分析功能。

---

## 项目简介 Project Introduction

本系统包含两个部分：

- 后端：Python Flask REST API
- 数据库：MySQL
- 客户端：Android App（Java）

主要实现题库练习与管理功能。

---

## 技术栈 Tech Stack

### Backend

- Python
- Flask
- MySQL
- RESTful API

### Client

- Android
- Java

---

## 项目结构 Project Structure

```
smart-question-system
│
├── backend/        # Flask 后端代码
│   ├── app.py
│   ├── insert.py
│   └── 123.sql
│
├── Android/        # Android 客户端
│
└── README.md
```

---

## 当前功能 Features (Ver1)

已实现：

- 用户登录 Login
- 用户注册 Register
- 题目获取 Question API
- 题目练习 Practice
- 收藏功能 Favorites

计划实现：

- Redis 登录缓存
- 错题分析
- 学习记录统计
- AI智能推荐

---

## 项目状态 Project Status

当前版本：

Version 1 （基础版本）

后续计划：

- Flask + Redis 登录系统
- Transformer题目分类
- 布鲁姆认知分类（Bloom Taxonomy）
- 薄弱点分析 Weakness Analysis

---

## 运行方式 Run Project

Backend：

```
python app.py
```

数据库：

导入：

```
backend/123.sql
```

---

## 作者 :Shaobao

Backend Developer (Python / Flask)