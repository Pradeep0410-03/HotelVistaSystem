(() => {
  'use strict';
  const $ = id => document.getElementById(id);
  const params = new URLSearchParams(location.search);
  const now = new Date();
  const today = [now.getFullYear(), String(now.getMonth()+1).padStart(2,'0'), String(now.getDate()).padStart(2,'0')].join('-');
  const trip = window.HotelVistaIntent.parse(params, window.HOTEL_VISTA.properties, today);
  const api = window.HotelVistaAuth.createClient(window.fetch.bind(window));
  let ready = false, busy = false, completed = false;
  const register = document.body.dataset.account === 'register';
  const bookingReturn = params.get('return') === 'bookings';
  const hasSelection = !!trip || bookingReturn;
  if (trip) {
    const property = window.HOTEL_VISTA.properties.find(p => p.id === trip.property);
    const estimate = window.HotelVistaCatalogue.estimate(property.price, trip.checkin, trip.checkout, trip.rooms);
    $('trip-summary').hidden = false;
    $('trip-property').textContent = property.name;
    $('trip-dates').textContent = trip.checkin + ' → ' + trip.checkout + ' · ' + estimate.nights + (estimate.nights === 1 ? ' night' : ' nights');
    $('trip-guests').textContent = trip.guests + (trip.guests === 1 ? ' guest' : ' guests') + ' · ' + trip.rooms + (trip.rooms === 1 ? ' room' : ' rooms');
    $('trip-price').textContent = new Intl.NumberFormat('en-IN', {style:'currency',currency:'INR',maximumFractionDigits:0}).format(estimate.subtotal) + ' estimated subtotal';
    $('switch-account').href = window.HotelVistaIntent.link(register ? 'login.html' : 'register.html', trip);
    const back = window.HotelVistaIntent.link('/frontend/pages/hotels.html', trip) + '&destination=' + encodeURIComponent(property.city) + '#stays';
    $('back-to-stays').href = back;
    $('back-to-stays').textContent = '← Back to stays';
    $('edit-trip').href = back;
  } else if (params.has('property')) {
    $('account-status').hidden = false;
    $('account-status').textContent = 'Your stay selection is invalid or its dates have passed. Go back to stays to choose again.';
  }
  $('toggle-password').addEventListener('click', () => {
    const show = $('password').type === 'password';
    $('password').type = show ? 'text' : 'password';
    $('toggle-password').textContent = show ? 'Hide' : 'Show';
    $('toggle-password').setAttribute('aria-label', show ? 'Hide password' : 'Show password');
    $('toggle-password').setAttribute('aria-pressed', String(show));
  });
  function validateConfirmation() {
    if (!register) return;
    const password=$('password').value;
    const strong=password.length>=12 && password.length<=128 && /[A-Z]/.test(password) && /[a-z]/.test(password) && /[0-9]/.test(password) && /[^A-Za-z0-9\s]/.test(password);
    $('password').setCustomValidity(strong?'':'Use 12–128 characters with uppercase, lowercase, a number and a symbol.');
    const mismatch = $('confirm-password').value !== $('password').value;
    $('confirm-password').setCustomValidity(mismatch ? 'Passwords must match.' : '');
    $('confirm-password').setAttribute('aria-invalid', String(mismatch && !!$('confirm-password').value));
    $('password-error').hidden = !mismatch || !$('confirm-password').value;
  }
  if (register) {
    ['password', 'confirm-password'].forEach(id => $(id).addEventListener('input', validateConfirmation));
    $('full-name').addEventListener('input', () => $('full-name').setCustomValidity($('full-name').value.trim() ? '' : 'Enter your name.'));
  }
  if (params.get('return') === 'bookings') {
    $('back-to-stays').href = 'bookings.html';
    $('back-to-stays').textContent = '← Back to booking review';
    $('switch-account').href = (register ? 'login.html' : 'register.html') + '?return=bookings';
  }
  $('continue-account').href = $('back-to-stays').href;
  function message(text) {
    $('account-status').hidden = false; $('account-status').textContent = text; $('account-status').focus();
  }
  function clearPassword() {
    $('password').value = ''; $('password').type = 'password';
    $('toggle-password').textContent = 'Show';
    $('toggle-password').setAttribute('aria-label', 'Show password');
    $('toggle-password').setAttribute('aria-pressed', 'false');
    if (register) { $('confirm-password').value = ''; validateConfirmation(); }
  }
  $('account-form').addEventListener('submit', async event => {
    event.preventDefault();
    validateConfirmation();
    if (!$('account-form').reportValidity()) return;
    if (!ready || busy || completed) return;
    busy = true;
    $('account-submit').disabled = true;
    $('account-form').setAttribute('aria-busy', 'true');
    const label = $('account-submit').textContent;
    $('account-submit').textContent = register ? 'Creating account…' : 'Signing in…';
    try {
      if (register) {
        await api.register({fullName:$('full-name').value.trim(), email:$('email').value.trim(), password:$('password').value});
        clearPassword(); completed = true;
        message(hasSelection ? 'Your account has been created. Sign in to continue; your booking selection is preserved.' : 'Your account has been created. Sign in to continue.');
        $('switch-account').focus();
      } else {
        await api.login($('email').value.trim(), $('password').value);
        clearPassword(); completed = true;
        window.location.assign($('back-to-stays').href);
      }
    } catch (error) {
      message(error.status === 0 && register
        ? 'We could not confirm whether your account was created. Try signing in before registering again.'
        : error.message);
    } finally {
      busy = false; $('account-form').setAttribute('aria-busy', 'false');
      $('account-submit').disabled = completed || !ready;
      $('account-submit').textContent = label;
    }
  });
  async function connect() {
    ready = false; $('account-submit').disabled = true;
    $('retry-connection').hidden = true;
    $('preview-notice').textContent = 'Checking account services…';
    try {
      await api.csrf();
      const account = await api.me();
      if (account) {
        completed = true; $('account-form').hidden = true;
        $('continue-account').hidden = false;
        $('preview-notice').textContent = 'You are signed in as ' + account.fullName + '. Continue below, or sign out from the homepage.';
        $('continue-account').textContent = bookingReturn ? 'Continue to booking review' : trip ? 'Continue to your stay' : 'Continue to home';
      } else {
        ready = true; $('account-submit').disabled = false;
        $('preview-notice').textContent = register
          ? 'Create your Vista Booking account. No booking or payment is made at this step.'
          : hasSelection ? 'Sign in to continue. Your selected stay is reserved only after you confirm the booking.' : 'Sign in to access your account and bookings.';
      }
    } catch {
      $('preview-notice').textContent = 'Account services are unavailable here. You can still browse Vista Booking. No details have been submitted.';
      $('retry-connection').hidden = false;
    }
  }
  $('retry-connection').addEventListener('click', connect);
  connect();
})();
