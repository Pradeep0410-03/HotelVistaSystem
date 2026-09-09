const {test} = require('node:test');
const assert = require('node:assert/strict');
require('../js/property-client.js');
const property = {id:7,name:'A stay',propertyType:'HOTEL',city:'Goa',address:'A road',description:null,timezone:'Asia/Kolkata'};
const json = data => new Response(JSON.stringify(data),{headers:{'Content-Type':'application/json'}});
test('city search encodes input and sends page to public same-origin API', async () => {
  const api = HotelVistaProperties.createClient(async (url,options) => {
    const query = new URL(url,'http://localhost').searchParams;
    assert.equal(query.get('city'),'Goa & City'); assert.equal(query.get('page'),'2'); assert.equal(query.get('size'),'12');
    assert.equal(options.credentials,'same-origin');
    return json({items:[property],page:2,size:12,hasNext:true});
  });
  assert.equal((await api.list(' Goa & City ',2)).hasNext,true);
});
test('empty API results stay empty without substituting sample properties',async () => {
  const api = HotelVistaProperties.createClient(async () => json({items:[],page:0,size:12,hasNext:false}));
  assert.deepEqual((await api.list()).items,[]);
});
test('HTML fallback and malformed page are errors, not empty results',async () => {
  await assert.rejects(HotelVistaProperties.createClient(async () => new Response('<html>preview</html>')).list(),/unavailable/);
  await assert.rejects(HotelVistaProperties.createClient(async () => json({items:[property],page:9,size:12,hasNext:false})).list(),/could not be read/);
});
test('detail validates identity and distinguishes a removed listing',async () => {
  assert.equal((await HotelVistaProperties.createClient(async () => json(property)).detail(7)).id,7);
  await assert.rejects(HotelVistaProperties.createClient(async () => json(property)).detail(8),/could not be read/);
  await assert.rejects(HotelVistaProperties.createClient(async () => new Response('',{status:404})).detail(7),/no longer listed/);
});
test('invalid city and IDs cannot send requests',async () => {
  const api = HotelVistaProperties.createClient(() => {throw new Error('Unexpected fetch');});
  await assert.rejects(api.list('a'.repeat(101)),/at most 100/);
  await assert.rejects(api.detail('../auth/me'),/Invalid property/);
});
test('availability sends only trip fields and handles unavailable rooms',async () => {
  const trip = {checkin:'2026-10-10',checkout:'2026-10-13',guests:4,rooms:2,price:1};
  const api = HotelVistaProperties.createClient(async url => {
    const query = new URL(url,'http://localhost');
    assert.equal(query.pathname,'/api/properties/7/availability');
    assert.equal(query.searchParams.get('rooms'),'2'); assert.equal(query.searchParams.has('price'),false);
    return json({propertyId:7,currency:'INR',timezone:'Asia/Kolkata',cancellationDeadline:'2026-10-09T18:30:00Z',cancellationPolicy:'Free before check-in',options:[]});
  });
  assert.deepEqual((await api.availability(7,trip)).options,[]);
});
