(function(root) {
  'use strict';
  function createClient(fetcher) {
    async function request(path,options={}) {
      let response;
      try { response=await fetcher(path,{...options,credentials:'same-origin',cache:'no-store',redirect:'error',signal:AbortSignal.timeout(20000),headers:{Accept:'application/json',...options.headers}}); }
      catch { const error=new Error('The result could not be confirmed. Check My bookings before retrying; your request key is preserved.');error.status=0;throw error; }
      if(!(response.headers.get('content-type')||'').includes('json'))throw new Error('Booking services are unavailable here.');
      const body=await response.json();
      if(!response.ok) { const error=new Error(response.status===401?'Sign in to manage bookings.':body.detail||'The request could not be completed.');error.status=response.status;throw error; }
      return body;
    }
    async function post(path,body) {
      const csrf=await request('/api/auth/csrf');
      if(csrf.headerName!=='X-CSRF-TOKEN'||!csrf.token)throw new Error('Account services are unavailable.');
      return request(path,{method:'POST',headers:{'Content-Type':'application/json',[csrf.headerName]:csrf.token},body:JSON.stringify(body)});
    }
    return {list:page=>request('/api/bookings?page='+page),create:d=>post('/api/bookings',{propertyId:d.propertyId,roomTypeId:d.roomTypeId,checkin:d.checkin,checkout:d.checkout,guests:d.guests,rooms:d.rooms,expectedSubtotal:d.expectedSubtotal,requestKey:d.requestKey}),cancel:id=>post('/api/bookings/'+id+'/cancel',{})};
  }
  root.HotelVistaBookings={createClient};
})(typeof window!=='undefined'?window:globalThis);
