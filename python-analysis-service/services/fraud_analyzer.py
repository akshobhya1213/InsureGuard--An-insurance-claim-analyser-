"""
Explainable fraud/suspicion scoring engine.

This is a deliberately simple, rule-based module -- easy to read and
explain in an interview. It does NOT use Watson to "detect fraud"; it
scans the raw report text (and, where useful, the Watson keywords) for
concrete textual indicators and adds up point values for each one that
is found. The same input always produces the same score (deterministic),
and different inputs produce different scores (dynamic).

Score is capped at 100 and mapped to three bands:
    0-39   -> LOW RISK
    40-69  -> REVIEW REQUIRED
    70-100 -> SUSPICIOUS
"""

import re

# indicator_name -> (compiled regex patterns that trigger it, points awarded)
INDICATOR_RULES = [
    (
        "Previous damage mentioned",
        25,
        [
            r"\balready\s+(been\s+)?damaged\b",
            r"\bpre[-\s]?existing\s+damage\b",
            r"\bprior\s+damage\b",
            r"\bold\s+damage\b",
        ],
    ),
    (
        "Previous incident mentioned",
        20,
        [
            r"\blast\s+(month|week|year)\b.*\b(accident|incident|hit|crash)\b",
            r"\bprevious\s+(accident|incident|claim)\b",
            r"\bhappened\s+before\b",
        ],
    ),
    (
        "Repeated incident mentioned",
        15,
        [
            r"\bagain\b",
            r"\bsecond\s+time\b",
            r"\bonce\s+more\b",
            r"\brepeated(ly)?\b",
        ],
    ),
    (
        "Suspicious wording",
        15,
        [
            r"\bnot\s+sure\s+(how|what|when)\b",
            r"\bdon'?t\s+remember\b",
            r"\bcan'?t\s+recall\b",
            r"\bsomehow\b",
            r"\bmysteriously\b",
            r"\bno\s+witness(es)?\b",
        ],
    ),
]

MAX_SCORE = 100


def _detect_timeline_contradiction(text: str) -> bool:
    """
    Flags reports that mention the damage/incident happening at two
    different points in time within the same description - e.g. both
    "yesterday" and "last month" attached to a damage/incident event.
    This is a simple heuristic, not a full timeline parser.
    """
    time_markers = [
        r"\byesterday\b",
        r"\btoday\b",
        r"\bthis\s+morning\b",
        r"\blast\s+night\b",
        r"\blast\s+week\b",
        r"\blast\s+month\b",
        r"\blast\s+year\b",
        r"\ba\s+few\s+(days|weeks|months)\s+ago\b",
    ]
    found = set()
    for pattern in time_markers:
        if re.search(pattern, text, re.IGNORECASE):
            found.add(pattern)

    # Distinguishing "recent" markers from "past" markers.
    recent = {r"\byesterday\b", r"\btoday\b", r"\bthis\s+morning\b", r"\blast\s+night\b"}
    past = {r"\blast\s+week\b", r"\blast\s+month\b", r"\blast\s+year\b", r"\ba\s+few\s+(days|weeks|months)\s+ago\b"}

    return bool(found & recent) and bool(found & past)


def analyze_text_for_fraud(text: str) -> dict:
    """
    Scan the report text for suspicious indicators and compute a score.

    Returns:
        {
          "score": int,
          "status": "LOW RISK" | "REVIEW REQUIRED" | "SUSPICIOUS",
          "indicators": [str, ...]
        }
    """
    text = text or ""
    score = 0
    indicators = []

    for name, points, patterns in INDICATOR_RULES:
        if any(re.search(p, text, re.IGNORECASE) for p in patterns):
            score += points
            indicators.append(name)

    if _detect_timeline_contradiction(text):
        score += 25
        indicators.append("Timeline contradiction")

    score = min(score, MAX_SCORE)

    if score >= 70:
        status = "SUSPICIOUS"
    elif score >= 40:
        status = "REVIEW REQUIRED"
    else:
        status = "LOW RISK"

    return {
        "score": score,
        "status": status,
        "indicators": indicators,
    }
