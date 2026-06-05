import json
import random
import re
from pathlib import Path

from docx import Document


SOURCE_DOCX = Path("src/main/resources/data/2026年春概论课程知识要点.docx")
CHAPTER_OUTPUT = Path("src/main/resources/data/questions.json")
EXAM_OUTPUT = Path("src/main/resources/data/exam_questions.json")

CHAPTER_RE = re.compile(r"^(导论\b|第[一二三四五六七八九十]+章)\s*(.*)$")


def clean_text(text):
    text = re.sub(r"\s+", " ", text or "").strip()
    text = re.sub(r"^\d+[.．、]\s*", "", text)
    return text


def display_title(title):
    return title.replace("新民民主主义", "新民主主义")


def shorten(text, limit=96):
    text = clean_text(text)
    if len(text) <= limit:
        return text
    cut = text[:limit].rstrip("，；、。")
    return cut + "……"


def extract_sections():
    document = Document(str(SOURCE_DOCX))
    sections = []
    current = None

    for paragraph in document.paragraphs:
        text = clean_text(paragraph.text)
        if not text:
            continue

        if CHAPTER_RE.match(text):
            if current:
                sections.append(current)
            current = {"title": display_title(text), "points": []}
            continue

        if current and len(text) >= 12:
            current["points"].append(text)

    if current:
        sections.append(current)

    return sections


def option(label, text):
    return f"{label}. {shorten(text)}"


def rotate_options(correct_text, distractors, offset):
    letters = ["A", "B", "C", "D"]
    items = [correct_text] + distractors[:3]
    correct_index = offset % 4
    ordered = []
    source_index = 1
    for idx in range(4):
        if idx == correct_index:
            ordered.append(correct_text)
        else:
            ordered.append(items[source_index])
            source_index += 1
    return [option(letters[i], ordered[i]) for i in range(4)], letters[correct_index]


def make_single(chapter_id, title, point, distractors, offset):
    options, answer = rotate_options(point, distractors, offset)
    return {
        "chapter": chapter_id,
        "type": "single",
        "question": f"关于“{title}”，下列说法正确的是（）。",
        "options": options,
        "answer": answer,
        "explanation": f"本题考查本章知识点：{shorten(point, 140)}",
    }


def make_judge(chapter_id, title, point, false_point, offset):
    is_true = offset % 2 == 0
    statement = point if is_true else false_point
    return {
        "chapter": chapter_id,
        "type": "judge",
        "question": f"判断：以下内容属于“{title}”的核心知识点：{shorten(statement, 120)}",
        "options": ["A. 对", "B. 错"],
        "answer": "A" if is_true else "B",
        "explanation": "该表述来自本章知识点。" if is_true else "该表述不属于本章核心知识点，注意区分不同章节内容。",
    }


def make_multiple(chapter_id, title, correct_points, wrong_points, offset):
    letters = ["A", "B", "C", "D"]
    pairs = [(correct_points[0], True), (correct_points[1], True), (wrong_points[0], False), (wrong_points[1], False)]
    shift = offset % 4
    pairs = pairs[shift:] + pairs[:shift]
    answers = "".join(letters[i] for i, (_, is_correct) in enumerate(pairs) if is_correct)
    return {
        "chapter": chapter_id,
        "type": "multiple",
        "question": f"下列属于“{title}”相关知识点的有（）。",
        "options": [option(letters[i], pairs[i][0]) for i in range(4)],
        "answer": answers,
        "explanation": "正确选项均来自本章知识点，其余选项来自其他章节，用于辨析。",
    }


def build_questions():
    random.seed(20260605)
    sections = extract_sections()
    all_points = []
    for idx, section in enumerate(sections, start=1):
        for point in section["points"]:
            all_points.append((idx, point))

    questions = []
    for chapter_id, section in enumerate(sections, start=1):
        title = section["title"]
        points = section["points"]
        other_points = [point for other_id, point in all_points if other_id != chapter_id]

        if not points:
            continue

        for i in range(12):
            point = points[i % len(points)]
            start = (chapter_id * 13 + i * 3) % len(other_points)
            distractors = [other_points[(start + j) % len(other_points)] for j in range(3)]
            questions.append(make_single(chapter_id, title, point, distractors, i))

        for i in range(6):
            point = points[(12 + i) % len(points)]
            false_point = other_points[(chapter_id * 17 + i * 5) % len(other_points)]
            questions.append(make_judge(chapter_id, title, point, false_point, i))

        for i in range(6):
            correct = [points[(18 + i * 2) % len(points)], points[(19 + i * 2) % len(points)]]
            wrong = [other_points[(chapter_id * 19 + i * 4) % len(other_points)], other_points[(chapter_id * 19 + i * 4 + 1) % len(other_points)]]
            questions.append(make_multiple(chapter_id, title, correct, wrong, i))

    for idx, question in enumerate(questions, start=1):
        question["id"] = idx

    return questions


def main():
    questions = build_questions()
    CHAPTER_OUTPUT.write_text(json.dumps(questions, ensure_ascii=False, indent=2), encoding="utf-8")
    EXAM_OUTPUT.write_text("[]\n", encoding="utf-8")
    print(f"Wrote {len(questions)} chapter questions to {CHAPTER_OUTPUT}")
    print(f"Cleared exam question bank: {EXAM_OUTPUT}")


if __name__ == "__main__":
    main()
