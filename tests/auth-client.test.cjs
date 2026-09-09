const {test} = require('node:test');
const assert = require('node:assert/strict');
require('../js/auth-client.js');
const {createClient} = globalThis.HotelVistaAuth;
const json = (body,status=200) => new Response(JSON.stringify(body),{status,headers:{'content-type':'application/json'}});
function sequence(responses) {
 const calls=[];
 const client=createClient(async (url,opts)=>{calls.push({url,...opts});const result=responses.shift();if(result instanceof Error)throw result;assert.ok(result,'unexpected request');return result;});
 return {client,calls};
}
const token = value => json({headerName:'X-CSRF-TOKEN',token:value});
test('login gets CSRF then posts encoded credentials and confirms account',async()=>{
 const {client,calls}=sequence([token('first'),new Response(null,{status:204}),json({id:1,fullName:'Person'})]);
 assert.equal((await client.login('a+b@example.com','p&=word')).id,1);
 assert.equal(calls[1].url,'/api/auth/login');
 assert.equal(calls[1].headers['X-CSRF-TOKEN'],'first');
 assert.equal(new URLSearchParams(calls[1].body).get('password'),'p&=word');
 for(const call of calls){assert.equal(call.credentials,'same-origin');assert.equal(call.cache,'no-store');assert.equal(call.redirect,'error');assert.ok(!call.url.includes('password'));}
});
test('registration allows only account fields and does not auto-login',async()=>{
 const {client,calls}=sequence([token('one'),json({id:1},201)]);
 await client.register({fullName:'Name',email:'a@example.com',password:'password123',role:'ADMIN'});
 assert.deepEqual(Object.keys(JSON.parse(calls[1].body)).sort(),['email','fullName','password']);
 assert.equal(calls.length,2);
});
test('logout retrieves fresh CSRF instead of reusing login token',async()=>{
 const {client,calls}=sequence([token('one'),new Response(null,{status:204}),json({id:1}),token('two'),new Response(null,{status:204})]);
 await client.login('a@example.com','password123');await client.logout();
 assert.equal(calls[4].headers['X-CSRF-TOKEN'],'two');
});
test('HTML preview fallback cannot trigger a credential POST',async()=>{
 const {client,calls}=sequence([new Response('<html>preview</html>',{headers:{'content-type':'text/html'}})]);
 await assert.rejects(client.login('a@example.com','password123'),{status:503});
 assert.equal(calls.length,1);assert.equal(calls[0].method,undefined);
});
test('anonymous me is null; other failures are not mistaken for anonymous success',async()=>{
 assert.equal(await sequence([json({},401)]).client.me(),null);
 await assert.rejects(sequence([json({},500)]).client.me(),{status:500});
});
test('failed registration is not automatically retried',async()=>{
 const {client,calls}=sequence([token('one'),new Error('lost connection')]);
 await assert.rejects(client.register({fullName:'Name',email:'a@example.com',password:'password123'}),{status:0});
 assert.equal(calls.length,2);
});
