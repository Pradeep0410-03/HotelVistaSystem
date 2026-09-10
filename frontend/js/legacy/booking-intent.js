/* Only catalogue selection belongs in the URL, never credentials or a trusted price. */
(function(root) {
  'use strict';
  const fields = ['property', 'checkin', 'checkout', 'guests', 'rooms'];
  function link(page, trip) {
    if (!['login.html', 'register.html', 'index.html','/frontend/pages/hotels.html'].includes(page)) throw new Error('Unsupported destination');
    const params = new URLSearchParams();
    fields.forEach(key => params.set(key, String(trip[key])));
    return page + '?' + params.toString();
  }
  function parse(params, properties, today) {
    const property = properties.find(p => p.id === params.get('property'));
    const checkin = params.get('checkin') || '', checkout = params.get('checkout') || '';
    const guests = Number(params.get('guests')), rooms = Number(params.get('rooms'));
    const {validDate} = root.HotelVistaCatalogue;
    if (!property || !validDate(checkin) || !validDate(checkout) || checkin < today || checkout <= checkin ||
        !Number.isInteger(guests) || guests < 1 || guests > 20 || !Number.isInteger(rooms) || rooms < 1 || rooms > 8 ||
        rooms > guests || rooms > property.rooms || guests > property.capacity * rooms) return null;
    return {property: property.id, checkin, checkout, guests, rooms};
  }
  root.HotelVistaIntent = {link, parse};
})(typeof window !== 'undefined' ? window : globalThis);
