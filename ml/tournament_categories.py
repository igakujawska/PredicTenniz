"""Version-aware main-tour categories; Sackmann level A alone is not an ATP tier.

Reference sources and exceptions are documented in ml/CATEGORIES.md.
"""
ATP_500_IDS = {407, 807, 495, 425, 500, 311, 414, 418, 747, 329, 337, 328, 6932}
UPGRADED_2025 = {424, 451, 308}
LEGACY_IDS = {'M020':339,'M001':338,'M004':807,'M010':440,'M016':741,'M035':418,'M015':747,'M014':438,'M056':8998}
NON_STANDARD_IDS = {96, 7696, 8888, 9210, 9900}  # Olympics, NextGen, ATP/Laver/United Cup.


def category_for(level, tournament_id, year):
    if level == 'G': return 'GRAND_SLAM'
    if level == 'M': return 'MASTERS_1000'
    if level == 'F': return 'FINALS'
    if level != 'A': return None
    code = tournament_id.rsplit('-',1)[-1]
    event_id = int(code) if code.isdigit() else LEGACY_IDS[code]
    if event_id in NON_STANDARD_IDS: return None
    if event_id in ATP_500_IDS or (year >= 2025 and event_id in UPGRADED_2025) or (year == 2022 and event_id == 9410):
        return 'ATP_500'
    return 'ATP_250'
