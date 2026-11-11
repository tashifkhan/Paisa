"""Rewrites common JVM idioms in Kotlin files to Paisa's common platform helpers."""
import re, sys

def ensure_import(s, imp):
    if re.search(r'^import ' + re.escape(imp) + r'$', s, re.M):
        return s
    m = re.search(r'^package [^\n]+\n', s, re.M)
    # put after the package line's following blank line, before first import
    first = re.search(r'^import ', s, re.M)
    if first:
        return s[:first.start()] + f'import {imp}\n' + s[first.start():]
    return s[:m.end()] + f'\nimport {imp}\n' + s[m.end():]

def convert(s):
    o = s
    # Locale-sensitive case changes: Kotlin's are locale-invariant.
    s = re.sub(r'\.(lowercase|uppercase)\(Locale\.(getDefault\(\)|ROOT|US|ENGLISH)\)', r'.\1()', s)
    s = re.sub(r'\.(lowercase|uppercase)\(java\.util\.Locale\.(getDefault\(\)|ROOT|US)\)', r'.\1()', s)
    s = s.replace('System.currentTimeMillis()', 'nowMillis()')
    # SimpleDateFormat(...).format(Date()) and .format(Date(x))
    def fmt(m):
        pat, loc, arg = m.group(1), m.group(2), m.group(3)
        machine = ', machine = true' if 'US' in loc else ''
        millis = 'nowMillis()' if arg.strip() == '' else arg.strip()
        return f'formatDate({millis}, "{pat}"{machine})'
    s = re.sub(r'SimpleDateFormat\("([^"]*)",\s*((?:java\.util\.)?Locale\.(?:getDefault\(\)|US|ROOT))\)\s*\.format\(\s*(?:java\.util\.)?Date\(([^()]*(?:\([^()]*\))?[^()]*)\)\s*\)', fmt, s)
    # "%.Nf".format(x) and friends
    s = re.sub(r'"%\.(\d)f"\.format\(([^()]*(?:\([^()]*\))?[^()]*)\)', lambda m: f'formatFixed({m.group(2).strip()}, {m.group(1)})', s)
    s = re.sub(r'"%\.(\d)f ([^"%]*)"\.format\(([^()]*(?:\([^()]*\))?[^()]*)\)', lambda m: f'"${{formatFixed({m.group(3).strip()}, {m.group(1)})}} {m.group(2)}"', s)
    s = re.sub(r'String\.format\(Locale\.US, "%\.(\d)f", ([^()]*)\)', lambda m: f'formatFixed({m.group(2).strip()}, {m.group(1)}, machine = true)', s)
    s = re.sub(r'Math\.toDegrees\(', 'radiansToDegrees(', s)
    s = re.sub(r'(?:android\.graphics\.)?Color\.parseColor\(', 'parseColor(', s)
    s = re.sub(r'\bBackHandler\(', 'PlatformBackHandler(', s)
    s = re.sub(r'^import androidx\.activity\.compose\.BackHandler\n', '', s, flags=re.M)
    # Calendar
    s = re.sub(r'^import java\.util\.Calendar\n', '', s, flags=re.M)
    s = re.sub(r'(?:java\.util\.)?Calendar\.getInstance\((?:java\.util\.)?TimeZone\.getTimeZone\("UTC"\)\)', 'PaisaCalendar.getUtcInstance()', s)
    s = re.sub(r'\.clone\(\) as (?:java\.util\.)?Calendar\b', '.clone()', s)
    s = re.sub(r'(?<![\w.])(?:java\.util\.)?Calendar\b', 'PaisaCalendar', s)
    if s != o:
        if 'nowMillis(' in s: s = ensure_import(s, 'codes.tashif.paisa.platform.nowMillis')
        if 'formatDate(' in s: s = ensure_import(s, 'codes.tashif.paisa.platform.formatDate')
        if 'formatFixed(' in s: s = ensure_import(s, 'codes.tashif.paisa.platform.formatFixed')
        if 'radiansToDegrees(' in s: s = ensure_import(s, 'codes.tashif.paisa.platform.radiansToDegrees')
        if re.search(r'(?<![\w.])parseColor\(', s): s = ensure_import(s, 'codes.tashif.paisa.ui.platform.parseColor')
        if 'PlatformBackHandler(' in s: s = ensure_import(s, 'codes.tashif.paisa.ui.platform.PlatformBackHandler')
        if 'PaisaCalendar' in s: s = ensure_import(s, 'codes.tashif.paisa.platform.PaisaCalendar')
        # Drop JVM imports that are no longer referenced.
        for imp, name in [('java.util.Calendar', 'Calendar'), ('java.util.Locale', 'Locale'),
                          ('java.util.Date', 'Date'), ('java.text.SimpleDateFormat', 'SimpleDateFormat'),
                          ('java.util.TimeZone', 'TimeZone'), ('codes.tashif.paisa.data.sql.nowMillis', None),
                          ('codes.tashif.paisa.data.sql.randomHexId', None)]:
            line = f'import {imp}\n'
            if line in s:
                body = s.replace(line, '')
                if name is None or not re.search(r'(?<![\w.])' + name + r'\b', body):
                    s = body
    return s

for path in sys.argv[1:]:
    s = open(path).read()
    t = convert(s)
    if t != s:
        open(path, 'w').write(t)
        print('converted', path)
