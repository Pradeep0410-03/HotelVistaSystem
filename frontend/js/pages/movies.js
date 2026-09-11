(() => {
 'use strict';
 const $=id=>document.getElementById(id), api=VistaMovies.createClient(fetch.bind(window)),auth=HotelVistaAuth.createClient(fetch.bind(window));
 const money=n=>new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR'}).format(n);
 const when=s=>new Intl.DateTimeFormat('en-IN',{dateStyle:'medium',timeStyle:'short',timeZone:s.timezone||'Asia/Kolkata'}).format(new Date(s.starts_at))+' · '+(s.timezone||'Asia/Kolkata');
 const storageKey='vista-movie-selection-v1';
 let city='',movie=null,show=null,seats=[],chosen=new Set(),draft=null,busy=false,movieEpoch=0,showEpoch=0,seatEpoch=0,ticketEpoch=0,ticketPage=0;
 function element(tag,text,cls){const n=document.createElement(tag);if(text!==undefined)n.textContent=text;if(cls)n.className=cls;return n;}
 function button(text,fn,disabled=false){const n=element('button',text);n.type='button';n.disabled=disabled;n.addEventListener('click',fn);return n;}
 function status(text){$('movie-status').textContent=text;}
 function save(){try{if(draft)sessionStorage.setItem(storageKey,JSON.stringify(draft));else sessionStorage.removeItem(storageKey);return true;}catch{return false;}}
 function total(){return Math.round(seats.filter(s=>chosen.has(s.label)).reduce((n,s)=>n+Number(s.price),0)*100)/100;}
 function persist(){if(!draft?.pending && show){draft={show,city,seats:[...chosen],created:Date.now()};save();}}
 function locked(){return busy||!!draft?.pending;}
 function renderSelection(){
  const pending=!!draft?.pending;
  $('seat-controls').disabled=locked();$('movie-browse').disabled=locked();
  $('movie-selection').textContent=chosen.size?'Seats: '+[...chosen].join(', '):'Choose your seats above.';
  $('movie-total').textContent=money(pending?draft.request.expectedTotal:total());
  $('movie-confirm').disabled=busy||!chosen.size;
  $('movie-confirm').textContent=pending?'Retry same reservation':'Confirm reservation';
  $('movie-discard').disabled=busy;
 }
 function renderSeats(){
  $('seat-map').replaceChildren();const rows=new Map();
  for(const s of seats){const row=s.label.charAt(0);if(!rows.has(row)){const n=element('div',undefined,'seat-row');n.append(element('span',row));rows.set(row,n);$('seat-map').append(n);}
   const b=button(s.label+(s.available?' · '+money(s.price):' ×'),()=>{if(locked())return;if(chosen.has(s.label))chosen.delete(s.label);else if(chosen.size<10)chosen.add(s.label);else return status('Choose no more than 10 seats.');persist();renderSeats();},!s.available);
   b.className='seat';b.setAttribute('aria-label',s.label+(s.available?', '+money(s.price):', unavailable'));b.setAttribute('aria-pressed',String(chosen.has(s.label)));rows.get(row).append(b);
  }renderSelection();
 }
 function pagination(id,data,load){const node=$(id);node.replaceChildren();if(data.page===0&&!data.hasNext)return;node.append(button('Previous',()=>load(data.page-1),data.page===0),element('span','Page '+(data.page+1)),button('Next',()=>load(data.page+1),!data.hasNext));}
 async function loadMovies(page=0){
  const token=++movieEpoch;++showEpoch;movie=null;$('show-section').hidden=true;$('movie-list').textContent='Loading movies…';$('movie-pages').replaceChildren();
  try{const data=await api.movies(city,page);if(token!==movieEpoch)return;$('movie-list').replaceChildren();
   for(const m of data.items){const card=element('article',undefined,'movie-card');card.append(element('h3',m.title),element('p',m.language+' · '+m.duration_minutes+' minutes'),button('Choose showtime',()=>{movie=m;loadShows();}));$('movie-list').append(card);}
   if(!data.items.length)$('movie-list').textContent='No upcoming movies found in this city. Try another city.';
   pagination('movie-pages',data,loadMovies);
  }catch(e){if(token===movieEpoch){$('movie-list').textContent=e.message;$('movie-list').append(button('Retry',()=>loadMovies(page)));}}
 }
 async function loadShows(page=0){
  const token=++showEpoch,m=movie;$('show-section').hidden=false;$('show-heading').textContent=m.title+' · Showtimes';$('show-heading').focus();$('show-list').textContent='Loading showtimes…';$('show-pages').replaceChildren();
  try{const data=await api.shows(m.id,city,page);if(token!==showEpoch)return;$('show-list').replaceChildren();
   for(const s of data.items){const card=element('article',undefined,'movie-card');card.append(element('h3',s.cinema_name),element('p',s.city+' · '+s.address),element('p',when(s)),button('Choose seats',()=>openShow({...s,title:m.title})));$('show-list').append(card);}
   if(!data.items.length)$('show-list').textContent='No upcoming screenings found.';
   pagination('show-pages',data,loadShows);
  }catch(e){if(token===showEpoch){$('show-list').textContent=e.message;$('show-list').append(button('Retry',()=>loadShows(page)));}}
 }
 async function openShow(s,restore=[]){
  if(locked())return;const token=++seatEpoch;show=s;chosen=new Set();draft=null;save();$('seat-section').hidden=false;$('seat-heading').textContent=s.title+' · Choose seats';$('show-detail').textContent=s.cinema_name+' · '+s.city+' · '+when(s);$('seat-map').textContent='Loading seats…';$('seat-heading').focus();seats=[];renderSelection();
  try{const data=await api.seats(s.id);if(token!==seatEpoch)return;seats=data;chosen=new Set(restore.filter(label=>seats.some(x=>x.label===label&&x.available)));persist();renderSeats();if(chosen.size<restore.length)status('Some previously selected seats are no longer available. Please choose again.');}
  catch(e){if(token===seatEpoch){$('seat-map').textContent=e.message;$('seat-map').append(button('Retry seat map',()=>openShow(s,restore)));}}
 }
 async function reservations(page=0){
  const token=++ticketEpoch;ticketPage=page;$('reservation-list').replaceChildren();$('reservation-pages').replaceChildren();$('reservation-status').textContent='Loading reservations…';
  try{const account=await auth.me();if(token!==ticketEpoch)return;if(!account){$('reservation-status').textContent='Sign in to see your movie reservations.';const a=element('a','Sign in');a.href='/login.html?return=movies';$('reservation-list').append(a);return;}
   const data=await api.list(page);if(token!==ticketEpoch)return;$('reservation-status').textContent=data.items.length?'':'No movie reservations yet.';
   for(const b of data.items){const card=element('article',undefined,'movie-card');card.append(element('h3',b.title),element('p',b.status),element('p',b.cinema_name+' · '+b.city),element('p',when(b)),element('p','Seats: '+b.seats.map(x=>x.label).join(', ')),element('strong',money(b.total_amount)),element('p',b.reference,'reservation-ref'),element('p',b.accepted_policy));
    if(b.status==='CONFIRMED'&&new Date(b.starts_at)>new Date())card.append(button('Cancel reservation',async event=>{if(!window.confirm('Cancel this movie reservation and release its seats?'))return;const btn=event.currentTarget;btn.disabled=true;try{await api.cancel(b.id);await reservations(page);if(show?.id===b.show_id&&!locked())await openShow(show);}catch(e){$('reservation-status').textContent=e.message;btn.disabled=false;}}));
    $('reservation-list').append(card);
   }pagination('reservation-pages',data,reservations);
  }catch(e){if(token===ticketEpoch)$('reservation-status').textContent=e.message;}
 }
 $('movie-confirm').addEventListener('click',async()=>{
  if(busy||!chosen.size)return;let refreshSeats=false;busy=true;renderSelection();$('movie-signin').hidden=true;
  try{
   const account=await auth.me();
   if(!account){persist();if(!save())throw Error('Your browser cannot preserve this selection. Sign in first, then choose seats.');location.assign('/login.html?return=movies');return;}
   if(draft?.pending && draft.owner!==account.id)throw Error('This pending request belongs to another signed-in account. Sign in with that account to check its result.');
   if(!draft?.pending){
    const latest=await api.seats(show.id);const selected=[...chosen];const oldTotal=total();seats=latest;
    chosen=new Set(selected.filter(label=>seats.some(s=>s.label===label&&s.available)));persist();
    if(chosen.size!==selected.length||total()!==oldTotal){renderSeats();status('Availability or prices changed. Review the updated selection before confirming.');return;}
    draft={show,city,seats:selected,created:Date.now(),owner:account.id,pending:true,request:{showId:show.id,seats:selected,expectedTotal:total(),requestKey:crypto.randomUUID()}};
    if(!save()){draft.pending=false;throw Error('Your browser cannot save booking retries. Enable session storage before confirming.');}
   }
   status('Confirming your reservation…');const result=await api.book(draft.request);
   if(!result?.reference)throw Object.assign(new Error('The result could not be read. Check reservations, then retry the same request.'),{status:0});
   draft=null;save();chosen.clear();$('seat-section').hidden=true;status('Reservation '+result.status.toLowerCase()+': '+result.reference+'. No payment collected.');await reservations(0);$('movie-reservations').scrollIntoView({behavior:'smooth'});
  }catch(e){status(e.message);
   if(e.status===401){$('movie-signin').hidden=false;}
   if([400,403,404,409].includes(e.status)&&draft?.pending){draft.pending=false;delete draft.request;save();chosen.clear();refreshSeats=true;}
  }finally{busy=false;if(refreshSeats)await openShow(show);renderSelection();}
 });
 $('movie-discard').addEventListener('click',()=>{if(draft?.pending&&!confirm('This request may already have succeeded. Check My movie reservations before starting another booking. Clear this selection?'))return;draft=null;save();chosen.clear();$('seat-section').hidden=true;renderSelection();status('Selection cleared.');});
 $('movie-search').addEventListener('submit',event=>{event.preventDefault();if(locked())return;city=$('movie-city').value.trim();loadMovies();});
 $('reservation-refresh').addEventListener('click',()=>reservations(ticketPage));
 window.addEventListener('hotelvista-session',()=>reservations(0));
 let stored;try{stored=JSON.parse(sessionStorage.getItem(storageKey));}catch{}
 if(stored&&Number.isSafeInteger(stored.show?.id)&&Array.isArray(stored.seats)&&stored.seats.length<=10&&stored.seats.every(x=>/^[A-Z][1-9][0-9]{0,2}$/.test(x))){
  city=typeof stored.city==='string'?stored.city:'';$('movie-city').value=city;
  if(stored.pending&&stored.request?.requestKey&&stored.owner){draft=stored;show=stored.show;chosen=new Set(stored.seats);$('seat-section').hidden=false;$('seat-heading').textContent=show.title+' · Pending reservation';$('show-detail').textContent=show.cinema_name;$('seat-map').textContent='Check your reservations below, or retry the same request to retrieve its result.';status('A previous request has an unconfirmed result. Its selection and retry key have been preserved.');renderSelection();}
  else if(Date.now()-stored.created<2*60*60*1000)setTimeout(()=>openShow(stored.show,stored.seats),0);
 }else{draft=null;save();}
 loadMovies();reservations();
})();
