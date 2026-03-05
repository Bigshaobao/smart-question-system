from pathlib import Path
from openai import OpenAI
from flask import Flask, request, render_template, redirect, url_for, flash, jsonify, session
import logging
import os
import mysql.connector
from mysql.connector import pooling
from werkzeug.security import generate_password_hash, check_password_hash
from flask_cors import CORS
import insert
import re
import config

app = Flask(__name__)
CORS(app, resources={r"/*": {"origins": "*"}})
app.secret_key = os.urandom(24)  # 用于保护session

# 创建连接池
pool = pooling.MySQLConnectionPool(
    pool_name=config.DB_POOL_NAME, 
    pool_size=config.DB_POOL_SIZE, 
    **config.DB_CONFIG
)

# 配置日志
print("Setting up logging configuration")
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    filename=config.LOG_FILE_PATH,  # 使用绝对路径
    filemode='a'
)
print(f"Logging configuration set up. Log file: {config.LOG_FILE_PATH}")

# 初始化 OpenAI 客户端
client = OpenAI(
    api_key=config.OPENAI_API_KEY,
    base_url=config.OPENAI_BASE_URL
)

def replace_subject_id(sql_text, new_subject_id):
    pattern = re.compile(
        r"(INSERT INTO question\s*\(.*?\)\s*VALUES\s*\(\s*'[^']+'\s*,\s*\d+\s*,\s*)(\d+)(\s*,)",
        re.IGNORECASE
    )
    return pattern.sub(r"\g<1>" + str(new_subject_id) + r"\g<3>", sql_text)

def get_db_connection():
    return pool.get_connection()

@app.route('/upload', methods=['POST'])
def upload_file():
    logging.info("Upload request received")
    
    # 获取 subject_id，确保是整数
    subject_id = request.form.get('subject_id')
    if not subject_id or not subject_id.isdigit():
        return jsonify({"error": "subject_id 是必填且必须为整数"}), 400
    subject_id = int(subject_id)
    
    if 'file' not in request.files:
        logging.warning("No file part in the request")
        return 'No file part', 400

    file = request.files['file']
    if file.filename == '':
        logging.warning("No selected file")
        return 'No selected file', 400

    # 保存文件到临时目录
    temp_file_path = Path('/www/app/temp') / file.filename
    temp_file_path.parent.mkdir(parents=True, exist_ok=True)  # 确保目录存在
    file.save(temp_file_path)
    logging.info(f"File saved to {temp_file_path}")

    # 获取文件的绝对路径
    absolute_path = temp_file_path.resolve()

    # 处理文件并获取内容
    try:
        # 使用绝对路径创建文件对象
        file_object = client.files.create(file=absolute_path, purpose="file-extract")
        file_id = file_object.id  # 获取文件ID
        logging.info(f"File uploaded to OpenAI with ID: {file_id}")

        # 使用文件ID获取文件内容
        file_content = client.files.content(file_id=file_id).text
    except Exception as e:
        logging.error(f"Error processing file: {e}")
        return str(e), 500

    # 构建消息并发送到 AI
    messages = [
    {
        "role": "system",
        "content": (
            "你是 Kimi，由 Moonshot AI 提供的人工智能助手，你更擅长中文和英文的对话。"
        ),
    },
    {
        "role": "system",
        "content": file_content,
    },
    {
        "role": "user",
        "content": (
            "请读取以下文本内容，识别其中的题目类型，并生成对应的 SQL 插入语句。\n\n"
            "题目类型包括：\n"
            "1. 选择题（带有多个选项，如 A、B、C、D）；\n"
            "2. 填空题（题干中有空格或下划线，且无选项）；\n"
            "3. 判断题（题干通常为陈述句，答案是“对”或“错”）；\n"
            "4. 简答题（题干较长，没有选项，也不是填空题，答案是简短的文字说明）。\n\n"
            "请根据以下规则判断题目类型：\n"
            "- 如果题目有选项（A/B/C/D 等），且只有一个正确答案则判断为单项选择题，question_type 为 1。\n"
            "- 如果题目有选项（A/B/C/D 等），存在多个正确答案则判断为多项选择题，question_type 为 5。\n"
            "- 如果题干中存在空格或下划线，且无选项，则判断为填空题，question_type 为 2。\n"
            "- 如果题干是陈述句且答案为“对”或“错”，则判断为判断题，question_type 为 3。\n"
            "- 其余无选项的题目且答案为较短文字说明的，判断为简答题，question_type 为 4。\n\n"
            "请严格按照以下格式生成 SQL 语句：\n\n"
            "-- 单项和多项选择题：\n"
            "INSERT INTO question (question_text, question_type, subject_id, standard_answer) VALUES ('题目文本', 1, 1, '正确答案');\n"
            "INSERT INTO options (option_id, question_id, option_text, is_correct) VALUES (NULL, 1, '选项内容', 是否为正确答案（0或1）);\n\n"
            "-- 填空题、判断题、简答题：\n"
            "INSERT INTO question (question_text, question_type, subject_id, standard_answer) VALUES ('题目文本', 题目类型编号, 1, '正确答案');\n\n"
            "请确保所有题目的 SQL 语句完整且格式统一，题目编号由数据库自增生成，不需要你指定。\n"
        )
    },
]

    try:
        completion = client.chat.completions.create(
            model="moonshot-v1-128k",
            messages=messages,
            temperature=0.3,
            max_tokens=10000,
        )
        response_content = completion.choices[0].message.content
        response_content = replace_subject_id(response_content, subject_id)
        insert.import_questions_from_content(response_content)
        logging.info("Response received from AI")
    except Exception as e:
        logging.error(f"Error getting response from AI: {e}")
        return str(e), 500

    response_file_path = Path('/www/app/response.txt')
    with open(response_file_path, 'w', encoding='utf-8') as f:
        f.write(response_content)
    logging.info(f"Response saved to {response_file_path}")

    return jsonify({"response": response_content})

@app.route('/register', methods=['POST'])
def register():
    data = request.get_json()
    user_name = data.get('user_name')
    password = data.get('password')
    user_type = data.get('user_type', 1)  # 如果没有提供 user_type，则默认为 1

    if not user_name or not password:
        return jsonify({'message': 'Missing user_name or password', 'status': 'error'}), 400

    # 密码加密
    hashed_password = generate_password_hash(password)

    # 插入数据库
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute('INSERT INTO user (user_name, password, user_type) VALUES (%s, %s, %s)',
                       (user_name, hashed_password, user_type))
        conn.commit()
        cursor.close()
        conn.close()
        return jsonify({'message': '注册成功', 'status': 'success'}), 201
    except mysql.connector.Error as err:
        logging.error(f"Database error: {err}")
        return jsonify({'message': 'Database error', 'status': 'error'}), 500

@app.route('/login', methods=['POST'])
def login():
    data = request.get_json()
    user_name = data['user_name']
    password = data['password']

    # 查询数据库
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute('SELECT id, password, user_type FROM user WHERE user_name = %s', (user_name,))
        user = cursor.fetchone()
        cursor.close()
        conn.close()

        if user and check_password_hash(user[1], password):  # 注意这里的索引，password 是第二个元素
            return jsonify({'message': '登录成功', 'status': 'success', 'user_id': user[0], 'user_type': user[2]})
        else:
            return jsonify({'message': '用户名或密码错误', 'status': 'error'}), 401
    except mysql.connector.Error as err:
        logging.error(f"Database error: {err}")
        return jsonify({'message': 'Database error', 'status': 'error'}), 500

@app.route('/admin_login', methods=['GET', 'POST'])
def admin_login():
    print("Received POST request to /admin_login")
    if request.method == 'POST':
        data = request.get_json()
        print("Received data:", data)
        user_name = data.get('user_name')
        password = data.get('password')

        if not user_name or not password:
            print("Missing user_name or password")
            return jsonify({'message': '用户名和密码不能为空', 'status': 'error'}), 400

        try:
            conn = get_db_connection()
            cursor = conn.cursor()
            cursor.execute('SELECT password, user_type FROM user WHERE user_name = %s', (user_name,))
            user = cursor.fetchone()
            cursor.close()
            conn.close()
            print("Database query executed")

            if user and check_password_hash(user[0], password) and user[1] == 1:
                session['admin_logged_in'] = True
                print("Login successful")
                return jsonify({'message': '登录成功', 'status': 'success'}), 200
            else:
                print("Login failed: incorrect credentials or not an admin")
                return jsonify({'message': '用户名或密码错误，或非管理员', 'status': 'error'}), 401
        except mysql.connector.Error as e:
            print("Database error:", e)
            return jsonify({'message': '数据库错误', 'status': 'error', 'error': str(e)}), 500
    return render_template('admin_login.html')

@app.route('/admin_dashboard')
def admin_dashboard():
    if 'admin_logged_in' not in session or not session['admin_logged_in']:
        return redirect(url_for('admin_login'))
    return render_template('admin_dashboard.html')

@app.route('/get_users')
def get_users():
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute('SELECT id, user_name, user_type FROM user')
        users = cursor.fetchall()
        cursor.close()
        conn.close()
        user_list = [{'id': user[0], 'user_name': user[1], 'user_type': user[2]} for user in users]
        return jsonify(user_list)
    except mysql.connector.Error as err:
        logging.error(f"Database error: {err}")
        return jsonify({'message': 'Database error', 'status': 'error'}), 500

@app.route('/admin_add_user', methods=['GET', 'POST'])
def admin_add_user():
    if request.method == 'POST':
        data = request.get_json()
        user_name = data['user_name']
        password = data['password']
        user_type = data['user_type']
        hashed_password = generate_password_hash(password)
        try:
            conn = get_db_connection()
            cursor = conn.cursor()
            cursor.execute('INSERT INTO user (user_name, password, user_type) VALUES (%s, %s, %s)',
                           (user_name, hashed_password, user_type))
            conn.commit()
            cursor.close()
            conn.close()
            return jsonify({'message': '用户添加成功', 'status': 'success'}), 201
        except mysql.connector.Error as err:
            logging.error(f"Database error: {err}")
            return jsonify({'message': 'Database error', 'status': 'error'}), 500
    return render_template('admin_add_user.html')

@app.route('/delete_user/<int:user_id>', methods=['DELETE'])
def delete_user(user_id):
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute('DELETE FROM user WHERE id = %s', (user_id,))
        conn.commit()
        cursor.close()
        conn.close()
        return jsonify({'message': '用户删除成功', 'status': 'success'})
    except mysql.connector.Error as err:
        logging.error(f"Database error: {err}")
        return jsonify({'message': 'Database error', 'status': 'error'}), 500

@app.route('/admin_edit_user/<int:user_id>', methods=['GET', 'POST'])
def admin_edit_user(user_id):
    if request.method == 'POST':
        data = request.get_json()
        user_name = data['user_name']
        password = data['password']
        user_type = data['user_type']
        hashed_password = generate_password_hash(password)
        try:
            conn = get_db_connection()
            cursor = conn.cursor()
            cursor.execute('UPDATE user SET user_name = %s, password = %s, user_type = %s WHERE id = %s',
                           (user_name, hashed_password, user_type, user_id))
            conn.commit()
            cursor.close()
            conn.close()
            return jsonify({'message': '用户更新成功', 'status': 'success'})
        except mysql.connector.Error as err:
            logging.error(f"Database error: {err}")
            return jsonify({'message': 'Database error', 'status': 'error'}), 500
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute('SELECT user_name, user_type FROM user WHERE id = %s', (user_id,))
        user = cursor.fetchone()
        cursor.close()
        conn.close()
        return render_template('admin_edit_user.html', user={'id': user_id, 'user_name': user[0], 'user_type': user[1]})
    except mysql.connector.Error as err:
        logging.error(f"Database error: {err}")
        return jsonify({'message': 'Database error', 'status': 'error'}), 500

@app.route('/get_questions', methods=['GET'])
def get_questions():
    try:
        limit = int(request.args.get('limit', 10))
        offset = int(request.args.get('offset', 0))
        subject_id = request.args.get('subject_id')

        conn = get_db_connection()
        cursor = conn.cursor()
        if subject_id:
            # 带科目筛选
            cursor.execute('''
                SELECT 
                    q.question_id, 
                    q.question_text, 
                    q.question_type, 
                    q.standard_answer,
                    s.subject_name
                FROM question q
                LEFT JOIN subject s ON q.subject_id = s.subject_id
                WHERE q.subject_id = %s
                ORDER BY q.question_id ASC
                LIMIT %s OFFSET %s
            ''', (subject_id, limit, offset))
        else:
            # 不筛选科目
            cursor.execute('''
                SELECT 
                    q.question_id, 
                    q.question_text, 
                    q.question_type, 
                    q.standard_answer,
                    s.subject_name
                FROM question q
                LEFT JOIN subject s ON q.subject_id = s.subject_id
                ORDER BY q.question_id ASC
                LIMIT %s OFFSET %s
            ''', (limit, offset))

        questions = cursor.fetchall()
        question_list = []

        for question in questions:
            question_id = question[0]
            question_text = question[1]
            question_type = question[2]
            standard_answer = question[3]
            subject_name = question[4]

            # 查询选项
            cursor.execute('''
                SELECT option_id, option_text, is_correct
                FROM options
                WHERE question_id = %s
            ''', (question_id,))
            options = cursor.fetchall()

            options_list = [{
                'option_id': opt[0],
                'option_text': opt[1],
                'is_correct': bool(opt[2])
            } for opt in options]

            question_list.append({
                'question_id': question_id,
                'question_text': question_text,
                'question_type': question_type,
                'standard_answer': standard_answer,
                'subject_name': subject_name,
                'options': options_list
            })

        # 查询总数（分页辅助）
        if subject_id:
            cursor.execute('SELECT COUNT(*) FROM question WHERE subject_id = %s', (subject_id,))
        else:
            cursor.execute('SELECT COUNT(*) FROM question')
        total_count = cursor.fetchone()[0]

        cursor.close()
        conn.close()

        return jsonify({
            'status': 'success',
            'total_count': total_count,
            'questions': question_list
        }), 200

    except mysql.connector.Error as err:
        logging.error(f"Database error while fetching questions: {err}")
        return jsonify({'message': '数据库错误', 'status': 'error'}), 500

@app.route('/get_objective_questions', methods=['GET'])
def get_objective_questions():
    conn = None
    cursor = None
    try:
        page = int(request.args.get('page', 1))
        per_page = int(request.args.get('per_page', 10))
        offset = (page - 1) * per_page
        subject_id = request.args.get('subject_id')  # 新增参数

        conn = get_db_connection()
        cursor = conn.cursor()

        # 获取总数
        cursor.execute('''
            SELECT COUNT(*)
            FROM question q
            LEFT JOIN subject s ON q.subject_id = s.subject_id
            WHERE q.question_type IN (1, 2, 3, 5)
            AND (%s IS NULL OR q.subject_id = %s)
        ''', (subject_id, subject_id))
        total_count = cursor.fetchone()[0]

        # 获取分页数据
        cursor.execute('''
            SELECT q.question_id, q.question_text, q.question_type, q.standard_answer, s.subject_name
            FROM question q
            LEFT JOIN subject s ON q.subject_id = s.subject_id
            WHERE q.question_type IN (1, 2, 3, 5)
            AND (%s IS NULL OR q.subject_id = %s)
            ORDER BY q.question_id ASC
            LIMIT %s OFFSET %s
        ''', (subject_id, subject_id, per_page, offset))
        questions = cursor.fetchall()

        result = []
        for q in questions:
            question_id = q[0]
            cursor.execute('SELECT option_id, option_text, is_correct FROM options WHERE question_id = %s', (question_id,))
            options = cursor.fetchall()
            result.append({
                'question_id': q[0],
                'question_text': q[1],
                'question_type': q[2],
                'standard_answer': q[3],
                'subject_name': q[4],
                'options': [
                    {
                        'option_id': opt[0],
                        'option_text': opt[1],
                        'is_correct': bool(opt[2])
                    } for opt in options
                ]
            })

        return jsonify({
            'status': 'success',
            'total': total_count,
            'page': page,
            'per_page': per_page,
            'pages': (total_count + per_page - 1) // per_page,
            'questions': result
        }), 200

    except Exception as e:
        logging.error(f"[get_objective_questions] Error: {e}")
        return jsonify({'status': 'error', 'message': '服务器内部错误'}), 500

    finally:
        if cursor:
            cursor.close()
        if conn:
            conn.close()

@app.route('/get_dictation_questions', methods=['GET'])
def get_dictation_questions():
    conn = None
    cursor = None
    try:
        page = int(request.args.get('page', 1))
        per_page = int(request.args.get('per_page', 10))
        offset = (page - 1) * per_page
        subject_id = request.args.get('subject_id')  # 新增参数

        conn = get_db_connection()
        cursor = conn.cursor()

        # 获取总数
        cursor.execute('''
            SELECT COUNT(*)
            FROM question q
            LEFT JOIN subject s ON q.subject_id = s.subject_id
            WHERE q.question_type IN (2, 4)
            AND (%s IS NULL OR q.subject_id = %s)
        ''', (subject_id, subject_id))
        total_count = cursor.fetchone()[0]

        # 获取分页数据
        cursor.execute('''
            SELECT q.question_id, q.question_text, q.question_type, q.standard_answer, s.subject_name
            FROM question q
            LEFT JOIN subject s ON q.subject_id = s.subject_id
            WHERE q.question_type IN (2, 4)
            AND (%s IS NULL OR q.subject_id = %s)
            ORDER BY q.question_id ASC
            LIMIT %s OFFSET %s
        ''', (subject_id, subject_id, per_page, offset))
        questions = cursor.fetchall()

        result = []
        for q in questions:
            result.append({
                'question_id': q[0],
                'question_text': q[1],
                'question_type': q[2],
                'standard_answer': q[3],
                'subject_name': q[4]
            })

        return jsonify({
            'status': 'success',
            'total': total_count,
            'page': page,
            'per_page': per_page,
            'pages': (total_count + per_page - 1) // per_page,
            'questions': result
        }), 200

    except Exception as e:
        logging.error(f"[get_dictation_questions] Error: {e}")
        return jsonify({'status': 'error', 'message': '服务器内部错误'}), 500

    finally:
        if cursor:
            cursor.close()
        if conn:
            conn.close()


@app.route('/get_subjects', methods=['GET'])
def get_subjects():
    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # 查询subject表的所有记录
        cursor.execute('SELECT subject_id, subject_name FROM subject')
        subjects = cursor.fetchall()

        # 将查询结果转换为列表字典形式
        subjects_list = []
        for subject in subjects:
            subject_dict = {
                'subject_id': subject[0],
                'subject_name': subject[1]
            }
            subjects_list.append(subject_dict)

        cursor.close()
        conn.close()

        # 返回JSON格式的响应
        return jsonify({'subjects': subjects_list, 'status': 'success'}), 200

    except mysql.connector.Error as err:
        logging.error(f"Database error while fetching subjects: {err}")
        return jsonify({'message': '数据库错误', 'status': 'error'}), 500

@app.route('/add_subject', methods=['POST'])
def add_subject():
    try:
        # 从请求体中获取数据
        data = request.get_json()
        subject_name = data.get('subject_name')
        
        # 检查是否提供了科目名称
        if not subject_name:
            return jsonify({'message': '缺少科目名称', 'status': 'error'}), 400

        conn = get_db_connection()
        cursor = conn.cursor()

        # 插入新科目到数据库
        cursor.execute('INSERT INTO subject (subject_name) VALUES (%s)', (subject_name,))
        conn.commit()  # 提交事务

        cursor.close()
        conn.close()

        # 返回成功响应
        return jsonify({'message': '科目添加成功', 'status': 'success'}), 201

    except mysql.connector.Error as err:
        logging.error(f"Database error while adding subject: {err}")
        return jsonify({'message': '数据库错误', 'status': 'error'}), 500
        
@app.route('/delete_subject/<int:subject_id>', methods=['DELETE'])
def delete_subject(subject_id):
    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # 删除指定ID的科目
        cursor.execute('DELETE FROM subject WHERE subject_id = %s', (subject_id,))
        conn.commit()  # 提交事务

        if cursor.rowcount == 0:
            cursor.close()
            conn.close()
            # 如果没有删除任何行，表示没有找到对应的科目
            return jsonify({'message': '未找到指定的科目', 'status': 'error'}), 404

        cursor.close()
        conn.close()

        # 返回成功响应
        return jsonify({'message': '科目删除成功', 'status': 'success'}), 200

    except mysql.connector.Error as err:
        logging.error(f"Database error while deleting subject: {err}")
        return jsonify({'message': '数据库错误', 'status': 'error'}), 500
        
@app.route('/add_favorite', methods=['POST'])
def add_favorite():
    data = request.get_json()
    user_id = data.get('user_id')
    question_id = data.get('question_id')

    if not user_id or not question_id:
        return jsonify({'message': 'Missing user_id or question_id', 'status': 'error'}), 400

    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # 查询题目的subject_id
        cursor.execute('SELECT subject_id FROM question WHERE question_id = %s', (question_id,))
        row = cursor.fetchone()
        if row is None:
            return jsonify({'message': '题目不存在', 'status': 'error'}), 404
        subject_id = row[0]

        # 插入收藏，带subject_id
        cursor.execute('INSERT INTO WrongQuestion (user_id, question_id, subject_id) VALUES (%s, %s, %s)', 
                       (user_id, question_id, subject_id))
        conn.commit()
        cursor.close()
        conn.close()
        return jsonify({'message': '收藏成功', 'status': 'success'}), 201

    except mysql.connector.Error as err:
        logging.error(f"Database error: {err}")
        return jsonify({'message': 'Database error', 'status': 'error'}), 500

@app.route('/favorite_subjects/<int:user_id>', methods=['GET'])
def get_favorite_subjects(user_id):
    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # 查询该用户收藏的所有科目，去重
        cursor.execute('''
            SELECT DISTINCT s.subject_id, s.subject_name
            FROM WrongQuestion w
            JOIN subject s ON w.subject_id = s.subject_id
            WHERE w.user_id = %s
        ''', (user_id,))
        rows = cursor.fetchall()

        subjects = [{'subject_id': row[0], 'subject_name': row[1]} for row in rows]

        cursor.close()
        conn.close()

        return jsonify({'status': 'success', 'subjects': subjects}), 200
    except mysql.connector.Error as err:
        logging.error(f"Database error: {err}")
        return jsonify({'message': 'Database error', 'status': 'error'}), 500

@app.route('/favorite_questions', methods=['GET'])
def get_favorite_questions():
    conn = None
    cursor = None
    try:
        user_id = request.args.get('user_id', type=int)
        subject_id = request.args.get('subject_id', type=int)
        page = int(request.args.get('page', 1))
        per_page = int(request.args.get('per_page', 10))

        if not user_id:
            return jsonify({'message': 'Missing user_id', 'status': 'error'}), 400

        offset = (page - 1) * per_page

        conn = get_db_connection()
        cursor = conn.cursor()

        # 查询收藏题目的总数
        count_query = '''
            SELECT COUNT(*)
            FROM WrongQuestion w
            JOIN question q ON w.question_id = q.question_id
            WHERE w.user_id = %s
            AND (%s IS NULL OR w.subject_id = %s)
        '''
        cursor.execute(count_query, (user_id, subject_id, subject_id))
        total_count = cursor.fetchone()[0]

        # 查询收藏题目及分页
        query = '''
            SELECT q.question_id, q.question_text, q.question_type, q.standard_answer, q.subject_id
            FROM WrongQuestion w
            JOIN question q ON w.question_id = q.question_id
            WHERE w.user_id = %s
            AND (%s IS NULL OR w.subject_id = %s)
            ORDER BY q.question_id ASC
            LIMIT %s OFFSET %s
        '''
        cursor.execute(query, (user_id, subject_id, subject_id, per_page, offset))
        questions = cursor.fetchall()

        result = []
        for q in questions:
            question_id = q[0]

            # 查询选项
            cursor.execute('SELECT option_id, option_text, is_correct FROM options WHERE question_id = %s', (question_id,))
            options = cursor.fetchall()

            # 查询科目名称
            cursor.execute('SELECT subject_name FROM subject WHERE subject_id = %s', (q[4],))
            subject_name_row = cursor.fetchone()
            subject_name = subject_name_row[0] if subject_name_row else None

            result.append({
                'question_id': q[0],
                'question_text': q[1],
                'question_type': q[2],
                'standard_answer': q[3],
                'subject_name': subject_name,
                'options': [
                    {
                        'option_id': opt[0],
                        'option_text': opt[1],
                        'is_correct': bool(opt[2])
                    } for opt in options
                ]
            })

        return jsonify({
            'status': 'success',
            'total': total_count,
            'page': page,
            'per_page': per_page,
            'pages': (total_count + per_page - 1) // per_page,
            'questions': result
        }), 200

    except Exception as e:
        logging.error(f"[get_favorite_questions] Error: {e}")
        return jsonify({'status': 'error', 'message': '服务器内部错误'}), 500

    finally:
        if cursor:
            cursor.close()
        if conn:
            conn.close()

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)