import re
import mysql.connector
from mysql.connector import Error
import config

def parse_sql_to_structure(sql_content):
    """
    按顺序解析 SQL 内容：
    - 识别 question 语句，形成题目列表；
    - 识别 option 语句，并按顺序附加到最近的题目中；
    完全不使用 SQL 中写的 question_id 数字。
    """
    question_pattern = re.compile(
        r"INSERT INTO question\s*\(.*?\)\s*VALUES\s*\(\s*'(.+?)'\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*'(.+?)'\s*\);",
        re.IGNORECASE | re.DOTALL
    )
    option_pattern = re.compile(
        r"INSERT INTO options\s*\(.*?\)\s*VALUES\s*\(\s*NULL\s*,\s*\d+\s*,\s*'(.+?)'\s*,\s*(\d)\s*\);",
        re.IGNORECASE
    )

    questions = []
    for q_match in question_pattern.finditer(sql_content):
        q_text, q_type, subject_id, std_ans = q_match.groups()
        questions.append({
            "question_text": q_text.strip(),
            "question_type": int(q_type),
            "subject_id": int(subject_id),
            "standard_answer": std_ans.strip(),
            "options": []
        })

    opt_iter = option_pattern.finditer(sql_content)
    q_index = 0
    for opt in opt_iter:
        if q_index >= len(questions):
            break
        opt_text, is_correct = opt.groups()
        questions[q_index]["options"].append({
            "option_text": opt_text.strip(),
            "is_correct": int(is_correct)
        })
        next_q = question_pattern.search(sql_content, opt.end())
        next_opt = option_pattern.search(sql_content, opt.end())
        if next_q and (not next_opt or next_q.start() < next_opt.start()):
            q_index += 1

    return questions

def insert_question_and_options(cursor, question):
    """
    插入题目与选项到 MySQL，并打印选项插入用的实际 SQL 和参数。
    """
    cursor.execute(
        """INSERT INTO question (question_text, question_type, subject_id, standard_answer)
           VALUES (%s, %s, %s, %s)""",
        (question["question_text"],
         question["question_type"],
         question["subject_id"],
         question["standard_answer"])
    )
    qid = cursor.lastrowid
    if question["question_type"] in [1, 5] and question["options"]:
        for opt in question["options"]:
            o_sql = """
                INSERT INTO options (question_id, option_text, is_correct)
                VALUES (%s, %s, %s)
            """
            params = (qid, opt["option_text"], opt["is_correct"])
            print("执行选项 SQL:", o_sql.strip(), "参数:", params)
            cursor.execute(o_sql, params)

def import_questions_from_content(sql_content):
    """
    解析并插入题目的主流程，使用配置文件中的数据库配置。
    """
    try:
        conn = mysql.connector.connect(**config.DB_CONFIG)
        cursor = conn.cursor()
        questions = parse_sql_to_structure(sql_content)
        for q in questions:
            print(f"准备插入题目: {q['question_text']}")
            insert_question_and_options(cursor, q)
        conn.commit()
        print("✅ 总共插入题目数：", len(questions))
    except Error as e:
        print("❌ 数据库操作失败:", e)
        if conn.is_connected():
            conn.rollback()
    finally:
        if conn.is_connected():
            cursor.close()
            conn.close()

if __name__ == "__main__":
    sql_content = """
INSERT INTO question (question_text, question_type, subject_id, standard_answer) VALUES ('下列哪些数是偶数？', 5, 1, 'A,C');
INSERT INTO options (option_id, question_id, option_text, is_correct) VALUES (NULL, 1, 'A. 2', 1);
INSERT INTO options (option_id, question_id, option_text, is_correct) VALUES (NULL, 1, 'B. 3', 0);
INSERT INTO options (option_id, question_id, option_text, is_correct) VALUES (NULL, 1, 'C. 4', 1);
INSERT INTO options (option_id, question_id, option_text, is_correct) VALUES (NULL, 1, 'D. 5', 0);
"""
    import_questions_from_content(sql_content)
