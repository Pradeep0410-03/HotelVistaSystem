const {test} = require('node:test');
const assert = require('node:assert/strict');
require('../frontend/js/legacy/catalogue.js');
require('../frontend/js/legacy/booking-intent.js');
const {link,parse} = globalThis.HotelVistaIntent;
const properties = [{id:'goa',capacity:2,rooms:3}];
const trip = {property:'goa',checkin:'2027-10-10',checkout:'2027-10-13',guests:4,rooms:2};
const read = value => parse(new URLSearchParams(value),properties,'2027-10-01');
test('selection survives login, registration and catalogue links',()=>{
  for(const page of ['login.html','register.html','index.html']) assert.deepEqual(read(link(page,trip).split('?')[1]),trip);
});
test('unknown, expired, impossible and over-capacity selections are rejected',()=>{
  for(const change of [{property:'missing'},{checkin:'2027-09-30'},{checkout:trip.checkin},{checkin:'2027-02-30'},{guests:5},{rooms:4},{rooms:1.5},{guests:1},{rooms:0}]) assert.equal(read({...trip,...change}),null);
});
test('URLs cannot select external return paths or carry credentials and price',()=>{
  assert.throws(()=>link('https://evil.example',trip));
  const url=link('login.html',{...trip,password:'secret',email:'someone@example.com',price:1,returnTo:'https://evil.example'});
  for(const key of ['password','email','price','returnTo']) assert.equal(new URLSearchParams(url.split('?')[1]).has(key),false);
});
