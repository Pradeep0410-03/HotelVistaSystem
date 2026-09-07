(() => {
"use strict";
const {properties, destinations} = window.HOTEL_VISTA;
const $ = id => document.getElementById(id);
const money = value => new Intl.NumberFormat("en-IN",{style:"currency",currency:"INR",maximumFractionDigits:0}).format(value);
const escape = value => String(value).replace(/[&<>"']/g, c => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
const {dayNumber,validDate,estimate} = window.HotelVistaCatalogue;
const dateString = date => [date.getFullYear(),String(date.getMonth()+1).padStart(2,"0"),String(date.getDate()).padStart(2,"0")].join("-");
const nextDay = value => { const d = new Date(value+"T12:00:00"); d.setDate(d.getDate()+1); return dateString(d); };
const storageKey = "hotelvista:saved:v1";
let saved = new Set();
try { const stored = JSON.parse(localStorage.getItem(storageKey) || "[]"); if(Array.isArray(stored)) saved = new Set(stored.filter(id => properties.some(p => p.id===id))); } catch {}
const state = {query:"",guests:2,rooms:1,checkin:"",checkout:"",budget:15000,types:[],amenities:[],sort:"recommended",savedOnly:false};
let draftGuests=2, draftRooms=1, toastTimer, previousFocus;
const today = dateString(new Date());
$("checkin").min=today;
$("checkin").value=nextDay(today);
$("checkout").min=nextDay($("checkin").value);
$("checkout").value=nextDay(nextDay($("checkin").value));
state.checkin=$("checkin").value; state.checkout=$("checkout").value;
$("year").textContent=new Date().getFullYear();
$("city-options").innerHTML=[...new Set(properties.map(p=>p.city))].map(city=>'<option value="'+escape(city)+'"></option>').join("");
$("destination-grid").innerHTML=destinations.map(d=>'<button type="button" class="destination-card" data-city="'+escape(d.query)+'"><div class="destination-photo"><img src="'+d.image+'" alt="'+escape(d.city)+'" loading="lazy" width="220" height="160"></div><strong>'+escape(d.city)+'</strong><span class="destination-arrow" aria-hidden="true">↗</span><small>'+escape(d.caption)+'</small></button>').join("");
function announce(message) {
  clearTimeout(toastTimer); $("toast").textContent=message; $("toast").classList.add("visible");
  toastTimer=setTimeout(()=>$("toast").classList.remove("visible"),2600);
}
function syncSaved() {
  $("saved-count").textContent=saved.size;
  $("saved-nav").setAttribute("aria-pressed",String(state.savedOnly));
  $("saved-only").checked=state.savedOnly;
}
function filterProperties() { return window.HotelVistaCatalogue.filter(properties,state,saved); }
function render() {
  const list=filterProperties();
  $("results-title").textContent=state.savedOnly?"Your saved stays":state.query?"Stays for “"+state.query+"”":"Stays worth exploring";
  $("results-summary").textContent=list.length+" sample "+(list.length===1?"stay":"stays")+" · "+state.guests+" "+(state.guests===1?"guest":"guests")+" · "+state.rooms+" "+(state.rooms===1?"room":"rooms");
  $("property-grid").innerHTML=list.map(p=>'<article class="property-card"><div class="property-photo"><img src="'+p.image+'" alt="'+escape(p.name)+' — illustrative room photo" loading="lazy" width="420" height="280"><span class="property-badge">'+escape(p.tag)+'</span><button type="button" class="save-button" data-save="'+p.id+'" aria-pressed="'+saved.has(p.id)+'" aria-label="'+(saved.has(p.id)?"Unsave ":"Save ")+escape(p.name)+'">'+(saved.has(p.id)?"♥":"♡")+'</button></div><div class="property-body"><p class="property-location">'+escape(p.city)+' · '+escape(p.type)+'</p><h3>'+escape(p.name)+'</h3><p class="property-description">'+escape(p.description)+'</p><div class="amenities">'+p.amenities.map(a=>'<span class="amenity">'+escape(a)+'</span>').join("")+'</div><div class="property-footer"><div class="price"><small>From</small><strong>'+money(p.price)+'</strong><span>/ night</span></div><button type="button" class="detail-button" data-detail="'+p.id+'" aria-label="View '+escape(p.name)+'">View stay ↗</button></div></div></article>').join("");
  $("empty-state").hidden=list.length>0;
  syncSaved();
}
function closeGuests() {$("guest-panel").hidden=true;$("guest-toggle").setAttribute("aria-expanded","false");}
function updateGuests() {
  $("guest-number").textContent=draftGuests; $("room-number").textContent=draftRooms;
  $("guest-toggle").innerHTML=draftGuests+" "+(draftGuests===1?"guest":"guests")+" · "+draftRooms+" "+(draftRooms===1?"room":"rooms")+' <span aria-hidden="true">⌄</span>';
  document.querySelectorAll("[data-step]").forEach(button=>{
    const [key,delta]=button.dataset.step.split(":");
    const value=key==="guests"?draftGuests:draftRooms;
    button.disabled=(Number(delta)<0&&value===1)||(Number(delta)>0&&value===(key==="guests"?20:8));
  });
}
function readTrip() {
  const checkin=$("checkin").value, checkout=$("checkout").value;
  let error="";
  if(!validDate(checkin)||!validDate(checkout))error="Choose valid check-in and check-out dates.";
  else if(checkin<dateString(new Date()))error="Check-in cannot be in the past.";
  else if(dayNumber(checkout)<=dayNumber(checkin))error="Check-out must be after check-in.";
  else if(draftRooms>draftGuests)error="Choose at least one guest per room.";
  $("search-error").hidden=!error;$("search-error").textContent=error;
  if(error) return false;
  state.checkin=checkin;state.checkout=checkout;state.guests=draftGuests;state.rooms=draftRooms;
  return true;
}
function saveUrl() {
  const params=new URLSearchParams();
  if(state.query)params.set("destination",state.query);
  params.set("checkin",state.checkin);params.set("checkout",state.checkout);
  params.set("guests",state.guests);params.set("rooms",state.rooms);
  try {history.replaceState(null,"","?"+params.toString()+location.hash);}catch {}
}
function submitSearch(scroll=true) {
  if(!readTrip())return;
  state.query=$("destination").value.trim().slice(0,100);
  closeGuests();saveUrl();render();
  if(scroll)$("stays").scrollIntoView({behavior:"smooth",block:"start"});
}
$("search-form").addEventListener("submit",e=>{e.preventDefault();submitSearch();});
$("checkin").addEventListener("change",()=>{
  if(!validDate($("checkin").value))return;
  $("checkout").min=nextDay($("checkin").value);
  if($("checkout").value<=$("checkin").value)$("checkout").value=$("checkout").min;
});
$("guest-toggle").addEventListener("click",()=>{
  const open=$("guest-panel").hidden;$("guest-panel").hidden=!open;$("guest-toggle").setAttribute("aria-expanded",String(open));
});
$("guest-panel").addEventListener("click",e=>{
  const button=e.target.closest("[data-step]");if(!button)return;
  const [key,delta]=button.dataset.step.split(":");
  if(key==="guests")draftGuests=Math.max(1,Math.min(20,draftGuests+Number(delta)));
  else draftRooms=Math.max(1,Math.min(8,draftRooms+Number(delta)));
  updateGuests();
});
$("guest-done").addEventListener("click",()=>{closeGuests();$("guest-toggle").focus();});
document.addEventListener("click",e=>{if(!e.target.closest(".guest-field"))closeGuests();});
document.addEventListener("keydown",e=>{if(e.key==="Escape"&&!$("guest-panel").hidden){closeGuests();$("guest-toggle").focus();}});
$("destination-grid").addEventListener("click",e=>{
  const button=e.target.closest("[data-city]");if(!button)return;
  $("destination").value=button.dataset.city;resetFilters();submitSearch();
});
function syncTypes() {
  document.querySelectorAll('input[name="type"]').forEach(input=>input.checked=state.types.includes(input.value));
  document.querySelectorAll("[data-type]").forEach(button=>{
    const active=button.dataset.type?state.types.length===1&&state.types[0]===button.dataset.type:state.types.length===0;
    button.classList.toggle("selected",active);button.setAttribute("aria-pressed",String(active));
  });
}
function resetFilters() {
  state.budget=15000;state.types=[];state.amenities=[];state.savedOnly=false;state.sort="recommended";
  $("budget").value=15000;$("budget-value").textContent=money(15000);$("sort").value="recommended";
  document.querySelectorAll('input[name="amenity"]').forEach(input=>input.checked=false);
  syncTypes();syncSaved();
}
$("reset-filters").addEventListener("click",()=>{resetFilters();render();});
$("clear-search").addEventListener("click",()=>{
  resetFilters();$("destination").value="";state.query="";draftGuests=2;draftRooms=1;updateGuests();state.guests=2;state.rooms=1;saveUrl();render();
});
$("budget").addEventListener("input",e=>{state.budget=Number(e.target.value);$("budget-value").textContent=money(state.budget);render();});
document.querySelectorAll('input[name="type"]').forEach(input=>input.addEventListener("change",()=>{
  state.types=Array.from(document.querySelectorAll('input[name="type"]:checked'),i=>i.value);syncTypes();render();
}));
document.querySelectorAll('input[name="amenity"]').forEach(input=>input.addEventListener("change",()=>{
  state.amenities=Array.from(document.querySelectorAll('input[name="amenity"]:checked'),i=>i.value);render();
}));
document.querySelectorAll("[data-type]").forEach(button=>button.addEventListener("click",()=>{
  state.types=button.dataset.type?[button.dataset.type]:[];syncTypes();render();
}));
$("sort").addEventListener("change",e=>{state.sort=e.target.value;render();});
$("saved-only").addEventListener("change",e=>{state.savedOnly=e.target.checked;render();});
$("saved-nav").addEventListener("click",()=>{
  const target=!state.savedOnly;
  if(target){resetFilters();state.query="";$("destination").value="";state.guests=2;state.rooms=1;draftGuests=2;draftRooms=1;updateGuests();saveUrl();}
  state.savedOnly=target;render();$("stays").scrollIntoView({behavior:"smooth"});
});
$("explore-resorts").addEventListener("click",()=>{
  resetFilters();$("destination").value="";state.types=["Resort"];syncTypes();submitSearch();
});
$("property-grid").addEventListener("click",e=>{
  const save=e.target.closest("[data-save]"), detail=e.target.closest("[data-detail]");
  if(save){
    const id=save.dataset.save;
    if(saved.has(id))saved.delete(id);else saved.add(id);
    let persisted=true;try{localStorage.setItem(storageKey,JSON.stringify([...saved]));}catch{persisted=false;}
    render();
    const replacement=document.querySelector('[data-save="'+id+'"]');
    if(replacement)replacement.focus();else $("saved-only").focus();
    announce(persisted?(saved.has(id)?"Saved on this device":"Removed from saved stays"):"Saved list updated for this visit; browser storage is unavailable.");
  }
  if(detail)openProperty(detail.dataset.detail,detail);
});
function openProperty(id,trigger) {
  const p=properties.find(item=>item.id===id);if(!p)return;
  previousFocus=trigger;
  const {nights,subtotal}=estimate(p.price,state.checkin,state.checkout,state.rooms);
  $("detail-content").innerHTML='<img class="detail-hero" src="'+p.image+'" alt="'+escape(p.name)+' — illustrative room photo"><div class="detail-body"><p class="property-location">'+escape(p.area)+' · '+escape(p.city)+' · '+escape(p.type)+'</p><h2 id="detail-title">'+escape(p.name)+'</h2><p>'+escape(p.details)+'</p><div class="detail-meta">'+p.amenities.map(a=>'<span>'+escape(a)+'</span>').join("")+'<span>Up to '+p.capacity+' guests per room</span></div><div class="stay-summary"><span>'+escape(state.checkin)+' → '+escape(state.checkout)+'<br>'+nights+' '+(nights===1?"night":"nights")+' · '+state.rooms+' '+(state.rooms===1?"room":"rooms")+' · '+state.guests+' '+(state.guests===1?"guest":"guests")+'</span><strong>'+money(subtotal)+'</strong><span>'+money(p.price)+' × '+nights+' nights × '+state.rooms+' rooms</span><span>Estimated subtotal</span></div><p class="detail-notice">This is a sample property with illustrative photos and pricing. Taxes and fees are not included. Live availability, final prices and reservations are not connected yet; no booking or payment will be made.</p></div>';
  $("property-dialog").showModal();document.body.classList.add("dialog-open");
}
$("close-dialog").addEventListener("click",()=>$("property-dialog").close());
$("property-dialog").addEventListener("click",e=>{
  if(e.target!==$("property-dialog"))return;
  const r=e.target.getBoundingClientRect();
  if(e.clientX<r.left||e.clientX>r.right||e.clientY<r.top||e.clientY>r.bottom)e.target.close();
});
$("property-dialog").addEventListener("close",()=>{document.body.classList.remove("dialog-open");if(previousFocus?.isConnected)previousFocus.focus();});
const params=new URLSearchParams(location.search);
$("destination").value=(params.get("destination")||"").slice(0,100);
const inDate=params.get("checkin"),outDate=params.get("checkout");
if(validDate(inDate||"")&&validDate(outDate||"")&&inDate>=today&&outDate>inDate){
  $("checkin").value=inDate;$("checkout").min=nextDay(inDate);$("checkout").value=outDate;
}
const guests=Number(params.get("guests")),rooms=Number(params.get("rooms"));
if(Number.isInteger(guests)&&guests>=1&&guests<=20)draftGuests=guests;
if(Number.isInteger(rooms)&&rooms>=1&&rooms<=8&&rooms<=draftGuests)draftRooms=rooms;
updateGuests();readTrip();state.query=$("destination").value.trim();render();
})();
