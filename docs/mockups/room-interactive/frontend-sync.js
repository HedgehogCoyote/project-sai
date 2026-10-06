'use strict';
// Match the approved Vue room flow without mixing mock and account storage.
const syncInspector = inspectorHTML;
inspectorHTML = function (object) {
  const target = space().events.find(item => item.id === object.event);
  const installed = target ? target.modules : space().modules;
  const labels = { places: '장소', packing: '준비물', ...names };
  const options = [['', '장식으로만 사용'], ...installed
    .filter(key => key in labels)
    .map(key => [key, labels[key]])];
  const select = `<select id="binding">${options.map(([key, label]) =>
    `<option value="${key}" ${object.module === key ? 'selected' : ''}>${esc(label)}</option>`
  ).join('')}</select>`;
  return syncInspector(object).replace(/<select id="binding">[\s\S]*?<\/select>/, select);
};

const syncNav = navHTML;
navHTML = function (active) {
  return syncNav(active).replace('우리 방', '공간 홈');
};

const syncRoom = roomHTML;
roomHTML = function () {
  return syncRoom().replace('오른쪽 메뉴에서', '도구 메뉴에서');
};

function hasRoomDraft() {
  return editing && JSON.stringify(draft) !== JSON.stringify(space()?.room);
}

// Capture runs before the existing handlers change the selected space.
document.addEventListener('click', event => {
  const control = event.target.closest('[data-action]');
  if (!control) return;
  if (['spaces', 'enter', 'room', 'events', 'module'].includes(control.dataset.action)
      && hasRoomDraft()
      && !window.confirm('저장하지 않은 가구 배치가 있습니다. 저장하지 않고 나갈까요?')) {
    event.preventDefault();
    event.stopImmediatePropagation();
  }
}, true);

window.addEventListener('beforeunload', event => {
  if (!hasRoomDraft()) return;
  event.preventDefault();
  event.returnValue = '';
});

// A tool removed from an event must not silently open a space-level tool.
document.addEventListener('click', event => {
  if (editing || event.target.closest('[data-action]')) return;
  const control = event.target.closest('[data-object]');
  if (!control) return;
  const object = space()?.room.find(item => item.id === control.dataset.object);
  if (!object?.module) return;
  const target = space().events.find(item => item.id === object.event);
  const available = object.event ? target?.modules : space().modules;
  if (!available?.includes(object.module)) {
    event.preventDefault();
    event.stopImmediatePropagation();
    toast('연결한 도구가 없어요. 도구를 추가하거나 방 꾸미기에서 연결을 바꿔주세요.');
  }
}, true);

render();
