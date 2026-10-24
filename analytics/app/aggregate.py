import re
import statistics
from typing import List, Dict, Any, Optional, Tuple
from app.schemas import (
    QuestionModel,
    ResponseRowModel,
    QuestionStatModel,
    GroupsModel,
    GroupItemModel,
    TextThemeTermModel,
)
from app.readiness import normalize_answer

STOP_WORDS = {
    "a", "about", "above", "after", "again", "against", "all", "am", "an", "and",
    "any", "are", "aren't", "as", "at", "be", "because", "been", "before", "being",
    "below", "between", "both", "but", "by", "can't", "cannot", "could", "couldn't",
    "did", "didn't", "do", "does", "doesn't", "doing", "don't", "down", "during",
    "each", "few", "for", "from", "further", "had", "hadn't", "has", "hasn't",
    "have", "haven't", "having", "he", "he'd", "he'll", "he's", "her", "here",
    "here's", "hers", "herself", "him", "himself", "his", "how", "how's", "i",
    "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is", "isn't", "it", "it's",
    "its", "itself", "let's", "me", "more", "most", "mustn't", "my", "myself",
    "no", "nor", "not", "of", "off", "on", "once", "only", "or", "other", "ought",
    "our", "ours", "ourselves", "out", "over", "own", "same", "shan't", "she",
    "she'd", "she'll", "she's", "should", "shouldn't", "so", "some", "such",
    "than", "that", "that's", "the", "their", "theirs", "them", "themselves",
    "then", "there", "there's", "these", "they", "they'd", "they'll", "they're",
    "they've", "this", "those", "through", "to", "too", "under", "until", "up",
    "very", "was", "wasn't", "we", "we'd", "we'll", "we're", "we've", "were",
    "weren't", "what", "what's", "when", "when's", "where", "where's", "which",
    "while", "who", "who's", "whom", "why", "why's", "with", "won't", "would",
    "wouldn't", "you", "you'd", "you'll", "you're", "you've", "your", "yours",
    "yourself", "yourselves"
}

def compute_question_stat(q: QuestionModel, responses: List[ResponseRowModel]) -> QuestionStatModel:
    """Computes aggregate statistics for a single question across all responses."""
    q_type = q.type.upper() if q.type else ""
    key = q.key

    # Extract valid answers for this question
    answers = []
    for r in responses:
        if r.answers and key in r.answers and r.answers[key] is not None:
            answers.append(r.answers[key])

    n = len(answers)

    if q_type == "RATING":
        if n == 0:
            return QuestionStatModel(key=key, type=q_type, n=0, mean=0.0, median=0.0, norm_mean=0.0, distribution={})
        num_vals = []
        for a in answers:
            try:
                num_vals.append(float(a))
            except (ValueError, TypeError):
                pass

        if not num_vals:
            return QuestionStatModel(key=key, type=q_type, n=0, mean=0.0, median=0.0, norm_mean=0.0, distribution={})

        mean_val = round(sum(num_vals) / len(num_vals), 2)
        med_val = round(statistics.median(num_vals), 2)

        min_val = float(q.rating.min) if q.rating and q.rating.min is not None else 1.0
        max_val = float(q.rating.max) if q.rating and q.rating.max is not None else 5.0
        if max_val == min_val:
            norm_mean = 100.0
        else:
            norm_mean = round(max(0.0, min(100.0, (mean_val - min_val) / (max_val - min_val) * 100.0)), 1)

        # Full distribution of counts from min to max
        dist: Dict[str, int] = {}
        curr = int(min_val)
        while curr <= int(max_val):
            dist[str(curr)] = 0
            curr += 1
        for val in num_vals:
            k = str(int(round(val)))
            dist[k] = dist.get(k, 0) + 1

        return QuestionStatModel(
            key=key,
            type=q_type,
            n=n,
            mean=mean_val,
            median=med_val,
            norm_mean=norm_mean,
            distribution=dist,
        )

    if q_type == "SINGLE_CHOICE":
        if n == 0:
            return QuestionStatModel(key=key, type=q_type, n=0, distribution={}, percentages={})
        counts: Dict[str, int] = {}
        if q.options:
            for opt in q.options:
                counts[str(opt.value)] = 0

        for a in answers:
            val_str = str(a)
            counts[val_str] = counts.get(val_str, 0) + 1

        percentages: Dict[str, float] = {
            k: round(v / n, 2) for k, v in counts.items()
        }

        # If options have scores, compute normalized mean
        norm_mean = None
        if q.options and any(opt.score is not None for opt in q.options):
            score_map = {str(opt.value): float(opt.score) for opt in q.options if opt.score is not None}
            scored_vals = [score_map[str(a)] for a in answers if str(a) in score_map]
            if scored_vals:
                norm_mean = round(sum(scored_vals) / len(scored_vals), 1)

        return QuestionStatModel(
            key=key,
            type=q_type,
            n=n,
            norm_mean=norm_mean,
            distribution=counts,
            percentages=percentages,
        )

    if q_type == "MULTI_CHOICE":
        if n == 0:
            return QuestionStatModel(key=key, type=q_type, n=0, distribution={}, percentages={})
        counts: Dict[str, int] = {}
        if q.options:
            for opt in q.options:
                counts[str(opt.value)] = 0

        for a in answers:
            if isinstance(a, list):
                for item in a:
                    val_str = str(item)
                    counts[val_str] = counts.get(val_str, 0) + 1
            else:
                val_str = str(a)
                counts[val_str] = counts.get(val_str, 0) + 1

        percentages = {k: round(v / n, 2) for k, v in counts.items()}

        return QuestionStatModel(
            key=key,
            type=q_type,
            n=n,
            distribution=counts,
            percentages=percentages,
        )

    if q_type == "YES_NO":
        if n == 0:
            return QuestionStatModel(key=key, type=q_type, n=0, share_yes=0.0, norm_mean=0.0, distribution={"yes": 0, "no": 0})
        yes_cnt = 0
        no_cnt = 0
        for a in answers:
            if a is True or str(a).strip().lower() in ("true", "yes", "1"):
                yes_cnt += 1
            else:
                no_cnt += 1

        share_yes = round(yes_cnt / n, 2)
        norm_mean = round(share_yes * 100.0, 1)

        return QuestionStatModel(
            key=key,
            type=q_type,
            n=n,
            share_yes=share_yes,
            norm_mean=norm_mean,
            distribution={"yes": yes_cnt, "no": no_cnt},
        )

    if q_type == "NUMBER":
        if n == 0:
            return QuestionStatModel(key=key, type=q_type, n=0, mean=0.0, median=0.0, histogram={})
        num_vals = []
        for a in answers:
            try:
                num_vals.append(float(a))
            except (ValueError, TypeError):
                pass

        if not num_vals:
            return QuestionStatModel(key=key, type=q_type, n=0, mean=0.0, median=0.0, histogram={})

        mean_val = round(sum(num_vals) / len(num_vals), 2)
        med_val = round(statistics.median(num_vals), 2)

        # 5-bin histogram
        min_v = min(num_vals)
        max_v = max(num_vals)
        hist: Dict[str, int] = {}
        if max_v == min_v:
            hist[f"{min_v:.0f}"] = len(num_vals)
        else:
            bin_width = (max_v - min_v) / 5.0
            for i in range(5):
                b_start = min_v + i * bin_width
                b_end = min_v + (i + 1) * bin_width
                label = f"{b_start:.1f}-{b_end:.1f}"
                hist[label] = 0

            for v in num_vals:
                bin_idx = int((v - min_v) / bin_width)
                if bin_idx >= 5:
                    bin_idx = 4
                b_start = min_v + bin_idx * bin_width
                b_end = min_v + (bin_idx + 1) * bin_width
                label = f"{b_start:.1f}-{b_end:.1f}"
                hist[label] = hist.get(label, 0) + 1

        return QuestionStatModel(
            key=key,
            type=q_type,
            n=n,
            mean=mean_val,
            median=med_val,
            histogram=hist,
        )

    # TEXT or other
    return QuestionStatModel(key=key, type=q_type, n=n)

def compute_satisfaction_index(question_stats: List[QuestionStatModel]) -> Optional[float]:
    """
    Section 7, Step 4:
    A satisfaction index: the mean of all rating questions normalised to 0 to 100.
    """
    rating_stats = [q for q in question_stats if q.type == "RATING" and q.norm_mean is not None and q.n > 0]
    if rating_stats:
        return round(sum(q.norm_mean for q in rating_stats) / len(rating_stats), 1)

    # Fallback to any questions with norm_mean if no rating questions
    all_scored = [q for q in question_stats if q.norm_mean is not None and q.n > 0]
    if all_scored:
        return round(sum(q.norm_mean for q in all_scored) / len(all_scored), 1)

    return None

def apply_k_suppression(group_data: Dict[str, Dict[str, Any]], k: int = 5) -> Tuple[Dict[str, GroupItemModel], List[str]]:
    """
    Applies the Section 5 & 7 K-suppression rule:
    1. Any group with fewer than K (5) responses is hidden.
    2. If exactly ONE group is hidden, the next smallest visible group is hidden too
       to prevent subtraction from total.
    Returns: (visible_groups, suppressed_names)
    """
    if not group_data:
        return {}, []

    hidden = [g for g, d in group_data.items() if d["n"] < k]
    visible = [g for g, d in group_data.items() if d["n"] >= k]

    # Rule: If exactly one group is hidden and visible groups exist,
    # hide the next smallest visible group too.
    if len(hidden) == 1 and visible:
        smallest_visible = min(visible, key=lambda g: group_data[g]["n"])
        hidden.append(smallest_visible)
        visible.remove(smallest_visible)

    suppressed = sorted(hidden)
    visible_result = {}
    for g in visible:
        visible_result[g] = GroupItemModel(
            n=group_data[g]["n"],
            index=group_data[g].get("index"),
        )

    return visible_result, suppressed

def compute_group_breakdowns(
    responses: List[ResponseRowModel],
    questions: List[QuestionModel],
) -> GroupsModel:
    """Computes breakdowns by branch and by year with K=5 suppression."""
    branch_map: Dict[str, List[ResponseRowModel]] = {}
    year_map: Dict[str, List[ResponseRowModel]] = {}

    for r in responses:
        if r.branch:
            b_key = str(r.branch).strip().upper()
            branch_map.setdefault(b_key, []).append(r)
        if r.year is not None:
            y_key = str(r.year).strip()
            year_map.setdefault(y_key, []).append(r)

    # Calculate index per group
    rating_questions = [q for q in questions if q.type == "RATING"]

    def calc_group_index(resps: List[ResponseRowModel]) -> Optional[float]:
        if not rating_questions or not resps:
            return None
        norms = []
        for r in resps:
            for q in rating_questions:
                if r.answers and q.key in r.answers:
                    n_val = normalize_answer(q, r.answers[q.key])
                    if n_val is not None:
                        norms.append(n_val)
        if norms:
            return round(sum(norms) / len(norms), 1)
        return None

    raw_branch_data = {}
    for b, resps in branch_map.items():
        raw_branch_data[b] = {
            "n": len(resps),
            "index": calc_group_index(resps),
        }

    raw_year_data = {}
    for y, resps in year_map.items():
        raw_year_data[y] = {
            "n": len(resps),
            "index": calc_group_index(resps),
        }

    vis_branch, supp_branch = apply_k_suppression(raw_branch_data, k=5)
    vis_year, supp_year = apply_k_suppression(raw_year_data, k=5)

    all_suppressed = sorted(list(set(supp_branch + supp_year)))

    return GroupsModel(
        branch=vis_branch,
        year=vis_year,
        suppressed=all_suppressed,
    )

def compute_text_themes(
    questions: List[QuestionModel],
    responses: List[ResponseRowModel],
) -> Dict[str, List[TextThemeTermModel]]:
    """
    Section 7, Step 4:
    For TEXT questions, extracts the 10 most common meaningful words
    (stop words removed, used by at least 3 DIFFERENT respondents).
    Never returns full answers.
    """
    text_questions = [q for q in questions if q.type and q.type.upper() == "TEXT"]
    results: Dict[str, List[TextThemeTermModel]] = {}

    for q in text_questions:
        key = q.key
        doc_freq: Dict[str, set] = {}
        term_count: Dict[str, int] = {}

        for idx, r in enumerate(responses):
            if r.answers and key in r.answers and r.answers[key]:
                text = str(r.answers[key]).lower()
                words = re.findall(r"\b[a-z]{3,}\b", text)
                unique_words_in_doc = set()
                for w in words:
                    if w not in STOP_WORDS:
                        term_count[w] = term_count.get(w, 0) + 1
                        unique_words_in_doc.add(w)

                for w in unique_words_in_doc:
                    doc_freq.setdefault(w, set()).add(idx)

        # Filter by at least 3 distinct respondents
        qualifying_terms = [
            (term, term_count[term])
            for term, respondents in doc_freq.items()
            if len(respondents) >= 3
        ]

        # Sort by total count descending, then alphabetical
        qualifying_terms.sort(key=lambda x: (-x[1], x[0]))

        top_10 = qualifying_terms[:10]
        results[key] = [
            TextThemeTermModel(term=term, count=cnt)
            for term, cnt in top_10
        ]

    return results
