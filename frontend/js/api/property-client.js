/* Read-only property API; deliberately separate from the sample catalogue. */
(function(root) {
  'use strict';
  function createClient(fetcher) {
    async function read(path, signal) {
      const response = await fetcher(path, {credentials:'same-origin', cache:'no-store', redirect:'error', headers:{Accept:'application/json'}, signal});
      if (!response.ok) throw new Error(response.status === 404 ? 'This property is no longer listed.' : response.status === 400 ? 'Check your dates: choose 1–30 nights, no past check-in, and check-out within the next 365 days. Guests: 1–100; rooms: 1–20.' : 'Properties could not be loaded. Please try again.');
      if (!(response.headers.get('content-type') || '').includes('application/json')) throw new Error('Property services are unavailable here. You can explore the sample booking flow.');
      return response.json();
    }
    function property(value) {
      return value && Number.isSafeInteger(value.id) && value.id > 0 && ['name','city','address','propertyType','timezone'].every(key => typeof value[key] === 'string') && (value.description === null || typeof value.description === 'string');
    }
    return {
      async list(city = '', page = 0, signal) {
        city = city.trim();
        if (city.length > 100 || !Number.isInteger(page) || page < 0 || page > 10000) throw new Error('Enter a city of at most 100 characters and a valid page.');
        const query = new URLSearchParams({city, page:String(page), size:'12'});
        const result = await read('/api/properties?' + query, signal);
        if (!result || !Array.isArray(result.items) || result.items.length > 12 || !result.items.every(property) || result.page !== page || result.size !== 12 || typeof result.hasNext !== 'boolean') throw new Error('The property response could not be read. Please try again.');
        return result;
      },
      async availability(id, trip, signal) {
        if (!Number.isSafeInteger(id) || id < 1) throw new Error('Invalid property.');
        const query = new URLSearchParams({checkin:trip.checkin,checkout:trip.checkout,guests:String(trip.guests),rooms:String(trip.rooms)});
        const result = await read('/api/properties/' + id + '/availability?' + query,signal);
        if (!result || result.propertyId !== id || !Array.isArray(result.options) || result.currency !== 'INR' || typeof result.cancellationPolicy !== 'string' || typeof result.timezone !== 'string' || !Number.isFinite(Date.parse(result.cancellationDeadline)) || !result.options.every(option => Number.isSafeInteger(option.roomTypeId) && typeof option.name === 'string' && Number.isFinite(option.nightlyPrice) && option.nightlyPrice >= 0 && Number.isFinite(option.subtotal) && option.subtotal >= 0 && Number.isInteger(option.availableRooms) && option.availableRooms >= trip.rooms)) throw new Error('Availability could not be read.');
        return result;
      },
      async detail(id, signal) {
        if (!Number.isSafeInteger(id) || id < 1) throw new Error('Invalid property.');
        const result = await read('/api/properties/' + id, signal);
        if (!property(result) || result.id !== id) throw new Error('The property details could not be read.');
        return result;
      }
    };
  }
  root.HotelVistaProperties = {createClient};
})(typeof window !== 'undefined' ? window : globalThis);
