const {test}=require('node:test');const assert=require('node:assert/strict');require('../js/booking-client.js');
const json=(data,status=200)=>new Response(JSON.stringify(data),{status,headers:{'Content-Type':'application/json'}});
test('booking uses CSRF and excludes supplied owner and status',async()=>{
  const calls=[];const api=HotelVistaBookings.createClient(async(path,opts)=>{calls.push([path,opts]);return path.endsWith('csrf')?json({headerName:'X-CSRF-TOKEN',token:'t'}):json({id:1});});
  await api.create({propertyId:1,roomTypeId:2,checkin:'2026-10-01',checkout:'2026-10-03',guests:2,rooms:1,expectedSubtotal:20,requestKey:'key',userId:99,status:'CONFIRMED'});
  const options=calls[1][1],body=JSON.parse(options.body);assert.equal(options.headers['X-CSRF-TOKEN'],'t');assert.equal(options.credentials,'same-origin');assert.equal(body.userId,undefined);assert.equal(body.status,undefined);assert.equal(body.requestKey,'key');
});
test('failed booking POST is never automatically retried',async()=>{
  let posts=0;const api=HotelVistaBookings.createClient(async path=>{if(path.endsWith('csrf'))return json({headerName:'X-CSRF-TOKEN',token:'t'});posts++;throw Error('network');});
  await assert.rejects(api.create({requestKey:'same'}),/could not be confirmed/);assert.equal(posts,1);
});
test('booking conflict message remains available to the review screen',async()=>{
  const api=HotelVistaBookings.createClient(async path=>path.endsWith('csrf')?json({headerName:'X-CSRF-TOKEN',token:'t'}):json({detail:'Price changed'},409));
  await assert.rejects(api.create({}),error=>error.status===409&&error.message==='Price changed');
});
