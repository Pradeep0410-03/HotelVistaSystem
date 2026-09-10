(() => {
  'use strict';
  const $=id=>document.getElementById(id), api=HotelVistaBookings.createClient(fetch.bind(window)), auth=HotelVistaAuth.createClient(fetch.bind(window)), properties=HotelVistaProperties.createClient(fetch.bind(window));
  const stay=window.HotelVistaStay;
  const key='hotelvista-booking-draft'; let account=null,draft=null,page=0,hasNext=false,busy=false;
  const money=value=>new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR'}).format(value);
  function node(tag,text){const n=document.createElement(tag);n.textContent=text;return n;}
  function status(text){$('booking-status').textContent=text;}
  function clearDraft(){try{sessionStorage.removeItem(key);}catch{}draft=null;$('booking-review').hidden=true;}
  function saveDraft(){sessionStorage.setItem(key,JSON.stringify(draft));}
  function valid(d){return d && Number.isSafeInteger(d.propertyId)&&d.propertyId>0&&Number.isSafeInteger(d.roomTypeId)&&d.roomTypeId>0&&/^\d{4}-\d{2}-\d{2}$/.test(d.checkin)&&/^\d{4}-\d{2}-\d{2}$/.test(d.checkout)&&Number.isInteger(d.guests)&&d.guests>=1&&d.guests<=100&&Number.isInteger(d.rooms)&&d.rooms>=1&&d.rooms<=20;}
  async function review(){
    try{draft=JSON.parse(sessionStorage.getItem(key));}catch{draft=null;}
    if(!valid(draft)){clearDraft();return;}
    if(draft.accountId && draft.accountId!==account.id){clearDraft();status('The previous selection belonged to another account. Choose your stay again.');return;}
    $('booking-review').hidden=false;$('confirm-booking').disabled=true;
    try{
      const [property,availability]=await Promise.all([properties.detail(draft.propertyId,AbortSignal.timeout(15000)),properties.availability(draft.propertyId,draft,AbortSignal.timeout(15000))]);
      const room=availability.options.find(r=>r.roomTypeId===draft.roomTypeId);
      if(!room)throw new Error('Your selected room is no longer available. Browse properties to choose again.');
      draft.accountId=account.id;
      // Preserve the key and submitted price when the outcome of a prior POST is uncertain.
      if(!draft.submitted){draft.expectedSubtotal=room.subtotal;draft.requestKey=crypto.randomUUID();}
      saveDraft();
      $('review-details').replaceChildren(node('h3',stay.name(property.name)+' · '+room.name),node('p',draft.checkin+' → '+draft.checkout+' · '+stay.count(draft.rooms,'room')+' · '+stay.count(draft.guests,'guest')),node('p','Total payable at hotel: '+money(draft.expectedSubtotal)+'. No additional taxes or fees are calculated in this project version.'),node('p',availability.cancellationPolicy),node('p','Free cancellation before '+new Intl.DateTimeFormat('en-IN',{dateStyle:'medium',timeStyle:'short',timeZone:availability.timezone}).format(new Date(availability.cancellationDeadline))+' ('+availability.timezone+').'));
      $('confirm-booking').disabled=false;
    }catch(error){$('review-details').textContent=error.message;}
  }
  async function list(){
    const result=await api.list(page);hasNext=result.hasNext;
    $('bookings-list').replaceChildren(...result.items.map(booking=>{
      const article=node('article','');article.className='trip-card';
      const photo=document.createElement('img');photo.src=stay.photo(booking.propertyId);photo.alt='Illustrative accommodation photo';photo.className='trip-photo';photo.loading='lazy';
      const body=node('div','');body.className='trip-body';
      const badge=node('span',booking.status.toLowerCase());badge.className='booking-badge '+booking.status.toLowerCase();
      const heading=node('h2',stay.name(booking.propertyName));
      const dates=node('p',stay.date(booking.checkin)+' — '+stay.date(booking.checkout));dates.className='trip-dates';
      const reference=node('details','');reference.className='booking-reference';reference.append(node('summary','Booking reference'),node('p',booking.reference));
      const policy=node('details','');policy.append(node('summary','Cancellation policy'),node('p',booking.acceptedPolicy));
      body.append(badge,heading,dates,node('p',booking.roomName+' · '+stay.count(booking.rooms,'room')+' · '+stay.count(booking.guests,'guest')),reference,policy);
      const price=node('div','');price.className='trip-price';price.append(node('span','Total for your stay'),node('strong',money(booking.totalAmount)),node('span','Pay at the property'));
      article.append(photo,body,price);
      if(booking.status==='CONFIRMED' && new Date(booking.cancellationDeadline)>new Date()){
        const cancel=node('button','Cancel booking');cancel.type='button';cancel.className='text-button';
        cancel.addEventListener('click',async()=>{
          if(!window.confirm('Cancel this booking and release the rooms?'))return;
          cancel.disabled=true;
          try{await api.cancel(booking.id);await list();status('Booking cancelled. The rooms are available again.');}
          catch(error){status(error.message);cancel.disabled=false;}
        });price.append(cancel);
      }
      return article;
    }));
    $('bookings-page').textContent='Page '+(page+1);$('bookings-prev').disabled=page===0;$('bookings-next').disabled=!hasNext;
    if(!result.items.length)status('No bookings on this page yet.');
  }
  async function refresh(){
    if(busy)return;busy=true;
    $('confirm-booking').disabled=true;$('bookings-list').replaceChildren();$('booking-review').hidden=true;$('booking-login').hidden=true;
    try{
      account=await auth.me();
      if(!account){status('Sign in to review your stay and view your bookings.');$('booking-login').hidden=false;return;}
      status('Bookings for '+account.fullName);await list();await review();
    }catch(error){status(error.message);}
    finally{busy=false;}
  }
  $('confirm-booking').addEventListener('click',async()=>{
    if(busy||!draft)return;busy=true;$('confirm-booking').disabled=true;
    try{
      draft.submitted=true;saveDraft();
      const booking=await api.create(draft);clearDraft();
      page=0;await list();status(booking.status==='CONFIRMED' ? 'Booking confirmed: '+booking.reference+'. Pay at hotel.' : 'Booking '+booking.reference+' is '+booking.status+'. No new booking was created.');
    }catch(error){
      status(error.message);
      if(error.status===401){$('booking-login').hidden=false;}
      else if(error.status===409){draft.submitted=false;saveDraft();status(error.message+' Refresh to review your selection again.');}
      else $('confirm-booking').disabled=false;
    }finally{busy=false;}
  });
  $('discard-booking').addEventListener('click',()=>{if(!busy)clearDraft();});
  $('booking-refresh').addEventListener('click',refresh);
  $('bookings-prev').addEventListener('click',async()=>{if(page>0&&!busy){page--;await refresh();}});
  $('bookings-next').addEventListener('click',async()=>{if(hasNext&&!busy){page++;await refresh();}});
  document.addEventListener('visibilitychange',()=>{if(!document.hidden)refresh();});
  refresh();
})();
