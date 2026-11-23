import hashlib
import importlib
import struct
import zlib
from urllib.parse import quote
import os
from pathlib import Path
import re
import shlex
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

repo = Path.cwd()
base = Path(os.environ.get('DZLOG_QA_OUTPUT', 'pr122-android-evidence')).resolve()
apk=repo/'app/build/outputs/apk/debug/app-debug.apk'
pkg='com.dudoziworkshop.dzlog.qa.rename122fix'
out=base/'runtime-evidence'
out.mkdir(parents=True, exist_ok=True)
os.chdir(out)
sys.path.insert(0,str(repo/'.github/scripts'))
journey=importlib.import_module('android16_journey_smoke')
media=importlib.import_module('android16_capture_media_smoke')
journey.DEVICE_XML='/data/local/tmp/dzlog-pr122-ui.xml'
adb=journey.adb
adb_binary='adb'
test_path='Pictures/DZlog/PR122QA/'
fixtures=[
    (test_path,'Draper_flower_010.png',(82,174,91)),
    (test_path,'Draper_branch_010.png',(172,87,93)),
    (test_path,'Duke_branch_011.png',(91,92,178)),
    (test_path+'original/','Draper_flower_010.png',(172,167,81)),
    (test_path+'original/','Duke_branch_011.png',(34,107,130)),
    (test_path+'original/','Draper_leaf_010 (2).png',(56,90,90)),
]
def shell(*args,timeout=40):
    return adb('shell',shlex.join(args),timeout=timeout)

def poll_ui(expect, timeout=65):
    end=time.monotonic()+timeout
    while time.monotonic()<end:
        root=journey.hierarchy()
        for n in root.iter('node'):
            if expect(n): return root,n
        time.sleep(1)
    raise AssertionError('no matching node: '+str([(n.get('text'),n.get('resource-id')) for n in root.iter('node') if n.get('text') or n.get('resource-id')][:48]))

def label(n):
    return n.get('text') or n.get('content-desc') or ''

def selector(*labels):
    wanted={x.casefold() for x in labels}
    return lambda n: label(n).casefold() in wanted

def bounds(node):
    values=tuple(map(int,re.findall(r'\d+',node.get('bounds',''))))
    assert len(values)==4 and values[2]>values[0] and values[3]>values[1], values
    return values

def box(root,node):
    parent={c:n for n in root.iter() for c in n}
    current=node
    while current in parent and current.get('clickable')!='true':
        current=parent[current]
    return bounds(current if current.get('clickable')=='true' else node)

def tap_matching(f, timeout=65, check_nav=False):
    root,node=poll_ui(f,timeout)
    x1,y1,x2,y2=box(root,node)
    if check_nav: assert_nav_clear((x1,y1,x2,y2))
    shell('input','tap',str((x1+x2)//2),str((y1+y2)//2))
    return label(node)

def tap(*labels,timeout=65,check_nav=False):
    return tap_matching(selector(*labels),timeout,check_nav)

def checkpoint(name):
    root=journey.hierarchy()
    ET.ElementTree(root).write(out/f'{name}.xml',encoding='utf-8')
    data=subprocess.run([str(adb_binary),'exec-out','screencap','-p'],capture_output=True,check=True,timeout=40)
    (out/f'{name}.png').write_bytes(data.stdout)
    return root

def assert_nav_clear(action):
    raw=shell('dumpsys','window','displays')
    (out/'latest-window-insets.txt').write_text(raw)
    nav=[]
    for line in raw.splitlines():
        if 'type=navigationBars' in line and ('visible=true' in line or 'visible=1' in line):
            frame=re.search(r'frame=\[(\d+),(\d+)\]\[(\d+),(\d+)\]',line)
            if frame:
                x1,y1,x2,y2=map(int,frame.groups())
                if x1<x2 and y1<y2: nav.append((x1,y1,x2,y2))
    assert nav,'No visible system navigation inset'
    x1,y1,x2,y2=action
    assert all(x2<=a1 or a2<=x1 or y2<=b1 or b2<=y1 for a1,b1,a2,b2 in nav),(action,nav)
    print('PASS action outside real navigation bounds',action,nav,flush=True)

def sample_png(color):
    def chunk(name, data):
        return struct.pack(">I",len(data)) + name + data + struct.pack(">I", zlib.crc32(name+data))
    width = height = 32
    scanline = b"\0" + bytes(color)*width
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB",width,height,8,2,0,0,0))
            + chunk(b"IDAT", zlib.compress(scanline*height)) + chunk(b"IEND", b""))


def insert_fixtures():
    hashes={}
    for index,(directory,name,color) in enumerate(fixtures):
        data=sample_png(color)
        local=out/f'fixture-{index}'
        local.write_bytes(data)
        full='/storage/emulated/0/'+directory+name
        shell('mkdir','-p','/storage/emulated/0/'+directory)
        adb('push',str(local),full)
        path=quote(full, safe='/')
        shell('am','broadcast','-a','android.intent.action.MEDIA_SCANNER_SCAN_FILE','-d','file://'+path)
        hashes[(directory,name)]=hashlib.sha256(data).hexdigest()
    for _ in range(12):
        current={(r['relative_path'],r['_display_name']) for r in media.query_media() if r['relative_path'].startswith(test_path)}
        if current==set(hashes): return hashes
        time.sleep(3)
    raise AssertionError((current,set(hashes)))

def assert_media(expected):
    rows=[r for r in media.query_media() if r['relative_path'].startswith(test_path)]
    assert {(r['relative_path'],r['_display_name']) for r in rows}==set(expected),rows
    for (directory,name),sha in expected.items():
        full='/storage/emulated/0/'+directory+name
        ret=subprocess.run([str(adb_binary),'exec-out',shlex.join(['cat',full])],capture_output=True,check=True,timeout=30)
        assert hashlib.sha256(ret.stdout).hexdigest()==sha, full

def onboarding():
    actions = (
        '시작하기','사용 준비하기','Pictures 폴더 선택하기',
        'DZlog 시작하기','다음','나중에','안내 닫기',
        '폴더 선택 화면으로','DZlog 폴더 연결',
        'Use this folder','이 폴더 사용','Allow','허용','Pictures','Wait',
    )
    for _ in range(19):
        root, node = poll_ui(selector(*actions,'앨범 보기','앨범 보기 >'), timeout=65)
        visible = {label(n).casefold() for n in root.iter('node')}
        if visible.intersection(('앨범 보기','앨범 보기 >')):
            checkpoint('01-home')
            print('PASS fresh install, Pictures SAF grant and starter to home',flush=True)
            return
        if any('documentsui' in (n.get('package') or '').lower() for n in root.iter('node')):
            priorities = ('Allow','허용')
            if journey.contains(root,"use this folder") or "use this folder" in visible or '이 폴더 사용' in visible:
                enabled = [n for n in root.iter('node') if selector('Use this folder','이 폴더 사용')(n)
                           and n.get('enabled') == 'true']
                priorities += ('Use this folder','이 폴더 사용','Pictures') if enabled else ('Pictures',)
            else:
                priorities += ('Pictures',)
        else:
            priorities = ('Wait','Pictures 폴더 선택하기','사용 준비하기','DZlog 시작하기',
                          '다음','시작하기','나중에','안내 닫기','폴더 선택 화면으로','DZlog 폴더 연결')
        options = [s for s in priorities if s.casefold() in visible]
        if not options:
            raise AssertionError(('Unexpected onboarding',label(node),sorted(visible)))
        chosen = next(n for n in root.iter('node') if selector(options[0])(n))
        x1,y1,x2,y2 = box(root,chosen)
        shell('input','tap',str((x1+x2)//2),str((y1+y2)//2))
        print('onboarding:', options[0],flush=True)
    raise AssertionError('onboarding did not reach home')

def prepare_ui():
    assert shell('getprop','ro.build.version.sdk').strip() in ('35','36')
    shell('input','keyevent','KEYCODE_WAKEUP')
    shell('wm','dismiss-keyguard')
    for k in ('window_animation_scale','transition_animation_scale','animator_duration_scale'):
        shell('settings','put','global',k,'0')
    shell('wm','size','640x1280')
    shell('wm','density','320')
    shell('settings','put','system','font_scale','1.35')
    shell('cmd','overlay','enable-exclusive','--user','0','--category','com.android.internal.systemui.navbar.threebutton')
    assert '[x] com.android.internal.systemui.navbar.threebutton' in shell('cmd','overlay','list','--user','0','android')
    adb('install','-r',str(apk),timeout=200)
    print('PASS verified debug APK installed',flush=True)
    for p in ('android.permission.CAMERA','android.permission.READ_MEDIA_IMAGES'):
        shell('pm','grant',pkg,p)
    shell('am','start','-W','-n',f'{pkg}/com.dudoziworkshop.dzlog.MainActivity',timeout=80)

def position_preview(replacement='leaf'):
    tap('앨범 보기','앨범 보기 >')
    tap('PR122QA')
    tap('선택')
    tap('전체 선택')
    tap('이름 변경')
    tap('같은 위치')
    tap_matching(lambda n: label(n).startswith('2. '))
    tap('새 문구')
    shell('input','text',replacement)
    checkpoint('02-position-with-ime')
    ime=shell('dumpsys','input_method')
    assert re.search(r'(?:mInputShown|mIsInputViewShown|mShowRequested)=true',ime), 'Keyboard was not visible in rename editor'
    tap('미리보기')
    root=poll_ui(selector('이름 변경 미리보기'))[0]
    assert journey.contains(root,'변경 가능 1') and journey.contains(root,'제외 2')
    checkpoint('03-preview')
    print('PASS compact 320dp / font 135% / second slot / default exclusions',flush=True)

def write_permission(allow):
    tap('변경',check_nav=True)
    def button(n):
        ident=(n.get('resource-id') or '').rsplit('/',1)[-1]
        if allow:
            return ident in ('allow_button','permission_allow_button') or label(n).casefold() in ('allow','허용')
        return ident in ('deny_button','permission_deny_button') or label(n).casefold() in ("don't allow",'don’t allow','deny','cancel','허용 안 함','거부','취소')
    checkpoint('04-permission-request' if not allow else '06-permission-retry')
    tap_matching(button,timeout=40)
    if not allow:
        poll_ui(lambda n:'사진 수정 권한이 허용되지 않았습니다' in label(n))
        checkpoint('05-permission-denied')
    else:
        poll_ui(selector('이름 변경 결과'))
        checkpoint('07-result')
        root=poll_ui(lambda n:'변경 성공 파일 2개' in label(n))[0]
        assert journey.contains(root,'성공 1') and journey.contains(root,'제외 2')
        tap('확인',check_nav=True)

def main():
    phase=sys.argv[1] if len(sys.argv)>1 else 'all'
    if phase=='seed':
        insert_fixtures(); return
    if phase in ('setup','all'):
        prepare_ui()
    if phase=='setup': return
    if phase in ('all','resume'):
        for attempt in range(25):
            try:
                shell('mkdir','-p','/storage/emulated/0/Pictures/DZlog')
                break
            except (RuntimeError,subprocess.TimeoutExpired):
                if attempt == 24:
                    raise
                time.sleep(4)
        hashes=insert_fixtures()
        print('PASS real MediaStore PNG fixture index',flush=True)
        onboarding()
        position_preview()
        write_permission(False)
        assert_media(hashes)
        print('PASS Android denied edit: all fixture paths and bytes unchanged',flush=True)
        write_permission(True)
        for directory in (test_path,test_path+'original/'):
            hashes[(directory,'Duke_leaf_011.png')]=hashes.pop((directory,'Duke_branch_011.png'))
        assert_media(hashes)
        print('PASS Android approved edit: paired names and bytes verified, excluded photos unchanged',flush=True)
        shell('am','force-stop',pkg)
        shell('wm','set-ignore-orientation-request','true')
        shell('wm','user-rotation','lock','1')
        shell('am','start','-W','-n',f'{pkg}/com.dudoziworkshop.dzlog.MainActivity',timeout=80)
        root,_=poll_ui(selector('앨범 보기','앨범 보기 >'))
        window=bounds(next(root.iter('node')))
        assert window[2]-window[0]>window[3]-window[1], ('Expected landscape UI',window)
        position_preview('stem')
        root,node=poll_ui(selector('변경'))
        assert_nav_clear(box(root,node))
        checkpoint('08-landscape-preview')
        assert_media(hashes)
        print('PASS landscape rename preview and footer; no additional files changed',flush=True)
    if phase=='inspect':
        checkpoint('inspection')
    if phase=='onboarding':
        onboarding()

if __name__=='__main__':
    try: main()
    except Exception:
        import traceback
        traceback.print_exc()
        try: checkpoint('failed')
        except Exception: pass
        raise
