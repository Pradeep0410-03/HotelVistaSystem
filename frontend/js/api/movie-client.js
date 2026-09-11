(function(root){
 'use strict';
 function createClient(fetcher){
  async function request(path,options={}){
   let response,body;
   try{response=await fetcher(path,{...options,credentials:'same-origin',cache:'no-store',redirect:'error',signal:AbortSignal.timeout(20000),headers:{Accept:'application/json',...options.headers}});}
   catch{throw Object.assign(new Error('Result not confirmed. Check your reservations or retry this same request.'),{status:0});}
   try{if(!(response.headers.get('content-type')||'').includes('json'))throw Error();body=await response.json();}
   catch{throw Object.assign(new Error('Movie services returned an unreadable response. Check reservations before retrying.'),{status:0});}
   if(!response.ok)throw Object.assign(new Error(body.detail||body.message||'Movie services are unavailable.'),{status:response.status});
   return body;
  }
  async function post(path,body){
   const csrf=await request('/api/auth/csrf');
   if(csrf.headerName!=='X-CSRF-TOKEN'||!csrf.token)throw Object.assign(new Error('Account services are unavailable.'),{status:503});
   return request(path,{method:'POST',headers:{'Content-Type':'application/json',[csrf.headerName]:csrf.token},body:JSON.stringify(body)});
  }
  return {movies:(city,page)=>request('/api/movies?'+new URLSearchParams({city,page})),shows:(id,city,page)=>request('/api/movies/'+id+'/shows?'+new URLSearchParams({city,page})),seats:id=>request('/api/movie-shows/'+id+'/seats'),list:page=>request('/api/movie-bookings?page='+page),book:d=>post('/api/movie-bookings',{showId:d.showId,seats:d.seats,expectedTotal:d.expectedTotal,requestKey:d.requestKey}),cancel:id=>post('/api/movie-bookings/'+id+'/cancel',{})};
 }
 root.VistaMovies={createClient};
})(typeof window!=='undefined'?window:globalThis);
