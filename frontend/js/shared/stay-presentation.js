/* Shared display helpers. Photos are illustrative, never verified property photography. */
(function(root){
 const base='assets/optimized/';
 const rooms=['images-delhi-delhi-1.webp','images-delhi-delhi-2.webp','images-jaipur-jaipur-1.webp','images-jaipur-jaipur-2.webp','images-mumbai-mumbai-1.webp','images-bengaluru-bengaluru-3.webp','images-chennai-chennai-1.webp','images-hyderabad-hyderabad-1.webp'];
 const category={RESORT:['property-types-resorts.webp',...rooms],VILLA:['property-types-cottages.webp','property-types-homestays.webp',...rooms],HOMESTAY:['property-types-homestays.webp','property-types-cottages.webp',...rooms]};
 root.HotelVistaStay={name:value=>String(value).replace(/\s*\(Demo\)\s*$/,''),photo:(id,type,offset=0)=>{const images=category[type]||rooms;return base+images[(Math.abs(Number(id)||0)+offset)%images.length];},date:value=>new Intl.DateTimeFormat('en-IN',{day:'numeric',month:'short',year:'numeric'}).format(new Date(value+'T12:00:00')),count:(n,word)=>n+' '+word+(n===1?'':'s')};
})(typeof window!=='undefined'?window:globalThis);
