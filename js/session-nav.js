(() => {
  'use strict';
  const nav = document.getElementById('account-nav');
  const api = window.HotelVistaAuth.createClient(window.fetch.bind(window));
  let account = null, checking = false;
  function publish() {
    window.dispatchEvent(new CustomEvent('hotelvista-session', {detail: account}));
  }
  function render() {
    nav.replaceChildren();
    if (!account) {
      const login = document.createElement('a'); login.href='login.html'; login.textContent='Sign in';
      const register = document.createElement('a'); register.href='register.html'; register.textContent='Register'; register.className='register-link';
      nav.append(login, register); return;
    }
    const name = document.createElement('span');name.className='account-name';name.textContent=account.fullName;name.title=account.fullName;
    const logout = document.createElement('button');logout.type='button';logout.className='register-link';logout.textContent='Sign out';
    logout.addEventListener('click', async () => {
      logout.disabled=true;
      try { await api.logout(); account=null; render(); publish(); }
      catch { logout.disabled=false; window.dispatchEvent(new CustomEvent('hotelvista-auth-error')); }
    });
    const bookings=document.createElement('a');bookings.href='bookings.html';bookings.textContent='My bookings';
    nav.append(name,bookings,logout);
    if(account.role==='ADMIN') { const admin=document.createElement('a');admin.href='admin.html';admin.textContent='Administration';nav.append(admin); }
  }
  async function refresh() {
    if(checking)return;
    checking=true;
    try { account=await api.me(); }
    catch { account=null; }
    finally { checking=false;render();publish(); }
  }
  window.addEventListener('pageshow', refresh);
  document.addEventListener('visibilitychange', () => {if(!document.hidden)refresh();});
  refresh();
})();
