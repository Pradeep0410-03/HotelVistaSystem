const {test}=require('node:test');
const assert=require('node:assert/strict');
const vm=require('node:vm');
const fs=require('node:fs');
const source=fs.readFileSync('frontend/js/pages/account.js','utf8');
function screen(search,register=false,signedIn=false) {
 const nodes={};let destination;
 const node=id=>nodes[id] ||= {value:'',href:id==='back-to-stays'?'/':'',textContent:'',hidden:false,listeners:{},addEventListener(event,fn){this.listeners[event]=fn},setCustomValidity(){},setAttribute(){},reportValidity(){return true},focus(){}};
 const api={csrf:async()=>({}),me:async()=>signedIn?{fullName:'Guest'}:null,login:async()=>({}),register:async()=>({})};
 const window={HOTEL_VISTA:{properties:[]},HotelVistaIntent:{parse:()=>null},HotelVistaAuth:{createClient:()=>api},fetch:()=>{},location:{assign:url=>destination=url}};
 vm.runInNewContext(source,{window,document:{getElementById:node,body:{dataset:{account:register?'register':'login'}}},location:{search},URLSearchParams,Intl,Date});
 return {node,get destination(){return destination}};
}
test('ordinary sign-in uses neutral copy and returns home',async()=>{
 const s=screen('');await new Promise(setImmediate);
 assert.equal(s.node('preview-notice').textContent,'Sign in to access your account and bookings.');
 await s.node('account-form').listeners.submit({preventDefault(){}});
 assert.equal(s.destination,'/');
});
test('booking sign-in preserves review and registration links',async()=>{
 const s=screen('?return=bookings');await new Promise(setImmediate);
 assert.equal(s.node('switch-account').href,'register.html?return=bookings');
 await s.node('account-form').listeners.submit({preventDefault(){}});
 assert.equal(s.destination,'bookings.html');
});
test('already signed-in visitors continue home without hotel wording',async()=>{
 const s=screen('',false,true);await new Promise(setImmediate);
 assert.equal(s.node('continue-account').href,'/');
 assert.equal(s.node('continue-account').textContent,'Continue to home');
 assert.equal(s.node('account-form').hidden,true);
});
test('ordinary registration success does not claim a stay selection',async()=>{
 const s=screen('',true);await new Promise(setImmediate);
 await s.node('account-form').listeners.submit({preventDefault(){}});
 assert.equal(s.node('account-status').textContent,'Your account has been created. Sign in to continue.');
});
test('movie sign-in returns to movies and keeps register context',async()=>{
 const s=screen('?return=movies');await new Promise(setImmediate);
 assert.equal(s.node('switch-account').href,'register.html?return=movies');
 await s.node('account-form').listeners.submit({preventDefault(){}});
 assert.equal(s.destination,'/frontend/pages/movies.html#movie-reservations');
});
