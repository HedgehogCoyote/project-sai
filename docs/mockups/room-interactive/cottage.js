'use strict';
// Atmosphere layer for the standalone mock; furniture coordinates remain unchanged.
let cottageTime='evening';
try{cottageTime=localStorage.getItem('sai-cottage-time')==='day'?'day':'evening';}catch{}
const cottageRoomSVG=roomSVG;
roomSVG=function(){
  const evening=cottageTime==='evening';
  let svg=cottageRoomSVG();
  const palette=evening?{'#faeee2':'#c9a4b4','#ead7c3':'#b894a8','#edddcd':'#aca0b9','#dbc4b0':'#97879f','#e1c7a6':'#d3ae9a','#d5b894':'#bc928c','#c6a785':'#98747c','#dccbb8':'#946b7e','#d4c0aa':'#847082','#bdd9df':'url(#cottage-sky)'}:{'#faeee2':'#f2e4d1','#ead7c3':'#dec9b2','#edddcd':'#d5dfd0','#dbc4b0':'#bccab8','#e1c7a6':'#ddc1a0','#bdd9df':'url(#cottage-sky)'};
  for(const [from,to] of Object.entries(palette))svg=svg.replaceAll(from,to);
  const sky=evening?['#ad92bf','#ecb2bc']:['#a9c8c3','#dfebe2'];
  svg=svg.replace('</defs>',`<linearGradient id="cottage-sky" x2="0" y2="1"><stop stop-color="${sky[0]}"/><stop offset="1" stop-color="${sky[1]}"/></linearGradient><radialGradient id="cottage-glow"><stop stop-color="#ffda9e" stop-opacity="${evening?'.52':'.18'}"/><stop offset="1" stop-color="#ffda9e" stop-opacity="0"/></radialGradient><linearGradient id="window-light" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#ffe6b1" stop-opacity="${evening?'.32':'.38'}"/><stop offset="1" stop-color="#ffe6b1" stop-opacity="0"/></linearGradient><clipPath id="floor-clip"><polygon points="${polygon(0,0,8,8)}"/></clipPath></defs>`);
  // Wall decorations have no placement or pointer targets.
  svg=svg.replace('<polygon points="'+polygon(0,0,8,8)+'" fill="url(#wood)"',`<g aria-hidden="true" pointer-events="none"><path d="M460 20L140 180M460 20L780 180" stroke="${evening?'#78596d':'#9c8575'}" stroke-width="5" stroke-linecap="round"/><path d="M518 94Q550 147 592 132T733 200" fill="none" stroke="${evening?'#796176':'#a6988a'}" stroke-width="1.5"/>${[[532,115],[563,135],[595,133],[633,144],[670,167],[711,190]].map(([x,y])=>`<circle cx="${x}" cy="${y}" r="21" fill="url(#cottage-glow)"/><circle cx="${x}" cy="${y}" r="3" fill="${evening?'#ffe3ac':'#e9d1a5'}"/>`).join('')}<path d="M327 118L362 100V133L327 151Z" fill="${evening?'#96758a':'#d4bea5'}" stroke="${evening?'#76576d':'#ad9379'}" stroke-width="3"/><path d="M333 122L356 110V129L333 141Z" fill="${evening?'#eac9b1':'#efe5cd'}"/></g><polygon points="${polygon(0,0,8,8)}" fill="url(#wood)"`);
  // Insert light after the floor and before the editing grid/object layers.
  const floorEnd='fill="url(#wood)" stroke="'+(evening?'#98747c':'#c6a785')+'" stroke-width="2"/>';
  svg=svg.replace(floorEnd,floorEnd+`<g aria-hidden="true" pointer-events="none" clip-path="url(#floor-clip)"><path d="M177 299L289 243L574 370L435 459Z" fill="url(#window-light)"/><ellipse cx="628" cy="304" rx="128" ry="78" fill="url(#cottage-glow)"/></g>`);
  return svg;
};
const cottageRoomHTML=roomHTML;
roomHTML=function(){const html=cottageRoomHTML();const controls=`<div class="cottage-controls" role="group" aria-label="방 분위기"><span>방 분위기</span>${btn('☀ 낮','cottage-time',cottageTime==='day'?'active':'',`data-time="day" aria-pressed="${cottageTime==='day'}"`)}${btn('☾ 저녁','cottage-time',cottageTime==='evening'?'active':'',`data-time="evening" aria-pressed="${cottageTime==='evening'}"`)}</div>`;return html.replace('<div class="scene-bar">',controls+'<div class="scene-bar">');};
const cottageRender=render;
render=function(){document.body.dataset.time=cottageTime;document.body.classList.toggle('cottage-room',page==='room');cottageRender();};
document.addEventListener('click',e=>{const a=e.target.closest('[data-action="cottage-time"]');if(!a)return;cottageTime=a.dataset.time;try{localStorage.setItem('sai-cottage-time',cottageTime);}catch{}render();});
render();
