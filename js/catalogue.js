/* Pure catalogue logic, shared by the UI and dependency-free checks. */
(function(root) {
  "use strict";
  const normalize = value => String(value).trim().toLowerCase().replace(/bangalore/g,"bengaluru").replace(/new delhi/g,"delhi");
  const dayNumber = value => Date.parse(value+"T00:00:00Z")/86400000;
  const validDate = value => /^\d{4}-\d{2}-\d{2}$/.test(value) && Number.isFinite(dayNumber(value)) && new Date(dayNumber(value)*86400000).toISOString().slice(0,10)===value;
  function filter(properties,state,saved) {
    const q=normalize(state.query);
    const list=properties.filter(p=>
      (!q || normalize([p.name,p.city,p.area,p.type].join(" ")).includes(q)) &&
      p.price<=state.budget && (!state.types.length || state.types.includes(p.type)) &&
      state.amenities.every(a=>p.amenities.includes(a)) &&
      (!state.savedOnly || saved.has(p.id)) &&
      p.capacity*state.rooms>=state.guests && p.rooms>=state.rooms
    );
    if(state.sort==="price-low")list.sort((a,b)=>a.price-b.price);
    if(state.sort==="price-high")list.sort((a,b)=>b.price-a.price);
    if(state.sort==="name")list.sort((a,b)=>a.name.localeCompare(b.name));
    return list;
  }
  function estimate(price,checkin,checkout,rooms) {
    if(!validDate(checkin)||!validDate(checkout)||dayNumber(checkout)<=dayNumber(checkin)||!Number.isInteger(rooms)||rooms<1)throw new RangeError("Invalid trip");
    const nights=dayNumber(checkout)-dayNumber(checkin);
    return {nights,subtotal:price*nights*rooms};
  }
  root.HotelVistaCatalogue={normalize,dayNumber,validDate,filter,estimate};
})(typeof window!=="undefined"?window:globalThis);
