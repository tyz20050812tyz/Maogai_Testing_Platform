import argparse
import html
import json
import re
from pathlib import Path

from docx import Document


CHAPTER_RE = re.compile(r"^(导论\b|第[一二三四五六七八九十]+章)\s*(.*)$")


def normalize_text(text):
    return re.sub(r"\s+", " ", text or "").strip()


def paragraph_to_html(text):
    escaped = html.escape(text, quote=True)
    if re.match(r"^\d+[.．、]", text):
        return f"<p class=\"knowledge-point\">{escaped}</p>"
    return f"<p>{escaped}</p>"


def extract_sections(docx_path):
    document = Document(str(docx_path))
    sections = []
    current = None

    for paragraph in document.paragraphs:
        text = normalize_text(paragraph.text)
        if not text:
            continue

        match = CHAPTER_RE.match(text)
        if match:
            if current:
                sections.append(current)
            current = {
                "title": text,
                "paragraphs": [],
            }
            continue

        if current:
            current["paragraphs"].append(text)

    if current:
        sections.append(current)

    return sections


def section_to_chapter(section, index):
    lines = [
        "<div class=\"chapter-content\">",
        f"<h1>{html.escape(section['title'], quote=True)}</h1>",
    ]
    lines.extend(paragraph_to_html(text) for text in section["paragraphs"])
    lines.append("</div>")

    return {
        "id": index,
        "title": section["title"],
        "docxPath": "",
        "order": index,
        "content": "\n".join(lines),
    }


def main():
    parser = argparse.ArgumentParser(description="Convert a course outline docx into chapters.json.")
    parser.add_argument(
        "input",
        nargs="?",
        default="src/main/resources/data/2026年春概论课程知识要点.docx",
        help="Path to the source .docx file.",
    )
    parser.add_argument(
        "-o",
        "--output",
        default="src/main/resources/data/chapters.json",
        help="Path to write chapters JSON.",
    )
    args = parser.parse_args()

    input_path = Path(args.input)
    output_path = Path(args.output)
    sections = extract_sections(input_path)
    chapters = [section_to_chapter(section, index + 1) for index, section in enumerate(sections)]

    if not chapters:
        raise SystemExit(f"No chapters found in {input_path}")

    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(chapters, ensure_ascii=False, indent=2), encoding="utf-8")

    print(f"Wrote {len(chapters)} chapters to {output_path}")
    for chapter in chapters:
        print(f"{chapter['order']}. {chapter['title']}")


if __name__ == "__main__":
    main()
