# Chạy từ gốc repo: python tools/gen_icon.py → ghi 3 drawable + build/icon-preview.html (xem thử vùng hiển thị thật).
# Sinh icon Bình minh cho Android (adaptive icon) + trang xem thử đúng vùng hiển thị 72/108.
# Vector drawable không có filter bóng đổ: bóng giấy = bản sao mờ đặt lệch của từng lớp.
import os; os.makedirs('build', exist_ok=True)
R='app/src/main/res/drawable/'
SKY=('#FFF1C9','#FFD3A8')
SUN='#FFC93C'
HILLS=[('M-30,62Q28,48 54,60T108,54T166,50V150H-30Z','#FF9466'),
       ('M-30,72Q34,60 62,70T108,66T150,64V150H-30Z','#F5525E'),
       ('M-30,82Q30,74 56,82T108,78T160,76V150H-30Z','#B83262'),
       ('M-30,92Q36,86 62,92T108,90T150,90V150H-30Z','#6B1F5C')]
SUN_D='M54,30a18,18 0 1,0 0.01,0Z'
SCALE=0.8  # bố cục bản phác (vùng 90) thu về vùng hiển thị 72 quanh tâm 54
def argb(c,a='FF'): return '#'+a+c[1:]
def p(d,col): return f'        <path android:fillColor="{col}" android:pathData="{d}" />\n'
def shadow(d,dy,a): return f'        <group android:translateY="{dy}">\n    {p(d,"#"+a+"000000")}        </group>\n'
fg=shadow(SUN_D,1.6,'30')+shadow(SUN_D,0.8,'1A')+p(SUN_D,argb(SUN))
for d,c in HILLS: fg+=shadow(d,-1.4,'26')+shadow(d,-0.7,'1A')+p(d,argb(c))
head=('<?xml version="1.0" encoding="utf-8"?>\n<!-- {c} Sinh bằng tools/gen_icon.py, sửa ở đó. -->\n'
      '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
      '    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">\n'
      f'    <group android:pivotX="54" android:pivotY="54" android:scaleX="{SCALE}" android:scaleY="{SCALE}">\n')
tail='    </group>\n</vector>\n'
w=lambda f,s: open(R+f,'w',encoding='utf-8',newline='\n').write(s)
w('ic_launcher_foreground.xml', head.format(c='Bình minh cắt giấy: mặt trời mọc sau bốn lớp đồi — mỗi ngày một bình minh, học đều như mặt trời mọc.')+fg+tail)
mono=(f'        <path android:fillColor="#FF000000" android:pathData="{SUN_D}" />\n'
      f'        <path android:strokeColor="#FF000000" android:strokeWidth="5" android:strokeLineCap="round" android:pathData="M16,72Q36,64 56,71T94,68M24,86Q42,80 62,85T88,84" />\n')
w('ic_launcher_monochrome.xml', head.format(c='Icon theo màu hệ thống (Android 13+): chỉ dùng kênh alpha, nên vẽ mặt trời đặc + hai nét đồi.')+mono+tail)
w('ic_launcher_background.xml','<?xml version="1.0" encoding="utf-8"?>\n<!-- Trời bình minh: vàng nhạt chuyển cam đào. Sinh bằng tools/gen_icon.py. -->\n'
  f'<shape xmlns:android="http://schemas.android.com/apk/res/android">\n    <gradient android:angle="270" android:startColor="{argb(SKY[0])}" android:endColor="{argb(SKY[1])}" />\n</shape>\n')
# xem thử: SVG dựng lại đúng các lớp trên, cắt theo vùng hiển thị 18..90
def svg(size,mask,mono_=False):
    clip='<circle cx="54" cy="54" r="36"/>' if mask=='c' else '<rect x="18" y="18" width="72" height="72" rx="17"/>'
    k=f'{size}{mask}{mono_}'
    body=''
    if mono_:
        body=f'<rect width="108" height="108" fill="#D6E3FF"/><g fill="#1B3A6B" stroke="#1B3A6B">{"".join(l.replace("android:fillColor","fill").replace("android:strokeColor","stroke").replace("android:strokeWidth","stroke-width").replace("android:strokeLineCap","stroke-linecap").replace("android:pathData","d").replace("#FF000000","currentColor") for l in mono.splitlines())}</g>'
    else:
        body=f'<rect width="108" height="108" fill="url(#sky{k})"/>'
        def sp(d,dy,a): return f'<path d="{d}" transform="translate(0,{dy})" fill="#000" fill-opacity="{int(a,16)/255:.3f}"/>'
        body+=sp(SUN_D,1.6,'30')+sp(SUN_D,.8,'1A')+f'<path d="{SUN_D}" fill="{SUN}"/>'
        for d,c in HILLS: body+=sp(d,-1.4,'26')+sp(d,-.7,'1A')+f'<path d="{d}" fill="{c}"/>'
    return (f'<svg width="{size}" height="{size}" viewBox="18 18 72 72" style="color:#1B3A6B"><defs><linearGradient id="sky{k}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="{SKY[0]}"/><stop offset="1" stop-color="{SKY[1]}"/></linearGradient>'
            f'<clipPath id="k{k}">{clip}</clipPath></defs><g clip-path="url(#k{k})"><g transform="translate(54 54) scale({SCALE}) translate(-54 -54)">{body}</g></g></svg>')
open('build/icon-preview.html','w',encoding='utf-8').write('<html><meta charset="utf-8"><body style="margin:0;padding:16px;background:linear-gradient(135deg,#2b4a6f,#6b3d5e);display:flex;flex-wrap:wrap;gap:18px;align-items:end">'
  +svg(150,'c')+svg(150,'s')+svg(48,'c')+svg(48,'s')+svg(150,'c',True)+'</body></html>')
