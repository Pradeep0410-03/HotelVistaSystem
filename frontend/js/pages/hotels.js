(() => {
  'use strict';
  const $ = id => document.getElementById(id);
  const stay=window.HotelVistaStay;
  const api = window.HotelVistaProperties.createClient(window.fetch.bind(window));
  let requestedPage = 0;
  let city = new URLSearchParams(location.search).get('city') || new URLSearchParams(location.search).get('destination') || '', page = 0, hasNext = false, requestNumber = 0, controller;
  $('database-city').value = city.slice(0,100); city = $('database-city').value;
  function element(tag, text, className) {
    const node = document.createElement(tag); node.textContent = text;
    if (className) node.className = className;
    return node;
  }
  const localDate=d=>[d.getFullYear(),String(d.getMonth()+1).padStart(2,'0'),String(d.getDate()).padStart(2,'0')].join('-');
  const tomorrow=new Date();tomorrow.setDate(tomorrow.getDate()+1);const departure=new Date(tomorrow);departure.setDate(departure.getDate()+2);
  $('trip-checkin').min=localDate(new Date());$('trip-checkin').value=localDate(tomorrow);$('trip-checkout').value=localDate(departure);
  function validateTrip(){const valid=$('trip-checkout').value>$('trip-checkin').value;$('trip-checkout').setCustomValidity(valid?'':'Check-out must be after check-in.');return valid;}
  ['trip-checkin','trip-checkout'].forEach(id=>$(id).addEventListener('input',validateTrip));
  function availabilityForm(property) {
    const form = document.createElement('form'); form.className = 'availability-form';
    const fields = {};
    for (const [name,label,type,value] of [['checkin','Check-in','date',''],['checkout','Check-out','date',''],['guests','Guests','number','2'],['rooms','Rooms','number','1']]) {
      const wrapper = element('label',label); const input = document.createElement('input');
      input.type = type; input.required = true; input.value = $('trip-'+name).value || value;
      if(name==='checkin')input.min=localDate(new Date());
      if (type === 'number') { input.min = '1'; input.max = name === 'guests' ? '100' : '20'; }
      wrapper.append(input); form.append(wrapper); fields[name] = input;
    }
    const button = element('button','Check room availability','primary-button'); button.type = 'submit';
    const output = element('div',''); output.setAttribute('role','status');
    form.append(button,output);
    form.addEventListener('submit',async event => {
      event.preventDefault(); if (button.disabled || !form.reportValidity()) return;
      const trip = {checkin:fields.checkin.value,checkout:fields.checkout.value,guests:Number(fields.guests.value),rooms:Number(fields.rooms.value)};
      button.disabled = true; output.textContent = 'Checking every night…';
      try {
        const result = await api.availability(property.id,trip,AbortSignal.timeout(15000));
        output.replaceChildren(element('p', result.options.length ? 'Room options for ' + trip.checkin + ' to ' + trip.checkout + ' · ' + trip.rooms + ' room(s) · ' + trip.guests + ' guest(s)' : 'No room type meets this request for every night. Try different dates or fewer rooms.'));
        const money = value => new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR'}).format(value);
        result.options.forEach(option => {
          const row = element('div','', 'room-option');
          row.append(element('h3',option.name),element('p',option.availableRooms + ' rooms available · up to ' + option.capacityPerRoom + ' guests per room'),element('p',money(option.nightlyPrice) + ' per room/night · ' + money(option.subtotal) + ' stay subtotal'));
          const select = element('button','Review booking','primary-button'); select.type='button';
          select.addEventListener('click',()=>{
            try {
              sessionStorage.setItem('hotelvista-booking-draft',JSON.stringify({propertyId:property.id,roomTypeId:option.roomTypeId,...trip}));
              location.assign('bookings.html');
            } catch { output.append(element('p','Allow session storage to preserve your room selection.')); }
          });
          row.append(select); output.append(row);
        });
        output.append(element('p',result.cancellationPolicy), element('p','Free cancellation deadline: ' + new Intl.DateTimeFormat('en-IN',{dateStyle:'medium',timeStyle:'short',timeZone:result.timezone}).format(new Date(result.cancellationDeadline)) + ' (' + result.timezone + ').'),element('p','This check does not hold rooms. Review the price and cancellation policy before confirming. No additional taxes or fees are calculated in this project version.'));
      } catch(error) { output.textContent = error.message || 'Availability could not be checked.'; }
      finally { button.disabled = false; }
    });
    return form;
  }
  function card(property) {
    const article = element('article', '', 'database-card');
    const figure = element('figure','','listing-photo');
    const photo = document.createElement('img');
    photo.src=stay.photo(property.id,property.propertyType);photo.alt=property.propertyType.toLowerCase()+' interior · illustrative photo';photo.loading='lazy';photo.width=640;photo.height=420;
    figure.append(photo,element('figcaption','Sample stay · Illustrative photo'));article.append(figure);
    const body=element('div','','listing-body');
    body.append(element('span',property.propertyType.toLowerCase(),'property-kind'),element('h2',stay.name(property.name)),element('p',property.city,'listing-city'),element('p','Pay at the property','payment-note'),element('p','Choose your room and review the full price before confirming.','listing-summary'));
    article.append(body);
    const detail = element('div', ''); detail.hidden = true;
    const button = element('button', 'View rooms & dates', 'primary-button'); button.type = 'button'; button.setAttribute('aria-expanded','false');
    button.addEventListener('click', async () => {
      if (!detail.hidden) { detail.hidden = true; button.setAttribute('aria-expanded','false'); button.textContent = 'View rooms & dates'; return; }
      button.disabled = true;
      try {
        const value = await api.detail(property.id, AbortSignal.timeout(15000));
        const gallery=element('div','','room-gallery');
        for(let n=1;n<=2;n++){const image=document.createElement('img');image.src=stay.photo(property.id,property.propertyType,n);image.alt='Illustrative accommodation interior';image.loading='lazy';gallery.append(image);}
        detail.replaceChildren(gallery,element('p','Sample accommodation in '+value.city+'. Photos illustrate the experience and are not of a verified property.'),availabilityForm(property));
        detail.hidden = false; button.setAttribute('aria-expanded','true'); button.textContent = 'Hide details';
      } catch(error) {
        detail.replaceChildren(element('p',error.message || 'Details could not be loaded.'));
        detail.hidden = false; button.setAttribute('aria-expanded','true'); button.textContent = 'Hide message';
      } finally { button.disabled = false; }
    });
    body.append(button);detail.className='listing-details';article.append(detail); return article;
  }
  async function load(targetPage = 0) {
    requestedPage = targetPage;
    const number = ++requestNumber;
    if (controller) controller.abort(); controller = new AbortController();
    const activeController = controller;
    const timer = setTimeout(() => activeController.abort(),15000);
    $('database-grid').replaceChildren(); $('database-grid').setAttribute('aria-busy','true');
    $('database-status').textContent = 'Loading properties…'; $('database-retry').hidden = true;
    $('database-prev').disabled = true; $('database-next').disabled = true; $('database-page').textContent = '';
    try {
      const result = await api.list(city, targetPage, activeController.signal);
      if (number !== requestNumber) return;
      page = targetPage; hasNext = result.hasNext;
      $('database-title').textContent=city?'Stays in '+city:'Explore stays across India';
      $('database-grid').replaceChildren(...result.items.map(card));
      $('database-status').textContent = result.items.length ? result.items.length + ' listed properties' + (city ? ' in ' + city : '') + '.' : 'No properties found' + (city ? ' in ' + city : '') + '. Try another city or check back later.';
      $('database-page').textContent = 'Page ' + (page+1);
      $('database-prev').disabled = page === 0;
      $('database-next').disabled = !hasNext || page >= 10000;
    } catch(error) {
      if (number !== requestNumber) return;
      $('database-status').textContent = error.name === 'AbortError' ? 'The request timed out. Please try again.' : error.message || 'Properties could not be loaded.';
      $('database-retry').hidden = false;
    } finally {
      clearTimeout(timer);
      if (number === requestNumber) $('database-grid').setAttribute('aria-busy','false');
    }
  }
  $('database-search').addEventListener('submit', event => { event.preventDefault(); if(!validateTrip()||!$('database-search').reportValidity())return; city = $('database-city').value.trim(); load(0); });
  $('database-clear').addEventListener('click', () => { city = ''; $('database-city').value = ''; load(0); });
  $('database-retry').addEventListener('click', () => load(requestedPage));
  $('database-prev').addEventListener('click', () => { if (page > 0) load(page-1); });
  $('database-next').addEventListener('click', () => { if (hasNext) load(page+1); });
  const cities=['Goa','Delhi','Mumbai','Bengaluru','Chennai','Hyderabad','Jaipur','Udaipur','Kochi','Shimla','Manali','Rishikesh','Agra','Varanasi','Lucknow','Bhopal','Indore','Pune','Kolkata','Ahmedabad','Chandigarh','Amritsar','Jodhpur','Jaisalmer','Mysuru','Ooty','Coorg','Darjeeling','Gangtok','Puducherry'];
  cities.forEach(city=>{const option=document.createElement('option');option.value=city;$('database-cities').append(option);});
  document.querySelectorAll('[data-city]').forEach(button=>button.addEventListener('click',()=>{city=button.dataset.city;$('database-city').value=city;load(0);}));
  load(0);
})();
