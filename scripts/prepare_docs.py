#!/usr/bin/env python3
"""Build localized documentation sources from the two canonical READMEs."""
from pathlib import Path
import re, sys
root = Path(__file__).resolve().parents[1]
base = "https://github.com/Alox1d/dateparser-kotlin/blob/main/"
metadata = {
    "en": {
        "title": "DateParser Kotlin - Natural-language date and time parsing for KMP",
        "description": "Offline natural-language date and time parsing for Kotlin Multiplatform. English and Russian. Android, JVM and iOS."
    },
    "ru": {
        "title": "DateParser Kotlin - Распознавание дат и времени из текста для KMP",
        "description": "Парсер дат и времени из текста для Kotlin Multiplatform. Работает локально. Русский и английский. Android, JVM и iOS."
    }
}
for lang, filename in (("en", "README.md"), ("ru", "README.ru.md")):
    text = (root / filename).read_text()
    def link(match):
        label, url = match.group(1), match.group(2)
        if url == "README.md": url = "../en/index.md"
        elif url == "README.ru.md": url = "../ru/index.md"
        elif url.startswith("website/assets/"):
            # A README path is repository-relative; a localized MkDocs path is not.
            url = "../assets/" + url.removeprefix("website/assets/")
        elif not re.match(r"https?://|#", url): url = base + url
        return "[" + label + "](" + url + ")"
    text = re.sub(r"\[([^]]+)\]\(([^)]+)\)", link, text)
    front_matter = "\n".join(key + ": " + value for key, value in metadata[lang].items())
    text = "---\n" + front_matter + "\n---\n\n" + text
    target = root / "website" / lang / "index.md"
    if "--check" in sys.argv:
        if not target.exists() or target.read_text() != text: raise SystemExit("Stale docs: " + str(target))
    else:
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text)
