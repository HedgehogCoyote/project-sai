from pathlib import Path
import json, html, re
O=Path(__file__).resolve().parents[1]
screens=[]; actions=[]
def E(s): return html.escape(str(s))
def act(label,behavior,to=None,primary=False):
    n=len(actions)+1;actions.append(dict(n=n,label=label,behavior=behavior,to=to))
    href=f'?screen={to:02}' if to else '#'
    return f'<a class="button {"primary" if primary else ""}" href="{href}">{E(label)}<b class="mark">{n}</b></a>'
def field(label,value,behavior,kind='input'):
    n=len(actions)+1;actions.append(dict(n=n,label=label,behavior=behavior,to=None))
    x=f'<textarea>{E(value)}</textarea>' if kind=='area' else f'<input value="{E(value)}" type="{"password" if kind=="password" else "text"}">'
    return f'<label class="field">{E(label)} <b class="mark">{n}</b>{x}</label>'
def pick(label,items,behavior):
    n=len(actions)+1;actions.append(dict(n=n,label=label,behavior=behavior,to=None))
    return f'<label class="field">{E(label)} <b class="mark">{n}</b><select>'+''.join(f'<option>{E(x)}</option>' for x in items)+'</select></label>'
def checks(items,behavior):
    n=len(actions)+1;actions.append(dict(n=n,label='선택 항목',behavior=behavior,to=None))
    return f'<div class="checks"><div class="tiny">선택 <b class="mark">{n}</b></div>'+''.join(f'<label><input type="checkbox" {"checked" if done else ""}> <span>{E(label)}</span></label>' for label,done in items)+'</div>'
def badge(s):return f'<span class="badge">{E(s)}</span>'
def card(title,body,sub=''):return f'<section class="card"><h3>{title}</h3>'+ (f'<p class="muted">{sub}</p>' if sub else '')+body+'</section>'
def row(title,sub='',right=''):return f'<div class="row"><div><strong>{title}</strong><p>{sub}</p></div><div>{right}</div></div>'
def grid(*s):return '<div class="grid">'+''.join(s)+'</div>'
def hint(s):return f'<div class="hint">{s}</div>'
def buttons(*s):return '<div class="buttons">'+''.join(s)+'</div>'
def table(headers,rows):return '<table><thead><tr>'+''.join(f'<th>{h}</th>' for h in headers)+'</tr></thead><tbody>'+''.join('<tr>'+''.join(f'<td>{v}</td>' for v in r)+'</tr>' for r in rows)+'</tbody></table>'
def add(title,desc,body,section='공간 홈',context='친구 모임',mode='space',states='로딩 → 성공 또는 오류 안내. 오류가 나면 입력을 유지하고 다시 시도합니다.'):
    global actions
    screens.append(dict(id=len(screens)+1,title=title,desc=desc,body=body,section=section,context=context,mode=mode,actions=actions,states=states));actions=[]

# 01
add('로그인','내 공간으로 들어가는 첫 화면',
'<div class="authbrand"><span class="word">SAI<span>●</span></span><h1>함께하는 일들을,<br>한 공간에.</h1><p>나의 기록부터 친구들과의 여행까지.</p></div><div class="authform"><h2>다시 만나 반가워요</h2>'+
field('아이디','sai_user','아이디를 입력합니다. 빈 값이면 로그인할 수 없습니다.')+
field('비밀번호','example123!','비밀번호를 입력합니다. 실제 로그인은 기존 인증 API에 연결합니다.','password')+
buttons(act('로그인','입력을 확인하고 로그인 성공 시 내 공간으로 이동합니다. 실패하면 같은 화면에 오류를 표시합니다.',3,True),act('회원가입','회원가입 화면으로 이동합니다.',2))+'</div>',
mode='auth',states='실패: “아이디 또는 비밀번호를 확인해 주세요.” 제출 중 버튼 중복 클릭 방지. 소셜 로그인·비밀번호 재설정은 이번 범위에서 제외.')
#02
add('회원가입','필요한 정보를 입력하고 계정 만들기',
'<div class="authbrand"><span class="word">SAI<span>●</span></span><h1>나만의 공간을<br>시작하세요.</h1><p>이미 계정이 있다면 로그인하세요.</p></div><div class="authform"><h2>회원가입</h2>'+
grid(field('이름','지민','이름을 입력합니다.'),field('아이디','sai_user','가입 시 서버에서 중복을 검사합니다.'))+
grid(field('비밀번호','example123!','서버 비밀번호 정책을 충족해야 합니다.','password'),field('비밀번호 확인','example123!','두 비밀번호가 일치해야 합니다.','password'))+
field('이메일','jimin@example.test','이메일 형식을 검사합니다. 샘플 주소입니다.')+
field('전화번호','010-0000-0000','기존 가입 API의 입력 항목입니다. 샘플 번호입니다.')+
buttons(act('가입하기','필수 항목 확인 후 가입하고 로그인 화면으로 이동합니다. 중복 아이디 오류는 입력란 아래에 표시합니다.',1,True),act('로그인으로','입력을 취소하고 로그인으로 돌아갑니다.',1))+'</div>',mode='auth',
states='가입 요건은 기존 API와 대조해 확정합니다. 약관·인증·비밀번호 정책을 이미 구현됐다고 가정하지 않습니다.')
#03
add('내 공간','개인 공간과 함께 사용하는 공간을 한눈에',
buttons(act('공간 만들기','새 공간을 만드는 창을 엽니다.',4,True),act('받은 초대 2','받은 초대를 확인합니다.',8))+
grid(card('나의 공간',badge('나 혼자')+'<p>일상 메모와 해야 할 일</p>'+act('공간 열기','개인 공간의 홈을 엽니다. 멤버 초대 영역은 개인 상태에 맞춰 표시합니다.',5)),
card('친구 모임',badge('4명 함께')+'<p>평소 이야기와 함께하는 활동</p>'+act('공간 열기','친구 모임 공간 홈으로 이동합니다.',6)),
card('스터디',badge('3명 함께')+'<p>공부 계획을 함께 이어가는 공간</p>'+act('공간 열기','선택한 스터디 공간의 홈으로 이동합니다. 같은 공간 홈 구조를 사용합니다.',6))),
mode='global',section='내 공간',context='내 공간')
#04
add('공간 만들기','계속 사용할 공간의 이름을 정하기',
'<div class="formbox"><h2>어떤 공간을 만들까요?</h2>'+
field('공간 이름','친구 모임','필수 입력입니다. 공백만 있으면 생성할 수 없습니다.')+
field('한 줄 소개','함께 계획하고 기록하는 공간','선택 입력입니다. 공간 홈 제목 아래 표시합니다.')+
hint('혼자 시작해도 괜찮아요. 만든 뒤 친구를 초대할 수 있어요.')+
buttons(act('공간 만들기','공간을 만들고 내가 관리자가 됩니다. 빈 공간 홈으로 이동합니다.',5,True),act('취소','아무것도 만들지 않고 내 공간으로 돌아갑니다.',3))+'</div>',
mode='global',context='내 공간',states='만드는 중 중복 제출을 막습니다. 실패해도 이름·소개 입력은 유지합니다.')
#05
add('처음 만든 공간','빈 화면에서도 다음 행동을 바로 알기',
'<div class="empty"><div class="emptyicon">+</div><h2>이 공간을 어떻게 채울까요?</h2><p>기능을 하나 추가하거나, 함께할 사람을 초대해 보세요.</p>'+
buttons(act('첫 기능 추가','기록·할 일·일정 등 필요한 기능을 고릅니다.',10,True),act('사람 초대','이 공간에 참여할 사람을 초대합니다.',7))+'</div>'+
card('특정한 일을 함께한다면','<p>여행이나 모임처럼 하나의 활동으로 묶어 보세요.</p>'+act('활동 만들기','활동의 목적과 제목을 입력합니다.',18)),
states='기록이나 숫자를 임의로 채우지 않습니다. 개인 공간도 같은 빈 상태를 사용합니다.')
#06
add('공간 홈','평소 콘텐츠와 활동을 명확히 구분하기',
buttons(act('사람 초대','공간 초대 창을 엽니다.',7),act('활동 만들기','공간 안에 특정 활동을 만듭니다.',18,True))+
grid(card('오늘 할 일',row('모임 날짜 정하기','친구 모임 · 오늘',badge('미완료'))+row('준비물 정리','제주 여행 · 10월 15일',badge('활동'))+act('할 일 전체 보기','공간과 활동의 할 일을 소속 표시와 함께 확인합니다.',13)),
card('최근 기록',row('다음 모임 이야기','공간 기록 · 오늘 14:20')+row('여행 준비 메모','제주 여행 · 어제')+act('기록 전체 보기','공간 기록 목록으로 이동합니다.',11)))+
card('진행 중인 활동',row('제주 여행','2026.10.16–10.18 · 4명',act('활동 열기','제주 여행의 활동 홈을 엽니다.',20,True))+row('연말 모임','날짜 미정 · 준비 중',act('상세 보기','선택한 활동의 홈을 엽니다. 날짜 미정도 허용합니다.',20)))+
act('보관한 활동 보기','완료·보관한 활동을 확인합니다.',30))
#07
add('사람 초대','누구를 어느 공간에 초대하는지 확인하기',
'<div class="formbox"><h2>친구 모임에 초대하기</h2><p class="muted">초대를 수락하면 이 공간의 기록과 활동을 볼 수 있어요.</p>'+
field('초대할 아이디','sai_friend','상대방의 로그인 아이디로 사용자를 찾는 제안입니다. 기존 숫자 ID 초대 API 앞에 사용자 조회 연결이 필요합니다.')+
act('사용자 찾기','아이디에 해당하는 사용자의 이름을 확인합니다. 찾은 사람을 확인한 뒤 초대를 전송합니다.',7)+hint('초대 대상: 하린 · @sai_friend · 이름을 확인하고 초대해 주세요.')+
buttons(act('초대 보내기','초대 요청을 보냅니다. 성공 메시지 후 멤버 화면의 초대 중 목록에 반영합니다.',9,True),act('취소','초대를 보내지 않고 공간 홈으로 돌아갑니다.',6))+'</div>'+
card('초대할 때 확인해 주세요','<p>이미 참여 중인 사람, 나 자신, 초대가 진행 중인 사람은 다시 초대할 수 없어요.</p>'),
states='대상을 찾을 수 없음 / 이미 초대함 / 권한 없음 / 전송 실패를 각각 안내합니다. 실제 초대 전송이 일어나는 목업은 아닙니다.')
#08
add('받은 초대','수락과 거절의 결과를 바로 이해하기',
card('스터디에서 초대했어요',row('스터디','초대한 사람: 지민 · 3명 참여 중')+
hint('수락하면 이 공간의 멤버가 되고 기록과 활동을 볼 수 있어요.')+
buttons(act('수락','초대를 수락하고 내 공간 목록에 스터디를 추가합니다.',3,True),act('거절','해당 초대를 거절합니다. 공간 멤버로 추가되지 않습니다.',8)))+
card('주말 독서 모임에서 초대했어요',row('주말 독서 모임','초대한 사람: 수연 · 2명 참여 중')+
buttons(act('수락','이 초대를 수락하고 해당 공간이 내 공간에 나타납니다.',3,True),act('거절','이 초대만 거절합니다.',8))),
mode='global',section='받은 초대',context='받은 초대',states='초대 없음 / 이미 처리됨 / 만료·취소됨 / 처리 실패. 같은 초대를 두 번 처리하지 않습니다.')
#09
add('함께하는 사람','현재 멤버와 수락 대기 중인 초대를 구분하기',
buttons(act('사람 초대','새 초대를 보냅니다.',7,True))+
card('현재 멤버 4명',table(['이름','역할','상태'],[['지민 (나)',badge('소유자'),'참여 중'],['수연',badge('관리자'),'참여 중'],['민수','멤버','참여 중'],['도현','멤버','참여 중']]))+
card('초대 중',row('하린 · @sai_friend','보낸 날짜: 10월 5일',badge('수락 대기'))),
section='멤버',states='역할은 읽기 전용입니다. 역할 변경·강제 퇴장·소유권 이전은 이번 범위에서 제외합니다.')
#10
add('기능 추가','어디에 추가할지 먼저 선택하기',
pick('추가할 위치',['친구 모임 · 공간 전체','제주 여행 · 특정 활동'],'공간 전체 또는 특정 활동을 선택합니다. 선택한 위치에만 기능이 추가됩니다.')+
grid(card('기록','<p>메모와 생각을 남겨요.</p>'+badge('사용 중')),
card('할 일','<p>해야 할 일을 확인해요.</p>'+badge('사용 중')),
card('일정','<p>약속과 중요한 날짜를 모아요.</p>'+act('추가하기','선택 위치에 일정을 추가합니다. 추가 후 해당 탐색 메뉴가 나타납니다.',15,True)),
card('장소·지도','<p>여행에서 갈 곳을 정리해요.</p>'+act('추가하기','선택한 활동에 장소 기능을 추가합니다.',23)),
card('비용 정산','<p>누가 얼마를 썼는지 정리해요.</p>'+act('추가하기','선택한 활동에 비용 정산 기능을 추가합니다.',25)),
card('투표','<p>함께 선택하고 결정해요.</p>'+act('추가하기','선택 위치에 투표 기능을 추가합니다.',27)))+
hint('사용 중인 기능은 중복해서 추가할 수 없어요.'),
section='기능 추가',states='설치 전 상태 예시입니다. 다른 화면의 일정 탭은 설치 후 상태입니다. 메뉴와 설치 상태는 실제 구현에서 함께 갱신됩니다. 기능 제거는 이번 범위 제외.')
#11
add('기록 목록','필요한 기록을 찾아 열기',
grid(field('기록 검색','','현재 위치의 제목과 본문에서 검색합니다.'),pick('범위',['모든 기록','공간 기록','제주 여행 기록'],'모든 기록에서는 각 기록의 소속을 함께 표시합니다.'))+
buttons(act('새 기록','비어 있는 편집 화면을 엽니다.',12,True))+
card('최근 수정한 기록',row('여행 준비 메모','제주 여행 · 지민 · 오늘 14:20',act('열기','선택한 기록을 읽고 편집합니다.',12))+row('다음 모임 이야기','공간 기록 · 수연 · 어제',act('열기','선택한 공간 기록을 엽니다.',12))+row('장소 후보 모음','제주 여행 · 민수 · 10월 3일',act('열기','선택한 기록을 엽니다.',12))),
section='기록',states='검색 결과 없음 / 아직 기록 없음 / 읽기 실패. 공간·활동 이름은 데이터 소속에 따라 표시합니다.')
#12
add('기록 작성·편집','자동 저장 대신 명시적인 저장으로 단순하게',
field('제목','여행 준비 메모','기록 제목을 입력합니다. 저장 전 필수 확인합니다.')+
field('내용','이번 여행에서 함께 하고 싶은 일을 모아봐요.\n\n준비할 것\n• 숙소 예약 확인\n• 각자 준비물 정리\n\n가고 싶은 곳\n바닷가 산책과 작은 카페','일반 텍스트를 작성합니다. 서식 편집·첨부·공동 커서는 이번 범위 제외합니다.','area')+
buttons(act('저장','현재 소속에 제목과 내용을 저장하고 성공 표시를 보여줍니다.',11,True),act('목록으로','수정 내용이 있으면 저장하지 않고 나갈지 확인합니다.',11))+
hint('저장 위치: 친구 모임 › 제주 여행 · 아직 저장하지 않은 변경사항이 있어요.'),
section='기록',context='친구 모임 › 제주 여행',mode='event',states='저장 중 버튼 비활성 / 성공 시 목록 갱신 / 실패 시 29번. 저장되지 않은 입력을 유지합니다.')
#13
add('할 일 목록','완료 여부·담당자·소속을 한 줄에서 보기',
buttons(act('할 일 추가','새 할 일을 입력합니다.',14,True))+
pick('보여줄 항목',['전체','미완료','완료'],'완료 여부로 필터링합니다.')+
checks([('모임 날짜 정하기 · 지민 · 오늘 · 친구 모임',False),('준비물 정리 · 수연 · 10월 15일 · 제주 여행',False),('숙소 예약 확인 · 민수 · 제주 여행',True)],'체크하면 완료로, 해제하면 미완료로 바뀝니다. 서버 실패 시 이전 상태로 되돌리고 안내합니다.')+
card('할 일 상세',row('준비물 정리','제주 여행 · 수연',act('수정','선택한 할 일을 기존 값으로 엽니다.',14))),
section='할 일',states='빈 목록에는 “첫 할 일을 추가해 보세요.” 완료 체크가 필터에서 사라지는 동작을 안내합니다.')
#14
add('할 일 추가·수정','할 일의 이름부터 입력하기',
'<div class="formbox">'+field('할 일','준비물 정리','필수 입력입니다.')+
grid(pick('담당자',['미지정','지민','수연','민수','도현'],'현재 공간 또는 활동 참여자 중 담당자를 고릅니다. 미지정도 가능합니다.'),field('마감일 (선택)','2026-10-15','날짜 없이도 저장할 수 있습니다.'))+
pick('소속',['친구 모임','제주 여행'],'새 할 일을 만들 때 소속을 선택합니다. 기존 할 일의 소속 이동은 이번 범위 제외합니다.')+
buttons(act('저장','할 일을 저장하고 목록에 반영합니다.',13,True),act('취소','입력을 버리고 목록으로 돌아갑니다.',13))+'</div>',section='할 일')
#15
days=''.join(f'<div class="day"><small>{i}</small>'+('<span class="eventbar">제주 여행</span>' if i in [16,17,18] else '<span class="eventbar soft">모임 날짜 투표</span>' if i==8 else '')+'</div>' for i in range(1,32))
add('공간 일정','날짜별 약속을 모아서 보기',
buttons(act('이전 달','이전 달로 이동합니다.'),act('오늘','오늘이 포함된 달로 이동합니다.'),act('다음 달','다음 달로 이동합니다.'),act('일정 추가','새 일정을 입력합니다.',16,True))+
card('2026년 10월','<div class="calendar">'+''.join('<b>'+d+'</b>' for d in ['월','화','수','목','금','토','일'])+'<div></div>'*3+days+'</div>')+
act('제주 여행 열기','활동 표시를 누르면 연결된 활동 홈으로 이동합니다.',20),
section='일정',states='달력은 월요일 시작. 여행 Event 기간은 연결된 활동으로 표시하고 개별 약속과 구분합니다.')
#16
add('일정 추가','약속의 시간과 소속을 명확하게',
'<div class="formbox">'+field('일정 이름','여행 준비 모임','필수 입력입니다.')+
grid(field('날짜','2026-10-08','달력에서 선택하거나 날짜를 입력합니다.'),field('시간','19:00','종일을 선택하면 시간 입력을 비활성화합니다.'))+
checks([('종일',False)],'종일 일정으로 전환합니다. 시간 대신 해당 날짜 전체를 사용합니다.')+
pick('소속',['친구 모임','제주 여행'],'어느 공간·활동에 표시할지 선택합니다.')+
field('메모 (선택)','준비물과 이동 방법을 정해요.','약속에 대한 짧은 설명입니다.')+
buttons(act('일정 저장','일정을 저장하고 달력에 표시합니다.',15,True),act('취소','저장하지 않고 달력으로 돌아갑니다.',15))+'</div>',section='일정',
states='유효하지 않은 날짜·시간은 해당 입력란에 안내합니다. 시간대는 현재 서비스의 기준을 명시하고 여행 국가 확장은 별도 설계합니다.')
#17
add('활동 목록','계속 쓰는 공간 안에서 특정 활동 찾아가기',
buttons(act('활동 만들기','새 활동을 만듭니다.',18,True),act('보관한 활동','보관 목록을 확인합니다.',30))+
pick('활동 상태',['진행 전·진행 중','완료','전체'],'현재 보고 싶은 상태로 필터링합니다.')+
card('제주 여행',badge('진행 전')+'<h2>10월 16일 — 10월 18일</h2><p>4명이 함께하는 2박 3일 여행</p>'+act('활동 열기','제주 여행 홈을 엽니다.',20,True))+
card('연말 모임',badge('날짜 미정')+'<p>날짜가 정해지지 않아도 먼저 준비할 수 있어요.</p>'+act('활동 열기','날짜 미정 활동 홈을 엽니다.',20)),section='활동')
#18
add('활동 만들기','목적과 제목을 먼저, 날짜는 선택',
'<div class="formbox">'+pick('어떤 활동인가요?',['여행','모임','직접 구성'],'템플릿을 고릅니다. 다음 단계의 추천 기능 구성이 달라집니다.')+
field('활동 이름','제주 여행','필수 입력입니다.')+
grid(field('시작일 (선택)','2026-10-16','미정이면 비워 둘 수 있습니다.'),field('종료일 (선택)','2026-10-18','입력할 경우 시작일보다 빠를 수 없습니다.'))+
hint('소속 공간: 친구 모임 · 참여자는 활동을 만든 후 선택할 수 있어요.')+
buttons(act('다음: 사용할 기능','아직 활동을 만들지 않고 기능 선택 단계로 이동합니다.',19,True),act('취소','생성하지 않고 활동 목록으로 돌아갑니다.',17))+'</div>',section='활동')
#19
add('활동에 넣을 기능','추천 구성에서 필요 없는 기능 빼기',
card('제주 여행 · 여행 템플릿','<p>필요한 기능만 골라 시작하세요. 나중에 더 추가할 수 있어요.</p>'+
checks([('일정 — 날짜별 계획',True),('장소·지도 — 갈 곳 저장',True),('준비물 — 챙길 것 확인',True),('비용 정산 — 함께 쓴 돈',True),('기록 — 여행 메모',True),('투표 — 함께 결정',True)],'선택한 기능만 활동에 설치됩니다. 기록·투표를 포함해 자유롭게 선택할 수 있습니다.'))+
buttons(act('활동 만들기','활동과 선택한 기능을 함께 생성합니다. 실패하면 입력을 유지합니다.',20,True),act('이전','제목·날짜를 수정하러 돌아갑니다. 입력은 유지합니다.',18)),
section='활동',states='중복 생성 방지. 실제 구현에서는 활동 생성과 기능 설치의 일관성이 필요합니다.')
#20
add('활동 홈','여행에서 지금 필요한 것만 요약하기',
'<div class="heroline">'+badge('진행 전')+'<span>2026.10.16–10.18 · 4명 참여</span></div>'+
buttons(act('참여자 보기','공간 멤버 중 활동 참여자를 고릅니다.',21),act('활동 마치기','완료·보관 단계로 이동합니다.',30))+
grid(card('여행 일정',row('1일차 · 10월 16일','10:00 공항 도착 → 12:00 점심 → 15:00 해변 산책')+act('일정 보기','날짜별 일정을 확인합니다.',22,True)),
card('함께 쓴 돈','<div class="big">240,000원</div><p>4명 균등 부담 · 1인 60,000원</p>'+act('정산 보기','결제자와 부담자에 따른 정산을 봅니다.',25)))+
grid(card('준비물','<div class="big">2 / 5</div><p>완료한 준비물</p>'+act('준비물 보기','체크리스트를 엽니다.',24)),
card('정해야 할 일',row('저녁 메뉴','2 / 4명 참여')+act('투표하기','진행 중인 투표를 엽니다.',27))),
mode='event',context='친구 모임 › 제주 여행',section='활동 홈')
#21
add('활동 참여자','공간 멤버 중 함께할 사람 선택하기',
hint('이 활동은 친구 모임 멤버가 볼 수 있어요. 참여자 선택은 비공개 설정이 아닙니다.')+
checks([('지민 · 나',True),('수연',True),('민수',True),('도현',True)],'이 활동에 참여할 멤버를 선택합니다. 체크를 해제해도 공간 멤버에서 제거되지 않습니다.')+
buttons(act('참여자 저장','참여자 목록을 저장합니다. 기존 비용이 있는 참여자는 정산 영향 확인이 필요합니다.',20,True),act('취소','변경하지 않고 활동 홈으로 돌아갑니다.',20)),
mode='event',context='친구 모임 › 제주 여행',section='참여자',
states='활동 편집 권한이 없는 사용자는 목록만 봅니다. 비용 기록이 있는 참여자를 자동으로 정산에서 제외하지 않습니다.')
#22
add('날짜별 여행 일정','하루 단위로 장소와 시간을 배치하기',
buttons(act('1일차 · 10/16','선택 날짜의 일정을 표시합니다.'),act('2일차 · 10/17','2일차 목록으로 전환합니다.'),act('3일차 · 10/18','3일차 목록으로 전환합니다.'),act('일정 추가','현재 날짜를 채운 일정 입력 화면을 엽니다.',16,True))+
card('1일차 · 10월 16일',row('10:00 · 공항 도착','제주공항',act('수정','이 일정의 시간·메모를 수정합니다.',16))+row('12:00 · 점심 식사','장소 미정',act('장소 고르기','저장한 장소에서 고릅니다.',23))+row('15:00 · 해변 산책','협재해변',act('수정','해변 산책 일정의 내용을 수정합니다.',16)))+
hint('시간을 바꾸면 시간순으로 정렬됩니다. 아직 정하지 않은 장소는 나중에 연결할 수 있어요.'),
mode='event',context='친구 모임 › 제주 여행',section='일정')
#23
add('장소·지도','장소 검색과 저장 목록을 함께 보기',
field('장소 검색','애월','검색어로 장소 후보를 찾습니다. 지도 제공자의 장소 검색 연결이 필요한 설계입니다.')+
grid(card('검색 결과',row('애월 해변','제주시 애월읍',act('저장','현재 활동의 장소 목록에 추가합니다.',23,True))+row('애월 카페','제주시 애월읍',act('저장','현재 활동에 저장합니다.',23))),
card('저장한 장소 3개',row('제주공항','도착 장소',act('일정에 넣기','선택 장소를 채워 일정 작성 화면을 엽니다.',16))+row('협재해변','1일차 15:00',badge('일정에 연결됨'))+row('성산일출봉','날짜 미정',act('일정에 넣기','선택 장소를 일정에 연결합니다.',16))))+
'<div class="mapmock"><span class="mapcaption">지도 영역 · 위치·경로는 설명용 배치</span><div class="road r1"></div><div class="road r2"></div><span class="pin p1">1 · 공항</span><span class="pin p2">2 · 해변</span><span class="pin p3">3 · 일출봉</span></div>',
mode='event',context='친구 모임 › 제주 여행',section='장소·지도',
states='검색 중 / 결과 없음 / 이미 저장함 / 지도 로딩 실패. 지도 실패해도 저장한 장소 목록은 접근할 수 있게 합니다.')
#24
add('준비물','체크 여부와 누가 챙기는지 확인하기',
grid(field('준비물 이름','우산','새 준비물 이름을 입력합니다.'),pick('챙길 사람',['미지정','지민','수연','민수','도현'],'참여자 중 담당자를 고릅니다.'))+
act('준비물 추가','입력한 준비물을 목록에 추가합니다.',24,True)+
card('2 / 5개 준비 완료',checks([('신분증 · 각자',True),('충전기 · 각자',True),('선크림 · 수연',False),('수영복 · 각자',False),('상비약 · 지민',False)],'완료 체크를 바꾸면 준비 완료 수가 갱신됩니다. “각자”는 공동 목록의 안내 문구이며 개인별 완료 추적은 별도 확장입니다.')),
mode='event',context='친구 모임 › 제주 여행',section='준비물')
#25
add('비용 정산','결제한 돈과 부담할 돈을 구분하기',
buttons(act('지출 추가','새 지출의 금액·결제자·부담자를 입력합니다.',26,True))+
grid(card('함께 쓴 돈','<div class="big">240,000원</div><p>모든 지출의 합계</p>'),card('내 부담액','<div class="big">60,000원</div><p>이번 예시는 4명 균등 부담</p>'))+
card('지출 내역',table(['내역','금액','결제자','부담'],[['렌터카','100,000원','지민','4명'],['식비','90,000원','수연','4명'],['숙소','50,000원','민수','4명']]))+
card('정산할 금액',row('도현 → 지민','보낼 금액','40,000원')+row('도현 → 수연','보낼 금액','20,000원')+row('민수 → 수연','보낼 금액','10,000원'))+
hint('보내야 할 금액을 계산해 보여줍니다. 실제 송금 기능은 제공하지 않습니다.'),
mode='event',context='친구 모임 › 제주 여행',section='비용 정산',
states='지출 없음 / 금액 오류 / 참여자 변경. 샘플 순잔액: 지민 +40,000, 수연 +30,000, 민수 -10,000, 도현 -60,000.')
#26
add('지출 추가','누가 결제했고 누가 나눠 낼지 입력하기',
'<div class="formbox">'+field('사용 내역','렌터카','필수 입력입니다.')+
grid(field('금액 (원)','100000','양의 정수만 입력합니다. 이번 설계는 원화만 지원합니다.'),pick('결제한 사람',['지민','수연','민수','도현'],'실제 결제한 참여자를 선택합니다.'))+
checks([('지민',True),('수연',True),('민수',True),('도현',True)],'비용을 나눠 부담할 사람을 선택합니다. 한 명 이상 필요합니다.')+
hint('4명 균등 분담 · 1인 25,000원. 나머지 원 단위 처리 규칙은 저장 전에 안내합니다.')+
buttons(act('지출 저장','지출을 저장하고 정산 잔액을 다시 계산합니다.',25,True),act('취소','저장하지 않고 정산 화면으로 돌아갑니다.',25))+'</div>',
mode='event',context='친구 모임 › 제주 여행',section='비용 정산',
states='다중 통화·불균등 분담·송금 연동은 제외. 원 단위 나머지는 참여자 고정 순서로 1원씩 배분하도록 설계 검증 필요.')
#27
add('투표 참여·결과','한 가지를 선택하고 결과 확인하기',
card('저녁 메뉴를 골라 주세요',badge('진행 중')+'<p>4명 중 2명 참여 · 10월 15일 18:00 마감</p>'+
pick('내 선택',['선택해 주세요','흑돼지 구이','해산물','현지에서 결정'],'한 가지를 고릅니다. 제출 전에는 집계되지 않습니다.')+
buttons(act('투표하기','한 표를 제출하고 결과를 갱신합니다. 마감 전 다시 선택해 변경할 수 있습니다.',27,True))+
table(['선택지','현재 득표'],[['흑돼지 구이','2표'],['해산물','0표'],['현지에서 결정','0표']]))+
act('새 투표 만들기','제목과 선택지를 입력합니다.',28),
mode='event',context='친구 모임 › 제주 여행',section='투표',
states='진행 중 / 참여 완료 / 마감됨. 마감 이후에는 제출·변경이 불가능하고 결과만 표시합니다.')
#28
add('투표 만들기','제목과 선택지만으로 함께 결정하기',
'<div class="formbox">'+field('투표 제목','저녁 메뉴를 골라 주세요','필수 입력입니다.')+
field('선택지 1','흑돼지 구이','최소 두 개의 서로 다른 선택지가 필요합니다.')+
field('선택지 2','해산물','비어 있거나 중복된 선택지는 저장할 수 없습니다.')+
field('선택지 3 (선택)','현지에서 결정','선택 입력입니다.')+
field('마감 일시 (선택)','2026-10-15 18:00','미입력 시 생성자가 마감할 때까지 진행합니다.')+
buttons(act('투표 만들기','단일 선택 투표를 생성합니다.',27,True),act('취소','생성하지 않고 투표 목록으로 돌아갑니다.',27))+'</div>',
mode='event',context='친구 모임 › 제주 여행',section='투표',states='익명·복수 선택·가중치 투표는 제외. 생성자가 수동 마감하는 동작은 결과 화면에서 권한에 따라 표시합니다.')
#29
add('저장 실패·이동 확인','오류가 나도 입력 내용 지키기',
'<div class="error"><strong>기록을 저장하지 못했어요.</strong><p>작성한 내용은 이 화면에 남아 있어요. 연결을 확인하고 다시 시도해 주세요.</p></div>'+
field('제목','여행 준비 메모','실패 전 입력을 그대로 유지합니다.')+
field('내용','이번 여행에서 함께 하고 싶은 일을 모아봐요.\n숙소 예약 확인과 준비물 정리가 필요해요.','재시도 전에도 계속 수정할 수 있습니다.','area')+
buttons(act('다시 저장','현재 입력으로 저장을 다시 시도합니다. 성공하면 기록 목록으로 이동합니다.',11,True),act('계속 작성','오류 안내를 접고 편집을 이어갑니다.',12))+
card('저장하지 않고 나가시겠어요?','<p>“목록으로”를 눌렀을 때 나타나는 확인창 예시입니다.</p>'+
buttons(act('계속 작성하기','이동을 취소하고 입력을 유지합니다.',12),act('저장하지 않고 나가기','저장되지 않은 변경을 버리고 목록으로 이동합니다.',11))),
mode='event',context='친구 모임 › 제주 여행',section='기록',
states='상단은 저장 실패, 하단은 이동 확인 상태를 함께 설명한 검토 화면입니다. 실제 UI에서는 해당 상황의 안내만 표시합니다.')
#30
add('활동 완료·보관','활동이 끝나도 기록은 남기기',
card('제주 여행을 마칠까요?','<p>완료하면 진행 중인 활동에서 빠집니다. 여행 기록과 비용 내역은 계속 볼 수 있어요.</p>'+
buttons(act('완료로 표시','활동 상태를 완료로 바꿉니다. 기록을 삭제하지 않습니다.',30,True),act('아직 진행 중','완료하지 않고 활동 홈으로 돌아갑니다.',20)))+
card('완료한 활동',row('제주 여행','2026.10.16–10.18 · 완료',act('보관하기','주 활동 목록에서 접어 보관 목록으로 옮깁니다. 영구 삭제가 아닙니다.',30)))+
card('보관한 활동',row('지난 피크닉','2026.05.10 · 보관됨',act('기록 보기','보관한 활동 내용을 읽기 중심으로 엽니다.',20))+act('다시 열기','보관을 해제합니다. 완료 상태는 유지하며 진행 중 재개는 별도 확인합니다.',17)),
section='활동',states='완료 전 / 완료 후 / 보관 후의 세 상태를 한 장에서 비교합니다. 실제 화면에서는 해당 상태에 맞는 조작만 표시합니다.')

add('AI 기능 추천 · 향후 예시','추천 근거를 확인하고 직접 선택하기',
hint('향후 기능 예시 · 실제 기록 분석과 AI 호출은 아직 연결하지 않았습니다.')+
card('여행 일정 기능을 추가해 볼까요?',badge('기능 추천')+'<p>이 공간에서 날짜·장소를 다룬 기록을 바탕으로 기능을 추천하는 예시입니다.</p>'+
buttons(act('추천 근거 보기','추천에 사용한 기록 제목과 이유를 보여줍니다. 열람 권한이 있는 기록만 근거로 사용해야 합니다.',31),act('기능 확인','추천 기능의 설명과 추가 위치를 확인합니다. 자동 설치하지 않습니다.',10,True),act('나중에','이 추천을 접습니다. 숨긴 추천이 즉시 반복되지 않도록 합니다.',6)))+
card('왜 추천했나요?',row('여행 준비 메모','여행 날짜와 장소를 정리한 기록')+row('장소 후보 모음','갈 곳을 모아둔 기록')+'<p class="muted">실제 기록을 분석한 결과가 아닌 설명용 예시입니다.</p>')+
checks([('이 공간의 기록으로 기능 추천 받기',True)],'공간 단위 분석 동의를 명시적으로 받는 방향의 설계입니다. 권한·외부 전송·보관 범위는 AI 설계에서 확정합니다.'),
section='공간 홈',states='향후: 동의 전 / 분석 중 / 근거 부족 / 추천 있음 / 숨김 / 실패. 비용·응답시간·권한 누출·추천 정확도 실험 계획을 별도 승인받습니다.')
add('공동 편집 · 향후 예시','함께 보고 수정하는 상태를 구분하기',
hint('향후 기능 예시 · 아래 접속자·커서·저장 상태는 정적 목업입니다.')+
card('여행 준비 메모',row('함께 편집 중','지민 · 수연',badge('2명 접속 예시'))+
field('기록 내용','이번 여행에서 함께 하고 싶은 일을 모아봐요.\n\n숙소 예약 확인\n각자 준비물 정리\n\n수연이 장소 후보를 추가하고 있어요.','향후 여러 사람이 같은 문서를 수정합니다. 실제 동시 입력은 연결하지 않았습니다.','area')+
'<div class="hint">│ 수연의 커서 · 편집 위치 예시</div>'+
buttons(act('변경 기록 보기','누가 어떤 내용을 바꿨는지 이력을 확인하는 향후 동작입니다.',32),act('기록 목록','소속 활동의 기록 목록으로 돌아갑니다.',11)))+
card('연결이 끊겼을 때','<p>“연결이 끊겼어요. 작성 내용은 이 기기에 보관 중입니다.”</p>'+
act('다시 연결','연결을 복구하고 변경 내용을 동기화합니다. 충돌·권한 변경 시 처리 정책은 별도 설계합니다.',32)),
mode='event',context='친구 모임 › 제주 여행',section='기록',
states='향후: 연결 중 / 동기화 완료 / 오프라인 / 재연결 / 권한 회수. 기본 버전의 수동 저장과 구분하며 협업 도입 시 저장 경험을 다시 승인받습니다.')

assert len(screens)==32
CSS=r"""
.s25 .content{display:grid;grid-template-columns:1fr 1fr}.s25 .content>.buttons,.s25 .content>.grid,.s25 .content>.hint{grid-column:1/-1}.s29 textarea{height:155px}.s32 textarea{height:135px}.s32 .card{padding:18px}
*{box-sizing:border-box}body{margin:0;background:#f0edf3;font-family:"Malgun Gothic","Segoe UI",sans-serif;color:#19151f;font-size:15px}a{color:inherit;text-decoration:none}button,input,select,textarea{font:inherit}input,select,textarea{width:100%;border:1px solid #d7d0df;border-radius:9px;background:white;color:#211a2a;padding:13px 14px;outline-color:#6b4288}textarea{height:210px;resize:none;line-height:1.8}.sheet{width:1440px;min-height:1000px;background:white}.reviewbar{height:50px;background:#24132f;color:white;display:flex;justify-content:space-between;align-items:center;padding:0 25px;font-size:13px;letter-spacing:.3px}.reviewbar b{font-size:16px}.layout{display:grid;grid-template-columns:190px 970px 280px;min-height:924px}.sidebar{background:#faf8fc;border-right:1px solid #e8e2ee;padding:28px 15px;display:flex;flex-direction:column;gap:7px}.word{font-size:32px;font-weight:850;letter-spacing:-1.8px;display:block;margin:0 12px 28px}.word span{color:#4d2a64;font-size:14px;margin-left:4px}.nav{padding:12px;border-radius:9px;font-weight:600;color:#63576b}.nav.selected{background:#eee5f5;color:#442059}.sidecap{font-size:11px;color:#97869f;letter-spacing:1px;margin:24px 12px 5px}.sidefoot{margin-top:auto;padding:18px 12px;color:#7b6c84;font-size:12px}.workspace{padding:0 28px 28px;background:white}.top{height:74px;display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid #ede8f0;font-size:13px;color:#81728c}.profile{background:#eee4f5;color:#492664;padding:9px 12px;border-radius:50%;font-weight:700}.title{margin:28px 0 24px}.title .eyebrow{font-size:11px;letter-spacing:1.5px;color:#8d769b;font-weight:700;margin-bottom:8px}.title h1{font-size:28px;letter-spacing:-1.1px;margin:0 0 8px}.title p{color:#7a6c84;margin:0;line-height:1.6}.content{display:flex;flex-direction:column;gap:18px}.card{border:1px solid #e5deec;border-radius:14px;padding:22px;background:#fff}.card h3{font-size:17px;margin:0 0 15px;letter-spacing:-.4px}.card>p{color:#786980;line-height:1.7}.grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px}.grid:has(>.card:nth-child(3)){grid-template-columns:repeat(3,minmax(0,1fr))}.row{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:15px 0;border-bottom:1px solid #efeaf3}.row:last-child{border-bottom:0}.row strong{font-size:15px}.row p{font-size:12px;color:#8a7b93;margin:7px 0 0}.buttons{display:flex;gap:10px;flex-wrap:wrap;align-items:center}.button{display:inline-flex;align-items:center;justify-content:center;gap:9px;border:1px solid #dcd2e3;background:white;padding:11px 14px;border-radius:9px;color:#452858;font-size:13px;font-weight:700;white-space:nowrap}.button.primary{background:#3e204f;color:white;border-color:#3e204f}.mark{display:inline-flex;border-radius:50%;width:19px;height:19px;align-items:center;justify-content:center;background:#ede4f4;color:#542e70;font-size:10px;flex-shrink:0}.primary .mark{background:#ffffff25;color:white}.field{display:block;font-size:13px;font-weight:700;color:#51425d}.field input,.field select,.field textarea{display:block;margin-top:9px}.field>.mark{vertical-align:middle;margin-left:5px}.badge{display:inline-block;background:#f0e9f6;color:#634279;padding:5px 9px;font-size:11px;font-weight:700;border-radius:6px}.hint{border-left:3px solid #83539c;border-radius:3px;background:#f7f3fa;padding:15px 18px;color:#6c537c;font-size:13px;line-height:1.7}.muted{color:#887891;font-size:13px}.checks{display:flex;flex-direction:column;gap:0}.checks label{display:flex;align-items:center;gap:12px;padding:15px 0;border-bottom:1px solid #eee7f3;font-size:14px}.checks input{accent-color:#492661;width:18px;height:18px}.tiny{font-size:11px;color:#897493}.formbox{border:1px solid #e6dfee;background:#fcfbfd;border-radius:16px;padding:28px;display:flex;flex-direction:column;gap:21px;max-width:740px;margin:auto;width:100%}.formbox h2{margin:0;font-size:23px}.formbox>p{margin:0}.empty{text-align:center;padding:48px 20px;border:1px dashed #d5c6df;border-radius:16px;background:#fdfbff}.emptyicon{font-size:40px;color:#75458d;background:#f1e9f7;width:72px;height:72px;margin:auto;border-radius:22px}.empty h2{font-size:24px}.empty p{color:#8b7797}.empty .buttons{justify-content:center}.big{font-size:34px;letter-spacing:-1.2px;font-weight:700;color:#3d2250;margin:12px 0}.heroline{display:flex;align-items:center;gap:14px;color:#897394;font-size:13px}table{border-collapse:collapse;width:100%;font-size:13px}th{text-align:left;color:#927c9f;font-size:11px;font-weight:600;padding:12px;background:#faf7fc}td{padding:15px 12px;border-bottom:1px solid #ece4f2}tr:last-child td{border-bottom:0}.calendar{display:grid;grid-template-columns:repeat(7,1fr);gap:3px}.calendar>b{text-align:center;font-size:11px;color:#927d9d;padding:8px}.day{height:66px;border-top:1px solid #eee7f2;padding:5px}.day small{font-size:11px;color:#6b5c74}.eventbar{display:block;background:#eae0f2;color:#58356e;padding:6px 3px;margin-top:4px;border-radius:4px;font-size:10px;white-space:nowrap}.eventbar.soft{background:#f4edf8}.mapmock{height:210px;position:relative;overflow:hidden;border:1px solid #e7e1ed;border-radius:14px;background:repeating-linear-gradient(25deg,#f4f1f6 0px,#f4f1f6 34px,#eee9f2 35px,#eee9f2 36px)}.mapcaption{position:absolute;top:12px;left:15px;font-size:11px;color:#918198}.road{position:absolute;height:8px;background:white;width:90%;transform:rotate(-13deg);top:120px;left:20px}.r2{transform:rotate(27deg);top:110px}.pin{position:absolute;background:#4b2861;color:white;padding:10px 12px;border-radius:16px 16px 16px 2px;font-size:12px}.p1{left:18%;top:40%}.p2{left:45%;top:58%}.p3{left:74%;top:32%}.error{border:1px solid #ebc7ce;background:#fff5f7;color:#933a4d;border-radius:12px;padding:18px}.error p{font-size:13px;margin-bottom:0}.notes{background:#faf8fc;border-left:1px solid #e8e1ef;padding:26px 20px;color:#7b6986}.notes h3{color:#4d2c63;font-size:14px;margin:0 0 8px}.notes>.lead{font-size:11px;line-height:1.6;margin:0 0 20px}.note{margin:0 0 17px}.note strong{font-size:12px;color:#4f365f;display:block;margin-bottom:5px}.note p{font-size:11px;line-height:1.7;margin:0}.note .mark{margin-right:5px}.note .dest{font-size:10px;color:#a08bad;margin-top:5px}.foot{height:26px;background:#f7f3fa;border-top:1px solid #e9e2ef;font-size:10px;display:flex;justify-content:space-between;padding:7px 22px;color:#8b7895}.auth .layout{grid-template-columns:1160px 280px}.auth .workspace{padding:70px 60px}.auth .content{display:grid;grid-template-columns:1fr 1.05fr;align-items:center;gap:60px;height:770px}.authbrand .word{margin:0 0 65px;font-size:45px}.authbrand h1{font-size:43px;line-height:1.5;letter-spacing:-2px}.authbrand p{color:#8b7a96}.authform{border:1px solid #e6deed;border-radius:18px;padding:28px;display:flex;flex-direction:column;gap:20px}.authform h2{font-size:23px;margin:0 0 8px}.auth .field input{padding:11px}.gallery{max-width:1280px;margin:35px auto}.gallery h1{font-size:32px}.gallerygrid{display:grid;grid-template-columns:repeat(3,1fr);gap:20px}.gallerycard{background:white;padding:20px;border-radius:12px;border:1px solid #e1d9e9}.gallerycard img{width:100%;margin-top:12px}.gallerycard p{font-size:13px;color:#8a7994}
"""
def nav(label,to,selected=False):return f'<a class="nav {"selected" if selected else ""}" href="?screen={to:02}">{label}</a>'
def render(s):
    sb='<aside class="sidebar"><span class="word">SAI<span>●</span></span>'+nav('▦  내 공간',3,s['mode']=='global' and s['section']=='내 공간')+nav('✉  받은 초대  2',8,s['section']=='받은 초대')+'<div class="sidecap">지속되는 공간</div>'+nav('친구 모임',6,True)
    if s['mode']!='global':
        entries=[('공간 홈',6),('기록',11),('할 일',13),('일정',15),('활동',17),('멤버',9),('기능 추가',10)]
        if s['mode']=='event':
            sb+=nav('← 공간으로',6)+'<div class="sidecap">활동 · 제주 여행</div>'
            entries=[('활동 홈',20),('일정',22),('장소·지도',23),('준비물',24),('비용 정산',25),('기록',12),('투표',27)]
        if s['id']==5: entries=[('공간 홈',5),('활동',17),('멤버',9),('기능 추가',10)]
        if s['id'] in (10,31): entries=[e for e in entries if e[0]!='일정']
        sb+=''.join(nav(k,v,s['section']==k) for k,v in entries)
    sb+='<div class="sidefoot">지민 · 로그인됨<br><br>'+nav('로그아웃',1)+'</div></aside>'
    notes='<aside class="notes"><h3>이 화면의 동작</h3><p class="lead">번호는 동작 설명을 위한 표시입니다.<br>실제 제품에서는 표시하지 않습니다.</p>'
    for a in s['actions']:
        # Full explanations are in the companion document. Shortened only for compact review rail.
        text=a['behavior']
        if len(text)>77:text=text[:75]+'…'
        notes+=f'<div class="note"><strong><b class="mark">{a["n"]}</b>{E(a["label"])}</strong><p>{E(text)}</p>'+ (f'<div class="dest">→ {a["to"]:02}번 화면</div>' if a['to'] else '')+'</div>'
    notes+='</aside>'
    top=f'<div class="top"><span>내 공간 &nbsp; / &nbsp; {E(s["context"])}</span><span class="profile">지</span></div><div class="title"><div class="eyebrow">{"ACTIVITY" if s["mode"]=="event" else "SPACE"}</div><h1>{E(s["title"])}</h1><p>{E(s["desc"])}</p></div>'
    return f'<div class="sheet s{s['id']} {"auth" if s["mode"]=="auth" else ""}"><div class="reviewbar"><b>{s["id"]:02} / 32 &nbsp; {E(s["title"])}</b><span>SAI · PC 낮 테마 · 화면/동작 검토용</span></div><div class="layout">'+('' if s['mode']=='auth' else sb)+'<main class="workspace">'+('' if s['mode']=='auth' else top)+'<div class="content">'+s['body']+'</div></main>'+notes+'</div><div class="foot"><span>정적 목업 · 가상 데이터 · 입력/저장/초대의 실제 API 호출 없음</span><span>White / Black / Deep Purple · v3</span></div></div>'
data=json.dumps(screens,ensure_ascii=False)
pages=json.dumps([render(s) for s in screens],ensure_ascii=False)
gallery='<div class="gallery"><span class="word">SAI<span>●</span></span><h1>PC 낮 테마 · 32개 화면</h1><p>계정 설정·테마 전환을 제외한 기본 흐름 30장 + AI·협업 향후 예시 2장. 각 화면 오른쪽에 동작 설명을 붙였습니다.</p><p><a href="interaction-guide.md">동작 설명서</a> · <a href="SAI-PC-Light-32-PNG.zip">PNG 32장 다운로드</a></p><div class="gallerygrid">'+''.join(f'<a class="gallerycard" href="?screen={s["id"]:02}"><b>{s["id"]:02} · {s["title"]}</b><p>{s["desc"]}</p><img loading="lazy" src="png/{s["id"]:02}.png" alt="{s["title"]}"></a>' for s in screens)+'</div></div>'
doc='<!doctype html><html lang="ko"><meta charset="utf-8"><title>SAI PC 낮 테마 목업 32장</title><style>'+CSS+'</style><body><div id="root"></div><script>const pages='+pages+';const gallery='+json.dumps(gallery,ensure_ascii=False)+';const id=Number(new URLSearchParams(location.search).get("screen"));document.getElementById("root").innerHTML=id>=1&&id<=32?pages[id-1]:gallery;</script></body></html>'
(O/'index.html').write_text(doc,encoding='utf-8')
(O/'screens.json').write_text(data,encoding='utf-8')
guide=['# SAI PC 낮 테마 — 32개 화면과 동작 설명','2026-10-05 · 정적 화면 승인 자료. 제품 코드는 변경하지 않았습니다. AI 추천과 실시간 공동 편집은 31·32번 향후 예시로 포함합니다.','## 이번 시안에서 단순하게 만든 점','- 지속되는 공간과 특정 활동을 왼쪽 탐색에서 구분합니다. 활동 안에는 해당 활동의 기능만 표시합니다.\n- UI 명칭은 공간 / 활동 / 일정으로 통일합니다. Event는 활동이고, 일정은 날짜·시간이 있는 개별 항목입니다.\n- 계정 설정, 테마 전환, 프로필 편집, 소셜 로그인, 결제·송금, 자동 저장, 파일 첨부, 권한 변경 UI는 제외합니다.\n- 기록은 일반 텍스트와 명시적 저장부터 시작합니다. 지도·정산은 요구된 여행 기능 설계에 포함하되 실제 API 연동 완료를 의미하지 않습니다.\n- 모든 날짜·이름·금액·사용자 ID는 예시입니다. 지도는 지리 정보가 아닌 배치 목업입니다.\n- 각 PNG 오른쪽 설명 영역은 검토용입니다. 실제 제품 화면에는 포함하지 않습니다.','## 공통 요소','| 요소 | 동작 |\n|---|---|\n| 내 공간 | 03 공간 목록으로 이동 |\n| 받은 초대 | 08 수락 대기 초대 확인 |\n| 친구 모임 | 06 공간 홈 이동 |\n| 공간 안의 메뉴 | 해당 공간 데이터로 이동. 설치한 기능만 표시 |\n| 활동 안의 메뉴 | 제주 여행 등 현재 활동 데이터로 이동 |\n| 공간으로 | 활동을 닫고 상위 공간 홈 이동 |\n| 로그인 이름·아바타 | 읽기 전용 표시. 계정 설정으로 연결하지 않음 |\n| 로그아웃 | 미저장 내용이 있으면 이동 확인 후 세션 종료, 01 이동 |\n| 버튼 번호 | 검토 문서와 연결하는 표식, 제품에서는 제거 |\n| 화면 제목·안내·배지 | 현재 위치와 상태를 알려주는 읽기 전용 요소 |\n| 입력란 | 직접 입력. 필수값·형식 검증 후 오류를 해당 필드에 표시 |\n| 체크박스·선택상자 | 각 화면 설명의 범위 내에서 선택 상태 변경 |','## 화면별 모든 조작 설명']
for s in screens:
    guide += [f'### {s["id"]:02}. {s["title"]}',s['desc'],f'![{s["title"]}](png/{s["id"]:02}.png)','| 번호 | 요소 | 동작·결과 | 이동 |\n|---|---|---|---|']
    for a in s['actions']:guide.append(f'| {a["n"]} | {a["label"]} | {a["behavior"]} | '+(f'{a["to"]:02}' if a['to'] else '현재 화면')+' |')
    guide += ['','**상태·예외:** '+s['states'],'']
guide+=['## 구현 전 확인할 범위','이 자료는 화면·동작 설계입니다. 버튼 링크는 관련 화면을 보여주는 검토용 이동이며 실제 저장·초대·계산 실행이 아닙니다. 역할·참여자·정산 규칙과 지도 서비스는 해당 백엔드 설계를 제시하고 승인받아 구현합니다. 현재 API는 숫자 사용자 ID를 받습니다. 직관적인 초대를 위해 로그인 아이디로 사용자 이름을 확인하는 조회 단계가 필요하며, 이 연결은 아직 구현하지 않은 설계입니다.','## 검증 계획','1. PC 1440px 기준 모든 화면의 주요 입력과 안내가 잘리지 않는지 확인.\n2. 공간·활동 간 콘텐츠 소속과 메뉴 범위 일치 확인.\n3. 각 입력의 필수값·실패·중복 제출·미저장 이동 상태 검증.\n4. 초대 수락·거절 결과와 역할별 접근을 실제 API 연결 후 검사.\n5. 정산 총액, 개인 부담액, 순잔액 및 1원 나머지 처리 검증.\n6. 일반 글자 대비 4.5:1, 키보드 조작과 초점 확인.\n7. PNG와 실제 구현 캡처의 차이를 승인 후 보고.','## 파일 구성','- png/01.png … png/32.png: 개별 화면\n- index.html: 전체 목록과 화면 이동 검토 페이지\n- screens.json: 화면 및 동작 정의\n- source/build_mockups.py: 목업 전용 렌더링 원본\n- interaction-guide.md: 이 문서\n- SAI-PC-Light-32-PNG.zip: PNG와 문서 묶음\n\n제작: HTML/CSS로 화면을 정의한 뒤 브라우저에서 캡처한 PNG. 이미지 생성 모델을 사용하지 않아 한국어 문구·번호·상태를 일관되게 관리합니다.']
(O/'interaction-guide.md').write_text(re.sub(r'(\|[^\n]*\|)\n\n(?=\|)', r'\1\n', '\n\n'.join(guide)),encoding='utf-8')
print('Generated 32 review screens and action specification.')
