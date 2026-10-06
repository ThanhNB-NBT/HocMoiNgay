# Chạy từ gốc repo: python tools/gen_icon.py → ghi icon ngày (drawable/) + đêm (drawable-night/) + build/icon-preview.html.
# Icon Bình minh cắt giấy (adaptive icon): mặt trời/trăng sau bốn lớp đồi. Hình dáng chung, chỉ đổi bảng màu.
# Vector drawable không có filter bóng đổ: bóng giấy = bản sao mờ đặt lệch của từng lớp.
import math
import os

RES = 'app/src/main/res/'
SCALE = 0.8  # bố cục vẽ trên vùng 90, thu về vùng 72/108 mà launcher thực sự hiển thị
HILLS = ['M-30,62Q28,48 54,60T108,54T166,50V150H-30Z',
         'M-30,72Q34,60 62,70T108,66T150,64V150H-30Z',
         'M-30,82Q30,74 56,82T108,78T160,76V150H-30Z',
         'M-30,92Q36,86 62,92T108,90T150,90V150H-30Z']
SUN = 'M54,30a18,18 0 1,0 0.01,0Z'


def crescent(cx=54, cy=48, r=18, ox=61, oy=43, ro=15):
    """Trăng lưỡi liềm = đĩa (cx,cy,r) trừ đĩa (ox,oy,ro): cung ngoài phần không bị che + cung trong quay lại."""
    d = math.dist((cx, cy), (ox, oy))
    a = (r * r - ro * ro + d * d) / (2 * d)
    h = math.sqrt(r * r - a * a)
    mx, my = cx + a * (ox - cx) / d, cy + a * (oy - cy) / d
    p1 = (mx + h * (oy - cy) / d, my - h * (ox - cx) / d)
    p2 = (mx - h * (oy - cy) / d, my + h * (ox - cx) / d)
    f = lambda p: f'{p[0]:.2f},{p[1]:.2f}'
    return f'M{f(p1)}A{r},{r} 0 1,0 {f(p2)}A{ro},{ro} 0 0,1 {f(p1)}Z'


def star(x, y, s):  # sao 4 cánh
    return f'M{x},{y - s}Q{x},{y} {x + s},{y}Q{x},{y} {x},{y + s}Q{x},{y} {x - s},{y}Q{x},{y} {x},{y - s}Z'


THEMES = {
    'drawable': dict(
        note='Bình minh cắt giấy: mặt trời mọc sau bốn lớp đồi — mỗi ngày một bình minh, học đều như mặt trời mọc.',
        sky=('#FFF1C9', '#FFD3A8'), sky_note='Trời bình minh: vàng nhạt chuyển cam đào.',
        orb=SUN, orb_color='#FFC93C', stars=[],
        hills=['#FF9466', '#F5525E', '#B83262', '#6B1F5C']),
    'drawable-night': dict(
        note='Bản đêm (máy bật chế độ tối): trăng lưỡi liềm và sao sau bốn lớp đồi tím than, cùng bố cục bản ngày.',
        sky=('#1C1E4A', '#3B2C6E'), sky_note='Trời đêm: xanh than chuyển tím.',
        orb=crescent(), orb_color='#FFE9A8', stars=[star(30, 34, 3.2), star(80, 28, 2.6), star(86, 46, 2)],
        hills=['#6A4BA8', '#4F3590', '#352370', '#1F1548']),
}

HEAD = ('<?xml version="1.0" encoding="utf-8"?>\n<!-- {c} Sinh bằng tools/gen_icon.py, sửa ở đó. -->\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">\n'
        f'    <group android:pivotX="54" android:pivotY="54" android:scaleX="{SCALE}" android:scaleY="{SCALE}">\n')
TAIL = '    </group>\n</vector>\n'


def argb(c, a='FF'):
    return '#' + a + c[1:]


def layers(t):
    """(pathData, màu #RRGGBB, độ mờ 0..1, dịch dọc) theo thứ tự vẽ — dùng chung cho XML và trang xem thử."""
    out = [(s, '#FFF6D6', .9, 0) for s in t['stars']]
    out += [(t['orb'], '#000000', .19, 1.6), (t['orb'], '#000000', .1, .8), (t['orb'], t['orb_color'], 1, 0)]
    for d, c in zip(HILLS, t['hills']):
        out += [(d, '#000000', .15, -1.4), (d, '#000000', .1, -.7), (d, c, 1, 0)]
    return out


def xml_path(d, c, op, dy):
    p = f'<path android:fillColor="{argb(c, format(round(op * 255), "02X"))}" android:pathData="{d}" />'
    return f'        <group android:translateY="{dy}">\n            {p}\n        </group>\n' if dy else f'        {p}\n'


def write(folder, name, s):
    os.makedirs(RES + folder, exist_ok=True)
    open(RES + folder + '/' + name, 'w', encoding='utf-8', newline='\n').write(s)


for folder, t in THEMES.items():
    write(folder, 'ic_launcher_foreground.xml', HEAD.format(c=t['note']) + ''.join(xml_path(*l) for l in layers(t)) + TAIL)
    write(folder, 'ic_launcher_background.xml',
          f'<?xml version="1.0" encoding="utf-8"?>\n<!-- {t["sky_note"]} Sinh bằng tools/gen_icon.py. -->\n'
          f'<shape xmlns:android="http://schemas.android.com/apk/res/android">\n'
          f'    <gradient android:angle="270" android:startColor="{argb(t["sky"][0])}" android:endColor="{argb(t["sky"][1])}" />\n</shape>\n')

# Icon theo màu hệ thống (Android 13+) chỉ dùng kênh alpha, hệ thống tự tô sáng/tối → một bản cho cả hai chế độ.
MONO_LINES = 'M16,72Q36,64 56,71T94,68M24,86Q42,80 62,85T88,84'
write('drawable', 'ic_launcher_monochrome.xml',
      HEAD.format(c='Icon theo màu hệ thống (Android 13+): chỉ dùng kênh alpha, nên vẽ mặt trời đặc + hai nét đồi.')
      + f'        <path android:fillColor="#FF000000" android:pathData="{SUN}" />\n'
      + f'        <path android:strokeColor="#FF000000" android:strokeWidth="5" android:strokeLineCap="round" android:pathData="{MONO_LINES}" />\n'
      + TAIL)


# Xem thử: dựng lại đúng các lớp trên, cắt theo vùng hiển thị 18..90
def svg(t, size, mask, k):
    clip = '<circle cx="54" cy="54" r="36"/>' if mask == 'c' else '<rect x="18" y="18" width="72" height="72" rx="17"/>'
    body = f'<rect width="108" height="108" fill="url(#s{k})"/>' + ''.join(
        f'<path d="{d}" fill="{c}" fill-opacity="{op}" transform="translate(0,{dy})"/>' for d, c, op, dy in layers(t))
    return (f'<svg width="{size}" height="{size}" viewBox="18 18 72 72"><defs><linearGradient id="s{k}" x1="0" y1="0" x2="0" y2="1">'
            f'<stop offset="0" stop-color="{t["sky"][0]}"/><stop offset="1" stop-color="{t["sky"][1]}"/></linearGradient>'
            f'<clipPath id="k{k}">{clip}</clipPath></defs><g clip-path="url(#k{k})">'
            f'<g transform="translate(54 54) scale({SCALE}) translate(-54 -54)">{body}</g></g></svg>')


os.makedirs('build', exist_ok=True)
rows = [f'<div style="display:flex;gap:16px;align-items:end;padding:16px;background:{bg}">'
        + ''.join(svg(t, s, m, f'{f}{s}{m}'.replace('-', '')) for s, m in ((140, 'c'), (140, 's'), (48, 'c'), (48, 's'))) + '</div>'
        for (f, t), bg in zip(THEMES.items(), ('linear-gradient(135deg,#9cc3e6,#e8c8d8)', 'linear-gradient(135deg,#10131c,#2a2238)'))]
open('build/icon-preview.html', 'w', encoding='utf-8').write('<html><meta charset="utf-8"><body style="margin:0">' + ''.join(rows) + '</body></html>')
